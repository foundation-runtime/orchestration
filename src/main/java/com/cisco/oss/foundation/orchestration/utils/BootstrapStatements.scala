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

import java.nio.file.{Files, Paths}

import com.cisco.oss.foundation.orchestration.model.ScopeStatement
import com.google.common.collect.ImmutableList
import org.apache.commons.net.util.SubnetUtils
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.scriptbuilder.domain.{OsFamily, Statement, StatementList}

import scala.io.Source

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/6/14
 * Time: 9:01 AM
 * To change this template use File | Settings | File Templates.
 */
class BootstrapStatements(val networkAddress: List[(SubnetUtils, String)], val baseRepoUrl: String, val osVersion: String, val hasPublicIp: Boolean, val nodeName : String, val openPorts: List[String] = List(), provider: String) extends ScopeStatement {

  private val statements: ImmutableList.Builder[Statement] = ImmutableList.builder[Statement]
  private val baseDir:String = "/opt/cisco/scope/scripts"

  def addStatement(element: Statement) {
    statements.add(element)
  }

  def functionDependencies(family: OsFamily): java.lang.Iterable[String] = {
    ImmutableList.of[String]
  }

  def render(family: OsFamily): String = {
    if (family eq OsFamily.WINDOWS) throw new UnsupportedOperationException("windows not yet implemented")
    //val statements: ImmutableList.Builder[Statement] = ImmutableList.builder[Statement]

    statements.add(exec("sudo -s"))
    statements.add(exec("cd /etc/"))
    statements.add(exec("sed -i '/GATEWAY/d' /etc/sysconfig/network"))

    // skip swapfile definition if not supported.
	val cloudProvider = ScopeUtils.configuration.getString("cloud.provider")
    val setSwap : Boolean = Option(ScopeUtils.configuration.getBoolean(s"cloud.provider.$cloudProvider.swap.supported", true)) match {
      case Some(ss) => ss
      case None => true
    }
    if (setSwap) {
	    statements.add(exec("swap_size=`cat /proc/meminfo | sed -n -e 's/SwapTotal:\\s*\\([0-9]*\\) .*$/\\1/p'`"))
	    statements.add(exec("if [ $swap_size -eq 0 ]; then"))
	    statements.add(exec("# RedHat recommends the following formula: if MEM < 2GB then SWAP = MEM*2 else SWAP = MEM+2GB"))
	    statements.add(exec("  ram_size=`head -1 /proc/meminfo | awk '{print $2;}'`;"))
	    statements.add(exec("  if [ $ram_size -le 2097152 ]; then"))
	    statements.add(exec("       swap_size=$((ram_size*2));"))
	    statements.add(exec("  else"))
	    statements.add(exec("       swap_size=$((ram_size+2097152));"))
	    statements.add(exec("  fi"))
	    statements.add(exec("  echo 'Set Swap size to $((swap_size/1024)) MB'"))
	    statements.add(exec("  dd if=/dev/zero of=/swapfile1 bs=1024 count=$swap_size"))
	    statements.add(exec("  mkswap /swapfile1"))
	    statements.add(exec("  chown root:root /swapfile1"))
	    statements.add(exec("  chmod 0600 /swapfile1"))
	    statements.add(exec("  swapon /swapfile1"))
	    statements.add(exec("  echo '/swapfile1 swap swap defaults 0 0' >> /etc/fstab"))
	    statements.add(exec("fi"))
    }

    val mountDisk = cloudProvider.equals("aws")
    if (mountDisk) {
	    statements.add(exec("MOUNT_POINT=/data"))
	    statements.add(exec("[ `whoami` != root ] && { echo \"The script should be run as 'root' user.\"; exit 1; }"))
	    statements.add(exec("mounted=`mount | sed -n -e \"s/^\\(\\/dev[^ ]*\\) .*$/\\1/p\"`"))
	    statements.add(exec("for disk in `/sbin/fdisk -l | sed -n -e \"s/^Disk\\s*\\(\\/dev.*\\):.*$/\\1/p\"`; do"))
	    statements.add(exec("	if ! `echo \"$mounted\" | egrep -q ^$disk`; then"))
	    statements.add(exec("		/sbin/mkfs.ext3 $disk"))
	    statements.add(exec("		mkdir $MOUNT_POINT"))
	    statements.add(exec("		mount $disk $MOUNT_POINT"))
	    statements.add(exec("		echo \"$disk $MOUNT_POINT ext3 defaults 0 0\" >> /etc/fstab"))
	    statements.add(exec("		echo \"The $disk device mounted to the $MOUNT_POINT\""))
	    statements.add(exec("		break"))
	    statements.add(exec("	fi"))
	    statements.add(exec("done"))
    }

    statements.add(exec("rm -f /etc/localtime"))
    statements.add(exec("ln -s /usr/share/zoneinfo/UTC /etc/localtime"))

    statements.add(exec("sed -e s#debuglevel.*#debuglevel=10# -i.bak /etc/yum.conf"))
    statements.add(exec("echo 'errorlevel=10' >> /etc/yum.conf"))
    statements.add(exec("echo 'timeout=300' >> /etc/yum.conf"))
    statements.add(exec("mkdir -p /etc/yum.repos.d/save"))
    statements.add(exec("mv /etc/yum.repos.d/*.repo /etc/yum.repos.d/save/"))
    statements.add(exec("echo '[scope]' >> /etc/yum.repos.d/scope.repo"))
    statements.add(exec("echo 'name=scope_repo' >> /etc/yum.repos.d/scope.repo"))
    statements.add(exec(s"echo 'baseurl=${baseRepoUrl}yum/$osVersion/" + "$basearch' >> /etc/yum.repos.d/scope.repo"))
    statements.add(exec("echo 'enabled=1' >> /etc/yum.repos.d/scope.repo"))
    statements.add(exec("echo 'gpgcheck=0' >> /etc/yum.repos.d/scope.repo"))
    statements.add(exec("yum -y install ruby"))
    statements.add(exec("yum -y install libselinux-ruby"))
    if (osVersion == 5)
      statements.add(exec("yum -y install puppet-2.7.21-1"))
    else
      statements.add(exec("yum -y install puppet-2.7.21-1.el6"))
    statements.add(exec("yum -y install hiera"))
    statements.add(exec("yum -y install hiera-puppet"))
    statements.add(exec("yum -y install augeas"))
    statements.add(exec("yum -y install vim"))

    statements.add(exec("yum -y install wget"))
    statements.add(exec("yum -y install expect"))
    statements.add(exec("yum -y install sysstat"))
    statements.add(exec("yum -y install strace"))
    statements.add(exec("yum -y install dos2unix"))
    statements.add(exec("yum -y install ntp"))
    // Configure ntp service to synchronize clock on restart
    statements.add(exec("sed -e \"s#\\\"-u ntp:ntp#\\\"-x -u ntp:ntp#\" -i.bak /etc/sysconfig/ntpd"))

    if (Files.exists(Paths.get(s"$baseDir/bootstrap.sh")))
      Source.fromFile(s"$baseDir/bootstrap.sh").getLines().foreach(line => statements.add(exec(line)))

    statements.add(exec("yum -y install sipcalc"))
    statements.add(exec("sleep 10"))
    statements.add(exec("save=$IFS"))
    statements.add(exec("IFS='"))
    statements.add(exec("'"))
    statements.add(exec("for line in `ip -4 addr | grep inet | grep eth`"))
    statements.add(exec("do"))
    statements.add(exec("ip=`echo $line | awk '{ print $2 }'`"))
    statements.add(exec("network_ip=`ipcalc -n $ip`"))

    statements.add(exec("iptables -F"))
    statements.add(exec("iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP"))
    statements.add(exec("iptables -A INPUT -p tcp ! --syn -m state --state NEW -j DROP"))
    statements.add(exec("iptables -A INPUT -p tcp --tcp-flags ALL ALL -j DROP"))
    statements.add(exec("iptables -A INPUT -i lo -j ACCEPT"))

    networkAddress.foreach{
      case (util, gateway) => {
        statements.add(exec("if echo $network_ip | grep -q '" + util.getInfo.getNetworkAddress + "' ; then"))
        statements.add(exec("dev=`echo $line | awk '{ print $7 }'`"))
        if (hasPublicIp) {
          statements.add(exec("echo ADDRESS0=10.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo NETMASK0=255.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo GATEWAY0=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo ADDRESS1=172.16.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo NETMASK1=255.240.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo GATEWAY1=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo ADDRESS2=192.168.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo NETMASK2=255.255.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo GATEWAY2=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
        } else {
          statements.add(exec("echo ADDRESS0=0.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo NETMASK0=0.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev"))
          statements.add(exec("echo GATEWAY0=" + gateway + " >> /etc/sysconfig/network-scripts/route-$dev"))
        }
        statements.add(exec("iptables -A INPUT -i $dev -j ACCEPT"))
        statements.add(exec("fi"))
      }
    }

    statements.add(exec("done"))
    statements.add(exec(s"augtool set /files/etc/sysconfig/network/HOSTNAME $nodeName"))

    provider match {
      case "vsphere" => {
      }
      case _ => {
        statements.add(exec(s"hostname $nodeName"))
        statements.add(exec("find /etc/sysconfig/network-scripts/ -iname ifcfg-eth* -exec sh -c \"echo DHCP_HOSTNAME=" + nodeName + " >> {}\" \\;"))
      }
    }


    statements.add(exec("service network restart"))
    statements.add(exec("IFS=$save"))

    statements.add(exec("iptables -I INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT"))
    if (hasPublicIp) {
      statements.add(exec("public_ip=`curl ifconfig.me`"))
      statements.add(exec("echo 'Machine public IP '$public_ip"))
      statements.add(exec("dev=`ip -4 addr | grep inet | grep eth | grep $public_ip | awk '{ print $7 }'`"))
      statements.add(exec("echo 'Machine public DEV '$dev"))
      openPorts.foreach {
        case port =>
          statements.add(exec("iptables -A INPUT -i $dev -p tcp --dport " + port + " -j ACCEPT"))
      }
      statements.add(exec("iptables -A INPUT -i $dev -j DROP"))

    }
    statements.add(exec("iptables -P INPUT DROP"))
    statements.add(exec("iptables -P OUTPUT ACCEPT"))
    statements.add(exec("iptables -L -n"))
    statements.add(exec("iptables-save | tee /etc/sysconfig/iptables"))
    statements.add(exec("service iptables restart"))

    statements.add(exec("cd -"))

    new StatementList(statements.build).render(family)
  }
}
