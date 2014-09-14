/*
 * Copyright 2014 Cisco Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cisco.oss.foundation.orchestration.utils

import com.cisco.oss.foundation.orchestration.model.ScopeNodeMetadata
import com.google.common.collect.{ImmutableSet, Lists}
import com.google.common.util.concurrent.MoreExecutors._
import org.jclouds.compute.options.TemplateOptions.Builder._
import org.jclouds.concurrent.config.ExecutorServiceModule
import org.jclouds.scriptbuilder.ScriptBuilder
import org.jclouds.scriptbuilder.domain.OsFamily
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.sshj.config.SshjSshClientModule
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 */
object NetworkUtils {
  private val logger: Logger = LoggerFactory.getLogger(NetworkUtils.getClass)
  private val addressPattern = "inet ([0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+/[0-9]+) .* (eth[0-9]+)".r
  private val modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
  private val legalCommands = Lists.newArrayList("ip -4 addr | grep inet | grep eth",
    "cat >> [^;[:space:]]+ << EOF.*EOF",
    "service network restart")

  private val computeServiceContext =  JcloudsFactory.computeServiceContext()

  private val suffix = "vcs-foundation.com"



  def createDeleteRowsFromHostsFileScript(vmNames: List[String]) = {
    val builder = new ScriptBuilder()

    vmNames.foreach {
      case name => {
        builder.addStatement(exec(s"sed -i '/$name/d' /etc/hosts"))
        builder.addStatement(exec("sed -i '/^\\s*$/d' /etc/hosts"))
      }
    }

    builder
  }

  def createAddRowsToHostsFileScript(vmDetails: Map[String, String]) = {
    val builder = new ScriptBuilder()
    builder.addStatement(exec("echo '"))
    vmDetails.foreach {
      case (address, name) => {
        builder.addStatement(exec(s"$address $name"))
      }
    }

    builder.addStatement(exec("' >> /etc/hosts"))

    builder
  }

  def createAddRowsToHostsFileScript(vmDetails: List[ScopeNodeMetadata]) = {
    val builder = new ScriptBuilder()

    builder.addStatement(exec("echo '"))

    vmDetails.foreach {
      nodeMetaData => {
        val address = nodeMetaData.privateAddresses.head
        val name = nodeMetaData.hostname
        builder.addStatement(exec(s"$address $name"))
      }
    }

    builder.addStatement(exec("' >> /etc/hosts"))

    builder
  }

  def createHostsFileScript(configurationServerAddresses: List[ScopeNodeMetadata], vmDetails: List[ScopeNodeMetadata]) = {
    val builder = new ScriptBuilder()

    builder.addStatement(exec("echo '127.0.0.1 localhost.localdomain localhost"))
    builder.addStatement(exec("::1 localhost6.localdomain6 localhost6"))

    for (i <- 0 until configurationServerAddresses.size) {
      val address = configurationServerAddresses(i).privateAddresses.head
      val serverCount = i + 1
      builder.addStatement(exec(s"$address ccpserver$serverCount"))
    }

    if (configurationServerAddresses.size == 1) {
      val address = configurationServerAddresses(0).privateAddresses.head
      builder.addStatement(exec(s"$address ccpserver2"))
    }

    try {
      vmDetails.foreach {
        nodeMetaData => {
          val address = nodeMetaData.privateAddresses.head
          val name = nodeMetaData.hostname
          builder.addStatement(exec(s"$address $name"))
        }
      }
    } catch {
      case _ =>
    }

    builder.addStatement(exec("' > /etc/hosts"))

    builder
  }

  def getNetworkInitScript(networkAddress: String, gateway: String, scopeIp: String, osVersion: String, hasPublicIp: Boolean, openPorts: List[String] = List()) = {
    val builder = new ScriptBuilder()
    builder.addStatement(exec("sed -i '/GATEWAY/d' /etc/sysconfig/network"))
    // Configure ntp service to synchronize clock on restart
    builder.addStatement(exec("sed -e \"s#OPTIONS=\\\"-u ntp:ntp -p /var/run/ntpd.pid\\\"#OPTIONS=\\\"-x -u ntp:ntp -p /var/run/ntpd.pid\\\"#\" -i.bak /etc/sysconfig/ntpd"))

    builder.addStatement(exec("echo '[scope]' >> /etc/yum.repos.d/scope.repo"))
    builder.addStatement(exec("echo 'name=scope_repo' >> /etc/yum.repos.d/scope.repo"))
    builder.addStatement(exec(s"echo 'baseurl=http://$scopeIp/scope-base/$osVersion/" + "$basearch' >> /etc/yum.repos.d/scope.repo"))
    builder.addStatement(exec("echo 'enabled=1' >> /etc/yum.repos.d/scope.repo"))
    builder.addStatement(exec("echo 'gpgcheck=0' >> /etc/yum.repos.d/scope.repo"))
    builder.addStatement(exec("yum -y install epel-release"))
    builder.addStatement(exec("yum -y install ruby"))
    builder.addStatement(exec("yum -y install libselinux-ruby"))
    builder.addStatement(exec("yum -y install puppet-2.7.21-1"))
    builder.addStatement(exec("yum -y install hiera"))
    builder.addStatement(exec("yum -y install hiera-puppet"))
    builder.addStatement(exec("yum -y install augeas"))
    builder.addStatement(exec("yum -y install git"))

    builder.addStatement(exec("yum -y install sipcalc"))
    builder.addStatement(exec("save=$IFS"))
    builder.addStatement(exec("IFS='"))
    builder.addStatement(exec("'"))
    builder.addStatement(exec("for line in `ip -4 addr | grep inet | grep eth`"))
    builder.addStatement(exec("do"))
    builder.addStatement(exec("ip=`echo $line | awk '{ print $2 }'`"))
    builder.addStatement(exec("network_ip=`ipcalc -n $ip`"))
    builder.addStatement(exec("if echo $network_ip | grep -q '" + networkAddress + "' ; then"))
    builder.addStatement(exec("dev=`echo $line | awk '{ print $7 }'`"))
    if (hasPublicIp) {
      builder.addStatement(exec("echo ADDRESS0=10.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo NETMASK0=255.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo GATEWAY0=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo ADDRESS1=172.16.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo NETMASK1=255.240.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo GATEWAY1=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo ADDRESS2=192.168.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo NETMASK2=255.255.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo GATEWAY2=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
    } else {
      builder.addStatement(exec("echo ADDRESS0=0.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo NETMASK0=0.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
      builder.addStatement(exec("echo GATEWAY0=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
    }
    builder.addStatement(exec("fi"))
    builder.addStatement(exec("done"))
    builder.addStatement(exec("IFS=$save"))
    builder.addStatement(exec("iptables -F"))
    builder.addStatement(exec("iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP"))
    builder.addStatement(exec("iptables -A INPUT -p tcp ! --syn -m state --state NEW -j DROP"))
    builder.addStatement(exec("iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP"))
    builder.addStatement(exec("iptables -A INPUT -i lo -j ACCEPT"))
    builder.addStatement(exec("iptables -A INPUT -i $dev -j ACCEPT"))
    builder.addStatement(exec("iptables -I INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT"))
    if (hasPublicIp) {
      builder.addStatement(exec("public_ip=`curl ifconfig.me`"))
      builder.addStatement(exec("dev=`ip -4 addr | grep inet | grep eth | grep $public_ip | awk '{ print $7 }'`"))
      openPorts.foreach {
        case port =>
          builder.addStatement(exec("iptables -A INPUT -i $dev -p tcp --dport " + port + " -j ACCEPT"))
      }
      builder.addStatement(exec("iptables -A INPUT -i $dev -j DROP"))

    }
    builder.addStatement(exec("iptables -P INPUT DROP"))
    builder.addStatement(exec("iptables -P OUTPUT ACCEPT"))
    builder.addStatement(exec("iptables -L -n"))
    builder.addStatement(exec("iptables-save | tee /etc/sysconfig/iptables"))
    builder.addStatement(exec("service iptables restart"))
    builder.addStatement(exec("service network restart"))

    builder
  }


  def createDnsName(role: String, instanceName: String, systemId: String): String = {
    role + "." + instanceName + "." + systemId + "." + suffix
  }

  def configureIptables(zoneId: String, targetMachineId: String, openPorts: List[String]) = {
    val fullId = s"$zoneId/$targetMachineId"

    val nodeMetadata = computeServiceContext.getComputeService.getNodeMetadata(fullId)
    val builder = new ScriptBuilder()
    builder.addStatement(exec(s"iptables -F"))
    builder.addStatement(exec(s"iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP"))
    builder.addStatement(exec(s"iptables -A INPUT -p tcp ! --syn -m state --state NEW -j DROP"))
    builder.addStatement(exec(s"iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP"))
    builder.addStatement(exec(s"iptables -A INPUT -i lo -j ACCEPT"))
    builder.addStatement(exec(s"iptables -I INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT"))
    nodeMetadata.getPublicAddresses.foreach {
      case ip => {
        val result = computeServiceContext.getComputeService.runScriptOnNode(fullId, s"ip -4 addr | grep inet | grep eth | grep $ip", overrideLoginCredentials(ScopeUtils.getLoginForCommandExecution()))
        result.getExitStatus match {
          case 0 => {
            result.getOutput.trim match {
              case addressPattern(cdir, device) => {

                openPorts.foreach {
                  case port => {
                    builder.addStatement(exec(s"iptables -A INPUT -i $device -p tcp --dport $port -j ACCEPT"))
                  }
                }

              }
            }
          }
          case _ =>
        }
      }
    }
    nodeMetadata.getPrivateAddresses.foreach {
      case ip => {
        val result = computeServiceContext.getComputeService.runScriptOnNode(fullId, s"ip -4 addr | grep inet | grep eth | grep $ip", overrideLoginCredentials(ScopeUtils.getLoginForCommandExecution()))
        result.getExitStatus match {
          case 0 => {
            result.getOutput.trim match {
              case addressPattern(cdir, device) => {

                openPorts.foreach {
                  case port => {
                    builder.addStatement(exec(s"iptables -A INPUT -i $device -j ACCEPT"))
                  }
                }

              }
            }
          }
          case _ =>
        }
      }
    }
    builder.addStatement(exec(s"iptables -P OUTPUT ACCEPT"))
    builder.addStatement(exec(s"iptables -P INPUT DROP"))
    builder.addStatement(exec(s"iptables -L -n"))
    builder.addStatement(exec(s"iptables-save | sudo tee /etc/sysconfig/iptables"))
    builder.addStatement(exec(s"service iptables restart"))

    computeServiceContext.getComputeService.runScriptOnNode(fullId, builder.render(OsFamily.UNIX), overrideLoginCredentials(ScopeUtils.getLoginForCommandExecution()))
  }


}
