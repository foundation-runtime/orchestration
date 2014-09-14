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

package com.cisco.oss.foundation.orchestration.model

import org.springframework.beans.factory.FactoryBean
import com.fasterxml.jackson.databind.ObjectMapper
import com.cisco.oss.foundation.orchestration.utils.ScopeUtils

/**
 * Created by Yair Ogen on 12/15/13.
 */
class ObjectMapperFactory extends FactoryBean[ObjectMapper]{
  def getObject: ObjectMapper = ScopeUtils.mapper

  def getObjectType: Class[_] = classOf[ObjectMapper]

  def isSingleton: Boolean = true
}
