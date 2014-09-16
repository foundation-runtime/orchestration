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

import java.io.File
import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor}
import javax.annotation.Resource
import javax.servlet.http.HttpServletRequest

import com.cisco.oss.foundation.orchestration.scope.configuration.IComponentInstallation
import com.cisco.oss.foundation.orchestration.scope.dblayer.SCOPeDB
import com.cisco.oss.foundation.orchestration.scope.model._
import com.cisco.oss.foundation.orchestration.scope.provision.model.ProductRepoInfo
import com.cisco.oss.foundation.orchestration.scope.utils._
import com.cisco.oss.foundation.orchestration.scope.{ScopeConstants, ScopeErrorMessages}
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.jclouds.compute.domain.{ExecResponse, NodeMetadata}
import org.jclouds.scriptbuilder.ScriptBuilder
import org.jclouds.scriptbuilder.domain.OsFamily
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.ssh.SshKeys
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.JavaConversions._
import scala.collection.immutable.{HashMap, Map, SortedMap}
import scala.collection.mutable
import scala.concurrent._
import scala.concurrent.duration.Duration


trait BasicResource extends Slf4jLogger {

  implicit val executionContext = new FlowContextExecutor(ExecutionContext.fromExecutorService(new ThreadPoolExecutor(50, 500, 5L, java.util.concurrent.TimeUnit.MINUTES, new LinkedBlockingQueue[Runnable]())))

  @Autowired var scopedb: SCOPeDB = _

  @Autowired val vmUtils: VMUtils = null

  @Resource(name = "componentInstallationImpl") val componentInstallation: IComponentInstallation = null

  private val dnsPattern = ".*<dns-(.*)>.*".r

  protected val STARTING: String = "STARTING"

  def instantiateProduct(inst: Instance, productName: String, productVersion: String, uuid: Option[String]): Instance = {
    var instance = inst
    val systemId = instance.systemId

    logInfo(s"Instantiating product ${productName}-${productVersion} for system ${systemId}")
    instance = updateInstanceStatusDetails(instance, "Instantiating started")

    if (StringUtils.isBlank(systemId))
      throw new SystemIdMissing

    val system = scopedb.findSystem(systemId).getOrElse(throw new SystemNotFound)
    instance = updateInstanceStatusDetails(instance, "System found")

    var product = scopedb.getProductDetails(productName, productVersion) match {
      case Some(p) => p
      case None => throw new ProductNotFound
    }

    val currentUUID: String = uuid.getOrElse(UUID.getNextUUID())

    product = product.copy(productOptions = instance.product.productOptions)
    val newInstance = Instance(currentUUID, systemId, instance.instanceName, Some(STARTING), None, product, Map(), List[AccessPoint](), instance.deletable, SshKeys.generate().toMap)
    scopedb.createInstance(newInstance)
    val foundationEnabled = ScopeUtils.configuration.getBoolean(ScopeConstants.FOUNDATION_ENABLED, false)
    val foundationProductName = ScopeUtils.configuration.getString(ScopeConstants.FOUNDATION_NAME)
    val foundationBuildRequest = foundationProductName.equals(product.productName)

    foundationEnabled match {
      case true => {
        system.foundation match {
          case Some(foundation) => {
            if (foundationBuildRequest) {
              throw new SystemAlreadyExists()
            } else if (!foundation.status.getOrElse(STARTING).equalsIgnoreCase("started")) {
              instance = updateInstanceStatusDetails(newInstance, "Foundation instance exists but not ready")
              throw new SystemFoundationProductNotReady
            } else
              instance = updateInstanceStatusDetails(newInstance, "Foundation Instance found")
            product = deployProduct(product, newInstance, system)
          }
          case None => {
            if (foundationBuildRequest) {
              product = deployProduct(product, newInstance, system)
              newInstance
            } else {
              instance = updateInstanceStatusDetails(newInstance, "Foundation instance doesn't exist - Starting creation")
              val foundationVersion = ScopeUtils.configuration.getString(ScopeConstants.FOUNDATION_VERSION)
              logInfo(s"Start creating {} product for system : {}", foundationProductName, systemId)
              val scopeFoundationProduct = scopedb.getProductDetails(foundationProductName, foundationVersion)
              val foundationInstanceUuid: String = UUID.getNextUUID()
              val scopeFoundationInstance = Instance(foundationInstanceUuid,
                system.systemId,
                system.systemId,
                Some(STARTING),
                Some("Foundation product for all the system."),
                scopeFoundationProduct.get,
                Map(),
                List(),
                Some(false),
                SshKeys.generate().toMap)
              scopedb.createInstance(scopeFoundationInstance)
              scopedb.updateSystem(System(system.systemId, system.password, Some(scopeFoundationInstance)))
              deployProduct(scopeFoundationProduct.get, scopeFoundationInstance, system)
              val foundationProductFuture = future {
                var status = STARTING
                var foundationInstance: Instance = null
                while (status.equals(STARTING)) {
                  logTrace("Waiting for {} product for system {}", productName, system.systemId)
                  Thread.sleep(30 * 1000)
                  foundationInstance = scopedb.findInstance(foundationInstanceUuid).getOrElse(Instance("", "", "", Some(STARTING), None, scopeFoundationProduct.get, Map(), List(), None))
                  status = foundationInstance.status.getOrElse(STARTING)
                }
                if (status.equals(ScopeConstants.STARTED)) {
                  scopedb.updateSystem(system.copy(foundation = Some(foundationInstance)))
                  Some(foundationInstance)
                }
                else
                  None
              }

              foundationProductFuture onSuccess {
                case Some(foundationInstance) => {
                  logInfo(s"Finished creating ScopeFoundation instance for system : ${systemId}")
                  instance = updateInstanceStatusDetails(newInstance, "Finished creating ScopeFoundation instance")
                  if (!foundationBuildRequest) {
                    val updatedSystem: System = System(system.systemId, system.password, Some(foundationInstance))
                    product = deployProduct(product, newInstance, updatedSystem)
                  }
                }
                case None => {
                  logError("Error creating ScopeFoundation instance for system : {}", systemId)
                  instance = updateInstanceStatus(newInstance, ScopeConstants.FAILED, Some(s"Error creating ScopeFoundation instance for system $systemId"), None, None)
                  throw new SystemFoundationProductNotReady
                }

              }
              foundationProductFuture onFailure {
                case throwable => {
                  logError("Failed creating ScopeFoundation product for system : {}", systemId)
                  instance = updateInstanceStatus(newInstance, ScopeConstants.FAILED, Some(s"Error whike creating ScopeFoundation instance for system ${systemId}: ${throwable.getMessage}"), None, None)
                  logError(throwable.getMessage, throwable)
                }
              }
            }
          }
        }
      }
      case false =>
    }


    //strip the product options from the response
    val retProd = product.copy(productOptions = List())
    Instance(currentUUID, systemId, instance.instanceName, Some(STARTING), None, retProd, Map(), List[AccessPoint](), None)
  }

  def loadResources(resources: Option[scala.List[FileResource]]) {
    resources match {
      case Some(rs) => {
        val api = ResourcesUtilsFactory.instance()
        rs.foreach(r => {
          val fullFileName: String = s"${r.path}/${r.name}"
          api.loadFile(fullFileName)
          FileUtils.deleteQuietly(new File(fullFileName))
        })
      }
      case None =>
    }

  }

  def deployProduct(product: Product, inst: Instance, system: System): Product = {

    //configureModules(product, model, system, instance)
    val newProduct = product.copy(productOptions = inst.product.productOptions)

    val deployInstanceFuture = future {
      val instance = updateInstanceStatusDetails(inst, "Calling materializer to define deployment capture")
      val deploymentModelString: String = ModelUtils.materialize(product, instance)
      val models: List[DeploymentModelCapture] = ModelUtils.StringToPojo(deploymentModelString)
      runDeployment(models, instance, product, system, models.size)
    }

    deployInstanceFuture onFailure {
      case throwable => {
        val msg = s"Error while creating instance of product ${product.id} for system ${system.systemId}: ${throwable.getMessage}"
        logError(msg)
        updateInstanceStatus(inst, "failed", Some(msg), None, None)
        logError(throwable.getMessage, throwable)
      }
    }
    newProduct
  }

  private def runDeployment(models: List[DeploymentModelCapture], instance: Instance, product: Product, system: System, numberOfCaptures: Int) {

    if (models.size == 0)
      return
    logInfo(s"Start deployment number ${numberOfCaptures - models.size}")
    logInfo(s"Instance: ${instance.copy(rsaKeyPair = Map())}")
    val model = models.head
    val (deploymentModel, resources) = ModelUtils.processDeploymentModel(model, new ProductRepoInfo(product.repoUrl), puppetScriptName)

    loadResources(resources)

    val newInstance = instance.copy(status = Some(STARTING), accessPoints = instance.accessPoints ::: model.exposeAccessPoints.accessPoints)

    scopedb.updateInstance(newInstance)

    val f = createAndDeployVMs(system, newInstance, deploymentModel, model.installNodes.nodes, model.installModules)

    f.onSuccess {
      case inst: Instance => runDeployment(models.tail, inst, product, system, numberOfCaptures)
    }

    f.onFailure {
      case e: Exception => {
        validateInstanceStatus(newInstance)
        throw e
      }
    }
  }

  private def configureModulesPerStep(product: Product, modules: InstallModules, system: System, instance: Instance) {
    if (ScopeUtils.configuration.getBoolean(ScopeConstants.FOUNDATION_ENABLED, false)) {
      logInfo("Start inserting components into Configuration Server.")
      val configurationServersDetails = system.foundation.getOrElse(instance).machineIds.filter(set => set._2.hostname.contains("configurationServer"))
      // TODO: What to do with 2 configuration servers?
      val configurationServer = configurationServersDetails match {
        case a: Map[String, ScopeNodeMetadata] if a.size > 0 => Option(a.head._2)
        case _ => throw new IllegalStateException("Can't find configuration server address. system : " + system)
      }

      logDebug("Install component into configuration server : {}", configurationServer)
      modules.modules map {
        module => {
          module match {
            case m: PuppetModule => componentInstallation.prepare(product, instance, m, configurationServer.get)
            case m: PluginModule =>
          }
        }
      }
    }
  }


  val puppetScriptName: String = "scope"

  private def createAndDeployVMs(system: System, inst: Instance, deploymentModel: HashMap[String, HashMap[String, InstallationPart]], nodes: List[Node], stepsMap: Map[String, InstallModules]): Future[Instance] = {
    val result = promise[Instance]
    var instance = inst
    val rsaKeyPair = instance.rsaKeyPair match {
      case rsa: Map[String, String] if rsa.size > 0 => rsa
      case _ => throw new Exception("No RSA keyPair provided.")
    }
    val futuresList = nodes.map {
      node => {
        instance = updateInstanceStatusDetails(instance, s"Creating machine ${node.name}")
        vmUtils.createVM(system.systemId, instance.instanceName, instance.product.productName, node, rsaKeyPair)
      }
    }

    val createVmsFuture = Future.sequence(futuresList)
    createVmsFuture onSuccess {
      case vmDetails => {
        val instanceFromDB = scopedb.getInstanceInfo(instance)
        var newAccessPoints = List[AccessPoint]()
        var vmsIds = Map[String, ScopeNodeMetadata]()
        val tempAccessPoints = instanceFromDB.accessPoints
        vmDetails.foreach {
          case nodeMetaData => {
            logInfo(s"VM creation done for ${nodeMetaData.hostname}")
            val ipAddress = nodeMetaData.privateAddresses match {
              case addresses: Set[String] if (addresses.size > 0) => addresses.head
              case _ => {
                val scopeNodeMetadata: ScopeNodeMetadata = vmUtils.vmMetaData(nodeMetaData.id)
                scopeNodeMetadata.privateAddresses.head
              }
            }

            instance = updateInstanceStatusDetails(instance, s"VM creation done for ${nodeMetaData.hostname} IP is: ${ipAddress}")
            for (tempAccessPoint <- tempAccessPoints) {
              val name = tempAccessPoint.name
              var url = tempAccessPoint.url.toLowerCase
              val pattern: String = s"<${nodeMetaData.hostname}>"
              if (url.contains(pattern)) {
                url = url.replace(pattern, ipAddress)
                newAccessPoints = AccessPoint(name, url) :: newAccessPoints
                instance = updateInstanceStatusDetails(instance, s"Added access point: $newAccessPoints")
              } else {
                if (!newAccessPoints.exists(ap => ap.name.equals(tempAccessPoint.name)))
                  newAccessPoints = tempAccessPoint.copy() :: newAccessPoints
              }

            }
            vmsIds += Tuple2(nodeMetaData.hostname, nodeMetaData)
          }
        }

        tempAccessPoints.foreach {
          case accessPoint => {
            val name = accessPoint.name
            var url = accessPoint.url
            url match {
              case dnsPattern(dnsService) => {
                url = url.replace(s"<dns-$dnsService>", NetworkUtils.createDnsName(dnsService, instance.instanceName, instance.systemId))
                newAccessPoints = AccessPoint(name, url) :: newAccessPoints
                instance = updateInstanceStatusDetails(instance, s"Added access point: $newAccessPoints")
              }
              case _ =>
            }
          }
        }

        newAccessPoints = filterAccessPoints(newAccessPoints)

        val product = instanceFromDB.product
        val vms = ScopeUtils.unionMap(instanceFromDB.machineIds, vmsIds)
        logDebug("VM list: {}", vms)
        val newInst = instanceFromDB.copy(product = product, machineIds = vms, accessPoints = newAccessPoints, rsaKeyPair = rsaKeyPair)

        scopedb.updateInstance(newInst)
        var newSystem = system
        if (ScopeUtils.configuration.getString(ScopeConstants.FOUNDATION_NAME).equals(instance.product.productName)) {
          newSystem = System(system.systemId, system.password, Some(newInst))
          scopedb.updateSystem(newSystem)
        }

        val configurationServersDetails = newSystem.foundation match {
          case Some(f) => f.machineIds.filter(set => set._2.hostname.contains("configurationServer"))
          case None => {
            if (ScopeUtils.configuration.getString(ScopeConstants.FOUNDATION_NAME).equals(instance.product.productName)) {
              newInst.machineIds.filter(set => set._2.hostname.contains("configurationServer"))
            } else {
              Map[String, ScopeNodeMetadata]()
            }
          }
        }

        logDebug("Found configuration servers : {}", configurationServersDetails.keys)

        val configurationServers = configurationServersDetails match {
          case addresses: Map[String, ScopeNodeMetadata] if addresses.size > 0 => addresses.values.toList
          case _ => List()
        }

        val vmHostsScript = NetworkUtils.createHostsFileScript(configurationServers, vmDetails)
        val groupName = newSystem.systemId + "-" + instance.instanceName
        val privateKey = instance.rsaKeyPair.getOrElse("private", throw new Exception(ScopeErrorMessages.NO_RSA + "for INSTANCE."))

        if (!ScopeUtils.configuration.getBoolean(ScopeConstants.CLOUD_ENV_HAS_DNS, false)) {
          logInfo(s"Configuring /etc/hosts for Group $groupName")
          val scriptResults = vmUtils.runScriptOnMatchingNodes(vmHostsScript.render(OsFamily.UNIX), "edit_hosts", Some(groupName), None, Some(List(instance.product.productName)), privateKey)
          if (!checkRunScriptResults(scriptResults.toMap, instance.instanceId)) {
            validateInstanceStatus(instance)
            result.failure(new IllegalStateException("Provisioning failed."))
            return result.future
          }

          val foundationProductName = ScopeUtils.configuration.getString(ScopeConstants.FOUNDATION_NAME)
          if (!foundationProductName.equals(instance.product.productName)) {
            val foundationGroup = newSystem.systemId + "-" + newSystem.systemId
            logInfo("Configuring /etc/hosts for configuration servers.")
            val productVmScript = NetworkUtils.createAddRowsToHostsFileScript(vmDetails)
            if (ScopeUtils.configuration.getBoolean(ScopeConstants.FOUNDATION_ENABLED, false)) {
              val foundationPrivateKey = newSystem.foundation.getOrElse(newInst).rsaKeyPair.getOrElse("private", throw new Exception(ScopeErrorMessages.NO_RSA + "for FOUNDATION."))
              vmUtils.runScriptOnMatchingNodes(productVmScript.render(OsFamily.UNIX), s"edit_hosts_${java.lang.System.currentTimeMillis()}", Some(foundationGroup), Some("configurationServer"), Some(List(foundationProductName)), foundationPrivateKey)
            }
          }
        }

        // Start deploy.
        val sortedDeploymentModel = SortedMap[String, HashMap[String, InstallationPart]](deploymentModel.toArray: _*)
        sortedDeploymentModel.foreach {
          case (step, hostsMap) => {
            logInfo(s"Start deploying ${step}.")

            configureModulesPerStep(product, stepsMap.get(step).get, newSystem, newInst)
            val currentHosts =
              for (
                host <- hostsMap.keys;
                hostDetails <- vmDetails
                if hostDetails.hostname.endsWith(host))
              yield hostDetails

            if (currentHosts.size == 0)
              logWarn(s"Could NOT find any hosts for modules from $step")

            val stepFutureList = currentHosts map {
              case hostDetails => {
                logInfo(s"Start deploy VM : { id: ${hostDetails.id}, name: ${hostDetails.hostname}, IP: ${hostDetails.privateAddresses.head} }")
                updateMachineStatus(instance, hostDetails.hostname, s"Start $step", None)
                val puppetRole = hostsMap.get(hostDetails.hostname)
                puppetRole match {
                  case Some(role) => {
                    val puppetApplyFuture = future {
                      Thread.sleep(1000)
                      try {
                        vmUtils.deployVM(hostDetails.copy(privateKey = Some(privateKey)), puppetScriptName, instance.product.repoUrl, instance.product.productName, instance.product.productVersion, role)
                      } catch {
                        case e: Exception => {
                          logError(s"Failed to deploy VM ${hostDetails.hostname}, error: ${e.toString}", e)
                          throw e
                        }
                      }

                      Some(hostDetails)
                    }

                    puppetApplyFuture onSuccess {
                      case Some(details) =>
                        updateMachineStatus(instance, details.hostname, s"Finished $step", None)
                        logInfo(s"Finished deploy VM: id ${details.id}, name ${details.hostname}, IP ${details.privateAddresses.head}")
                    }

                    puppetApplyFuture onFailure {
                      case throwable =>
                        logError("Provisioning failed", throwable)
                        instance = updateInstanceStatus(newInst, ScopeConstants.FAILED, Some(throwable.toString), None, None)
                    }
                    puppetApplyFuture
                  }
                  case None => future(None)
                }
              }
            }

            val stepFuture = Future.sequence(stepFutureList)

            Await.result(stepFuture, Duration.create(1, duration.HOURS))
            Thread.sleep(1000)
          }
        }
        if (validateInstanceStatus(newInst))
          result.success(newInst)
        else
          result.failure(new IllegalStateException("Provisioning failed."))
      }
    }

    createVmsFuture onFailure {
      case throwable => {
        logError("Create vm or provisioning failed", throwable)
        instance = updateInstanceStatus(instance, ScopeConstants.FAILED, Some(throwable.toString), None, None)
      }
    }
    return result.future
  }

  val urlPattern = "^.*(<.*>).*$".r

  private def filterAccessPoints(accessPoints: List[AccessPoint]): List[AccessPoint] = {
    accessPoints.filter(ac => ac.url match {
      case urlPattern(name) => false
      case _ => true
    })
  }

  /**
   * Helper method to retrieve the endpoint from the URL of the incoming servlet request. E.g., given a
   * servlet request send to http://foo:65535/context/a/b/c?x=y, we return http://foo:65535/context.
   * @param request the servlet request.
   * @return the endpoint.
   */
  protected def getEndpoint(request: HttpServletRequest): String = {
    var endpoint: String = request.getRequestURL.toString.replace(request.getRequestURI, "")
    endpoint += request.getContextPath
    endpoint
  }

  protected def checkRunScriptResults(scriptResults: Map[_ <: NodeMetadata, ExecResponse], instanceId: String): Boolean = {
    scriptResults.foreach {
      case (node, execResponse) => {
        execResponse.getExitStatus match {
          case 0 =>
          case status => {
            logError(s"stderr : ${execResponse.getError}")
            logError(s"stdout : ${execResponse.getOutput}")
            scopedb.updateMachineStatus(instanceId, vmUtils.getNameFromNodeMetadata(node), "FAILED")
            return false
          }
        }
      }
    }
    true
  }


  private def updateMachineStatus(instance: Instance, machineName: String, status: String, detail: Option[String]) {
    scopedb.updateMachineStatus(instance.instanceId, machineName, status)
  }

  private def updateInstanceStatus(instance: Instance, status: String, detail: Option[String], ip: Option[String], hostname: Option[String]): Instance = {

    // get updated instance, as machine IDs may have changed 
    val instanceFromDB = scopedb.findInstance(instance.instanceId).getOrElse(instance)
    val details = appendDetail(instanceFromDB, detail)
    if (status.equals(ScopeConstants.STARTED)) {
      val tempAccessPoints = instance.accessPoints
      var newAccessPoints = List[AccessPoint]()
      val truncatedHostname = hostname.getOrElse("").substring(hostname.getOrElse("").lastIndexOf('-') + 1)
      for (tempAccessPoint <- tempAccessPoints) {
        val name = tempAccessPoint.name
        var url = tempAccessPoint.url
        val pattern: String = s"<${truncatedHostname}>"
        if (url.contains(pattern))
          url = url.replace(pattern, ip.getOrElse(""))
        newAccessPoints = AccessPoint(name, url) :: newAccessPoints
      }

      val product = instance.product
      val newInst = instance.copy(status = Some(status), details = details, machineIds = ScopeUtils.unionMap(instance.machineIds, instanceFromDB.machineIds), accessPoints = newAccessPoints)
      scopedb.updateInstance(newInst)
      newInst
    } else {
      val newInst = instance.copy(status = Some(status), machineIds = ScopeUtils.unionMap(instance.machineIds, instanceFromDB.machineIds))
      scopedb.updateInstance(newInst)
      newInst
    }

  }

  private def updateInstanceStatusDetails(instance: Instance, detail: String): Instance = {
    val instanceFromDB = scopedb.findInstance(instance.instanceId).getOrElse(instance)
    val details = appendDetail(instanceFromDB, Some(detail))
    val newInst = instanceFromDB.copy(details = details)
    scopedb.updateInstance(newInst)
    newInst
  }

  private def appendDetail(instance: Instance, detail: Option[String]) = {
    val details =
      detail match {
        case None => instance.details
        case Some(value) => {
          instance.details match {
            case Some(oldValue) => Some(s"$oldValue\n$value")
            case None => Some(value)
          }
        }
      }
    details
  }

  private def validateInstanceStatus(instance: Instance) = {
    var status = true
    scopedb.findInstance(instance.instanceId) match {
      case Some(inst) => {
        inst.machineIds.foreach {
          case (key, value) => {
            status = status && value.provisionStatus.startsWith("Finished ")
          }
        }
        if (status) {
          scopedb.updateInstance(inst.copy(status = Some(ScopeConstants.STARTED)))
          updateSystemIfNeeded(instance)
        } else
          scopedb.updateInstance(inst.copy(status = Some(ScopeConstants.FAILED)))

      }
      case None =>
    }
    status
  }

  def updateSystemIfNeeded(instance: Instance) = {
    val foundationProductName = ScopeUtils.configuration.getString("foundation.name")
    val isFoundationInstance = foundationProductName.equals(instance.product.productName)
    if (isFoundationInstance) {
      val system = scopedb.findSystem(instance.systemId).getOrElse(throw new SystemNotFound)
      val fullInstance = scopedb.findInstance(instance.instanceId).getOrElse(instance)
      scopedb.updateSystem(system.copy(foundation = Some(fullInstance)))
    }
  }


  def deleteVmsFromConfigurationServer(configurationServer: ScopeNodeMetadata, machineIds: Map[String, ScopeNodeMetadata], systemUserId: String) = {
    machineIds.foreach {
      case (key, value) =>
        val name = vmUtils.addDomainName(value.hostname, systemUserId)

        this.componentInstallation.delete(configurationServer, name)
        val script = new ScriptBuilder().addStatement(exec(s"sed -i '/$name/d' /etc/hosts"))
        try {
          vmUtils.runScriptOnNode(script.render(OsFamily.UNIX), s"delete_from_hosts${java.lang.System.currentTimeMillis()}", configurationServer, configurationServer.privateKey.getOrElse(throw new IllegalArgumentException("missing rsa private key for configuration server.")))
        } catch {
          case e: Exception => logError("Delete VM: Removing hostname from Configuration server failed - Configuration server host not accessible")
        }

    }
  }

  def deleteDnsRecord(dnsName: String) {
    DnsUtilsFactory.instance().deleteDns(dnsName)
  }

  def deleteInstanceVMs(machineIds: Map[String, ScopeNodeMetadata], instance: Option[Instance] = None) = {
    machineIds.values match {
      case vals: Iterable[ScopeNodeMetadata] if vals.size > 0 => {
        vals.foreach((hostDetails) => {
          logInfo("Deleting vm: {} id: {} ip: {}", hostDetails.hostname, hostDetails.id, hostDetails.privateAddresses.head)
          vmUtils.deleteVM(hostDetails)
        })
      }
      case _ => {
        instance match {
          case Some(ins) => {
            val tags = Set(ins.instanceId,ins.product.productName,ins.systemId)
            val nodesByTags: mutable.Set[ScopeNodeMetadata] = vmUtils.listNodesByTags(tags)
            nodesByTags.foreach((hostDetails) => {
              logInfo("Deleting vm: {} id: {}", hostDetails.hostname, hostDetails.id)
              vmUtils.deleteVM(hostDetails)
            })
          }
          case None =>
        }
      }
    }
  }
}
