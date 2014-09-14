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

import org.junit.Test
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.MustMatchersForJUnit
import org.scalatest.junit.ShouldMatchersForJUnit
import org.jclouds.ssh.SshKeys
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class LoadBalancerUtilsTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit  {
  @Test
  def testLoadBalancerUtils() {

    val utils = new LoadBalancerUtils(6040, "upm", "upm", "test", "izek", SshKeys.generate().toMap)
    utils.addBackendServer("10.45.37.146:6040")
    utils.addBackendServer("10.45.37.52:6040")

    utils.createLoadBalancer("IM")


    println("end")
    Thread.sleep((10 hours).toMillis)
  }
}
