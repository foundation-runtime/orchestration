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

package com.cisco.oss.foundation.orchestration.utils

import scala.concurrent.{ExecutionContextExecutor, ExecutionContext}
import com.cisco.oss.foundation.flowcontext.FlowContextFactory

/**
 * Created by mgoldshm on 3/29/14.
 */


class FlowContextExecutor(ec: ExecutionContext) extends ExecutionContextExecutor {

  override def reportFailure(t: Throwable) {
    ec.reportFailure(t)
  }

  override def execute(runnable: Runnable) {
    ec.execute(new Runnable {
      val fc = FlowContextFactory.getFlowContext

      override def run() {
        val threadFC = FlowContextFactory.getFlowContext
        FlowContextFactory.addFlowContext(fc)
        try {
          runnable.run
        } finally {
          FlowContextFactory.addFlowContext(threadFC)
        }
      }
    })
  }
}
