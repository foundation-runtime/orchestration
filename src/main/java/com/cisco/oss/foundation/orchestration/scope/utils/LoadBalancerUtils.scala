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

package com.cisco.oss.foundation.orchestration.scope.utils

import com.cisco.oss.foundation.ip.utils.IpUtils
import com.cisco.oss.foundation.orchestration.scope.model.{Instance, Network, Node}
import com.google.common.collect.ImmutableList
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.scriptbuilder.domain.{OsFamily, Statement, StatementList}

import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext
import scala.io.Source

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 3/10/14
 * Time: 12:08 PM
 */
class LoadBalancerUtils(port: Int, applicationName: String, urlPrefix: String, instanceName: String, systemName: String, rsaKeyPair: Map[String, String]) {
  val statements: ImmutableList.Builder[Statement] = ImmutableList.builder[Statement]
  private var backendServers: List[String] = Nil

  def addBackendServer(server: String) {
    backendServers = server :: backendServers
  }

  /**
   *
   * @example {{{
   *                               upstream <applicationName>_units {
   *                                 server 10.45.37.146:6040 weight=10 max_fails=3 fail_timeout=30s; # Reverse proxy to  BES1
   *                                 server 10.45.37.52:6040 weight=10 max_fails=3 fail_timeout=30s; # Reverse proxy to  BES1
   *                               }
   *                               server {
   *                                listen  <ip>:<port>; # Listen on the external interface
   *                                server_name  <app>.<instance>.<system>.vcs-foundation.com; # The server name
   *                                access_log  /var/log/nginx/nginx.access.log;
   *                               error_log  /var/log/nginx/nginx_error.log debug;
   *                               location /<urlPrefix> {
   *                                 proxy_pass         http://<applicationName>_units; # Load balance the URL location "/" to the upstream upm_units
   *                                }
   *                                error_page   500 502 503 504  /50x.html;
   *                                location = /50x.html {
   *                                 root   /var/www/nginx-default;
   *                                }
   *                               }
   *          }}}
   * @param host
   * @param port
   * @return
   */
  def nginxConfigurationScript(host: String, port: String) = {
    statements.add(exec("yum -y install nginx"))
    statements.add(exec("service nginx start"))
    statements.add(exec(s"echo 'upstream ${applicationName}_units {"))
    backendServers.foreach {
      case server => {
        statements.add(exec(s"  server $server weight=10 max_fails=3 fail_timeout=30s;"))
      }
    }
    statements.add(exec("}"))

    statements.add(exec("server {"))
    statements.add(exec(s"  listen  0.0.0.0:$port; # Listen on the external interface"))
    statements.add(exec(s"  server_name  $applicationName.$instanceName.$systemName.vcs-foundation.com; # The server name"))
    statements.add(exec(s"  access_log  /var/log/nginx/nginx.access.log;"))
    statements.add(exec(s"  error_log /var/log/nginx/nginx_error.log debug;"))
    statements.add(exec(s"  location /$urlPrefix {"))
    statements.add(exec(s"    proxy_pass   http://${applicationName}_units; # Load balance the URL location '/' to the upstream upm_units"))
    statements.add(exec(s"  }"))
    statements.add(exec(s"  error_page   500 502 503 504  /50x.html;"))
    statements.add(exec(s"  location = /50x.html {"))
    statements.add(exec(s"    root   /var/www/nginx-default;"))
    statements.add(exec(s"  }"))
    statements.add(exec(s"}' >> /etc/nginx/conf.d/$applicationName.conf"))

    statements.add(exec("service nginx reload"))
    statements.add(exec("chkconfig nginx on"))
    new StatementList(statements.build).render(OsFamily.UNIX)

  }

  def createLoadBalancer(productRepoUrl: String, instance: Instance)(implicit ec:ExecutionContext) {
    val utils = new VMUtils

    val loadBalancerDescription = s"${productRepoUrl}loadbalancer.json"
    val nodeString = Source.fromFile(loadBalancerDescription).getLines().mkString

    val node = ScopeUtils.mapper.readValue(nodeString, classOf[Node])

    var newNetwork : List[Network] = Nil
    node.network.foreach{
      case net => {
        newNetwork = net.copy(openPorts = Option(List(port.toString))) :: newNetwork
      }
    }

    val createVMFuture = utils.createVM(systemName, instanceName, "load-balancer", node.copy(name = s"$systemName-$instanceName-lb-$applicationName", network = newNetwork), rsaKeyPair, IpUtils.getHostName, ScopeUtils.configuration.getInt("scope.http.port"), instance.instanceId)


    createVMFuture onSuccess {
      case nodeMetadata => {
        val results = utils.runScriptOnNode(nginxConfigurationScript(nodeMetadata.hostname, port.toString), "configure_load_balancer", nodeMetadata, rsaKeyPair.get("private").get, true)
        results.getExitStatus match {
          case 0 =>
          case _ => throw new IllegalStateException("Failed to configure load balancer.")
        }
      }
    }

    createVMFuture onFailure {
      case t => throw new IllegalStateException("Failed to create load balancer machine.", t)
    }
  }
}
