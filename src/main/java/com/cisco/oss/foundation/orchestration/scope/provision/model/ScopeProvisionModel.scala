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

package com.cisco.oss.foundation.orchestration.scope.provision.model

import com.cisco.oss.foundation.orchestration.scope.utils.ScopeUtils

/**
 * Created by mgoldshm on 4/10/14.
 */
object ScopeProvisionModel {
  final val FOUNDATION_PUPPET_PATH: String = "/etc/puppet/scope_puppet"
  final val PRODUCT_PUPPET_PATH: String = "/etc/puppet/prodpuppet"
}


class RoleInfo(val roleName: String, val envName: String, val puppetRoleScript: String, val hieraRole: String)

case class ProductRepoInfo(productUrl: String, repoName: String = "base") {
  val productPuppetRepoUrl = s"${productUrl}prodpuppet/"
  val productYumRepoUrl = s"${productUrl}yum/"
  val productMaterializerUrl = s"${productUrl}materializer/"
  val basePuppetRepoUrl = s"${ScopeUtils.configuration.getString("basePuppetRepoUrl")}/scope-base/puppet/"
}

