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

import org.springframework.data.mongodb.config.AbstractMongoConfiguration
import com.mongodb.Mongo
import org.springframework.context.annotation.Configuration
import com.cisco.oss.foundation.orchestration.scope.utils.ScopeUtils
import com.mongodb.MongoClient
import com.mongodb.ServerAddress
import com.mongodb.MongoClientOptions
import com.mongodb.WriteConcern

@Configuration
class MongoConfig extends AbstractMongoConfiguration {

  def getDatabaseName: String = "scope"

  def mongo: Mongo = {
    val host = ScopeUtils.configuration.getString("mongodb.host", "localhost")
    val port = ScopeUtils.configuration.getInt("mongodb.port", 27017)
    val address = new ServerAddress(host, port)
    val mongoOptions = new MongoClientOptions.Builder().writeConcern(WriteConcern.FSYNC_SAFE).build()
    new MongoClient(address, mongoOptions)
  }

}