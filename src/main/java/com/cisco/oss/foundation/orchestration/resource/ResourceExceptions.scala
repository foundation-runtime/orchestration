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

import org.springframework.web.bind.annotation.{ResponseStatus, ControllerAdvice, ExceptionHandler}
import org.springframework.http.{HttpHeaders, ResponseEntity, HttpStatus}
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import org.springframework.web.context.request.WebRequest
import com.cisco.oss.foundation.orchestration.utils.ScopeUtils

//@ResponseStatus(value = HttpStatus.CONFLICT, reason = "system already exists")
class SystemAlreadyExists() extends RuntimeException

//@ResponseStatus(value = HttpStatus.CONFLICT, reason = "foundation machine already exists")
class RepoAlreadyExists() extends RuntimeException

//@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "password cannot be null or empty")
class PasswordMissing() extends RuntimeException

//@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "system id cannot be null or empty")
class SystemIdMissing() extends RuntimeException

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "System does not exist")
class SystemNotFound() extends RuntimeException

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "System Foundation product does not ready")
class SystemFoundationProductNotReady() extends RuntimeException

class SystemFoundationProductMissAccessPoints(message: String = "") extends RuntimeException(message)

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "foundation machine does not exist")
class RepoNotFound() extends RuntimeException

//@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "foundation machine creation failed")
class RepoCreationFailed() extends RuntimeException

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "function description does not exist")
class FunctionDescriptionNotFound() extends RuntimeException

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Product does not exist")
class ProductNotFound() extends RuntimeException

//@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Product already exists")
class ProductAlreadyExists() extends RuntimeException

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Instance does not exist")
class InstanceNotFound() extends RuntimeException

//@ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Service does not exist")
class ServiceNotFound() extends RuntimeException

//@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "Cloud machine failed to create")
class InstanceCreationFailed() extends RuntimeException

//@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Naming mismatch:")
class InvalidParameterException(val message: String) extends RuntimeException(message)

//@ResponseStatus(value = HttpStatus.CONFLICT, reason = "Instance cannot be deleted during deployment")
class InstanceNotDeletable() extends RuntimeException

//@ResponseStatus(value = HttpStatus.NOT_IMPLEMENTED, reason = "Operation is not implemented")
class NotImplemented() extends RuntimeException

case class ErrorResponse(code:Int, description:String)

@ControllerAdvice
class RestResponseEntityExceptionHandler() extends ResponseEntityExceptionHandler {

  @ExceptionHandler(value = Array(classOf[PasswordMissing]))
  def handlePasswordMissing(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.BAD_REQUEST
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"password cannot be null or empty"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[SystemAlreadyExists]))
  def handleSystemAlreadyExists(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.CONFLICT
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"system already exists"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[RepoAlreadyExists]))
  def handleRepoAlreadyExists(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.CONFLICT
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"foundation machine already exists"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[SystemIdMissing]))
  def handleSystemIdMissing(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.BAD_REQUEST
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"system id cannot be null or empty"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[SystemNotFound]))
  def handleSystemNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.NOT_FOUND
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"System does not exist"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[RepoNotFound]))
  def handleRepoNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.NOT_FOUND
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"foundation machine does not exist"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[RepoCreationFailed]))
  def handleRepoCreationFailed(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"foundation machine creation failed"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[FunctionDescriptionNotFound]))
  def handleFunctionDescriptionNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.NOT_FOUND
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"function description does not exist"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[ProductNotFound]))
  def handleProductNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.NOT_FOUND
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Product does not exist"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[ProductAlreadyExists]))
  def handleProductAlreadyExists(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.CONFLICT
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Product already exists"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[InstanceNotFound]))
  def handleInstanceNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.NOT_FOUND
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Instance does not exist"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[ServiceNotFound]))
  def handleServiceNotFound(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.NOT_FOUND
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Service does not exist"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[InstanceCreationFailed]))
  def handleInstanceCreationFailed(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Cloud machine failed to create"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[InvalidParameterException]))
  def handleInvalidParameterException(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.BAD_REQUEST
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Naming mismatch"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }

  @ExceptionHandler(value = Array(classOf[InstanceNotDeletable]))
  def handleInstanceNotDeletable(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.CONFLICT
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Instance cannot be deleted during deployment"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }


  @ExceptionHandler(value = Array(classOf[NotImplemented]))
  def handleNotImplemented(ex: RuntimeException, request: WebRequest): ResponseEntity[Object] = {
    val status: HttpStatus = HttpStatus.NOT_IMPLEMENTED
    val bodyOfResponse = ScopeUtils.mapper.writeValueAsString(ErrorResponse(status.value,"Operation is not implemented"))
    handleExceptionInternal(ex, bodyOfResponse, new HttpHeaders(), status, request)
  }





}