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

import com.cisco.oss.foundation.orchestration.scope.model.{DeploymentModelCapture, InstallNodes, _}
import com.google.common.collect.Lists
import net.liftweb.json._
import org.junit.{AfterClass, BeforeClass, Test}
import org.scalatest.junit.{JUnitSuite, MustMatchersForJUnit, ShouldMatchersForJUnit}

import com.cisco.oss.foundation.orchestration.scope.provision.model.ProductRepoInfo

object ModelUtilsTest {
  val modelString = """
                      {
                      |  "preDeleteNodesScript" : {
                      |  "sections" : [
                      |  {
                      |  "nodes": ["test-node"],
                      |  "script": "dhclient -v -r -lf /var/lib/dhclient/dhclient-eth0.leases -B -d -H `hostname` eth0"
                      |  }
                      |  ] },
                      |  "setupProvisioningEnv": true,
                      |  "announceHostNames": true,
                      |  "exposeAccessPoints": {
                      |    "accessPoints": [
                      |      {
                      |        "url": "<izek-151-db0>",
                      |        "name": "oracle"
                      |      },
                      |      {
                      |        "url": "<izek-151-om0>",
                      |        "name": "om"
                      |      }
                      |    ]
                      |  },
                      |  "installModules": {
                      |    "step2": {
                      |      "modules": [
                      |        {
                      |          "nodes": [
                      |            "izek-151-ui0"
                      |          ],
                      |          "version": "3.54.0-0",
                      |          "name": "nds_resourceui",
                      |          "file": {
                      |            "additionalValues": [
                      |              {
                      |                "value": "izek-151-db0",
                      |                "key": "carm::oracle::host"
                      |              }
                      |            ],
                      |            "baseConfigProperties": [
                      |              "config.properties"
                      |            ]
                      |          }
                      |        }
                      |      ]
                      |    },
                      |    "step1": {
                      |      "modules": [
                      |        {
                      |          "nodes": [
                      |            "izek-151-om0"
                      |          ],
                      |          "version": "3.55.0-0",
                      |          "name": "nds_carm",
                      |          "file": {
                      |            "additionalValues": [
                      |              {
                      |                "value": "izek-151-db0",
                      |                "key": "carm::oracle::host"
                      |              }
                      |            ],
                      |            "baseConfigProperties": [
                      |              "config.properties"
                      |            ]
                      |          }
                      |        },
                      |        {
                      |          "nodes": [
                      |            "izek-151-om0"
                      |          ],
                      |          "version": "3.55.0-5-SNAPSHOT",
                      |          "name": "nds_bsm",
                      |          "file": {
                      |            "additionalValues": [
                      |              {
                      |                "value": "izek-151-db0",
                      |                "key": "bsm::oracle::host"
                      |              },
                      |              {
                      |                "value": "izek-151-om0:13131",
                      |                "key": "bsm::carm::host_port"
                      |              }
                      |            ],
                      |            "baseConfigProperties": [
                      |              "config.properties"
                      |            ]
                      |          }
                      |        },
                      |        {
                      |          "nodes": [
                      |            "izek-151-om0"
                      |          ],
                      |          "version": "2.54.1-0",
                      |          "name": "nds_oasm",
                      |          "file": {
                      |            "additionalValues": [
                      |              {
                      |                "value": "berabeu-cat.bernabeu-cat.bernabeu.phoenix.cisco.com:13131",
                      |                "key": "oasm::ccm::host_port"
                      |              },
                      |              {
                      |                "value": "berabeu-cat.bernabeu-cat.bernabeu.phoenix.cisco.com:13131",
                      |                "key": "oasm::lsm::host_port"
                      |              },
                      |              {
                      |                "value": "izek-151-om0:13131",
                      |                "key": "oasm::bsm::host_port"
                      |              },
                      |              {
                      |                "value": "izek-151-db0",
                      |                "key": "oasm::oracle::host"
                      |              },
                      |              {
                      |                "value": "berabeu-cat.bernabeu-cat.bernabeu.phoenix.cisco.com:13131",
                      |                "key": "oasm::nm::host_port"
                      |              }
                      |            ],
                      |            "baseConfigProperties": [
                      |              "config.properties"
                      |            ]
                      |          }
                      |        },
                      |        {
                      |          "nodes": [
                      |            "izek-151-om0"
                      |          ],
                      |          "version": "2.0.0-1-SNAPSHOT",
                      |          "name": "nds_authz_server",
                      |          "file": {
                      |            "additionalValues": [
                      |              {
                      |                "value": "berabeu-gw.bernabeu-cat.bernabeu.phoenix.cisco.com:5600",
                      |                "key": "authz_server::cmdc_hostPort"
                      |              },
                      |              {
                      |                "value": "berabeu-im.bernabeu-cat.bernabeu.phoenix.cisco.com:6040",
                      |                "key": "authz_server::upm_hostPort"
                      |              },
                      |              {
                      |                "value": "BsmRestClient",
                      |                "key": "authz_server::offerDatFetcher"
                      |              },
                      |              {
                      |                "value": "izek-151-om0:5253",
                      |                "key": "authz_server::bsm_hostPort"
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
                      |            "izek-151-db0"
                      |          ],
                      |          "version": "5.0.0-1",
                      |          "name": "nds_mgdb"
                      |        },
                      |        {
                      |          "nodes": [
                      |            "izek-151-om0"
                      |          ],
                      |          "version": "11.2",
                      |          "name": "oracle_instant_client"
                      |        }
                      |      ]
                      |    }
                      |  },
                      |  "schemaVersion": "0.1",
                      |  "installNodes": {
                      |    "nodes": [
                      |      {
                      |        "name": "izek-151-om0",
                      |        "region": "US-EAST",
                      |        "minDisk": "50",
                      |        "minCores": "4",
                      |        "osType": "RedHat",
                      |        "postConfiguration": true,
                      |        "minRam": "8000",
                      |        "osVersion": "6.0",
                      |        "arch": "x86-64",
                      |        "id": "",
                      |        "network": [
                      |          {
                      |            "nicType": "internal",
                      |            "nicAlias": "izek-151-om0"
                      |          }
                      |        ]
                      |      },
                      |      {
                      |        "name": "izek-151-ui0",
                      |        "region": "US-EAST",
                      |        "minDisk": "50",
                      |        "minCores": "4",
                      |        "osType": "RedHat",
                      |        "postConfiguration": true,
                      |        "minRam": "8000",
                      |        "osVersion": "6.0",
                      |        "arch": "x86-64",
                      |        "id": "",
                      |        "network": [
                      |          {
                      |            "nicType": "internal",
                      |            "nicAlias": "izek-151-ui0"
                      |          }
                      |        ]
                      |      },
                      |      {
                      |        "name": "izek-151-db0",
                      |        "region": "US-EAST",
                      |        "minDisk": "50",
                      |        "minCores": "4",
                      |        "image": "oracle",
                      |        "osType": "RedHat",
                      |        "postConfiguration": true,
                      |        "minRam": "4048",
                      |        "osVersion": "6.0",
                      |        "arch": "x86-64",
                      |        "id": "",
                      |        "network": [
                      |          {
                      |            "nicType": "internal",
                      |            "nicAlias": "izek-151-db0"
                      |          }
                      |        ]
                      |      }
                      |    ]
                      |  }
                      |}
                    """.stripMargin


  @BeforeClass def init() {


  }

  @AfterClass def stop() {
  }
}

class NetworkUtilsTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit {
  @Test
  def testManupulateNetwork() {
    NetworkUtils.configureIptables("DFW", "a5ce7260-805a-4f9d-a618-a4f9e1dd64b5", List("5015", "6401"))
  }
}

class ModelUtilsTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit {

  implicit val formats = Serialization.formats(NoTypeHints)

  @Test
  def testProcessDeploymentModel() {
    val pojo = ModelUtils.StringToPojo(ModelUtilsTest.modelString)
    val (br, r) = ModelUtils.processDeploymentModel(pojo.head, new ProductRepoInfo(""), "scope")
    println(ScopeUtils.mapper.writeValueAsString(br))
  }


  var installNodes: InstallNodes = InstallNodes(List(Node("id", "name", "arch", "osType", "osVersion", "region", 1, 1, 1, Lists.newArrayList((Network("nicType", None, "nicAlias", None, None))), false, None, None, None, None, true, None)))

  var installModules: Map[String, InstallModules] = Map(("step1" -> InstallModules(Lists.newArrayList(PuppetModule("name", "version", None, None, None), PuppetModule("name", "version", None, None, None)))), ("step2" -> InstallModules(Lists.newArrayList(PuppetModule("name", "version", None, None, None), PuppetModule("name", "version", None, None, None)))))

  var exposeAccessPoints: ExposeAccessPoints = ExposeAccessPoints(List[AccessPoint](AccessPoint("name", "url")))

  @Test
  def testFutures() {

    val d = DeploymentModelCapture("version", None, installNodes, true, true, installModules, exposeAccessPoints)

    val string = ScopeUtils.mapper.writeValueAsString(d)

    logInfo(string)

    val g = ScopeUtils.mapper.readValue(string, classOf[DeploymentModelCapture])
  }

  @Test
  def testAddARecord() {
    val s = "{ \"instanceId\": \"\", \"systemId\": \"izek\", \"instanceName\": \"gadwall\", \"status\": \"STARTING\", \"details\": \"Foundation Instance found\\nCalling materializer to define deployment capture\", \"product\": { \"id\": \"Conductor-3.1.0.0\", \"productName\": \"Conductor\", \"productOptions\": [{ \"key\": \"HA\", \"label\": \"High Availability Mode\", \"value\": \"False\", \"optionType\": \"boolean\", \"defaultValue\": \"False\", \"enumeration\": [\"True\", \"False\"] }], \"productVersion\": \"3.1.0.0\", \"repoUrl\": \"http://10.45.37.10/scope-products/Conductor-3.1.0.0/\" }, \"machineIds\": { }, \"accessPoints\": [], \"deletable\": null}"
    val i = ScopeUtils.mapper.readValue(s, classOf[Instance])
  }
}