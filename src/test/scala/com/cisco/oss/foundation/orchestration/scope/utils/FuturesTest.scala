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
import org.junit.Assert._
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.cisco.oss.foundation.flowcontext.FlowContextFactory



class FuturesTest {
  implicit val flowEC = new FlowContextExecutor(global)
  final val FCID = "111222333"
  @Test
  def flowContextTest {
    future {
      assertNull(FlowContextFactory.getFlowContext)
    }

    FlowContextFactory.createFlowContext(FCID)

    val t1 = future {
      assertEquals(FCID, FlowContextFactory.getFlowContext.getUniqueId)
    }

    val t2 = future {
      assertEquals(FCID, FlowContextFactory.getFlowContext.getUniqueId)
    }

    val seq = Future.sequence(Seq(t1, t2))
    seq onSuccess {
      case s:Seq[Unit] => assertEquals(FCID, FlowContextFactory.getFlowContext.getUniqueId)
    }

    seq onFailure {
      case t => fail(s"Failure encountered: ${t}")
    }

    Thread.sleep(1000)
  }


}
