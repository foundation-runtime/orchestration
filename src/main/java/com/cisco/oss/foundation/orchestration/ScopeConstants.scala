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

package com.cisco.oss.foundation.orchestration

object ScopeConstants {

  val BASE = "scope.http."
  val CONNECTION_IDLE_TIME = BASE + "connectionIdleTime"
  val IS_BLOCKING_CHANNEL_CONNECTOR = BASE + "isBlockingChannelConnector"
  val MAX_THREADS = BASE + "maxThreads"
  val MIN_THREADS = BASE + "minThreads"
  val NUMBER_OF_ACCEPTORS = BASE + "numberOfAcceptors"
  val PORT = BASE + "port"
  val HOST = BASE + "host"
  val REQUEST_HEADER_SIZE = BASE + "requestHeaderSize"
  val ACCEPT_QUEUE_SIZE = BASE + "acceptQueueSize"
  val BENTO_SYSTEM_ID = "service.scope.bento.system.id"

  val SCOPE_RESOURCES_FOLDER = "scope/resources/"

  val CLOUD_ENV_HAS_DNS = "cloud.env.hasDns"
  val FOUNDATION_NAME = "foundation.name"

  val FAILED = "FAILED"
  val STARTED = "STARTED"
}

object ScopeErrorMessages {
  val NO_RSA = "No rsa private key: "
}