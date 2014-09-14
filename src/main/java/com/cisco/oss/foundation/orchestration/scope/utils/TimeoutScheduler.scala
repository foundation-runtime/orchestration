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

import org.jboss.netty.util.{Timeout, TimerTask, HashedWheelTimer}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.Duration
import org.jboss.netty.handler.timeout.TimeoutException
import java.util.concurrent.TimeUnit

/**
 * User: Yair Ogen
 * Date: 10/15/13
 * Time: 12:06 PM
 * taken from: http://stackoverflow.com/a/16305056/200937
 */
object TimeoutScheduler {

  val timer = new HashedWheelTimer()

  def scheduleTimeout(promise: Promise[_], after: Duration) = {

    timer.newTimeout(new TimerTask {
      def run(timeout: Timeout) {
        promise.failure(new TimeoutException("Operation timed out after " + after.toMillis + " millis"))
      }
    }, after.toNanos, TimeUnit.NANOSECONDS)
  }

  def withTimeout[T](fut: Future[T])(implicit ec: ExecutionContext, after: Duration) = {
    val prom = Promise[T]()
    val timeout = TimeoutScheduler.scheduleTimeout(prom, after)
    val combinedFut = Future.firstCompletedOf(List(fut, prom.future))
    fut onComplete {
      case result => timeout.cancel()
    }
    combinedFut
  }
}
