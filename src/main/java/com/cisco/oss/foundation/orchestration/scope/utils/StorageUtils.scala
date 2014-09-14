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

import com.google.common.collect.ImmutableSet
import org.jclouds.concurrent.config.ExecutorServiceModule
import com.google.common.util.concurrent.MoreExecutors._
import org.jclouds.sshj.config.SshjSshClientModule
import org.jclouds.ContextBuilder
import org.jclouds.openstack.nova.v2_0.extensions.{VolumeAttachmentApi, VolumeApi}
import org.jclouds.openstack.nova.v2_0.options.CreateVolumeOptions
import org.jclouds.openstack.nova.v2_0.NovaApi

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 4/2/14
 * Time: 12:13 PM
 */
object StorageUtilsFactory {


  def instance() = {
    ScopeUtils.configuration.getString("cloud.provider") match {
      case "rackspace" => NullStorageUtils
      case "aws" => NullStorageUtils
      case "openstack" => OpenStackStorageUtils
      case "vsphere" => NullStorageUtils
      case _ => throw new UnsupportedOperationException("Could NOT match provider Storage API. ( provider : " + ScopeUtils.configuration.getString("cloud.provider") + " )")
    }
  }
}


trait StorageUtils {
  protected val modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule());
  protected val cloudProvider = ScopeUtils.configuration.getString("cloud.provider")
  protected val storageProviderName = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.storage")

  protected val username: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.user")
  protected val password: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.password")
  protected val location: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.location")

  protected var context: ContextBuilder = null

  def init() {
    context = ContextBuilder.newBuilder(storageProviderName)
      .credentials(username, password)
      .modules(modules)
  }

  def createStorage(size: Int, name: String)
  def deleteStorage(id: String): Boolean
  def attachStorage(volumeId: String, serverId: String, device: String)
  def detachVolumeFromServer(volumeId: String, serverId: String) : Boolean

}

object OpenStackStorageUtils extends StorageUtils {
  init()
  context.endpoint(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.endpoint"))
  private val novaApi = context.buildApi(classOf[NovaApi])

  def createStorage(size: Int, name: String) = {
    val volumeApi = novaApi.getVolumeExtensionForZone(location).get()
    val createVolumeOptions: CreateVolumeOptions = CreateVolumeOptions.Builder.availabilityZone(location)
                                                                               .volumeType("lvm")
                                                                               .name(name)
                                                                               .description("Scope server via JClouds")
    volumeApi.create(size, createVolumeOptions)
  }

  def deleteStorage(id: String) = {
    val volumeApi = novaApi.getVolumeExtensionForZone(location).get()
    volumeApi.delete(id)
  }

  def attachStorage(volumeId: String, serverId: String, device: String) = {
    val volumeAttachmentApi = novaApi.getVolumeAttachmentExtensionForZone(location).get()
    volumeAttachmentApi.attachVolumeToServerAsDevice(volumeId, serverId, device)
  }

  def detachVolumeFromServer(volumeId: String, serverId: String) = {
    val volumeAttachmentApi = novaApi.getVolumeAttachmentExtensionForZone(location).get()
    volumeAttachmentApi.detachVolumeFromServer(volumeId, serverId)
  }
}

object NullStorageUtils extends StorageUtils {
  def createStorage(size: Int, name: String): Unit = ???

  def deleteStorage(id: String): Boolean = ???

  def attachStorage(volumeId: String, serverId: String, device: String): Unit = ???

  def detachVolumeFromServer(volumeId: String, serverId: String): Boolean = ???
}
