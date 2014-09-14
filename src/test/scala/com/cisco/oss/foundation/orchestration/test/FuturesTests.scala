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

package com.cisco.oss.foundation.orchestration.test

import org.junit.Test
import com.cisco.oss.foundation.flowcontext.{FlowContext, FlowContextFactory}
import scala.concurrent._
import ExecutionContext.Implicits.global

/**
 * Created by Yair Ogen on 27/03/2014.
 */
class FuturesTests {


  @Test def testFutures() {

    println("before")
    var fc: FlowContext = null

    val futurelist = (1 to 5).map((x) => {

      FlowContextFactory.createFlowContext()
      fc = FlowContextFactory.getFlowContext
      getFuture

    })

    val futures = Future.sequence(futurelist)

    futures.onSuccess {
      case x => {
        FlowContextFactory.addFlowContext(fc)
        println(s"[${Thread.currentThread().getName}}] success: ${FlowContextFactory.getFlowContext}")
      }
    }

    println("after")

    Thread.sleep(20000)
  }

  def getFuture: Future[Any] = {
    val flowContext = FlowContextFactory.getFlowContext
    future {
      val flowCtxtString = flowContext.toString
      println(s"[${Thread.currentThread().getName}}]hello: $flowCtxtString")
    }
  }

}
