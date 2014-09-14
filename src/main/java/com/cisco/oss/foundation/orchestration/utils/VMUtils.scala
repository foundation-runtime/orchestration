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

import com.cisco.oss.foundation.configuration.ConfigUtil
import com.cisco.oss.foundation.orchestration.model.{InstallationPart, Node, ScopeNodeMetadata}
import com.cisco.oss.foundation.orchestration.scripting.ScalaScriptEngineWrapper
import com.cisco.oss.foundation.orchestration.provision.exception.ScopeProvisionException
import com.cisco.oss.foundation.orchestration.provision.model.{ProductRepoInfo, RoleInfo}
import com.google.common.base.Predicate
import org.apache.commons.net.util.SubnetUtils
import org.jclouds.compute.RunNodesException
import org.jclouds.compute.domain.{ComputeMetadata, ComputeType, NodeMetadata}
import org.jclouds.compute.options.TemplateOptions.Builder._
import org.jclouds.domain.LoginCredentials
import org.jclouds.scriptbuilder.domain.OsFamily
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.scriptbuilder.statements.ssh.{AuthorizeRSAPublicKeys, InstallRSAPrivateKey}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import scala.collection.JavaConversions._
import scala.collection.immutable.Map
import scala.concurrent.{ExecutionContext, future}
import scala.io.Source


object VMUtils {
  val cloudProvider = ScopeUtils.configuration.getString("cloud.provider")

  val imageVersion: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.image.version")
  val location: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.location")

  val imageScriptDir: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.image.directory","/opt/cisco/scope/scripts")
  val defaultImageId: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.image.id")
  val defaultImageScript: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.image.id.script", "")

  val networkAddress: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.privateNetwork.networkAddress")
  val netmask: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.privateNetwork.netmask")
  val gateway: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.privateNetwork.gateway")
  val privateNetworkId: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.privateNetwork.id")
  val publicNetworkId: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.publicNetwork.id")
  val baseRepoUrl: String = ScopeUtils.configuration.getString("baseRepoUrl")

  val privateSubnets = {
    var subnets = List[Tuple2[SubnetUtils,String]]()
    val keys = ConfigUtil.parseComplexArrayStructure(s"cloud.provider.$cloudProvider.privateNetwork")
    keys.foreach{
      case (key, value) => {
        val networkAddress = value.get("networkAddress")
        val netmask = value.get("netmask")
        val gateway = value.get("gateway")
        subnets = (new SubnetUtils(networkAddress, netmask), gateway) :: subnets
      }
    }

    subnets
  }

//  def subnetUtils = new SubnetUtils(VMUtils.networkAddress, VMUtils.netmask)

  def getDefaultSecurityGroups() = {
    val keys = ScopeUtils.configuration.getKeys(s"cloud.provider.$cloudProvider.defaultSecurityGroups")
    var defaults = List[String]()
    keys.foreach(key => {
      val name = ScopeUtils.configuration.getString(key)
      defaults = name :: defaults
    })
    defaults
  }

}

@Component
class VMUtils extends Slf4jLogger {
  @Autowired
  val engine: ScalaScriptEngineWrapper = null

  private val computeServiceContext = JcloudsFactory.computeServiceContext()


  def createVmTemplate(node: Node, group: String, rsaKeyPair: Map[String, String], tagsList: List[String] = List(), defaultSecurityGroup: List[String]) = {
    var tags = tagsList
    var networks = List[String]()
    var openPorts = List[String]()
    var hasPublicIP = false
    node.network.foreach {
      case net => {
        net.networkId match {
          case Some(networkId) => {
            networks = networkId :: networks
            if (net.nicType.equalsIgnoreCase("public")) {
              openPorts = net.openPorts.getOrElse(openPorts)
              hasPublicIP = true
              tags = "Public IP" :: tags
              net.dnsServices match {
                case Some(services) => tags = services ::: tags
                case None =>
              }
            }
          }
          case None => {
            net.nicType.toLowerCase match {
              case "public" => {
                openPorts = net.openPorts.getOrElse(openPorts)
                hasPublicIP = true
                tags = "Public IP" :: tags
                net.dnsServices match {
                  case Some(services) => tags = services ::: tags
                  case None =>
                }
                networks = VMUtils.publicNetworkId :: networks
              }
              case _ => {
                networks = VMUtils.privateNetworkId :: networks
              }
            }

          }
        }
      }
    }

//    val vgNames = List("vg_root", "vg_opt_nds", "vg_log")
//
//    node.nodeType match {
//      case "app" => {
//        val size = node.
//      }
//      case "db" =>
//    }
//
//
//    val mountDisks = new MapDiskStatements()
    //val script = generateInitScript(networkAddress, gateway, baseRepoMachine, imageVersion, hasPublicIP, openPorts)

    val (imageId, imageScript) = node.image match {
      case Some(img) => {
        val id: String = ScopeUtils.configuration.getString(s"cloud.provider.${VMUtils.cloudProvider}.image.id.$img")
        val script: String = ScopeUtils.configuration.getString(s"cloud.provider.${VMUtils.cloudProvider}.image.id.$img.script", "")
        (id, script)
      }
      case None => (VMUtils.defaultImageId, VMUtils.defaultImageScript)
    }

    def scriptString: Option[String] = {
      if (node.postConfiguration){
        val bootstrapStatements: BootstrapStatements = new BootstrapStatements(VMUtils.privateSubnets, VMUtils.baseRepoUrl, VMUtils.imageVersion, hasPublicIP, node.name, openPorts, VMUtils.cloudProvider)
        val script = VMUtils.cloudProvider match {
          case "vsphere" => {
            val shellScriptLoader: ShellScriptLoader = new ShellScriptLoader(VMUtils.imageScriptDir, imageScript)
            shellScriptLoader.addStatement(bootstrapStatements)
            shellScriptLoader
          }
          case _ => bootstrapStatements
        }

        script.addStatement(exec("sudo -s"))
        script.addStatement(new AuthorizeRSAPublicKeys(Set(rsaKeyPair.get("public").get)))
        script.addStatement(new InstallRSAPrivateKey(rsaKeyPair.get("private").get))
        Some(script.render(OsFamily.UNIX))
      } else {
        None
      }
    }

    logTrace(s"bootstrap script: \n$scriptString")

    val fullImageId = getFullImageId(imageId)
    JcloudsFactory.templateBuilder(fullImageId, VMUtils.location,
      node.minRam, node.minDisk, node.minCores, node.arch,
      tags, networks,
      node.name, group,
      scriptString, node.flpImage, node.iso, node.postConfiguration, node.folder)
  }

  def getFullImageId(imageIdSuffix: String): String = {
    val images = computeServiceContext.getComputeService.listImages()
    images.foreach(image => {
      if (image.getId.endsWith(imageIdSuffix))
        return image.getId
    })
    imageIdSuffix
  }

  def vmMetaData(id: String) = {
    val nodeMetadata: NodeMetadata = computeServiceContext.getComputeService.getNodeMetadata(id)
    JcloudsNodeMetaDataToScopeNodeMetaData(nodeMetadata, "STARTED")
  }

  def runScriptOnMatchingNodes(runScript: String, scriptName: String, groupName: Option[String], partialHostname: Option[String] = None, tagsToSearch: Option[List[String]] = None, privateKey: String) = {
    val filter = new Predicate[NodeMetadata] {
      def apply(nodeMetaData: NodeMetadata): Boolean = {
        // FIXME: AWS doesn't return groups
        val isInGroup = getGroupFromNodeMetadata(nodeMetaData) match {
          case group: String => {
            group.equalsIgnoreCase(groupName.getOrElse(throw new IllegalArgumentException("Group name could not be empty.")))
          }
          case _ => false
        }

        val hasTags = tagsToSearch match {
          case Some(tags) => nodeMetaData.getTags.containsAll(tags)
          case None => true
        }

        val isNameContains = partialHostname match {
          case Some(name) => {
        	  getNameFromNodeMetadata(nodeMetaData) != null && getNameFromNodeMetadata(nodeMetaData).contains(name)
          }
          case None => true
        }

        isInGroup && hasTags && isNameContains
      }
    }
    computeServiceContext.getComputeService.runScriptOnNodesMatching(filter,
      runScript,
      overrideLoginCredentials(LoginCredentials.builder()
        .user("root")
        .privateKey(privateKey).build()).nameTask(scriptName))
  }

  def runScriptOnNode(runScript: String, scriptName: String, nodeMetaData: ScopeNodeMetadata, privateKey: String) = {
    computeServiceContext.getComputeService.runScriptOnNode(nodeMetaData.id,
      runScript,
      overrideLoginCredentials(LoginCredentials.builder()
        .user("root")
        .privateKey(privateKey).build()).nameTask(scriptName))
  }

  def getServerListByGroupAndTags(group: String, tags: List[String] = List()) = {
    val filter = new Predicate[ComputeMetadata] {
      def apply(computeMetadata: ComputeMetadata): Boolean = {
        computeMetadata.getType.equals(ComputeType.NODE)
      }
    }
    val nodes = computeServiceContext.getComputeService.listNodesDetailsMatching(filter)

    nodes.filter(node => getGroupFromNodeMetadata(node).equals(group) && node.getTags.containsAll(tags))

  }

  def createVM(systemId: String, instanceName: String, productName: String, node: Node, rsaKeyPair: Map[String, String])(implicit ec:ExecutionContext) = {
    val groupName = s"$systemId-$instanceName".toLowerCase
    val createVmFuture = future {
      val template = createVmTemplate(node.copy(name = node.name.toLowerCase), groupName, rsaKeyPair, List(systemId, instanceName.toLowerCase, productName), VMUtils.getDefaultSecurityGroups)
      try {
        val nodeMetaData = computeServiceContext.getComputeService.createNodesInGroup(groupName, 1, template)
        val nodeMetaDataHead = waitForNodeDataToBeSet(nodeMetaData.head.getId())
        node.network.foreach {
          case (nic) => {
            if (nic.nicType.equalsIgnoreCase("internal")) {
              logTrace("Found internal NIC: {}", nic)
            }
            if (nic.nicType.equalsIgnoreCase("public")) {
              nic.dnsServices match {
                case Some(roles) => {
                  roles.foreach(role => {
                    DnsUtilsFactory.instance.createDomain(systemId, instanceName, role)
                    DnsUtilsFactory.instance.createARecord(systemId, instanceName, role, nodeMetaData.head.getPublicAddresses.head)
                  })
                }
                case None => {
                  logTrace("No DNS service found for node {}", node.name)
                }
              }
            }
          }
        }

        val allAddress = nodeMetaData.head.getPrivateAddresses.toSet ++ nodeMetaData.head.getPublicAddresses.toSet

        val nodeStatus = node.bakedImage match {
          case Some(true) => {
            "STARTED"
          }
          case Some(false) | None => {
            "STARTING"
          }
        }

        val group = s"$systemId-$instanceName".toLowerCase
        val nodeData = JcloudsNodeMetaDataToScopeNodeMetaData(nodeMetaDataHead, nodeStatus)
        
        nodeData
      }
    }

    createVmFuture onFailure {
      case e: RunNodesException => {
        // We create only one VM each time.
        val data = e.getNodeErrors.head
        logError("Failed to create VM with Error: {}, VM group: {}, VM name: {}", e.getExecutionErrors, getGroupFromNodeMetadata(data._1), data._1.getName)
      }

      case t: Throwable => {
        logError("Failed to create VM with Error: {}, VM group: {}, VM name: {}", t.getMessage, groupName, node.name)
      }

    }

    createVmFuture
  }


  private def filterPrivateAddress(address: String) = {
    var result = false
    VMUtils.privateSubnets.foreach{
      case s => {
        result = result | s._1.getInfo.isInRange(address)
      }
    }
    result
//    VMUtils.subnetUtils.getInfo.isInRange(address)
  }

  private def JcloudsNodeMetaDataToScopeNodeMetaData(nodeMetaData: NodeMetadata, vmstatus: String) = {

    val allAddress = nodeMetaData.getPrivateAddresses.toSet ++ nodeMetaData.getPublicAddresses.toSet
    val uri = nodeMetaData.getUri match {
        case null => ""
        case _ => nodeMetaData.getUri.toString
    }
    
    ScopeNodeMetadata(nodeMetaData.getId,
      getNameFromNodeMetadata(nodeMetaData),
      Option(ScopeUtils.configuration.getString("cloud.env.dnsName", null)),
      Option(nodeMetaData.getCredentials.getPrivateKey),
      allAddress.filter(filterPrivateAddress),
      allAddress.filterNot(filterPrivateAddress),
      getGroupFromNodeMetadata(nodeMetaData),
      nodeMetaData.getTags.toSet,
      uri,
      vmstatus)
  }

  def findVM(vmID: String) = {
    val nodeMetaData = computeServiceContext.getComputeService.listNodesByIds(Set(vmID))

    val nodeData = JcloudsNodeMetaDataToScopeNodeMetaData(nodeMetaData.head, "STARTED")
    Some(nodeData)
  }


  def deployVM(nodeMetadata: ScopeNodeMetadata, role: String, productRepoUrl: String, productName: String, productVersion: String, installationPart: InstallationPart) = {
    installationPart.puppet match {
      case Some(puppet) => {
        val productRepoInfo = new ProductRepoInfo(productRepoUrl)
        val jsonConfiguration = ScopeUtils.mapper.writeValueAsString(puppet.configuration)

        val roleInfo = new RoleInfo(role, "scope", puppet.script, jsonConfiguration)

        val script = new ProvisionStatements(productRepoInfo, roleInfo)
        val scriptOutput = runScriptOnNode(script.render(OsFamily.UNIX), s"apply_puppet${java.lang.System.currentTimeMillis()}", nodeMetadata, nodeMetadata.privateKey.getOrElse(throw new IllegalArgumentException("No rsa private key for node.")))

        scriptOutput.getExitStatus match {
          case 0 => logInfo("Finished deploy VM. {} in group {}", nodeMetadata.hostname, nodeMetadata.group)
          case _ => {
            logError("Failed to provision VM. {} in group {}", nodeMetadata.hostname, nodeMetadata.group)
            logError("Output : {} , Error: {}", scriptOutput.getOutput, scriptOutput.getError)
            throw new ScopeProvisionException(scriptOutput.getError)
          }
        }
      }
      case None => logDebug("No puppet provided.")
    }
    installationPart.plugin match {
      case Some(plugin) => {
        val pluginClass = engine.getPlugin(plugin.className)
        pluginClass match {
          case Some(p) => {
            logInfo(s"running plugin deploy step $plugin")
            p.run(Source.fromFile(plugin.configuration).getLines().mkString("\n"))
          }
          case None =>
        }
      }
      case None =>
    }

  }

  def deleteVM(nodeMetadata: ScopeNodeMetadata)(implicit ec:ExecutionContext) {
    val deleteFuture = future {
      computeServiceContext.getComputeService.destroyNode(nodeMetadata.id)
    }

    deleteFuture onSuccess {
      case n => logInfo("Request delete node {} sent", nodeMetadata.id)
    }

    deleteFuture onFailure  {
      case e => {
        logError("Failure while deleting node {}", nodeMetadata.id)
        logError("", e)
      }
    }
  }

  def waitForNodeDataToBeSet(nodeId:String) = {
	  var nodeMetaData : NodeMetadata = null;
	  while (nodeMetaData == null || nodeMetaData.getTags == null) {
	    nodeMetaData = computeServiceContext.getComputeService.getNodeMetadata(nodeId) 
	    if (nodeMetaData.getTags == null)
	    	Thread.sleep(3000)
	  }
	  nodeMetaData
  }

  def getGroupFromNodeMetadata(nodeMetaData : NodeMetadata) : String = {
    ScopeUtils.configuration.getString("cloud.provider") match {
    	case "aws" => nodeMetaData.getUserMetadata().get("group")
    	case _ => nodeMetaData.getGroup
    }
  }

  def getNameFromNodeMetadata(nodeMetaData : NodeMetadata) : String = {
    ScopeUtils.configuration.getString("cloud.provider") match {
    	case "aws" => nodeMetaData.getUserMetadata().get("Name")
    	case _ => nodeMetaData.getHostname
    }
  }


  def addDomainName(name: String, systemUserId: String) = {
    ScopeUtils.configuration.getString(s"cloud.env.$systemUserId.dnsName") match {
      case domain: String => s"$name.$domain"
      case _ => ScopeUtils.configuration.getString(s"cloud.env.dnsName") match {
        case globalDomain: String => s"$name.$globalDomain"
        case _ => name
      }
    }
  }

  def removeDomainName(name: String, systemUserId: String) = {

    ScopeUtils.configuration.getString(s"cloud.env.$systemUserId.dnsName") match {
      case domain: String => name.replace(domain, "")
      case _ => ScopeUtils.configuration.getString(s"cloud.env.dnsName") match {
        case globalDomain: String => name.replace(globalDomain, "")
        case _ => name
      }
    }
  }
}
