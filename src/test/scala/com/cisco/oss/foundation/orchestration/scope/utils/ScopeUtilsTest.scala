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

package com.cisco.oss.foundation.orchestration.scope.utils

import com.cisco.oss.foundation.orchestration.scope.model.{Products, Instance}
import com.fasterxml.jackson.databind.jsonschema.JsonSchema
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper
import org.junit.{Assert, Test}

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/4/14
 * Time: 12:57 PM
 * To change this template use File | Settings | File Templates.
 */
class ScopeUtilsTest extends Slf4jLogger {
  implicit val flowEC = new FlowContextExecutor(global)


  var m2 = Map(1-> "d")

  var m1 = Map(2-> "f", 1-> "d")

  @Test
  def uniunMapsTest() {
    val unionMap: Map[Int, String] = ScopeUtils.unionMap(m1, m2)
    Assert.assertEquals(Map(1-> "d", 2-> "f"), unionMap)
  }

  @Test
  def pojo2schema() = {

    val visitor = new SchemaFactoryWrapper()
    ScopeUtils.mapper.acceptJsonFormatVisitor(classOf[com.cisco.oss.foundation.orchestration.scope.model.Product], visitor)
    val schema = visitor.finalSchema();
    println(ScopeUtils.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(schema));
  }

}
