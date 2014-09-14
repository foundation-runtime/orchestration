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

import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.util.Random
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.MDC

//trait UUIDSupport {
//  def createUUID(){
//    MDC.put("UUID", UUID.getNextUUID)
//  }
//
//  def createUUID(uuid: String){
//    MDC.put("UUID", uuid)
//  }
//}

class UUID

object UUID {
  private var _genmachine: Int = _
  private var nextInc: AtomicInteger = new AtomicInteger((new Random()).nextInt())
  {
    var machinePiece: Int = 0
    try {
      val sb = new StringBuilder()
      val e = NetworkInterface.getNetworkInterfaces
      while (e.hasMoreElements()) {
        val ni = e.nextElement()
        sb.append(ni.toString)
      }
      machinePiece = sb.toString.hashCode << 16
    } catch {
      case e: Throwable => {
        machinePiece = (new Random().nextInt()) << 16
      }
    }
    var processPiece: Int = 0
    var processId = new java.util.Random().nextInt()
    try {
      processId = java.lang.management.ManagementFactory.getRuntimeMXBean
        .getName
        .hashCode
    } catch {
      case t: Throwable =>
    }
    val loader = classOf[UUID].getClassLoader
    val loaderId = if (loader != null) System.identityHashCode(loader) else 0
    val sb = new StringBuilder()
    sb.append(Integer.toHexString(processId))
    sb.append(Integer.toHexString(loaderId))
    processPiece = sb.toString.hashCode & 0xFFFF
    _genmachine = machinePiece | processPiece
  }
  //***** End of static fields/methods *****

  def getNextUUID(): String = {
    val time = (System.currentTimeMillis() / 1000).toInt
    val machine = _genmachine
    nextInc.compareAndSet(Integer.MAX_VALUE, 0)
    val inc = nextInc.getAndIncrement
    val bytes = Array.ofDim[Byte](12)
    val byteBuffer = ByteBuffer.wrap(bytes)
    byteBuffer.putInt(time)
    byteBuffer.putInt(machine)
    byteBuffer.putInt(inc)
    val buf = new StringBuilder(24)
    for (b <- bytes) {
      val x = b & 0xFF
      val s = Integer.toHexString(x)
      if (s.length == 1) buf.append("0")
      buf.append(s)
    }
    buf.toString
  }

}