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

package com.cisco.oss.foundation.orchestration.resource

import com.cisco.oss.foundation.orchestration.ScopeConstants
import com.wordnik.swagger.annotations.{ApiOperation, Api}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import com.cisco.oss.foundation.orchestration.utils.{ScopeUtils, UUID}
import org.springframework.http.HttpStatus
import javax.servlet.http.{HttpServletResponse, HttpServletRequest}
import com.cisco.oss.foundation.orchestration.model._
import scala.Predef.String
import java.util
import scala.collection.JavaConversions._
import com.cisco.oss.foundation.orchestration.model.Instance
import scala.Some
import com.cisco.oss.foundation.orchestration.model.InstanceDetails
import com.cisco.oss.foundation.orchestration.model.ProvisionRequest
import scala.collection.immutable.Map
import com.fasterxml.jackson.module.scala.JsonScalaEnumeration
import com.fasterxml.jackson.core.`type`.TypeReference

/**
 * User: Yair Ogen
 * Date: 11/7/13
 */


object Parameter {
  def toParameter(option: ProductOption) : Parameter = {
    val t : ParameterType.Value =  option.enumeration match {
      case Some(en) => {
        option.optionType match {
          case OptionType.STRING  => ParameterType.STRING_ENUM
          case OptionType.NUMBER  => ParameterType.NUMBER_ENUM
          case OptionType.BOOLEAN => ParameterType.BOOLEAN
          case OptionType.FILE => ParameterType.FILE
        }
      }
      case None => {
        option.optionType match {
          case OptionType.STRING  => ParameterType.STRING
          case OptionType.NUMBER  => ParameterType.NUMBER
          case OptionType.BOOLEAN => ParameterType.BOOLEAN
          case OptionType.FILE => ParameterType.FILE
        }
      }
    }
    
    new Parameter(name = option.key, value = option.value.getOrElse(""), `type` = t, restriction = option.enumeration, 
                           defaultValue = option.defaultValue, description = option.description,
                           required = option.required)
  }
  
}

@Controller
@RequestMapping(Array("/{serviceId}/service"))
@Api(value = "Provision", description = "Provision a service and get the provisioning status")
class ServiceResource extends BasicResource {


  @RequestMapping(method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ApiOperation(value = "Gets the list of services")
  @ResponseBody def getInstanceList(@PathVariable serviceId: String): util.List[InstanceDetails] = {

    ScopeUtils.time(logger, "getInstanceList-rest") {
      return new util.ArrayList[InstanceDetails]()
    }
  }

  /**
   * Return the service instance details.
   * @param requestId the request id.
   * @return a { @link InstanceDetails}.
   * @throws ServiceException if the control interface endpoint can't be retrieved.
   * @throws NotFoundException if the requestId is unknown.
   */
  @RequestMapping(value = Array("/{requestId}"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ApiOperation(value = "Get the service instance details")
  @ResponseBody def getInstance(@PathVariable serviceId: String, @PathVariable requestId: String, request: HttpServletRequest, response: HttpServletResponse): InstanceDetails = {
    ScopeUtils.time(logger, "getInstance-rest") {
      val controlInterfaceEndpoint: String = getEndpoint(request) + "/" + serviceId + "/service/" + requestId
      val instanceId = requestId
      val status = "PROVISIONED"
      val instanceDetails = InstanceDetails(controlInterfaceEndpoint, status, instanceId)
      logInfo("returning instance details: {}", instanceDetails)
      instanceDetails
    }
  }

  /**
   * Provision an instance of this service for a system.
   * @param provisionRequest the provisioning request.
   * @param request the servlet request.
   * @return a { @link ResponseEntity}.
   * @throws ServiceException if the service can't be provisioned.
   */
  @RequestMapping(method = Array(RequestMethod.POST), consumes = Array("application/json"))
  @ApiOperation(value = "Provision an instance of the service")
  @ResponseBody def provision(@PathVariable serviceId: String, @RequestBody provisionRequest: ProvisionRequest, request: HttpServletRequest, response: HttpServletResponse) {
    ScopeUtils.time(logger, "provision-rest") {
      val instanceId = provisionRequest.tenantId + "-" + provisionRequest.instanceId.getOrElse(UUID.getNextUUID())
      val bentoSystemId = ScopeUtils.configuration.getString(ScopeConstants.BENTO_SYSTEM_ID, "bento")
      logInfo("Provisioning for system {}.  Request/Instance id is {}", bentoSystemId, instanceId)
      val service = scopedb.findService(serviceId).getOrElse(throw new ServiceNotFound)
      val product = scopedb.getProductDetails(service.productName, service.productVersion).getOrElse(throw new ProductNotFound)
      
      val instance = Instance(instanceId, bentoSystemId, instanceId, Some("STOPPED"), None, product, Map(), List[AccessPoint](), None)
      scopedb.createInstance(instance)

      //    service.provision(provisionRequest, requestId, request)
      val endpoint: String = getEndpoint(request) + "/" + serviceId + "/request/" + instanceId
      logInfo("Status endpoint is {}", endpoint)
      response.setStatus(HttpStatus.ACCEPTED.value)
      response.setHeader("Location", endpoint)
    }
  }

  /**
   * Unprovision an instance of this service for a system.
   * @param request the servlet request.
   * @return a { @link ResponseEntity}.
   * @throws ServiceException if the service can't be provisioned.
   * @throws NotFoundException
   */
  @RequestMapping(value = Array("/{requestId}"), method = Array(RequestMethod.DELETE))
  @ApiOperation(value = "Unprovision an instance of the service")
  @ResponseBody
  def unprovision(@PathVariable serviceId: String, @PathVariable requestId: String, request: HttpServletRequest, response: HttpServletResponse) {
    ScopeUtils.time(logger, "unprovision-rest") {
      scopedb.findInstance(requestId) match {
        case None =>
        case Some(instance) => {

          logInfo("Unprovisioning.  Instance id is {}", instance.instanceId)
          deleteInstanceVMs(instance.machineIds)
          scopedb.deleteInstance(instance.systemId, instance.instanceId)
          val endpoint: String = getEndpoint(request) + "/" + serviceId + "/request/" + instance.instanceId
          logInfo("Status endpoint is {}", endpoint)
          response.setHeader("Location", endpoint)

        }
      }
      response.setStatus(HttpStatus.ACCEPTED.value)
    }
  }
}

@Controller
@RequestMapping(Array("/{serviceId}/request"))
@Api(value = "Provision", description = "Get the provisioning status")
class RequestResource extends BasicResource {
  /**
   * Return the provisioning status.
   * @param requestId the request id.
   * @return a { @link ProvisionStatus}.
   * @throws ServiceException if the control interface endpoint can't be retrieved.
   * @throws NotFoundException if the requestId is unknown.
   */
  @RequestMapping(value = Array("/{requestId}"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ApiOperation(value = "Get the provisioning status")
  @ResponseBody
  def getRequestStatus(@PathVariable serviceId: String, @PathVariable requestId: String, request: HttpServletRequest, response: HttpServletResponse): String = {
    ScopeUtils.time(logger, "getRequestStatus-rest") {
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)

      val status = "PROVISIONED" //instance.status.getOrElse("STARTING")

      val responseJson =
        s"""
        {
          "status": "$status"
        }
      """
      if (status == "PROVISIONED") {
        val instanceEndpoint: String = getEndpoint(request) + "/" + serviceId + "/service/" + requestId
        response.setStatus(HttpStatus.SEE_OTHER.value)
        response.setHeader("Location", instanceEndpoint)
        logInfo("Location header set: {}", instanceEndpoint)
      }
      logInfo("returning result: {}", responseJson);
      responseJson
    }
  }
}



@Controller
@RequestMapping(value = Array("/{serviceId}/service/{requestId}/control/parameters"))
@Api(value = "Parameters", description = "Get/Set parameters on a provisioned service")
class ParametersResource extends BasicResource {

  /**
   * Get a parameter.
   * @param request the servlet request.
   * @param parameterName the parameter name.
   * @return a { @link Parameter}.
   * @throws NotFoundException if the request id is unknown, or the named parameter is not found.
   * @throws ServiceException if the parameters can't be retrieved.
   */
  @RequestMapping(value = Array("/{parameterName}"), method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ApiOperation(value = "Get a parameter")
  @ResponseBody def getParameter(@PathVariable requestId: String, request: HttpServletRequest, @PathVariable parameterName: String, response: HttpServletResponse): Parameter = {
    //    response.setHeader(CONFIG_HEADER, configSchemaVersion)
    ScopeUtils.time(logger, "getParameter-rest") {
      //return new Parameter(parameterName, "", ParameterType.STRING, null, "DEFAULT", false, Option("Default description"))
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)
      instance.product.productOptions.find( e => e.key == parameterName) match {
        case Some(opt) => Parameter.toParameter(opt)
        case None => throw new InvalidParameterException(s"Parameter not found: $parameterName")
      }
    }
  }

  /**
   * Return all parameters.
   * @param request the servlet request.
   * @return { @link ParameterList}.
   * @throws NotFoundException if the request id is unknown.
   * @throws ServiceException if the parameters can't be retrieved.
   */
  @RequestMapping(method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ApiOperation(value = "Get all parameters")
  @ResponseBody def getParameters(@PathVariable requestId: String, request: HttpServletRequest, response: HttpServletResponse): util.List[Parameter] = {
    //    response.setHeader(CONFIG_HEADER, configSchemaVersion)
    ScopeUtils.time(logger, "getParameters-rest") {
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)
      instance.product.productOptions.map(Parameter.toParameter)
    }
  }


  /**
   * Set a parameter.
   * @param request the servlet request.
   * @param parameterName the parameter name.
   * @param parameter the parameter.
   * @throws NotFoundException if the request id is unknown.
   * @throws ServiceException if the parameter can't be set.
   */
  @RequestMapping(value = Array("/{parameterName}"), method = Array(RequestMethod.PUT), consumes = Array("application/json"))
  @ApiOperation(value = "Set a parameter")
  @ResponseBody def setParameter(@PathVariable requestId: String, request: HttpServletRequest, @PathVariable parameterName: String, @RequestBody(required = true) parameter: Parameter) {
    ScopeUtils.time(logger, "setParameter-rest") {
      if (!(parameterName == parameter.name)) {
        throw new InvalidParameterException("Naming mismatch: '" + parameterName + "' vs '" + parameter.name + "'")
      }
      setParameters(requestId, request, new util.ArrayList(List(parameter)))
    }
  }

  /**
   * !! N.B. This operation exists only for the purposes of UI testing and is not part of the Configuration
   * API specification !!
   *
   * Set the parameters to be exposed by this service.
   * @param request the servlet request.
   * @param parameterList the parameters.
   * @throws NotFoundException if the request id is unknown.
   */
  @RequestMapping(value = Array("/definitions"), method = Array(RequestMethod.PUT), consumes = Array("application/json"))
  @ApiOperation(value = "Set the parameter definitions", notes = "!! N.B. This operation exists only for the purposes of UI testing and is not part of the Configuration API specification !!")
  @ResponseBody def setParameterDefinitions(@PathVariable requestId: String, request: HttpServletRequest, @RequestBody(required = true) parameterList: util.List[Parameter]) {
    ScopeUtils.time(logger, "setParametersDefinitions-rest") {

    }
    //    service.setParameterDefinitions(requestId, parameterList)
  }

  /**
   * Set all parameters.
   * @param request the servlet request.
   * @throws NotFoundException if the request id is unknown.
   * @throws ServiceException if the parameter can't be set.
   */
  @RequestMapping(method = Array(RequestMethod.PUT), consumes = Array("application/json"))
  @ApiOperation(value = "Set all parameters")
  @ResponseBody def setParameters(@PathVariable requestId: String, request: HttpServletRequest, @RequestBody(required = true) parameterValues: util.List[Parameter]) {
    ScopeUtils.time(logger, "setParameters-rest") {
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)
      val newOptions  = instance.product.productOptions.map( opt => {
        parameterValues.find(pv => pv.name == opt.key) match {
          case Some(pv) => opt.copy(value = Option(pv.value))
          case None => opt
        }
      })
      val newInstance = instance.copy(product = instance.product.copy(productOptions = newOptions))
      scopedb.updateInstance(newInstance)
    }
  }

}


/**
 * Handle all the calls to /control/status.
 * @author Yair Ogen
 */
@Controller
@RequestMapping(value = Array("/{serviceId}/service/{requestId}/control/status"))
@Api(value = "Status", description = "Get/Set the provisioned service's status")
class StatusResource extends BasicResource {

  val "application/json" = "application/json"

  /**
   * Return the status of the provisioned service.
   * @param request the servlet request.
   * @return a { @link ResponseEntity}.
   * @throws NotFoundException if the request id is not known.
   */
  @RequestMapping(method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ApiOperation(value = "Get the provisioned service's status")
  @ResponseBody def getControlStatus(@PathVariable requestId: String, request: HttpServletRequest): ControlStatus = {
    ScopeUtils.time(logger, "getControlStatus-rest") {
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)

      val status = instance.status.getOrElse("STOPPED")

      ControlStatus(status, instance.details.getOrElse(""))
    }
  }

  /**
   * Set the provisioned service's status. In reality, this can only be used to "start" the service.
   * @param status the status.
   * @param request the servlet request.
   * @return a { @link ResponseEntity}.
   * @throws ServiceException if the service can't be started.
   * @throws NotFoundException if the request id is not known.
   */
  @RequestMapping(method = Array(RequestMethod.PUT))
  @ApiOperation(value = "Set the provisioned service's status", notes = "Basically used to start the service")
  @ResponseBody
  def setStatus(@PathVariable requestId: String, @RequestBody status: ControlStatusRequest, request: HttpServletRequest): ControlStatus = {
    ScopeUtils.time(logger, "setStatus-rest") {
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)

      status.status match {
        case statusValue if (statusValue == "START") => {
          logInfo("about to instantiate instance: {}", instance)
          val retInstance = instantiateProduct(instance, instance.product.productName, instance.product.productVersion, Some(instance.instanceId))
          ControlStatus(retInstance.status.getOrElse("NOTREADY"), instance.details.getOrElse(""))
        }
        case statusValue if (statusValue == "STOP") => throw new NotImplemented //ControlStatus("STOPPED", instance.details.getOrElse(""))
        case _ => throw new IllegalArgumentException("received illegal status")
      }

    }
  }

}

@Controller
@RequestMapping(value = Array("/{serviceId}/service/{requestId}/control/interfaces/functional"))
@Api(value = "Functional Interfaces", description = "Get the functional interfaces")
class FunctionalResource extends BasicResource {


  /**
   * Return a functional interface description.
   * @param request the servlet request.
   * @param functionName the functional interface name.
   * @return a { @link ResponseEntity}.
   * @throws NotFoundException if the request id is unknown, or if the functional interface is unknown.
   */
  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/{functionName}"), produces = Array("application/json"))
  @ApiOperation(value = "Get a functional interface description")
  @ResponseBody def getFunctionDescription(@PathVariable requestId: String, request: HttpServletRequest, @PathVariable functionName: String): FunctionDescription = {
    ScopeUtils.time(logger, "getFunctionDescription-rest") {
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)
      val accessPoints = instance.accessPoints
      for (accessPoint <- accessPoints) {
        if (accessPoint.name == functionName)
          return FunctionDescription(accessPoint.name, accessPoint.url, "N/A", "N/A")
      }
      throw new FunctionDescriptionNotFound
    }
  }

  /**
   * Return a list of the functional interfaces exposed by this service.
   * @return a { @link ResponseEntity}.
   */
  @RequestMapping(method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ApiOperation(value = "Get a functional interface description")
  @ResponseBody def getFunctionList(@PathVariable requestId: String): List[FunctionDescription] = {
    ScopeUtils.time(logger, "getFunctionList-rest") {
      val instance = scopedb.findInstance(requestId).getOrElse(throw new InstanceNotFound)
      val accessPoints = instance.accessPoints
      accessPoints.map((accessPoint: AccessPoint) => FunctionDescription(accessPoint.name, accessPoint.url, "N/A", "N/A")).toList
    }
  }


}


@Controller
@RequestMapping(value = Array("/{serviceId}/service/{requestId}/control/interfaces/docks"))
@Api(value = "Dock Interfaces", description = "Get/Set the dock interfaces")
class DocksResource extends BasicResource {


  /**
   * Delete a dock endpoint.
   * @param request the servlet request.
   * @param dockName the name of the dock endpoint to delete.
   * @throws NotFoundException if the request id is unknown or if the named dock is unknown.
   */
  @RequestMapping(method = Array(RequestMethod.DELETE), value = Array("/{dockName}/endpoint"))
  @ApiOperation(value = "Delete/clear a dock endpoint")
  @ResponseBody def deleteDockEndpoint(@PathVariable requestId: String, request: HttpServletRequest, @PathVariable dockName: String) {
    ScopeUtils.time(logger, "deleteDockEndpoint-rest") {

    }
  }

  /**
   * Return a dock description.
   * @param request the servlet request.
   * @param dockName the dock to get the description for.
   * @return a { @link DockDescription}.
   * @throws NotFoundException if the request id is unknown or if the named dock is unknown.
   */
  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/{dockName}"), produces = Array("application/json"))
  @ResponseBody
  @ApiOperation(value = "Get a dock description")
  def getDockDescription(@PathVariable requestId: String, request: HttpServletRequest, @PathVariable dockName: String): DockDescription = {
    ScopeUtils.time(logger, "getDockDescription-rest") {
      DockDescription(false, "", "")
    }
  }

  /**
   * Return the endpoint for a dock.
   * @param request the servlet request.
   * @param dockName the dock name.
   * @return a { @link DockEndpoint}.
   * @throws NotFoundException if the named dock is unknown or if the request id is unknown.
   * @throws ServiceException if the dock endpoint can't be retrieved.
   */
  @RequestMapping(method = Array(RequestMethod.GET), value = Array("/{dockName}/endpoint"), produces = Array("application/json"))
  @ResponseBody
  @ApiOperation(value = "Get a dock endpoint")
  def getDockEndpoint(@PathVariable requestId: String, request: HttpServletRequest, @PathVariable dockName: String): DockEndpoint = {
    ScopeUtils.time(logger, "getDockEndpoint-rest") {
      DockEndpoint("", "")
    }
  }

  /**
   * Return a list of the docks exposed by this service.
   * @return a { @link Docks}
   */
  @RequestMapping(method = Array(RequestMethod.GET), produces = Array("application/json"))
  @ResponseBody
  @ApiOperation(value = "Get a list of docks")
  def getDockList: List[Dock] = {
    ScopeUtils.time(logger, "getDockList-rest") {
      return List()
    }
  }

  /**
   * Set the endpoint for a dock.
   * @param request the servlet request
   * @param dockName the dock name.
   * @param dockEndpoint the { @link DockEndpoint}.
   * @throws NotFoundException if the request id is unknown or if the named dock is unknown.
   * @throws ServiceException if the dock endpoint can't be set.
   */
  @RequestMapping(method = Array(RequestMethod.PUT), value = Array("/{dockName}/endpoint"), consumes = Array("application/json"))
  @ApiOperation(value = "Set a dock endpoint")
  @ResponseBody
  def setDockEndpoint(@PathVariable requestId: String, request: HttpServletRequest, @PathVariable dockName: String, @RequestBody dockEndpoint: DockEndpoint) {
    ScopeUtils.time(logger, "setDockEndpoint-rest") {

    }
  }


}

