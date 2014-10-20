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

package com.cisco.oss.foundation.orchestration.scope.dblayer.mongodb

import com.cisco.oss.foundation.orchestration.scope.model.{Instance, Product, ScopeNodeMetadata}
import com.cisco.oss.foundation.orchestration.scope.utils.ScopeUtils
import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{MongodExecutable, MongodStarter}
import de.flapdoodle.embed.process.runtime.Network
import org.junit.{AfterClass, BeforeClass, Test}

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 1/28/14
 * Time: 12:55 PM
 * To change this template use File | Settings | File Templates.
 */

object SCOPeDBMongoImplTest {
  val mongoPort = Network.getFreeServerPort;
  var mongodExecutable: MongodExecutable = null;
  ScopeUtils.configuration.setProperty("mongodb.port", mongoPort.toString)
  ScopeUtils.configuration.setProperty("mongodb.host", "localhost")

  @BeforeClass
  def init() {


    val mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", mongoPort, false))
      .build();

    val runtime = MongodStarter.getDefaultInstance();


    try {
      mongodExecutable = runtime.prepare(mongodConfig);
      val mongod = mongodExecutable.start();

    } finally {

    }
    createScopeDB
  }

  def createScopeDB() {
    val mongodbConnetion = MongoConnection("localhost", mongoPort)
    val scopedb = mongodbConnetion("scope")
    val instances = scopedb("instances")

    val doc = """{
                |  "_id" : "52e76356e4b0ad9aa68837fe",
                |  "systemId" : "izek",
                |  "instanceName" : "www",
                |  "status" : "STARTED",
                |  "product" : {
                |    "_id" : "IMPPS-3.48.0.0",
                |    "productName" : "IMPPS",
                |    "productVersion" : "3.48.0.0",
                |    "productOptions" : [{
                |        "key" : "HA",
                |        "value" : "False",
                |        "label" : "High Availability Mode",
                |        "optionType" : "boolean",
                |        "defaultValue" : "False",
                |        "enumeration" : ["True", "False", ""]
                |      }],
                |    "repo" : {
                |      "hostDetails" : {
                |        "id" : "DFW/3a958ec0-b6c0-45b9-901c-236673e6502f",
                |        "hostname" : "IMPPS-3-48-0-0-repo",
                |        "privateKey" : "-----BEGIN RSA PRIVATE KEY-----\nMIIEogIBAAKCAQEAjOzk+8sbCNvze1sMR26Dz1mALAAULW+xuNn/COnm64yHqAKY\nsWA9Z9MNu53zikbzBedlyi4ghJBeEUw8Vm2C+sgugsFdeNAffMYBa9Nz58xP+TBJ\neMnRD3Ms/kqOlpALFts9ZL33+Ruc5M2NwirWK+mKm6I5ABDuXmsdbehGzYkJKtQA\ngUv0g9ZnqBuFXi01dLgVInjbSUSd0o73khmkVuTVC+lPWv/tQTsbj3bGpUMqMlah\n9joCmfLD/sV8XPYnQPyaiA8wKIQBvUQp0yYhoy6fCKpf68voedcPJflsMCCcFjzG\n8UNxO9KrbeCIHlT5Q4tjtzN66BQFSjFTRBCC7QIDAQABAoIBAGA+cWwdHAuC29iF\nJ++o5FA52bUzIJSfUYrjJrAZvmFkCkmN7GZHSeicVSarCuaG6fZAQF5B/mdKiVPa\n8uI7zFYlXM5j7MhTqw111Qak1OWbvYp7ldHuUt7wU+wfUx40pb4oYNdqfe4dE5uX\nXpca1kYLK2R69965sXG+Sc0D80bup8LqARXk0bX25I7qzn5YKkJ3g7sRrDtPfy6l\ngod+VWW/v2Ax/2iUOaMsBnFuEHsNBfHrxhzQ6uwaqerMNqZfaEMbZp91k0HKGMiW\nIg/hBBANRuGIXH6BwChHIIDnkCMdwkb/af3Ghgp0gtnnN1KgDxU4c9bRq6xGn7iv\njX7HoAECgYEA2xsXownXqCsSIHGyM7BMFQyCAc03npH8MhldPJ93XCjkpBZZM1Vc\nvoiuogvzQ9VsD3oBbJb8Q/d0bGaxj8dYUuyGe1jkiI61hhV4AwFrCuZ9I1urVt3E\n7yCI8byj4MBmsB1BdyJOv5RZwjiSedrZPq5ZgLpw+SeVsLWM5Qx8nu0CgYEApKe1\nS63ZQ4LZMpma7Ffl2+erTeYdMinNoOpSc4ylzNc6JdFTqp74yv8eowHVv6PBjAsz\nYd4V7Qyv1wIM+9F3cmsFia3d/APXUoQOTSb7Uh+xVysYjmi9TRiEXfOoYdD+xFTC\ny8Tum/uJWmup/22A3eN6BE2ASxtR3aliynNC9AECgYAXBYO4R8J3Ev92lTuqHq3/\n0C6gzdU4PhKHmQ6o3gCGmG1dqFN7B08VXfsrX6IR0IzoG2Om6z5aTdfXw/qIJuPq\n4ptGvpJUntoH2p2fgziiDpG7c1hPakHU9lAtRirZ5J1lh81nHR21F3tO4u7RCuOk\nqGETG4PfSf96b7j8IBHACQKBgElkZ68gAqPRAzdGuIN4eoCWtwi19XWSpJGBYNcY\nbh7sIIMS1xRKX+M77FAEVV5ig7cElxacg3FyIj7YLylfqLpbcdB4q2XV4HhrXGSP\nZ56neci88OkpTpe6weWO4hMXsTeaAGoLkb/9Uq/3JYMxMCu3ZX/de73+o3MGOX67\n+bABAoGAeI2hpBnuXBG7AeqXPKSgL9ImtFoTna3zIkSP051IQHOkQlwktHQV0rES\ndxODw08Lkysj1++rVeDofQ4nnckiWP4gTUU586ElwMeqVqYa6VRWgbFjAuwNH1uk\no/SvMS3vCkrxktyE+uUCRuLSlNbjSPcCwnRwEw2cmx5j3a8FZ/o=\n-----END RSA PRIVATE KEY-----\n",
                |        "privateAddresses" : ["10.234.0.72"],
                |        "publicAddresses" : [],
                |        "group" : "repo-IMPPS",
                |        "tags" : ["repo", "IMPPS"],
                |        "url" : "https://dfw.servers.api.rackspacecloud.com/v2/844864/servers/3a958ec0-b6c0-45b9-901c-236673e6502f",
                |        "provisionStatus" : "STARTING"
                |      },
                |      "status" : "READY"
                |    }
                |  },
                |  "machineIds" : {
                |    "izek-www-mongo0" : {
                |      "id" : "DFW/b459b4ab-6284-4cc6-86b2-26f00a4fd6e6",
                |      "hostname" : "izek-www-mongo0",
                |      "privateAddresses" : ["10.234.0.77"],
                |      "publicAddresses" : [],
                |      "group" : "izek-www",
                |      "tags" : ["izek", "www", "IMPPS"],
                |      "url" : "https://dfw.servers.api.rackspacecloud.com/v2/844864/servers/b459b4ab-6284-4cc6-86b2-26f00a4fd6e6",
                |      "provisionStatus" : "STARTING"
                |    },
                |    "izek-www-mongo1" : {
                |      "id" : "DFW/6c773278-2b89-4128-9728-f97ca995d234",
                |      "hostname" : "izek-www-mongo1",
                |      "privateAddresses" : ["10.234.0.6"],
                |      "publicAddresses" : [],
                |      "group" : "izek-www",
                |      "tags" : ["izek", "www", "IMPPS"],
                |      "url" : "https://dfw.servers.api.rackspacecloud.com/v2/844864/servers/6c773278-2b89-4128-9728-f97ca995d234",
                |      "provisionStatus" : "STARTING"
                |    },
                |    "izek-www-upm0" : {
                |      "id" : "DFW/29801dd7-728a-4cf6-9714-15be3bd6d511",
                |      "hostname" : "izek-www-upm0",
                |      "privateAddresses" : ["10.234.0.67"],
                |      "publicAddresses" : ["23.253.76.68"],
                |      "group" : "izek-www",
                |      "tags" : ["Public IP", "izek", "www", "IMPPS"],
                |      "url" : "https://dfw.servers.api.rackspacecloud.com/v2/844864/servers/29801dd7-728a-4cf6-9714-15be3bd6d511",
                |      "provisionStatus" : "STARTING"
                |    }
                |  },
                |  "accessPoints" : [{
                |      "name" : "upm",
                |      "url" : "http://10.234.0.67:6040/upm"
                |    }, {
                |      "name" : "pps",
                |      "url" : "http://10.234.0.67:6060/pps"
                |    }, {
                |      "name" : "console",
                |      "url" : "https://10.234.0.67:5015/ndsconsole/app.html"
                |    }],
                |  "rsaKeyPair" : {
                |    "public" : "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCOcItDmX5f9bGFOodXUuUvj8nU1+qjBgUagz5L2H7zVAMsMZzIlyTrU1vwcgbB3jwXSn70Io1/rAcOSoHJfbnk5iBVtDRs/XLQaJqVJVMmopWMqXVK0qXWzixYTSDlMu1OGAiWKQl14bm55IbqHCA//zbBSdyYK1r8icjrGuvY6jgNdyeZDlJACnN1p9qGejZVSKqKMPFBwTMmsTbl9yyIUjaKsMJ2v+KXe1M7D0okvlJgGPS2uPKrqfF1GuYzYaKSDHpL6dpq1WNssWldsEjAUSZkA2OaYunqkbptbaACzm9Asy6qHcjX7ZrSzmYsh/zUoHnwo1eHhjRpN3X2VDJd",
                |    "private" : "-----BEGIN RSA PRIVATE KEY-----\nMIIEogIBAAKCAQEAjnCLQ5l+X/WxhTqHV1LlL4/J1NfqowYFGoM+S9h+81QDLDGc\nyJck61Nb8HIGwd48F0p+9CKNf6wHDkqByX255OYgVbQ0bP1y0GialSVTJqKVjKl1\nStKl1s4sWE0g5TLtThgIlikJdeG5ueSG6hwgP/82wUncmCta/InI6xrr2Oo4DXcn\nmQ5SQApzdafahno2VUiqijDxQcEzJrE25fcsiFI2irDCdr/il3tTOw9KJL5SYBj0\ntrjyq6nxdRrmM2Gikgx6S+naatVjbLFpXbBIwFEmZANjmmLp6pG6bW2gAs5vQLMu\nqh3I1+2a0s5mLIf81KB58KNXh4Y0aTd19lQyXQIDAQABAoIBAEvkPu8niyPJnmnj\nw1SNgDsVG25iFEwD2xhDMR/sG8e9zWrwjB58GVmgsm2r81m79LxcHsQo3MdXevLs\nU7ZtXGxPCI9hUkV3zIKqGSK6HlGJcrDdYPcawO1wMERj49D6j2F6gKiw+K9sMy6O\nSivWDIDMk/nsRrJ09ydyqjBJybtqT1iKJE8t7avd6J4RcBE//atVkwjVC1M4gbYo\n+rDE1wRqqtoakXbDi13zEltUwojktfYQGDbBgA/adwGKwNIwTH54vbi/LnX1Qder\nzgVNUcXVaQRbByQMbxKYTbaCGZLqba3hAw+SlRo5XefHGXV2otvQd+OMLOnP4mNu\nMth7/WkCgYEA7XXUqqtCpF+cEGL905XHY/5U8gVVupLsuHlXzJrv35drKmqGI7KT\nftplzSD6/iAzYZ8jml5mho1YRVd5vNbcUJc/LWbedXwqmILkASCuzqFD8ZDtvQhO\nkduN930jYzn3cdQL+dIv9VvYHaQbN0V3QftiTF2x9dfPFVzafznw2pMCgYEAmY+D\n5RlqhDTGgfQErBzvpsBxUhhwJM6P0hAzn2OuKowY5FqpQJscHOBcH6JoLzypzLTj\n8ixhGAvEPfuCu7wG4Hfln+9CCLz2P99k76Rtn3WVX+usiS2ge+bmyQhPKeBnMghi\nDJbmzDD6HMPqymYW7hAaEL8fL2hFjjLC0ztgpU8CgYAW+MMuD4tO44DxhOIRSfgU\nEFKfZyy38+a6oeKAhKyX8MoJPGzlnyztpKscgQhG9U6DLyX+lQtOEPZtHt1EC3Uc\nxsTx23XB39UvE9qC0WqXyroL8H1PQDJ5ocfGHXEC9GSfume+Lzs8fToXA/0uB6ZV\n17/Wq2m/4rTB2E266RwHyQKBgCgMadWiH0mDihHyVhWdJlNS9Tr37Kdsx819NDlp\ngc7O4t8LVgDncxmE2gHWFV3ccFxXAOvz7w6aYv/XTG0xyIlaO6TOfWZSAdb+qBBn\nzb88p0xw5nqQT76ApcfgRuUhBUjjLs8hu+edDl3aKq3GmkxUHrgnYDa49wry6iS0\n46u5AoGASMjbfnlrLRO78wHpHvvM9ElbDYpcVVNOdPK09zwtocs8kRAWmxDXz+AB\nDWSUZA4XEjHYpllWf4I3r0k8WWmPwUhgWnW8g2RTZB5LhPgfy2sbSbR1HO7fRv6d\n+irp0NBQKbabqoFcbXAm8p+TXdw76wcoT3AmqEMqYVUhFbWjNl8=\n-----END RSA PRIVATE KEY-----\n"
                |  }
                |}""".stripMargin

    /* instanceId: String,
          systemId: String,
          instanceName: String,
          status: Option[String],
          details: Option[String],
          product: Product,
          machineIds: Map[String, ScopeNodeMetadata],
          accessPoints: scala.List[AccessPoint],
          deletable: Option[Boolean],
          rsaKeyPair: Map[String, String] = Map[String, String](...)*/
    val instance = Instance("52e76356e4b0ad9aa68837fe",
                            "izek",
                            "izek",
                            None,
                            None,
                            Product("",
                                    "",
                                    "",
                                    List(),
                                    "",
                                    None),
                            Map("izek-www-upm0" -> ScopeNodeMetadata("",
                                                                     "",
                                                                     None,
                                                                     None,
                                                                     Set(),
                                                                     Set(),
                                                                     "",
                                                                     Set(),
                                                                     "",
                                                                     "STARTING")),
                            List(),
                            Some(true))


    instances.insert(grater[Instance].asDBObject(instance))
  }

  @AfterClass
  def stop() {
    if (mongodExecutable != null)
      mongodExecutable.stop();
  }
}

class SCOPeDBMongoImplTest {



  @Test
  def testUpdateMachineStatus() {
    val db = new SCOPeDBMongoImpl
    val instance = db.findInstance("52e76356e4b0ad9aa68837fe")

    println(instance)

    db.updateMachineStatus("52e76356e4b0ad9aa68837fe", "izek-www-upm0", "STARTED", Some(scala.collection.mutable.Set[String]("test_module")))

    val instance1 = db.findInstance("52e76356e4b0ad9aa68837fe")

    println(instance1)
  }
}
