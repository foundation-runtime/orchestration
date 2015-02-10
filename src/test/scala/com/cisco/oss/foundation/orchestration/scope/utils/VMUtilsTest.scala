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

import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import com.cisco.oss.foundation.orchestration.scope.model.Node
import org.apache.commons.net.util.SubnetUtils
import org.jclouds.scriptbuilder.domain.OsFamily
import org.jclouds.ssh.SshKeys
import org.junit.{Assert, Ignore, Test}

import scala.collection.JavaConversions._
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/4/14
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
class VMUtilsTest extends Slf4jLogger  {
  implicit val flowEC = new FlowContextExecutor(global)
  val capture =
    """
      |{
      |  "setupProvisioningEnv": true,
      |  "announceHostNames": true,
      |  "exposeAccessPoints": {
      |    "accessPoints": [
      |      {
      |        "url": "http://<naama-test-upm0>:6040/upm",
      |        "name": "upm-private"
      |      },
      |      {
      |        "url": "http://<dns-upm>:6040/upm",
      |        "name": "upm-public"
      |      },
      |      {
      |        "url": "https://<dns-upm>:5015/ndsconsole/app.html",
      |        "name": "console"
      |      }
      |    ]
      |  },
      |  "installModules": {
      |    "step1": {
      |      "modules": [
      |        {
      |          "nodes": [
      |            "naama-test-upm0"
      |          ],
      |          "version": "4.49.2-2-SNAPSHOT",
      |          "name": "nds_upm",
      |          "file": {
      |            "additionalValues": [
      |              {
      |                "value": "set passwordQuality.checkNumericCharacterIncluded  false;;set passwordQuality.checkNonAlphaNumericIncluded  false;;set passwordQuality.checkNoVowelsIncluded  false;;set passwordQuality.checkMinimumPasswordLength  false;;set passwordQuality.minimumPasswordLength 5;;set upm.cdplugin.defaults.countryToPopulation.SWE 5;;set upm.cdplugin.defaults.countryToPopulation.FIN 5;;set upm.defaults.household.deviceQuota.ALL.integer 4;;set upm.defaults.household.deviceQuota.IPAD.integer 2;;set upm.defaults.household.deviceQuota.COMPANION.integer 1;;set upm.defaults.household.deviceQuota.PC.integer 3;;set upm.defaults.household.deviceQuota.IOS.integer 3;;set clp.isEnabled true;;set clp.connections.1.host clpserver1;;set clp.connections.2.host clpserver2",
      |                "key": "upm::config_props"
      |              },
      |              {
      |                "value": "%{::ipaddress}",
      |                "key": "upm::host"
      |              },
      |              {
      |                "value": "6040",
      |                "key": "upm::web_port"
      |              },
      |              {
      |                "value": "UPM-CD",
      |                "key": "upm::topic"
      |              },
      |              {
      |                "value": "27017",
      |                "key": "mongodb::port"
      |              },
      |              {
      |                "value": "false",
      |                "key": "upm::hornetq::is_enabled"
      |              },
      |              {
      |                "value": "false",
      |                "key": "ccp::enabled"
      |              },
      |              {
      |                "value": "mongodb",
      |                "key": "upm::dbname"
      |              },
      |              {
      |                "value": "set log4j.rootLogger error,logfile,clpAppender;;set log4j.appender.clpAppender com.nds.cab.infra.appenders.CLPAppender;;set CABLogger.asyncAppenderReferences [clpAppender];;",
      |                "key": "upm::log4j_mongo"
      |              },
      |              {
      |                "value": "canal-digital",
      |                "key": "flavor"
      |              },
      |              {
      |                "value": "naama-test-mongo0",
      |                "key": "mongodb::host"
      |              }
      |            ],
      |            "baseConfigProperties": [
      |              "config.properties"
      |            ]
      |          }
      |        },
      |        {
      |          "nodes": [
      |            "naama-test-upm0"
      |          ],
      |          "version": "1.0.0.0",
      |          "name": "nds_upm_bento"
      |        },
      |        {
      |          "nodes": [
      |            "naama-test-upm0"
      |          ],
      |          "version": "3.49.1-0",
      |          "name": "nds_umsui",
      |          "file": {
      |            "additionalValues": [
      |              {
      |                "value": "3.45.1-SNAPSHOT",
      |                "key": "ndsconsole::version"
      |              },
      |              {
      |                "value": "naama-test-upm0",
      |                "key": "upm::host"
      |              }
      |            ],
      |            "baseConfigProperties": [
      |              "config.properties"
      |            ]
      |          }
      |        }
      |      ]
      |    },
      |    "step0": {
      |      "modules": [
      |        {
      |          "nodes": [
      |            "naama-test-mongo0",
      |            "naama-test-mongo1"
      |          ],
      |          "version": "2.4.5.1",
      |          "name": "mongodb_tar"
      |        },
      |        {
      |          "nodes": [
      |            "naama-test-mongo0",
      |            "naama-test-mongo1"
      |          ],
      |          "version": "3.51.0-2",
      |          "name": "nds_emm",
      |          "ccp": {
      |            "processName": "emm",
      |            "baseConfigProperties": [
      |              "config.properties"
      |            ],
      |            "additionalValues": [
      |              {
      |                "value": "naama-test-mongo0",
      |                "key": "generic.mongodb.host.1"
      |              },
      |              {
      |                "value": "naama-test-mongo1",
      |                "key": "generic.mongodb.host.2"
      |              }
      |            ]
      |          }
      |        }
      |      ]
      |    }
      |  },
      |  "schemaVersion": "0.1",
      |  "installNodes": {
      |    "nodes": [
      |      {
      |        "name": "naama-test-mongo0",
      |        "region": "US-EAST",
      |        "minDisk": "10",
      |        "minCores": "16",
      |        "osType": "RedHat",
      |        "minRam": "256",
      |        "osVersion": "6.0",
      |        "arch": "x86-64",
      |        "id": "",
      |        "network": [
      |          {
      |            "nicType": "internal",
      |            "nicAlias": "naama-test-mongo0"
      |          }
      |        ]
      |      },
      |      {
      |        "name": "naama-test-mongo1",
      |        "region": "US-EAST",
      |        "minDisk": "10",
      |        "minCores": "16",
      |        "osType": "RedHat",
      |        "minRam": "256",
      |        "osVersion": "6.0",
      |        "arch": "x86-64",
      |        "id": "",
      |        "network": [
      |          {
      |            "nicType": "internal",
      |            "nicAlias": "naama-test-mongo1"
      |          }
      |        ]
      |      },
      |      {
      |        "name": "naama-test-upm0",
      |        "region": "US-EAST",
      |        "minDisk": "10",
      |        "minCores": "16",
      |        "osType": "RedHat",
      |        "minRam": "256",
      |        "osVersion": "6.0",
      |        "arch": "x86-64",
      |        "id": "",
      |        "network": [
      |          {
      |            "nicType": "internal",
      |            "nicAlias": "naama-test-upm0"
      |          }
      |
      |        ]
      |      }
      |    ]
      |  }
      |}
    """.stripMargin

  @Test
  def createInitScriptTest() {
    val util = new VMUtils()
    val script = new BootstrapStatements(List((new SubnetUtils("10.0.0.0/24"), "gateway")), "scopeIP", "osVersion", true, "nodename", List("5015", "6040", "6060"), "stub", "scopeMachineName", 6041, "instanceId")
    val stringScript = script.render(OsFamily.UNIX)
    val s1 = "iptables -A INPUT -i $dev -p tcp --dport 5015 -j ACCEPT"
    val s2 = "iptables -A INPUT -i $dev -p tcp --dport 6040 -j ACCEPT"
    val s3 = "iptables -A INPUT -i $dev -p tcp --dport 6060 -j ACCEPT"
    Assert.assertTrue(s"Does NOT contains ( $s1 )", stringScript.contains(s1))
    Assert.assertTrue(s"Does NOT contains ( $s2 )", stringScript.contains(s2))
    Assert.assertTrue(s"Does NOT contains ( $s3 )", stringScript.contains(s2))

    logInfo("script : {}", stringScript)
  }

  @Test
  def createVmTemplateTest() {
    val util = new VMUtils()
    val nodeJson =
      """
        |{
        |        "name": "naama-test-upm0",
        |        "region": "US-EAST",
        |        "minDisk": "10",
        |        "minCores": "1",
        |        "osType": "RedHat",
        |        "minRam": "256",
        |        "osVersion": "6.0",
        |        "arch": "x86-64",
        |        "id": "",
        |        "postConfiguration" : true,
        |        "network": [
        |          {
        |            "nicType": "internal",
        |            "nicAlias": "naama-test-upm0"
        |          },
        |          {
        |            "openPorts": [
        |              "6040",
        |              "6060",
        |              "5015"
        |            ],
        |            "dnsServices": [
        |              "upm"
        |            ],
        |            "nicType": "public",
        |            "nicAlias": "naama-test-upm0_public"
        |          }
        |        ]
        |}
      """.stripMargin

    val node = ScopeUtils.mapper.readValue(nodeJson, classOf[Node])

    val template = util.createVmTemplate(node, "test-test", SshKeys.generate().toMap, List(), List(), "scopeMachineName", 6041, "instanceId")


    val stringScript = template.getOptions.getRunScript.render(OsFamily.UNIX)
    val s1 = "iptables -A INPUT -i $dev -p tcp --dport 5015 -j ACCEPT"
    val s2 = "iptables -A INPUT -i $dev -p tcp --dport 6040 -j ACCEPT"
    val s3 = "iptables -A INPUT -i $dev -p tcp --dport 6060 -j ACCEPT"
    Assert.assertTrue(s"Does NOT contains ( $s1 )", stringScript.contains(s1))
    Assert.assertTrue(s"Does NOT contains ( $s2 )", stringScript.contains(s2))
    Assert.assertTrue(s"Does NOT contains ( $s3 )", stringScript.contains(s2))

    logInfo("script : {}", stringScript)
  }

  @Ignore
  @Test
  def createVmTest() {
    FlowContextFactory.createFlowContext
    ScopeUtils.configuration.setProperty("cloud.provider", "vsphere")
    val util = new VMUtils()

    val nodeJson = """
                     |{
                     |        "name": "junit-test-test1",
                     |        "region": "US-EAST",
                     |        "minDisk": "30",
                     |        "minCores": "1",
                     |        "osType": "RedHat",
                     |        "minRam": "256",
                     |        "osVersion": "6.0",
                     |        "arch": "x86-64",
                     |        "id": "",
                     |        "network": [
                     |          {
                     |            "nicType": "internal",
                     |            "nicAlias": "naama-test-upm0"
                     |          }
                     |        ]
                     |}
                   """.stripMargin

    val node = ScopeUtils.mapper.readValue(nodeJson, classOf[Node])

    val vmFuture = util.createVM("junit", "test", "test", node, SshKeys.generate().toMap, "scopeMachineName", 6041, "instanceId")
    vmFuture.onSuccess {
      case vm => //util.deleteVM(vm)
    }

    Await.result(vmFuture,50 minutes)
  }
  
  @Ignore
  @Test
  def createDNSTest() {
    try {
    DnsUtilsFactory.instance.createDomain("naama", "test", "upm")
    DnsUtilsFactory.instance.createARecord("naama", "test", "upm", "11.11.11.11")
    DnsUtilsFactory.instance().deleteDns("upm.test.naama.vcs-foundation.com")
    } catch {
      case e: Exception => {
        logInfo("Failed to create DNS : {}", Array(e, e))
      }
    }
  }
}
