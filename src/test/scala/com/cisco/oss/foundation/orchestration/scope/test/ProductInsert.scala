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

package com.cisco.oss.foundation.orchestration.scope.test

import org.springframework.context.support.ClassPathXmlApplicationContext
import com.cisco.oss.foundation.orchestration.scope.model._
import net.liftweb.json.NoTypeHints
import net.liftweb.json.Serialization
import net.liftweb.json.Serialization.write
import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import com.cisco.oss.foundation.orchestration.scope.model.AccessPoint
import com.cisco.oss.foundation.orchestration.scope.model.Product
import scala.Some
import com.cisco.oss.foundation.orchestration.scope.model.ProductOption
import com.cisco.oss.foundation.orchestration.scope.utils.ScopeUtils


object ProductInsert extends App {
  
  implicit val formats = Serialization.formats(NoTypeHints)

  val host = ScopeUtils.configuration.getString("mongodb.host", "localhost")
  val port = ScopeUtils.configuration.getInt("mongodb.port", 27017)
  val mongodbConnetion = MongoConnection(host,port)
  val scopedb = mongodbConnetion("scope")
  val systemsdb = scopedb("systems")
  val productsdb = scopedb("products")
  val servicesdb = scopedb("services")
  val productRepoUrl = "http://10.45.37.14/scope-products/test-1.0.0.0/"

  val context = new ClassPathXmlApplicationContext("META-INF/scopeServerApplicationContext.xml")
//  val mongoop = context.getBean(classOf[MongoOperations])

  systemsdb.insert(grater[System].asDBObject(System("2488","pwd", None)))
  
  var accessPoints = List[AccessPoint](AccessPoint("upm","http://<host>:6040/upm/households"),
                                       AccessPoint("ndsconsole","https://<host>:5015/ndsconsole/app.html"))

  var additionalInfo = Map[String, String] (
    ("info1", "1"),
    ("info2", "2")
  )

  var product = Product("MongoDB-1.48.0.0", "MongoDB", "1.48.0.0",
    List[ProductOption](
      ProductOption("sampleKey1", None, "String sample key", None, OptionType.STRING, "defVal1", Some(Array[String]("defVal1", "defVal2", "defVal3")), false, additionalInfo),
      ProductOption("sampleKey2", None, "int sample key", None, OptionType.NUMBER, "123", None, false, additionalInfo),
      ProductOption("sampleKey3", None, "boolean sample key", None, OptionType.BOOLEAN, "true", None, false, additionalInfo),
      ProductOption("sampleKey4", None, "file sample key", None, OptionType.FILE, "", None, false, additionalInfo)),
    productRepoUrl
  )
  
  println(ScopeUtils.mapper.writeValueAsString(product))

//  println(write(Instance("instId","systemId","instName",Some("failed"), Some("vm creation has failed or timed out"), product)))

  productsdb insert (grater[Product].asDBObject(product))


  val retProd = grater[Product].asObject((productsdb findOneByID("IdentityManagement-1.23.4.7")).get)
//  retProd.accessPoints match {
//    case Some(ap) => println ("found: " + ap)
//    case None => println ("nothing found")
//  }
//  mongoop.save(product, "products")

  product = Product("IM-3.48.0.0","IM", "3.48.0.0",
    List(
      ProductOption("sampleKey1", None, "String sample key", None, OptionType.STRING, "defVal1", Some(Array[String]("defVal1", "defVal2", "defVal3")), false),
      ProductOption("sampleKey2", None, "int sample key", None, OptionType.NUMBER, "123", None, false),
      ProductOption("sampleKey3", None, "boolean sample key", None, OptionType.BOOLEAN, "true", None, false),
      ProductOption("sampleKey4", None, "file sample key", None, OptionType.FILE, "", None, false)),
    productRepoUrl
  )

      println(write(product))
//  mongoop.save(product, "products")
  productsdb insert (grater[Product].asDBObject(product))

//  val retProd2 = grater[Product].asObject((productsdb findOneByID("IdentityManagement-1.23.4.8")).get)
//  retProd2.accessPoints match {
//    case Some(ap) => println ("found: " + ap)
//    case None => println ("nothing found")
//  }

  val service = Service("651","IdentityManagement", "1.23.4.7")
  servicesdb.insert(grater[Service].asDBObject(service))

}