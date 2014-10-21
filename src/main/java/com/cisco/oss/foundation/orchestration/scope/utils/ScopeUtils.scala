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

import java.net.URL
import java.util.Collections
import java.util.concurrent.TimeUnit

import com.cisco.oss.foundation.orchestration.scope.model.{Node, ScopeNodeMetadata}
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.configuration.reloading.FileChangedReloadingStrategy
import org.jclouds.domain.LoginCredentials
import org.jclouds.ssh.SshKeys
import org.slf4j.{Logger, LoggerFactory}

import scala.io.Source

object ScopeUtils {

  val configuration = {
    val config = new PropertiesConfiguration("config.properties")
    config.setReloadingStrategy(new FileChangedReloadingStrategy());
    config
  }
  val logger: Logger = LoggerFactory.getLogger("com.cisco.oss.foundation.orchestration.scope.utils.ScopeUtils-time")

  val mapper = getMapper

  def getMapper() = {
    val module = new DefaultScalaModule
    val mapper = new ObjectMapper()
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true)
    mapper.registerModule(module)
    mapper
  }

  def generateRsaPair() = {
    SshKeys.generate()
  }

  def getLoginUser(): String = {
    val cloudProvider = ScopeUtils.configuration.getString("cloud.provider")
    Option(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.loginUser")) match {
      case Some(user) => user
      case None => "root"
    }
  }

  def getLoginForCommandExecution(): LoginCredentials = {
    try {
      val user = "root"
      val privateKey = Source.fromFile(ScopeUtils.configuration.getString("service.scope.ssh.privatekey")).getLines().mkString("\n")
      return LoginCredentials.builder().
        user(user).privateKey(privateKey).build();
    } catch {
      case e: Throwable => return null;
    }
  }

  def time[T](logger: Logger, methodName: String)(method: => T): T = {

    ScopeUtils.logger.debug( """[Executing "{}"]""", methodName)

    val startTime = System.nanoTime()

    // call the method
    try {
      return method
    } catch {
      case e: Exception =>
        ScopeUtils.logger.error( """[Error in "{}": {}]""", Array[AnyRef](methodName, e.toString(), e): _*)
        throw e
    } finally {
      val endTime = System.nanoTime()

      val total = endTime - startTime

      var unit: TimeUnit = TimeUnit.NANOSECONDS
      var res = total

      if (ScopeUtils.logger.isDebugEnabled()) {
        val seconds = TimeUnit.SECONDS.convert(total, TimeUnit.NANOSECONDS)
        val millis = TimeUnit.MILLISECONDS.convert(total, TimeUnit.NANOSECONDS)
        val micros = TimeUnit.MICROSECONDS.convert(total, TimeUnit.NANOSECONDS)
        if (seconds > 0) {
          unit = TimeUnit.SECONDS
          res = seconds
        } else if (millis > 0) {
          unit = TimeUnit.MILLISECONDS
          res = millis
        } else if (micros > 0) {
          unit = TimeUnit.MICROSECONDS
          res = micros
        }
      }

      ScopeUtils.logger.debug( """["{}" took {} {}]""", Array[String](methodName, res + "", unit.toString()): _*)

    }

  }

  def getPathDepth(url: String) = {
    val urlObj = new URL(url)
    urlObj.getPath.split("/").length - 1
  }

  def unionMap[K, V](m1: Map[K, V], m2: Map[K, V]): Map[K, V] = {
    val tuples: List[(K, V)] = m2.toList ++ m1.toList
    tuples.toMap
  }

  def ScopeNodeMetadataList2NodeList(input: Iterable[ScopeNodeMetadata]) = {
    input.map(m => Node(m.id, m.hostname, "", "", "", "", 0, 0, 0, Collections.emptyList(), false, None, None, None, None, true, None, None)).toList
  }

}


//object HttpServerUtil {
//
//  def addFiltersToServletContextHandler(serviceName: String, threadPool: ThreadPool, context: ServletContextHandler) {
//    context.addFilter(new FilterHolder(new UUIDFilter(serviceName)), "/*", EnumSet.allOf(classOf[DispatcherType]))
//    context.addFilter(new FilterHolder(new ErrorHandlingFilter(serviceName)), "/*", EnumSet.allOf(classOf[DispatcherType]))
//    //    context.addFilter(new FilterHolder(new HttpMethodFilter(serviceName)), "/*", EnumSet.allOf(classOf[DispatcherType]))
//    //    context.addFilter(new FilterHolder(new RequestValidityFilter(serviceName)), "/*", EnumSet.allOf(classOf[DispatcherType]))
//    context.addFilter(new FilterHolder(new AvailabilityFilter(serviceName, threadPool)), "/*", EnumSet.allOf(classOf[DispatcherType]))
//    //    context.addFilter(new FilterHolder(new TraceFilter(serviceName)), "/*", EnumSet.allOf(classOf[DispatcherType]))*/
//    //    context.addFilter(new FilterHolder(new PingFilter(serviceName)), "/*", EnumSet.allOf(classOf[DispatcherType]))
//    context.addFilter(new FilterHolder(new CrossOriginFilter(serviceName)), "/*", EnumSet.allOf(classOf[DispatcherType]))
//
//  }
//
//  def getDefaultThreadPool(serviceName: String): QueuedThreadPool = {
//    val configuration = ScopeUtils.configuration
//    val queuedThreadPool = new QueuedThreadPool()
//    val minThreads = configuration.getInt("service." + serviceName + ".http.minThreads", 100)
//    val maxThreads = configuration.getInt("service." + serviceName + ".http.maxThreads", 1000)
//    queuedThreadPool.setMaxThreads(maxThreads)
//    queuedThreadPool.setMinThreads(minThreads)
//    queuedThreadPool
//  }
//
//
//}
