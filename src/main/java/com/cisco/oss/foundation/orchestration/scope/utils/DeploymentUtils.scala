package com.cisco.oss.foundation.orchestration.scope.utils

import java.util.concurrent.{ThreadPoolExecutor, LinkedBlockingQueue}
import javax.annotation.Resource

import com.cisco.oss.foundation.orchestration.scope.{ScopeErrorMessages, ScopeConstants}
import com.cisco.oss.foundation.orchestration.scope.configuration.IComponentInstallation
import com.cisco.oss.foundation.orchestration.scope.dblayer.SCOPeDB
import com.cisco.oss.foundation.orchestration.scope.model._
import org.springframework.beans.factory.annotation.Autowired
import scala.collection.JavaConversions._
import scala.collection.SortedMap
import scala.collection.immutable.HashMap
import scala.concurrent.duration.Duration
import scala.concurrent._

import scala.concurrent.{duration, Await, Future, ExecutionContext}

/**
 * Created by igreenfi on 07/10/2014.
 */
object DeploymentUtils extends Slf4jLogger {
  implicit val executionContext = new FlowContextExecutor(ExecutionContext.fromExecutorService(new ThreadPoolExecutor(50, 500, 5L, java.util.concurrent.TimeUnit.MINUTES, new LinkedBlockingQueue[Runnable]())))

  @Autowired var scopedb: SCOPeDB = _
  @Autowired val vmUtils: VMUtils = null

  @Resource(name = "componentInstallationImpl") val componentInstallation: IComponentInstallation = null

  val puppetScriptName: String = "scope"

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

  private def extractModulesName(roleOption: Option[InstallationPart]): scala.collection.mutable.Set[String] = {
    roleOption match {
      case Some(role) => {
        role.puppet match {
          case Some(puppet) => {
            puppet.modulesName
          }
          case None => scala.collection.mutable.Set[String]()
        }
      }
      case None => scala.collection.mutable.Set[String]()
    }
  }

  private def updateMachineStatus(instance: Instance, machineName: String, status: String, detail: Option[String], modulesName: Option[scala.collection.mutable.Set[String]]) {
    scopedb.updateMachineStatus(instance.instanceId, machineName, status, modulesName)
  }

  def deployModules(deploymentModel: HashMap[String, HashMap[String, InstallationPart]], stepsMap: Map[String, InstallModules],vmDetails:List[ScopeNodeMetadata], product: Product, system: System, instance: Instance) = {
    // Start deploy.
    var result: Instance = instance
    val privateKey = instance.rsaKeyPair.getOrElse("private", throw new Exception(ScopeErrorMessages.NO_RSA + "for INSTANCE."))
    val sortedDeploymentModel = SortedMap[String, HashMap[String, InstallationPart]](deploymentModel.toArray: _*)
    sortedDeploymentModel.foreach {
      case (step, hostsMap) => {
        logInfo(s"Start deploying ${step}.")

        configureModulesPerStep(product, stepsMap.get(step).get, system, instance)
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
            val puppetRole = hostsMap.get(hostDetails.hostname)
            val modulesName = extractModulesName(puppetRole)
            updateMachineStatus(instance, hostDetails.hostname, s"Start $step", None, Some(modulesName))
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
                    updateMachineStatus(instance, details.hostname, s"Finished $step", None, None)
                    logInfo(s"Finished deploy VM: id ${details.id}, name ${details.hostname}, IP ${details.privateAddresses.head}")
                }

                puppetApplyFuture onFailure {
                  case throwable =>
                    logError("Provisioning failed", throwable)
                    result = updateInstanceStatus(instance, ScopeConstants.FAILED, Some(throwable.toString), None, None)
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
}
