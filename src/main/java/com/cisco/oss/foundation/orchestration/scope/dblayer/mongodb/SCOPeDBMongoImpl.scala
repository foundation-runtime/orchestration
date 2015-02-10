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

import java.util
import java.util.concurrent.ConcurrentHashMap

import com.cisco.oss.foundation.orchestration.scope.dblayer.SCOPeDB
import com.cisco.oss.foundation.orchestration.scope.model._
import com.cisco.oss.foundation.orchestration.scope.resource.{InstanceNotFound, ProductAlreadyExists, ProductNotFound, SystemNotFound}
import com.cisco.oss.foundation.orchestration.scope.utils.{ScopeUtils, Slf4jLogger}
import com.mongodb.MongoException.DuplicateKey
import com.mongodb.WriteConcern
import com.mongodb.casbah.MongoClient
import com.mongodb.casbah.commons.MongoDBObject
import com.mongodb.casbah.commons.conversions.scala.RegisterJodaTimeConversionHelpers
import com.mongodb.casbah.query.Imports._
import com.novus.salat._
import com.novus.salat.global._
import org.joda.time.DateTime
import org.springframework.stereotype.Component

import scala.actors.threadpool.locks.ReentrantLock
import scala.compat.Platform

@Component
class SCOPeDBMongoImpl extends SCOPeDB with Slf4jLogger {

  val host = ScopeUtils.configuration.getString("mongodb.host", "localhost")
  val port = ScopeUtils.configuration.getInt("mongodb.port", 27017)

  RegisterJodaTimeConversionHelpers()
  val mongodbConnetion = MongoClient(host, port)
  val scopedb = mongodbConnetion("scope")
  scopedb.writeConcern = WriteConcern.FSYNC_SAFE
  val systemsdb = scopedb("systems")
  val productsdb = scopedb("products")
  val productsPatchesdb = scopedb("products-patches")
  val servicesdb = scopedb("services")
  val instancesdb = scopedb("instances")

  //  instancesdb.find().map(dbObj => grater[Instance].asObject(dbObj)).foreach{
  //    case instance => {
  //      val id = instance.instanceId
  //      val system = instance.systemId
  //      val lock: ReentrantLock = new ReentrantLock(true)
  //      instanceLockMap.put(system + "-" + id, Some(lock))
  //    }
  //  }


  def findAllSystems: util.List[System] = {

    ScopeUtils.time(logger, "findAllSystems-db") {
      val all = systemsdb.find()
      val systems = new util.ArrayList[System]()
      for (system <- all) systems.add(grater[System].asObject(system))
      systems
    }
  }

  def createSystem(system: System) = {
    ScopeUtils.time(logger, "createSystem-db") {
      systemsdb.insert(grater[System].asDBObject(system))
    }
  }

  def updateSystem(system: System) = {
    ScopeUtils.time(logger, "updateSystem-db") {
      systemsdb.update(MongoDBObject("_id" -> system.systemId), grater[System].asDBObject(system))
    }
  }

  def findSystem(systemId: String): Option[System] = {
    ScopeUtils.time(logger, "findSystem-db") {

      systemsdb.findOneByID(systemId) match {
        case None => None
        case Some(system) => Some(grater[System].asObject(system))
      }
    }

  }


  def deleteSystem(systemId: String) = {
    ScopeUtils.time(logger, "deleteSystem-db") {
      val system = findSystem(systemId).getOrElse(throw new SystemNotFound)
      systemsdb.remove(MongoDBObject("_id" -> system.systemId))
    }
  }

  def deleteInstance(systemId: String, instanceId: String) = {
    ScopeUtils.time(logger, "deleteInstance-db") {

      val lock = lockMap.get(systemId + "-" + instanceId)
      lock match {
        case Some(l) => l.lock()
        case None =>
        case _ =>
      }
      try {
        val instance = findInstance(instanceId)
        instance match {
          case Some(inst) => {
            if (inst.systemId == systemId) {
              instancesdb.remove(MongoDBObject("_id" -> instance.getOrElse(throw new InstanceNotFound).instanceId))
            } else {
              throw new IllegalArgumentException("instance id: " + instanceId + " is not associated with system id: " + systemId + ". Instead it has system id of: " + inst.systemId)
            }
          }
          case None => throw new InstanceNotFound

        }

      }
      finally {
        lock match {
          case Some(l) => l.lock()
          case None =>
          case _ =>
        }
      }
    }
  }

  def getProductDetails(productName: String, productVersion: String): Option[Product] = {

    ScopeUtils.time(logger, "getProductDetails-db") {

      logDebug("about to search for product details: {}-{}", productName, productVersion)
      productsdb.findOneByID(productName + "-" + productVersion) match {
        case None => None
        case Some(p) => Some(grater[Product].asObject(p))
      }
    }
  }

  def findAllProducts: util.List[Product] = {

    ScopeUtils.time(logger, "findAllProducts-db") {
      val all = productsdb.find()
      val prods = new util.ArrayList[Product]()
      for (prod <- all) prods.add(grater[Product].asObject(prod))
      prods
    }
  }


  def deleteProduct(productName: String, productVersion: String) {
    ScopeUtils.time(logger, "deleteProduct-db") {
      val product = getProductDetails(productName, productVersion).getOrElse(throw new ProductNotFound)
      productsdb.remove(MongoDBObject("_id" -> product.id))
    }
  }

  def getInstanceInfo(instance: Instance): Instance = {

    ScopeUtils.time(logger, "getInstanceInfo-db") {
      val lock = lockMap.get(instance.systemId + "-" + instance.instanceId)
      lock match {
        case Some(l) => l.lock()
        case None =>
        case _ =>
      }
      try {

        val instanceId = instance.instanceId

        val inst = findInstance(instanceId)

        inst.getOrElse(throw new InstanceNotFound)
      }
      finally {
        lock match {
          case Some(l) => l.unlock()
          case None =>
          case _ =>
        }
      }
    }

  }

  def findInstance(instanceId: String): Option[Instance] = {

    ScopeUtils.time(logger, "findInstance-db") {

      logTrace("searching for instance: {}", instanceId)

      instancesdb.findOneByID(instanceId) match {
        case None => None
        case Some(instance) => Some(grater[Instance].asObject(instance))
      }
    }

  }

  def findInstanceBySystemId(systemId: String): Option[util.Collection[Instance]] = {

    ScopeUtils.time(logger, "findInstanceBySystemId-db") {

      val query = MongoDBObject("systemId" -> systemId)
      val instances = instancesdb.find(query)
      val instanceList = new util.ArrayList[Instance]()
      for (instance <- instances) instanceList.add(grater[Instance].asObject(instance))
      if (instanceList.isEmpty) None else Some(instanceList)

    }

  }

  val lockMap = new ConcurrentHashMap[String, Option[ReentrantLock]]()

  def createInstance(instance: Instance): Unit = {
    ScopeUtils.time(logger, "createInstance-db") {
      //TODO: do we need lock here???
      val id = instance.instanceId
      val system = instance.systemId
      val lock: ReentrantLock = new ReentrantLock(true)
      lockMap.put(system + "-" + id, Some(lock))
      instancesdb.update(MongoDBObject("_id" -> instance.instanceId), grater[Instance].asDBObject(instance), true)
    }
  }

  import scala.collection.JavaConversions._

  def updateInstance(instance: Instance) = {
    ScopeUtils.time(logger, "updateInstance-db") {
      val lock = lockMap.getOrElseUpdate(instance.systemId + "-" + instance.instanceId, Some(new ReentrantLock(true)))
      lock match {
        case Some(l) => l.lock()
        case None =>
        case _ =>
      }
      try {
        instancesdb.update(MongoDBObject("_id" -> instance.instanceId), grater[Instance].asDBObject(instance))
      }
      finally {
        lock match {
          case Some(l) => l.unlock()
          case None =>
          case _ =>
        }
      }
    }
  }

  def createProduct(product: Product) = {
    ScopeUtils.time(logger, "createProduct-db") {
      try {
        productsdb.insert(grater[Product].asDBObject(product))
      } catch {
        case e: DuplicateKey => throw new ProductAlreadyExists()
      }

    }
  }

  def updateProduct(product: Product) = {
    ScopeUtils.time(logger, "updateProduct-db") {
      productsdb.update(MongoDBObject("_id" -> product.id), grater[Product].asDBObject(product))
    }
  }

  def createService(service: Service): Unit = {
    ScopeUtils.time(logger, "createService-db") {
      servicesdb.insert(grater[Service].asDBObject(service))
    }
  }

  def findService(id: String): Option[Service] = {
    ScopeUtils.time(logger, "findService-db") {

      servicesdb.findOneByID(id) match {
        case None => None
        case Some(service) => Some(grater[Service].asObject(service))
      }
    }
  }

  override def saveProductPatch(productId: String, patchData: UpdateInstanceData): Unit = {
    ScopeUtils.time(logger, "addProductPatch-db") {
      productsPatchesdb.save(grater[PatchDBObject].asDBObject(PatchDBObject(s"${Platform.currentTime.toString}_${patchData.patchName}_${productId}",
        productId,
        ScopeUtils.mapper.writeValueAsString(patchData))))
    }
  }

  def updateMachineHeartbeat(systemId: String, instanceId: String, machineName: String, lastHeartbeat: DateTime): Unit = {
    ScopeUtils.time(logger, "updateMachineHeartbeat-db") {
      val q = MongoDBObject("_id" -> instanceId)
      val u = $set(s"machineIds.$machineName.heartbeat" -> lastHeartbeat)
      val lock = lockMap.getOrElseUpdate(s"$systemId-$instanceId", Some(new ReentrantLock(true)))
      lock match {
        case Some(l) => l.lock()
        case None =>
        case _ =>
      }
      try {
        instancesdb.update(q, u)
      } finally {
        lock match {
          case Some(l) => l.unlock()
          case None =>
          case _ =>
        }
      }
    }
  }

  def updateMachineStatus(systemId: String, instanceId: String, machineName: String, status: String, modulesName: Option[scala.collection.mutable.Set[String]]): Unit = {
    ScopeUtils.time(logger, "updateMachineStatus-db") {
      val q = MongoDBObject("_id" -> instanceId)
      val u = $set(s"machineIds.$machineName.provisionStatus" -> status)
      val lock = lockMap.getOrElseUpdate(s"$systemId-$instanceId", Some(new ReentrantLock(true)))
      lock match {
        case Some(l) => l.lock()
        case None =>
        case _ =>
      }
      try {
        instancesdb.update(q, u)

        modulesName match {
          case Some(set) => {
//            val newSet: Set[String] = Set[String](set.toList: _*)
            val setInstallModules = $pushAll(s"machineIds.$machineName.installedModules" -> set.toList)
            instancesdb.update(q, setInstallModules)
          }
          case None =>
        }
      } finally {
        lock match {
          case Some(l) => l.unlock()
          case None =>
          case _ =>
        }
      }
    }
  }
}