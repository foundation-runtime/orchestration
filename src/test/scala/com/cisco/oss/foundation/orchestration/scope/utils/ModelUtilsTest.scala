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

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.MustMatchersForJUnit
import org.scalatest.junit.ShouldMatchersForJUnit
import net.liftweb.json._
import com.cisco.oss.foundation.orchestration.scope.model._
import com.cisco.oss.foundation.orchestration.scope.model.DeploymentModelCapture
import com.cisco.oss.foundation.orchestration.scope.model.InstallNodes
import com.google.common.collect.Lists
//import com.cisco.oss.foundation.orchestration.scope.provision.utils.ProvisionSftpUtils
import com.cisco.oss.foundation.orchestration.scope.provision.model.ProductRepoInfo

object ModelUtilsTest {
  val modelString = """
                      {
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

case class Ttt(val hh: String, val jj: String)

case class Zzz(val hh: Option[Ttt], val rr: String)


class NetworkUtilsTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit {
  @Test
  def testManupulateNetwork() {
    NetworkUtils.configureIptables("DFW", "a5ce7260-805a-4f9d-a618-a4f9e1dd64b5", List("5015", "6401"))
    //utils.NetworkUtils.fixPublicIfc("10.234.0.3")
  }
}

class ModelUtilsTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit {

  implicit val formats = Serialization.formats(NoTypeHints)

  @Test
  def testProcessDeploymentModel() {
    //MamaUtils.executeRemoteCommand("10.234.0.34", "cat > " + "/root/.ssh/" + new File(rsaFullPath).getName + " << EOF\n" + rsakey + "\nEOF")

    //MamaUtils.copyFileLocalToRemote("10.234.0.34", rsaFullPath, "/root/.ssh/" + new File(rsaFullPath).getName, "root")
    //ProvisionSftpUtils.copyLocalFolderToRemote("D:/SVN/scope/chpuppet","/etc/puppet","10.234.0.47","D:/SVN/scopeServer/id_rsa")

    val pojo = ModelUtils.StringToPojo(ModelUtilsTest.modelString)
    val (br, r) = ModelUtils.processDeploymentModel(pojo.head, new ProductRepoInfo(""), "scope")
    //

    println(ScopeUtils.mapper.writeValueAsString(br))
    //val fullConfiguration: Map[String, Any] = Map[String, Any]() ++ br.get("vgc1c").get.configuration

    //logInfo( ScopeUtils.mapper.writeValueAsString(br.get("vgc1c").get.configuration))
    //    val command: String = "cat > /etc/puppet/chpuppet/modules/role/manifests/imaas.pp << EOF\n" + br.get("vgc1c").get.script + "\nEOF"
    //MamaUtils.executeRemoteCommand("10.45.37.10",command)
  }


  @Test
  def testJson() {
    val jsonString: String = "{ \"rr\" : \"jjj\"}"

    val g = ScopeUtils.mapper.readValue(jsonString, classOf[Zzz])
    logInfo("" + g)
  }

  var installNodes: InstallNodes = InstallNodes(List(Node("id", "name", "arch", "osType", "osVersion", "region", 1, 1, 1, Lists.newArrayList((Network("nicType", None, "nicAlias", None, None))), false, None, None, None, None, true, None)))

  var installModules: Map[String, InstallModules] = Map(("step1" -> InstallModules(Lists.newArrayList(PuppetModule("name", "version", None, None, None), PuppetModule("name", "version", None, None,None)))), ("step2" -> InstallModules(Lists.newArrayList(PuppetModule("name", "version",  None, None, None), PuppetModule("name", "version", None, None, None)))))

  var exposeAccessPoints: ExposeAccessPoints = ExposeAccessPoints(List[AccessPoint](AccessPoint("name", "url")))

  @Test
  def testMatch() {
    for (i <- 1 until 1)
      println(i)

  }

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
    //ProvisionSftpUtils.copyLocalFolderToRemote("C:/Temp/test", "/tmp/izek", "10.234.0.29", "-----BEGIN RSA PRIVATE KEY-----\nMIIEoQIBAAKCAQEAk3CB6UKzpUiin0uPMDA+uxcTDwSjz8wg2OFG0uKSx6RJAmaw\nHyhhEHSKJeopBq4NgeZU6qn36VnmIU+VpFFBfTSZs0g4eTwl7N72c7mmTM0F+dL2\nARTkB01zQ31ncj6/4gULy3ojnAXB40iopblZLYKTPZ17PtZZ2NLyoMqQfrcA4D8f\nCxdz5EKSXecymo6yrHj/GJGG/M6RVrQNdZVKzaP06hLMvTb7AhM/qPK+a7G7+oK6\naJvWXyXuEt68KqcWjpfC2gqRUjDjaCWCwPMJJJvXevVfof+1gitq2oO8CiBnJ0aQ\nV1kz9t7JThOuBC88SB9fKnplQM6+V4Ir35us3wIBIwKCAQAMoz5V0oRtQL17Xj95\nKLTrdwGiM5kDLsD8pZhbN/3z2uGvv6iyNqk77LtiVeZCZrCzXOK1B0EioU49XpfM\nQXqkVPc7QLRiKbobnhUf3LZ7nIt70ED4x0a+zB/Slbho4M6eWDQ2AyefqLjY95lt\nSma6wgye4ZzZgBZUadpIS+B/46uRWqrNYRSCr9nT0nGGHk+3MfFmQBnjvZX4GzRp\nQEprUmEPDbxI2Ob1GMErX7xXnIkg40XqUenlCCH5solMS7KAXyvr5CUFOd5wXHpD\nYHtuPpYludEWSYxyz3T9BAS1hOtNQ0nrfF/3makwaFQeH6rN0jPC/FQdjCn7tsGE\nywiDAoGBAMSMICIv+tDEXrYBGbwe8LDFDpQvtqrw4iEbrD6qRRmJX2aP1gxDDUpt\n9iHoCJrwgwZ/pCNOq1Ik2sbnRbQzsC/4oJvtDGykzU5KUMFaQoJEmQivviPa8K9V\nZwWwqoIhNPS846tVIW8q3j1WSdye6kV92BYuzSPNQ/xGlijwERm7AoGBAMAJp97U\nWrPV4ERgNkoJkbU4zmqVRFiq80VBymtUt3e1g3dVRyvS8dS9/FAcN+N9DHWl2Gds\nNNt07EV1IMYng8QRoNEs7cobFcdUSIRUoDwvKKG9v18YX5+b+rr3VQL4h2Pk/Iac\nN2Ut/9YYXfwF04NpCEJuLWwa0i2YQoaM9IUtAoGAarJ32Atq5lv45nWgQY0jkykl\nK99jKZi1RSxHj7uEmNWLj3KnZb3/5pN2/HallfBHIMjytBTDZxtSMXY7yDlRBBlB\nPrPiLFl2xBm2z1ziRreGRotJ9jUGUI1ycM2eY+YkD9RBE93BsWBqBApFWoInZ411\nTd7kY+R1XRBgJNoX6WUCgYEAtRBrE+zKjE1Py3CoNy2YAqNGRzxHwVC5e9BRI16B\nGRjbCh00ecbkAxmHfruFJvI+8pUN3dsNQ/HmFZpSGeq+EKLoFa4Exd8F59MfzTnY\n6Em4mH+0b5qjTViNUTJXd9RiZYAhS7fcdVdBrJqhwbxZtmpYPqJlV0x9Bmr85UMS\nbu8CgYB4iniYOM3MCL5xz+YgjFPzC3fJFuU4NLPX1EZurx0qXcLd5u5uSa4NwkRw\nxh2KxB3CvE+hiBrdTkVWoRxJ7nJl8eg79bIeoPKx7VA14hPyLpKs/7Q76xgwRhgP\n6I72tcbvEWwxwZJBPx/MdLGouTEMMHhXx/9ekUFkdjiMgEsi2w==\n-----END RSA PRIVATE KEY-----")


  }
}