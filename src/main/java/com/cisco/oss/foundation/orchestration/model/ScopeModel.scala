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

package com.cisco.oss.foundation.orchestration.model

import org.springframework.data.annotation.Id
import scala.annotation.meta.field
import java.util.Collection
import scala.collection.immutable.Map
import com.novus.salat.annotations._
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.fasterxml.jackson.core.`type`.TypeReference
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type


object OptionType extends Enumeration {
  val STRING = Value("string")
  val NUMBER = Value("number")
  val BOOLEAN = Value("boolean")
  val FILE = Value("file")
}

class OptionTypeReference extends TypeReference[OptionType.type]

object ParameterType extends Enumeration {
  val STRING = Value("string")
  val STRING_ENUM = Value("string_enum")
  val NUMBER = Value("number")
  val NUMBER_ENUM = Value("number_enum")
  val BOOLEAN = Value("boolean")
  val FILE = Value("file")
}

class ParameterTypeReference extends TypeReference[ParameterType.type]

case class ProductOption(key: String, value: Option[String], label: String, description: Option[String], @JsonScalaEnumeration(classOf[OptionTypeReference]) optionType: OptionType.Value, defaultValue: String, enumeration: Option[Array[String]], required: Boolean = false, additionalInfo: Map[String, String] = Map[String, String]())

case class Products(products: List[Product])

case class Parameter(name: String, value: String, @JsonScalaEnumeration(classOf[ParameterTypeReference]) `type`: ParameterType.Value, restriction: Option[Array[String]], defaultValue: String, required: Boolean, description: Option[String])

//case class ParameterValue(name:String, value:String)

case class Product( @Key("_id") @(Id@field)id:String, productName: String, productVersion: String, productOptions: List[ProductOption], repoUrl: String)

case class Service(@Key("_id") @(Id@field) id: String, productName: String, productVersion: String)

case class HostDetails(hostName: String, hostId: String, hostIp: String)

case class Instance(@Key("_id") @(Id@field) instanceId: String, systemId: String, instanceName: String, status: Option[String], details: Option[String], product: Product, machineIds: Map[String, ScopeNodeMetadata], accessPoints: List[AccessPoint], deletable: Option[Boolean], rsaKeyPair: Map[String, String] = Map[String, String]())

case class AccessPoint(name: String, url: String)

case class System(@Key("_id") @(Id@field) systemId: String, password: String, foundation: Option[Instance])

case class InstallNodes(nodes: List[Node])

case class Node(id: String, name: String, arch: String, osType: String, osVersion: String, region: String, minDisk: Int, minRam: Int, minCores: Int, network: Collection[Network], swapFile: Boolean, image : Option[String], bakedImage: Option[Boolean]/*, nodeType: Option[String]*/, flpImage: Option[String], iso: Option[String], postConfiguration: Boolean = true, folder: Option[String])

case class Network(nicType: String, networkId: Option[String], nicAlias: String, dnsServices: Option[List[String]], openPorts: Option[List[String]])

case class InstallModules(modules: Collection[Module])

@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "moduleType",
  defaultImpl = classOf[PuppetModule]
)
@JsonSubTypes( Array (
  new Type(value = classOf[PuppetModule], name = "puppet"),
  new Type(value = classOf[PluginModule], name = "plugin")
))
trait Module{
  val moduleType: String = if (this.isInstanceOf[PuppetModule]) "puppet" else "plugin"
  val nodes: List[String] = List[String]()
}
case class PuppetModule(name: String, version: String = "0.0.0",  ccp: Option[Ccp], file: Option[ConfigurationFile], deployFile: Option[DeployFile]) extends Module
case class PluginModule(className: String, configuration: String) extends Module

case class DeployFile(content: String, destinationPath: String, owner: String, group: String, mode:String)

case class Ccp(processName: String, baseConfigProperties: Collection[String], additionalValues: Collection[CcpConfig])

case class ConfigurationFile(baseConfigProperties: Option[Collection[String]], additionalValues: Collection[CcpConfig])

case class CcpConfig(key: String, value: String)

case class ExposeAccessPoints(accessPoints: List[AccessPoint])

case class FileResource(name: String, path: String)

case class DeploymentModelCapture(schemaVersion: String, resources: Option[List[FileResource]], installNodes: InstallNodes, setupProvisioningEnv: Boolean, announceHostNames: Boolean, installModules: Map[String, InstallModules], exposeAccessPoints: ExposeAccessPoints)

case class ProvisionRequest(tenantId: String, instanceId: Option[String])

case class InstanceDetails(controlInterfaceEndpoint: String, status: String, instanceId: String)

case class FunctionDescription(name: String, endpoint: String, description: String, documentation: String)

case class DockDescription(connected: Boolean, description: String, name: String)

case class DockEndpoint(credentials: String, endpointURL: String)

case class Dock(name: String)

case class ControlStatus(status: String, description: String)

case class ControlStatusRequest(status: String)

case class InstallationPart(var puppet: Option[PuppetRole], var plugin: Option[PluginRole])
case class PuppetRole(var script: String, var configuration: Map[String, String], var hostname: List[String])
case class PluginRole(val className: String, val configuration: String)

object ConfigurationEnum extends Enumeration {
  type ConfigurationEnum = Value
  val None, Ccp, File = Value
}

case class ScopeNodeMetadata(val id: String, val hostname: String, fqdn: Option[String], val privateKey: Option[String],
                             val privateAddresses: Set[String], val publicAddresses: Set[String], group: String,
                             val tags: Set[String], val url: String, provisionStatus: String)

