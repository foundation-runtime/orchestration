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

package com.cisco.oss.foundation.orchestration.scope

import org.junit.{Assert, Test}
import com.mongodb.{MongoClient, Mongo}
import com.novus.salat._
import com.novus.salat.global._
import com.mongodb.casbah.Imports._
import com.cisco.oss.foundation.orchestration.scope.model.{System, ScopeNodeMetadata}
import scala.collection.immutable.Map

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 1/23/14
 * Time: 10:20 AM
 * To change this template use File | Settings | File Templates.
 */
class ScopeModelTest {
  @Test
  def testScopeNodeMetadata() {
    val mongodb: Mongo = new MongoClient("localhost")
    val col = mongodb.getDB("test").getCollection("ScopeModel")

    val node = ScopeNodeMetadata("id", "hostname",None, Some("rsaKey"), Set("private"), Set("public"), "group", Set("tag"), "url", "STARTING")

    col.insert(grater[ScopeNodeMetadata].asDBObject(node))

    val obj = col.findOne()

    val node1 = grater[ScopeNodeMetadata].asObject(obj)

    Assert.assertEquals(node, node1)

  }


  @Test
  def testSystem() {

    for (i <- 0 until 1)
      println(i)
    val mongodb: Mongo = new MongoClient("localhost")
    val col = mongodb.getDB("test").getCollection("ScopeModel")
    val c = new BasicDBObject()
    c.put("_id","izek")
    val obj = col.findOne(c)
    val newSystem = grater[System].asObject(obj)
    val ccpServersDetails = newSystem.foundation.get.machineIds.filter( set => set._2.hostname.contains("ccp") )

    val ccpServers = ccpServersDetails match {
      case addresses:Map[String, ScopeNodeMetadata] if (addresses.size > 0) => addresses.values.toList
      case _ => List()
    }

    println(ccpServers)
  }
}
