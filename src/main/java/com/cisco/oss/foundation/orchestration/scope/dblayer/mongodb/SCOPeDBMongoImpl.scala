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

import com.cisco.oss.foundation.orchestration.scope.dblayer.SCOPeDB
import com.cisco.oss.foundation.orchestration.scope.utils.Slf4jLogger
import com.cisco.oss.foundation.orchestration.scope.resource.{SystemNotFound, InstanceNotFound, ProductNotFound, ProductAlreadyExists}
import java.util
import com.cisco.oss.foundation.orchestration.scope.model.{Service, Instance, System, Product}
import com.cisco.oss.foundation.orchestration.scope.utils.ScopeUtils
import org.springframework.stereotype.Component
import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.MongoException.DuplicateKey
import java.util.concurrent.ConcurrentHashMap
import scala.actors.threadpool.locks.ReentrantLock


@Component
class SCOPeDBMongoImpl extends SCOPeDB with Slf4jLogger {

  //  @Autowired
  //  var mongoOp: MongoOperations = _

  val host = ScopeUtils.configuration.getString("mongodb.host", "localhost")
  val port = ScopeUtils.configuration.getInt("mongodb.port", 27017)

  val mongodbConnetion = MongoConnection(host, port)
  val scopedb = mongodbConnetion("scope")
  scopedb.writeConcern = WriteConcern.FsyncSafe
  val systemsdb = scopedb("systems")
  val productsdb = scopedb("products")
  val servicesdb = scopedb("services")
  val instancesdb = scopedb("instances")

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
      //      mongoOp.save(system, "systems")
    }
  }

  def updateSystem(system: System) = {
    ScopeUtils.time(logger, "updateSystem-db") {
      systemsdb.update(MongoDBObject("_id" -> system.systemId), grater[System].asDBObject(system))
      //      mongoOp.save(system, "systems")
    }
  }

  def findSystem(systemId: String): Option[System] = {
    ScopeUtils.time(logger, "findSystem-db") {

      systemsdb.findOneByID(systemId) match {
        case None => None
        case Some(system) => Some(grater[System].asObject(system))
      }


      //      val query1 = new BasicQuery("{ '_id' : '" + systemId + "'}");
      //      val result = mongoOp.find(query1, classOf[System], "systems")
      //
      //      if (result.size > 1) {
      //        throw new IllegalArgumentException("there shouldn't be more than one system for id: " + systemId)
      //      } else if (!result.isEmpty()) {
      //        logDebug("System was found")
      //        Some(result.get(0))
      //      } else {
      //        None
      //      }
    }

  }


  def deleteSystem(systemId: String) = {
    ScopeUtils.time(logger, "deleteSystem-db") {
      val system = findSystem(systemId).getOrElse(throw new SystemNotFound)
      systemsdb.remove(MongoDBObject("_id" -> system.systemId))
      //      mongoOp.remove(system.getOrElse(throw new SystemNotFound), "systems")
    }
  }

  def deleteInstance(systemId: String, instanceId: String) = {
    ScopeUtils.time(logger, "deleteInstance-db") {

      val lock = instanceLockMap.get(systemId + "-" + instanceId)
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
              //            mongoOp.remove(inst, "instances")
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

      //      // logInfo("""get product details for product "{}" and version "{}"""", productName, productVersion)
      //      val query1 = new BasicQuery("{ productName : '" + productName + "', productVersion : '" + productVersion + "' }");
      //      val result = mongoOp.find(query1, classOf[Product], "products")
      //
      //      if (result.size > 1)
      //        throw new IllegalArgumentException("there shouldn't be more than one product under this product name and version")
      //      else if (!result.isEmpty())
      //        Some(result.get(0))
      //      else
      //        None
    }
  }

  def findAllProducts: util.List[Product] = {

    ScopeUtils.time(logger, "findAllProducts-db") {
      val all = productsdb.find()
      val prods = new util.ArrayList[Product]()
      for (prod <- all) prods.add(grater[Product].asObject(prod))
      prods
      //      mongoOp.findAll(classOf[Product], "products")

    }
  }

  
  def deleteProduct(productName: String, productVersion: String) {
    ScopeUtils.time(logger, "deleteProduct-db") {
      val product = getProductDetails(productName, productVersion).getOrElse(throw new ProductNotFound)
      productsdb.remove(MongoDBObject("_id" -> product.id))
      //      mongoOp.remove(product.getOrElse(throw new ProductNotFound), "products")
    }
  }

  def getInstanceInfo(instance: Instance): Instance = {

    ScopeUtils.time(logger, "getInstanceInfo-db") {
      val lock = instanceLockMap.get(instance.systemId + "-" + instance.instanceId)
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

      //      val query1 = new BasicQuery("{ '_id' : '" + instanceId + "'}");
      //      val result = mongoOp.find(query1, classOf[Instance], "instances")
      //
      //      if (result.size > 1) {
      //        throw new IllegalArgumentException("there shouldn't be more than one instance for id: " + instanceId)
      //      } else if (!result.isEmpty()) {
      //        logDebug("Instance was found")
      //        Some(result.get(0))
      //      } else {
      //        None
      //      }
    }

  }

  def findInstanceBySystemId(systemId: String): Option[util.Collection[Instance]] = {

    ScopeUtils.time(logger, "findInstanceBySystemId-db") {

      val query = MongoDBObject("systemId" -> systemId)
      val instances = instancesdb.find(query)
      val instanceList = new util.ArrayList[Instance]()
      for (instance <- instances) instanceList.add(grater[Instance].asObject(instance))
      if (instanceList.isEmpty) None else Some(instanceList)

      //      instancesdb.findOneByID(instanceId) match {
      //        case None => None
      //        case Some(instance) => Some(grater[Instance].asObject(instance))
      //      }
      //
      //      val query1 = new BasicQuery("{ 'systemId' : '" + systemId + "'}");
      //      val result = mongoOp.find(query1, classOf[Instance], "instances")
      //
      //      if (!result.isEmpty()) {
      //        logDebug("Instances were found")
      //        Some(result)
      //      } else {
      //        None
      //      }
    }

  }

  val instanceLockMap = new ConcurrentHashMap[String, Option[ReentrantLock]]()

  def createInstance(instance: Instance): Unit = {
    ScopeUtils.time(logger, "createInstance-db") {
      //TODO: do we need lock here???
      val id = instance.instanceId
      val system = instance.systemId
      val lock: ReentrantLock = new ReentrantLock(true)
      instanceLockMap.put(system + "-" + id, Some(lock))
      instancesdb.update(MongoDBObject("_id" -> instance.instanceId), grater[Instance].asDBObject(instance), true)
      //      mongoOp.save(instance, "instances")
    }
  }

  def updateInstance(instance: Instance) = {
    ScopeUtils.time(logger, "updateInstance-db") {
      val lock = instanceLockMap.get(instance.systemId + "-" + instance.instanceId)
      lock match {
        case Some(l) => l.lock()
        case None =>
        case _ =>
      }
      try {
        instancesdb.update(MongoDBObject("_id" -> instance.instanceId), grater[Instance].asDBObject(instance))
        //      mongoOp.save(instance, "instances")
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
        //          mongoOp.insert(product, "products")
      } catch {
        case e: DuplicateKey => throw new ProductAlreadyExists()
      }

    }
  }

  def updateProduct(product: Product) = {
    ScopeUtils.time(logger, "updateProduct-db") {
      productsdb.update(MongoDBObject("_id" -> product.id), grater[Product].asDBObject(product))
      //      mongoOp.save(product, "products")
    }
  }

  def createService(service: Service): Unit = {
    ScopeUtils.time(logger, "createService-db") {
      servicesdb.insert(grater[Service].asDBObject(service))
      //      mongoOp.save(service, "services")
    }
  }

  def findService(id: String): Option[Service] = {
    ScopeUtils.time(logger, "findService-db") {

      servicesdb.findOneByID(id) match {
        case None => None
        case Some(service) => Some(grater[Service].asObject(service))
      }


      //      val query1 = new BasicQuery("{ '_id' : '" + id + "'}");
      //      val result = mongoOp.find(query1, classOf[Service], "services")
      //
      //      if (result.size > 1) {
      //        throw new IllegalArgumentException("there shouldn't be more than one service for id: " + id)
      //      } else if (!result.isEmpty()) {
      //        logDebug("Service was found")
      //        Some(result.get(0))
      //      } else {
      //        None
      //      }
    }
  }

  def updateMachineStatus(instanceId: String, machineName: String, status: String): Unit = {
    ScopeUtils.time(logger, "updateMachineStatus-db") {
      val q = MongoDBObject("_id" -> instanceId)
     // q.put("machineIds", machineName)
      val u = $set(s"machineIds.$machineName.provisionStatus" -> status)
      instancesdb.update(q, u)
    }
  }
}