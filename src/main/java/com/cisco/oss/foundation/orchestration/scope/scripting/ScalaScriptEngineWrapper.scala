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

package com.cisco.oss.foundation.orchestration.scope.scripting

import com.cisco.oss.foundation.orchestration.scope.utils.Slf4jLogger
import com.googlecode.scalascriptengine.{ScalaScriptEngine, RefreshAsynchronously}
import java.io.File
import com.googlecode.scalascriptengine.CompilationStatus.Complete
import com.googlecode.scalascriptengine.CompilationStatus.{Failed, Complete}
import org.apache.commons.io.FileUtils
import scala.collection.mutable.HashSet
import scala.collection.JavaConversions._

class ScalaScriptEngineWrapper(val sourceDirectory: File) extends Slf4jLogger with AutoCloseable {
  val scriptEngine: Option[RefreshAsynchronously] =
    if (sourceDirectory.exists() && sourceDirectory.isDirectory()) {
      val defaultConfig = ScalaScriptEngine.defaultConfig(sourceDirectory)
      val jarFiles = FileUtils.listFiles(sourceDirectory,Array("jar"),true)
      val ccp: HashSet[File] = new HashSet[File]()
      jarFiles.foreach(file => ccp +=  file)
      defaultConfig.compilationClassPaths.foreach(file => ccp += file)
      val clcp: HashSet[File] = new HashSet[File]()
      jarFiles.foreach(file => clcp +=  file)
      defaultConfig.classLoadingClassPaths.foreach(file => clcp += file)

      val modifiedConfig = defaultConfig.copy(classLoadingClassPaths = clcp.toSet, compilationClassPaths = ccp.toSet)
      Some(ScalaScriptEngine.onChangeRefreshAsynchronously(modifiedConfig, 500))
    } else {
      logError(s"source directory doesn't exists or not directory. ${sourceDirectory.getAbsolutePath}")
      None
    }

  this.scriptEngine.getOrElse(throw new IllegalStateException("Can't create script engine!")).doRefresh
  waitForTheFirstCompilationToFinish()

  def waitForTheFirstCompilationToFinish() {
    while (! scriptEngine.get.compilationStatus.step.isInstanceOf[Complete.type])
      try {
        Thread.sleep(50)
        if (scriptEngine.get.compilationStatus.step.isInstanceOf[Failed.type])
          throw new IllegalStateException("Plugin compilation Failed.")
      } catch {
        case e: InterruptedException => logTrace(e.getMessage(), e)
      }
  }

  def getPlugin(key: String): Option[IPlugin] = {
    try {
      scriptEngine match {
        case Some(engine) => {
          val plugin: IPlugin = engine.get(key).newInstance()
          Some(plugin)
        }
        case None => None
      }
    } catch {
      case e: ClassNotFoundException => {
        scriptEngine match {
          case Some(engine) => {
            engine.doRefresh
            None
          }
          case None => None
        }
      }
      case e: Exception => {
        logError("failed load plugin: {}", key)
        logError(e.getMessage, e)
        None
      }
    }
  }

  def close() {
    this.scriptEngine match {
      case Some(se) => se.shutdown
      case None =>
    }
  }

}
