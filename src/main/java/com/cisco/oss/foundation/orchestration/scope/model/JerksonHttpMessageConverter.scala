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

package com.cisco.oss.foundation.orchestration.scope.model


class JerksonHttpMessageConverter /* extends AbstractHttpMessageConverter[Object](new MediaType("application", "json", Charset.forName("UTF-8"))) {

  val json = new Json {
    def canWrite(clazz: Class[_]) = mapper.canSerialize(clazz)
    def canDeserialize(clazz: Class[_]) =
      mapper.canDeserialize(mapper.constructType(clazz))
  }

  override def writeInternal(o: Object, outputMessage: HttpOutputMessage) = {
    try {
      json.generate(o, outputMessage.getBody)
    } catch {
      case ex: Exception =>
        throw new HttpMessageNotWritableException(
          "Could not write JSON: " + ex.getMessage(), ex);
    }
  }

  override def readInternal(clazz: Class[_ <: Object],
    inputMessage: HttpInputMessage) = {
    try {
      json.parse(inputMessage.getBody())(Manifest.classType(clazz))
    } catch {
      case ex: JsonParseException =>
        throw new HttpMessageNotReadableException(
          "Could not read JSON: " + ex.getMessage(), ex);
    }
  }

  override def supports(clazz: Class[_]): Boolean = {
    throw new UnsupportedOperationException()
  }

  override def canRead(clazz: Class[_], mediaType: MediaType): Boolean = {
    json.canDeserialize(clazz) && canRead(mediaType)
  }

  override def canWrite(clazz: Class[_], medianType: MediaType): Boolean = {
    json.canWrite(clazz) && canWrite(medianType)
  }
}*/