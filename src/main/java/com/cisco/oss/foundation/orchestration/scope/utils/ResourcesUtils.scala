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

import com.cisco.oss.foundation.orchestration.scope.ScopeConstants
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.MoreExecutors._
import org.apache.commons.io.FilenameUtils
import org.jclouds.ContextBuilder
import org.jclouds.concurrent.config.ExecutorServiceModule
import org.jclouds.sshj.config.SshjSshClientModule
import org.jclouds.vsphere.FileManagerApi

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 1/27/14
 * Time: 1:05 PM
 * To change this template use File | Settings | File Templates.
 */
object ResourcesUtilsFactory {


  def instance() = {
    ScopeUtils.configuration.getString("cloud.provider") match {
      case "rackspace" => NullResourceApi
      case "aws" => NullResourceApi
      case "openstack" => NullResourceApi
      case "vsphere" => VSphereResourcesApi
      case _ => throw new UnsupportedOperationException("Could NOT match provider File API. ( provider : " + ScopeUtils.configuration.getString("cloud.provider") + " )")
    }
  }
}

trait ScopeResourcesApi extends Slf4jLogger {
  protected val modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
  protected val cloudProvider = ScopeUtils.configuration.getString("cloud.provider")
  protected val fileProviderName = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.file")

  protected val username: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.user")
  protected val password: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.password")

  protected var context: ContextBuilder = null

  protected def init() {
    context = ContextBuilder.newBuilder(fileProviderName)
      .credentials(username, password)
      .modules(modules)
    cloudProvider match {
      case "vsphere" | "openstack" => {
        val endpoint = Option(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.endpoint")) match {
          case Some(ep) => ep
          case None => {
            logError(s"Could not find value for 'cloud.provider.$cloudProvider.endpoint'")
            throw new NoSuchElementException(s"'cloud.provider.$cloudProvider.endpoint' doesn't map to an existing object")
          }
        }
        context.endpoint(endpoint)
      }
      case "aws" => {
        val endpoint = Option(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.endpoint")) match {
          case Some(ep) => {
            context.endpoint(ep)
          }
          case None => {
            logInfo(s"Could not find value for 'cloud.provider.$cloudProvider.endpoint'. Using default")
          }
        }
      }
      case _ =>
    }
  }

  def loadFile(file: String)
}

object VSphereResourcesApi extends ScopeResourcesApi {
  init()
  private val resourcesApi: FileManagerApi = context.buildInjector().getInstance(classOf[FileManagerApi])

  def loadFile(file: String): Unit = {
    val filename = FilenameUtils.getName(file)
    resourcesApi.uploadFile(file, s"${ScopeConstants.SCOPE_RESOURCES_FOLDER}$filename")
  }
}

object NullResourceApi extends ScopeResourcesApi {
  def loadFile(file: String): Unit = {
    logInfo("**** Unimplemented!!! ****")
  }
}