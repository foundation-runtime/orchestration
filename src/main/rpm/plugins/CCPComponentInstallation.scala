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

package com.cisco.scope.plugins

import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest}
import com.cisco.oss.foundation.orchestration.configuration.IComponentInstallation
import com.cisco.oss.foundation.orchestration.model.{Instance, Product, PuppetModule, ScopeNodeMetadata}
import com.cisco.oss.foundation.orchestration.utils.ScopeUtils
import com.nds.cab.infra.mamafriendlyclient.MAMAFriendlyClient

import scala.collection.JavaConversions._
import scala.io.Source

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 11/28/13
 * Time: 10:03 AM
 */
object CCPComponentInstallation {
  val httpClient = ApacheHttpClientFactory.createHttpClient("ccpClient", false);
//  httpClient.start()
  val INSTALL_COMPONENT = ("http://{0}:{1}/api/add-new-component-raw?host={2}&installPath={3}&processName={4}", HttpMethod.POST)
}

class CCPComponentInstallationImpl extends IComponentInstallation {
  val NEW_LINE: String = "\n"

  override def delete(configurationServer: ScopeNodeMetadata, fqdn: String): Unit = {
    val url = s"http://${configurationServer.privateAddresses.head}:5670/api/delete-hierarchy-node?fqdn=$fqdn"
    val request = HttpRequest.newBuilder()
      .uri(url)
      .httpMethod(HttpMethod.POST)
      .header("Accept", "text/plain")
      .build()
       CCPComponentInstallation.httpClient.execute(request);


  }

  override def prepare(product: Product,instance: Instance, puppetModule: PuppetModule, server: ScopeNodeMetadata): Unit = {
    puppetModule.ccp match {
      case Some(ccp) => {
        logDebug("Installing configuration into Configuration Server for {}", puppetModule)
        val baseUrl: String = s"${product.repoUrl}/materializer/config/${puppetModule.name}/"

        val ccpConfig = Source.fromURL(baseUrl + "ccpConfig.xml").getLines().mkString(NEW_LINE)

        var mergedProperties: String = ""
        ccp.baseConfigProperties.foreach {
          case propertiesFile => {
            mergedProperties += Source.fromURL(baseUrl + propertiesFile).getLines().mkString(NEW_LINE)
          }
        }
        mergedProperties += NEW_LINE + ccp.additionalValues.foldRight("")((t, s) => t.key + "=" + t.value + NEW_LINE + s)
        logDebug("ccpConfig for module {} is : \n{}", puppetModule, ccpConfig)
        logDebug("properties for module {} is : \n{}", puppetModule, mergedProperties)
        logDebug("List modules to configure : {} ", puppetModule)

        puppetModule.nodes.foreach {
          node => {
            logDebug("installing node : {}", node)
            install(server.privateAddresses.head, 5670, instance.systemId, instance.instanceName, puppetModule.version, addDomainName(node.toLowerCase), s"/opt/nds/installed/${ccp.processName}-${puppetModule.version}", ccp.processName, ccpConfig, mergedProperties, isNdsconsoleModule = false)
              }
            }
          }
        }
  }

  private def addDomainName(name: String) = {
    ScopeUtils.configuration.getString("cloud.env.dnsName") match {
      case domain: String => s"$name.$domain"
      case _ => name
    }
  }

  def install(ccpHost: String, ccpPort: Int, systemId: String, instanceName: String, version: String, fqdn: String, installPath: String, processName: String, configSchema: String, properties: String, isNdsconsoleModule: Boolean) = {
    ScopeUtils.time(logger, "ccp-installNewComponent") {
      val cl = new MAMAFriendlyClient(ccpHost, 27034, "root")
      cl.WaitForTokensInLog("/opt/nds/ccp/log/ccp.log", "ServerRecoveryDaemon started", 1000 * 60)
      cl.disConnect()
      val replacedInstallPath = installPath.replaceAll("/", "_")
      val getHierarchyUrl: String = s"http://$ccpHost:$ccpPort/api/hierarchy-tree?uniqueProcessName=$fqdn-$processName-$version-$replacedInstallPath"
      logInfo("hierarchy-tree : {}", getHierarchyUrl)

      val getHierarchyUrlRequest = HttpRequest.newBuilder()
      .uri(getHierarchyUrl)
      .httpMethod(HttpMethod.GET)
      .header("Accept", "text/plain")
      .build()

//      var response = ComponentInstallation.httpClient.newRequest(getHierarchyUrl)
//        .method(HttpMethod.GET)
//        .header("Accept", "text/plain")
//        .send();

      var response = CCPComponentInstallation.httpClient.execute(getHierarchyUrlRequest);

        if (response.getStatus == 200) {
        logDebug("component already installed on CCP.")
      }
      else {
          val addHierarchyUrl: String = s"http://$ccpHost:$ccpPort/api/add-and-get-hierarchy-tree?name=$systemId-$instanceName-$processName"

          val addHierarchyUrlRequest = HttpRequest.newBuilder()
            .uri(addHierarchyUrl)
            .httpMethod(HttpMethod.POST)
            .header("Accept", "text/plain")
            .build()

          logInfo("add-and-get-hierarchy-tree : {}", addHierarchyUrl)

        response = CCPComponentInstallation.httpClient.execute(addHierarchyUrlRequest)
        var groupId = ""
        if (response.getStatus() == 200) {
          groupId = response.getResponseAsString

        }
        else {
          logError("Got error from CCP : {}", response.getResponseAsString)
          throw new IllegalStateException("Failed to add Hierarchy tree to CCP :\n" + response.getResponseAsString)
        }

        val installComponentUrl: String = s"http://$ccpHost:$ccpPort/api/add-new-component-raw?fqdn=$fqdn&installPath=$installPath&processName=$processName&parentId=$groupId&isNdsconsoleModule=$isNdsconsoleModule"
        logInfo("add-new-component-raw : {}", installComponentUrl)

          val installComponentUrlRequest = HttpRequest.newBuilder()
            .uri(installComponentUrl)
            .httpMethod(HttpMethod.POST)
            .header("Accept", "text/plain")
            .entity(s"$configSchema~~$properties" getBytes "UTF-8")
            .contentType("text/plain")
            .build()

        response = CCPComponentInstallation.httpClient.execute(installComponentUrlRequest)

        if (response.getStatus() != 200) {
          logError("Got Error from CCP : {}", response.getResponseAsString)
          throw new IllegalStateException("Failed to add new component to CCP. got STATUS: " + response.getStatus() + " :\n" + response.getResponseAsString)
        }
      }
    }
  }
}