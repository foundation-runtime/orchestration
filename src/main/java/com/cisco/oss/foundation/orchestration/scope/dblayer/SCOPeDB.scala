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

package com.cisco.oss.foundation.orchestration.scope.dblayer

import com.cisco.oss.foundation.orchestration.scope.model.{Service, System, Product, Instance}
import java.util

trait SCOPeDB {

  def createSystem(system: System)
  def createService(service: Service)
  def findService(id:String):Option[Service]
  def findAllSystems:java.util.List[System]
  def updateSystem(system: System)
  def createProduct(product: Product)
  def updateProduct(product: Product)
  def deleteProduct(productName: String, productVersion: String)  
  def findSystem(systemId: String): Option[System]
  def createInstance(instance: Instance)
  def updateInstance(instance: Instance)
  def updateMachineStatus(instanceId: String, machineName: String, status: String)
  def findInstance(instanceId: String): Option[Instance]
  def findInstanceBySystemId(systemId: String): Option[util.Collection[Instance]]
  def deleteSystem(systemId: String)
  def deleteInstance(systemId: String, instanceId: String)
  def getInstanceInfo(instance: Instance): Instance
  def findAllProducts:java.util.List[Product]
  def getProductDetails(productName:String, productVersion:String):Option[Product]

}