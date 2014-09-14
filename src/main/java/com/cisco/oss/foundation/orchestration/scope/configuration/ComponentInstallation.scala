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

package com.cisco.oss.foundation.orchestration.scope.configuration

import com.cisco.oss.foundation.orchestration.scope.model.{Instance, Product, PuppetModule, ScopeNodeMetadata}

class MockComponentInstallationImpl extends IComponentInstallation {

  override def prepare(product: Product, instance: Instance, puppetModule: PuppetModule, server: ScopeNodeMetadata): Unit = {

  }

  def install(ccpHost: String, ccpPort: Int, systemId: String, instanceName: String, version: String, fqdn: String, installPath: String, processName: String, configSchema: String, properties: String, isNdsconsoleModule: Boolean): Unit = {
    logInfo(s"Install component into CCP server. $systemId")
    logInfo(s"Installation Done. $systemId")
  }

  override def delete(configurationServer: ScopeNodeMetadata, fqdn: String): Unit = {
    logInfo(s"Delete $fqdn from Configuration Server.")
  }
}
