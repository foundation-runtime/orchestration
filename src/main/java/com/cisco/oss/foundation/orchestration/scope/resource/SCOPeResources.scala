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

package com.cisco.oss.foundation.orchestration.scope.resource

import java.util.HashSet

import com.cisco.oss.foundation.orchestration.scope.model.{Instance, Product, System, _}
import com.cisco.oss.foundation.orchestration.scope.provision.model.ProductRepoInfo
import com.cisco.oss.foundation.orchestration.scope.utils._
import com.wordnik.swagger.annotations.Api
import org.apache.commons.lang.StringUtils
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import scala.collection.JavaConversions._
import scala.collection.immutable.Map

@Api(value = "systems", description = "systems rest api") // Swagger annotation
@Controller
@RequestMapping(Array[String]("/systems"))
class SystemsResources extends Slf4jLogger with BasicResource {

  //  @ExceptionHandler(Array(classOf[Exception]))
  //  @ResponseBody
  //  def handleException(e: Exception, response: HttpServletResponse): String = {
  //    //    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
  //    return e.getMessage();
  //  }

  @RequestMapping(method = Array[RequestMethod](RequestMethod.GET), produces = Array[String]("application/json"))
  @ResponseBody
  def getAllSystems(): java.util.List[System] = {

    ScopeUtils.time(logger, "getAllSystems-rest") {
      logInfo("returning all systems")
      val systems = scopedb.findAllSystems
      systems map (
        system => {
          system.copy(password = "****")
      })
    }

  }

  @RequestMapping(value = Array[String]("/{systemUserId}"), method = Array[RequestMethod](RequestMethod.POST))
  @ResponseBody
  def createSystem(@PathVariable("systemUserId") systemUserId: String, @RequestBody password: String) {

    ScopeUtils.time(logger, "createSystem-rest") {

      logInfo("creating system: {}", systemUserId)

      if (StringUtils.isBlank(password)) {
        logError("password cannot be null or empty")
        throw new PasswordMissing()
      }

      if (!scopedb.findSystem(systemUserId).isDefined) {
        scopedb.createSystem(System(systemUserId, password, None))
      } else {
        throw new SystemAlreadyExists()
      }

    }

  }

  @RequestMapping(value = Array[String]("/{systemId}/foundation"), method = Array[RequestMethod](RequestMethod.PUT), produces = Array[String]("application/json"), consumes = Array[String]("application/json"))
  @ResponseBody
  def createSystemFoundation(@PathVariable("systemId") systemId: String, @RequestBody instance: Instance): Instance = {
    logInfo("creating foundation for system: {}", systemId)
    val foundationProductName = ScopeUtils.configuration.getString("foundation.name")
    val foundationVersion = ScopeUtils.configuration.getString("foundation.version")
    super.instantiateProduct(instance, foundationProductName, foundationVersion, None)
  }

  @RequestMapping(value = Array[String]("/{systemId}"), method = Array[RequestMethod](RequestMethod.GET))
  @ResponseBody
  def getSystem(@PathVariable("systemId") systemId: String) = {

    ScopeUtils.time(logger, "getSystem-rest") {
      logInfo("getting system: {}", systemId)
      val system = scopedb.findSystem(systemId)
      system.getOrElse(throw new SystemNotFound).copy(password = "****")
    }
  }

  @RequestMapping(value = Array[String]("/{systemId}/instances"), method = Array[RequestMethod](RequestMethod.GET))
  @ResponseBody
  def getInstancesForSystem(@PathVariable("systemId") systemId: String) = {

    ScopeUtils.time(logger, "getInstancesForSystem-rest") {

      logInfo("getting instances for system: {}", systemId)

      val system = scopedb.findSystem(systemId).getOrElse(throw new SystemNotFound)

      logDebug("querying instances for system {}", systemId)

      val instances = scopedb.findInstanceBySystemId(system.systemId).getOrElse(new HashSet())

      logTrace("system instances are: {}", instances)

      instances.map((instance) => {
        instance.copy(rsaKeyPair = Map[String, String]())
      })
    }
  }

  @RequestMapping(value = Array[String]("/{systemId}/instances/{instanceId}"), method = Array[RequestMethod](RequestMethod.GET))
  @ResponseBody
  def getInstanceInfo(@PathVariable("systemId") systemId: String, @PathVariable("instanceId") instanceId: String): Instance = {

    ScopeUtils.time(logger, "getInstanceInfo-rest") {

      logInfo("getting instance info for system: {} with instanceId: {}", systemId, instanceId)
      val instance = scopedb.findInstance(instanceId).getOrElse(throw new InstanceNotFound)
      instance.copy(rsaKeyPair = Map[String, String]())
    }

  }

  private val domainPattern = "https?://([0-9a-zA-Z\\.\\-]+):?[0-9]*/.*".r

  @RequestMapping(value = Array[String]("/{systemId}/instances/{instanceId}"), method = Array[RequestMethod](RequestMethod.DELETE))
  @ResponseBody
  def deleteInstance(@PathVariable("systemId") systemUserId: String, @PathVariable("instanceId") instanceId: String) = {

    ScopeUtils.time(logger, "deleteInstance-rest") {
      val instance = scopedb.findInstance(instanceId).getOrElse(throw new InstanceNotFound)
      val system = scopedb.findSystem(systemUserId).getOrElse(throw new SystemNotFound)
      val deletable = instance.deletable.getOrElse(true)
      // undeletable flag used for UI layer only. REST call should delete instance despite the flag.
      if (!deletable) {
        logWarn("instance with Id: {} for system: {} will be deleted although it is marked as not deletable", instanceId, systemUserId)
      }


      system.foundation match {
        case Some(foundation) => {
          foundation.rsaKeyPair.get("private") match {
            case Some(key) => {
              val ccpServersDetails = foundation.machineIds.filter(set => set._2.hostname.contains("configurationServer"))

              val ccpServers = ccpServersDetails match {
                case addresses: Map[String, ScopeNodeMetadata] if (addresses.size > 0) => addresses.values.toList
                case _ => List()
              }
              ccpServers.map(
                server => super.deleteVmsFromConfigurationServer(server.copy(privateKey = Some(key)), instance.machineIds, systemUserId)
              )
            }
            case None => logWarn("Can't delete from /etc/hosts without private key of configuration server,")
          }

        }
        case None =>
      }

      instance.accessPoints.map(ap => {
        ap.url match {
          case domainPattern(dnsName) => super.deleteDnsRecord(dnsName)
          case _ =>
        }
      })



      val isFoundationInstance = ScopeUtils.configuration.getString("foundation.name").equals(instance.product.productName)
      if (isFoundationInstance) {
        // Delete foundation info from system
        scopedb.updateSystem(System(system.systemId, system.password, None))
      }
      super.deleteInstanceVMs(instance.machineIds, Some(instance))
      logInfo("deleting instance with Id: {} for system: {}", instanceId, systemUserId)
      scopedb.deleteInstance(systemUserId, instanceId)
    }

  }

  @RequestMapping(value = Array[String]("/{systemId}"), method = Array[RequestMethod](RequestMethod.DELETE))
  @ResponseBody
  def deleteSystem(@PathVariable("systemId") systemId: String) = {

    ScopeUtils.time(logger, "deleteSystem-rest") {
      logInfo("deleting system: {}", systemId)
      val system = scopedb.findSystem(systemId)
      system match {
        case Some(s) => {
          s.foundation match {
            case Some(instance) => {
              super.deleteInstanceVMs(instance.machineIds)
              logInfo("deleting foundation instance with Id: {} for system: {}", instance.instanceId, systemId)
              val instanceFromDB = scopedb.findInstance(instance.instanceId).getOrElse(null)
              if (instanceFromDB != null) {
                scopedb.deleteInstance(systemId, instance.instanceId)
              }
            }
            case None =>
          }
        }
        case None => throw new SystemNotFound
      }

      scopedb deleteSystem (systemId)
    }
  }

}

@Controller
@RequestMapping(value = Array[String]("/products"))
class ProductsResource extends Slf4jLogger with BasicResource {


  @RequestMapping(method = Array[RequestMethod](RequestMethod.GET), produces = Array[String]("application/json"))
  @ResponseBody
  def getAllProducts(): java.util.List[Product] = {

    ScopeUtils.time(logger, "getAllProducts-rest") {
      logInfo("returning all products")
      scopedb.findAllProducts
    }

  }

  @RequestMapping(value = Array[String]("/{productName}-{productVersion}/"), method = Array[RequestMethod](RequestMethod.PUT), consumes = Array[String]("application/json"))
  @ResponseStatus(HttpStatus.CREATED)
  def createProduct(@PathVariable("productName") productName: String, @PathVariable("productVersion") productVersion: String,
                    @RequestBody product: Product) {

    ScopeUtils.time(logger, "createProduct-rest") {
      logInfo("creating product {}-{}", productName, productVersion)
      scopedb.createProduct(product.copy(id = s"$productName-$productVersion"))
    }
  }

  @RequestMapping(value = Array[String]("/{productName}-{productVersion}"), method = Array[RequestMethod](RequestMethod.GET))
  @ResponseBody
  def getProductDetails(@PathVariable("productName") productName: String, @PathVariable("productVersion") productVersion: String): Product = {

    ScopeUtils.time(logger, "getProductDetails-rest") {
      logInfo("returning products details for {}-{}", productName, productVersion)
      scopedb.getProductDetails(productName, productVersion).getOrElse(throw new ProductNotFound())

    }

  }

  @RequestMapping(value = Array[String]("/{productName}-{productVersion}"), method = Array[RequestMethod](RequestMethod.DELETE))
  @ResponseStatus(HttpStatus.OK)
  def deleteProduct(@PathVariable("productName") productName: String, @PathVariable("productVersion") productVersion: String) {

    ScopeUtils.time(logger, "deleteProduct-rest") {
      logInfo("delete product {}-{}", productName, productVersion)
      scopedb.deleteProduct(productName, productVersion)
    }

  }

  @RequestMapping(value = Array[String]("/{productName}-{productVersion}/instantiate"), method = Array[RequestMethod](RequestMethod.POST), produces = Array[String]("application/json"), consumes = Array[String]("application/json"))
  @ResponseBody
  def instantiateProduct(@PathVariable("productName") productName: String, @PathVariable("productVersion") productVersion: String, @RequestBody instance: Instance): Instance = {

    ScopeUtils.time(logger, "instantiateProduct-rest") {

      super.instantiateProduct(instance, productName, productVersion, None)

    }

  }

  @RequestMapping(value = Array[String]("/{productName}-{productVersion}/instance/{instanceId}"), method = Array[RequestMethod](RequestMethod.PUT), produces = Array[String]("application/json"), consumes = Array[String]("application/json"))
  @ResponseBody
  def updateInstance(@PathVariable("productName") productName: String, @PathVariable("productVersion") productVersion: String, @PathVariable("instanceId") instanceId: String, @RequestBody updateInstanceData: UpdateInstanceData): Instance = {
    ScopeUtils.time(logger, "updateInstance-rest") {

      val instance: Instance = scopedb.findInstance(instanceId).getOrElse(throw new InstanceNotFound())
      val product: Product = scopedb.getProductDetails(productName, productVersion).getOrElse(throw new ProductNotFound())
      val system = scopedb.findSystem(instance.systemId).getOrElse(throw new SystemNotFound)

      scopedb.saveProductPatch(s"${productName}-${productVersion}", updateInstanceData)

      val repoUrl: String = updateInstanceData.updateUrl.getOrElse(product.repoUrl)
      val (deploymentModel, resources) = ModelUtils.processDeploymentModel(DeploymentModelCapture("1.0",
        None,
        InstallNodes(ScopeUtils.ScopeNodeMetadataList2NodeList(instance.machineIds.values)),
        false,
        false,
        updateInstanceData.installModules,
        ExposeAccessPoints(instance.accessPoints)), new ProductRepoInfo(repoUrl, updateInstanceData.patchName), puppetScriptName)

      loadResources(resources)
      deploymentUtils.deployModules(deploymentModel, updateInstanceData.installModules, instance.machineIds.values.toList, repoUrl, product, system, instance)
      instance
    }

  }


}