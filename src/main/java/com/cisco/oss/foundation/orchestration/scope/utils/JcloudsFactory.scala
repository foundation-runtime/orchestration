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

import java.util
import java.util.Properties
import com.cisco.oss.foundation.orchestration.scope.ScopeConstants
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.MoreExecutors._
import org.jclouds.ContextBuilder
import org.jclouds.aws.ec2.compute.AWSEC2TemplateOptions
import org.jclouds.compute.ComputeServiceContext
import org.jclouds.compute.options.TemplateOptions
import org.jclouds.concurrent.config.ExecutorServiceModule
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import org.jclouds.openstack.nova.v2_0.compute.options.NovaTemplateOptions
import org.jclouds.sshj.config.SshjSshClientModule
import org.jclouds.vsphere.compute.options.VSphereTemplateOptions

import scala.collection.JavaConversions._

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/13/14
 * Time: 4:22 PM
 */
object JcloudsFactory extends Slf4jLogger {
  private val modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule(), new SLF4JLoggingModule());
  private val overrides = new Properties()
  private val cloudProvider = ScopeUtils.configuration.getString("cloud.provider")
  private val serverProviderName = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.server")

  private val username: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.user")
  private val password: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.password")

  private val contextBuilder = ContextBuilder.newBuilder(serverProviderName)
    .credentials(username, password)
    .modules(modules)

  readJcloudsConfiguration

  def readJcloudsConfiguration(): Unit = {
    val keys: util.Iterator[String] = ScopeUtils.configuration.getKeys("jclouds")

    keys.foreach{
      case key => {
        val value = ScopeUtils.configuration.getString(key)
        overrides.setProperty(key, value)
      }
    }
//    contextBuilder.overrides(overrides)
  }

   def computeServiceContext() = {
     cloudProvider match {
       case "openstack" => {
         val endpoint = Option(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.endpoint")) match {
           case Some(ep) => ep
           case None => {
             logError(s"Could not find value for 'cloud.provider.$cloudProvider.endpoint'")
             throw new NoSuchElementException(s"'cloud.provider.$cloudProvider.endpoint' doesn't map to an existing object")
           }
         }
         contextBuilder.endpoint(endpoint)
       }
       case "vsphere" => {
         val endpoint = Option(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.endpoint")) match {
           case Some(ep) => ep
           case None => {
             logError(s"Could not find value for 'cloud.provider.$cloudProvider.endpoint'")
             throw new NoSuchElementException(s"'cloud.provider.$cloudProvider.endpoint' doesn't map to an existing object")
           }
         }
         contextBuilder.endpoint(endpoint)
         val initPass = Option(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.vm.initPassword")) match {
           case Some(pwd) => pwd
           case None => "master"
         }
         logTrace(s"VM init password $initPass")
         overrides.put("jclouds.vsphere.vm.password", initPass)


       }
       case "aws" => {
         val endpoint = Option(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.endpoint")) match {
           case Some(ep) => {
        	 contextBuilder.endpoint(ep)
           }
           case None => {
             logInfo(s"Could not find value for 'cloud.provider.$cloudProvider.endpoint'. Using default")
           }
         }
       }
       case _ =>
     }
     contextBuilder.overrides(overrides)
     contextBuilder.buildView(classOf[ComputeServiceContext])
   }

  def templateBuilder(imageId: String, location: String,
                      minRam: Int, minDisk:Int, minCores: Int, arch: String,
                      tags: List[String], networks :List[String],
                      name: String, group: String,
                      script: Option[String], flpImage: Option[String], isoImage: Option[String], postConfiguration: Boolean, folder: Option[String]) = {
    val options: TemplateOptions = computeServiceContext.getComputeService.templateOptions()
    options.inboundPorts(22)
           .tags(tags)
           .networks(networks)
           .nodeNames(ImmutableSet.of(name))
    script match {
      case Some(s) => options.runScript(s)
      case None =>
    }
    cloudProvider match {
      case "openstack" => {
         options.asInstanceOf[NovaTemplateOptions].securityGroupNames(getDefaultSecurityGroups)
         .generateKeyPair(true).keyPairName(group)
      }
      case "aws" => {
        val vPCSubnetId: String = ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.vpc.subnetId")
        options.asInstanceOf[AWSEC2TemplateOptions].subnetId(vPCSubnetId)
          .keyPair(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.keyPairName"))
          .securityGroupIds(ScopeUtils.configuration.getString(s"cloud.provider.aws.vpc.securityGroupID"))
          .securityGroups(List(group))
          .userMetadata("group", group)
          .userMetadata("name", name)
          .overrideLoginCredentials(ScopeUtils.getLoginForCommandExecution())
          .overrideLoginUser(ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.loginUser"))
          //.mapNewVolumeToDeviceName("/dev/sdb", minDisk, true)
          .mapEphemeralDeviceToDeviceName("/dev/sdb", "ephemeral0")
      }
      case "vsphere" => {
        flpImage match {
          case Some(image) => options.asInstanceOf[VSphereTemplateOptions].flpFileName(s"${ScopeConstants.SCOPE_RESOURCES_FOLDER}$image")
          case None =>
        }
        isoImage match {
          case Some(image) => options.asInstanceOf[VSphereTemplateOptions].isoFileName(s"${ScopeConstants.SCOPE_RESOURCES_FOLDER}$image")
          case None =>
        }

        options.asInstanceOf[VSphereTemplateOptions].postConfiguration(postConfiguration)

        folder match {
          case Some(f) => options.asInstanceOf[VSphereTemplateOptions].vmFolder(f)
          case None =>
        }

        options.asInstanceOf[VSphereTemplateOptions].datacenterName(location)

        ScopeUtils.configuration.getString(s"cloud.provider.$cloudProvider.switchType") match {
          case "distributed" => options.asInstanceOf[VSphereTemplateOptions].distributedVirtualSwitch(true)
          case "regular" => options.asInstanceOf[VSphereTemplateOptions].distributedVirtualSwitch(false)
          case _ => options.asInstanceOf[VSphereTemplateOptions].distributedVirtualSwitch(false)
        }
      }
      case _ =>
    }

    computeServiceContext.getComputeService.templateBuilder
      .imageId(imageId)
      .minRam(minRam)
      .minDisk(minDisk)
      .minCores(minCores)
      .osArchMatches(arch)
      .locationId(location)
      .options(options)
      //.biggest()
      .build()
  }
  
  private def getDefaultSecurityGroups() ={
    val keys = ScopeUtils.configuration.getKeys(s"cloud.provider.$cloudProvider.defaultSecurityGroups")
    var defaults = List[String]()
    keys.foreach(key => {
      val name = ScopeUtils.configuration.getString(key)
      defaults = name :: defaults
    })
    defaults
  }

}
