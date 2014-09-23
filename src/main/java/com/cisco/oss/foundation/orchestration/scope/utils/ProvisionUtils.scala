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

import org.jclouds.scriptbuilder.domain.Statements.exec
import org.jclouds.scriptbuilder.domain.OsFamily
import org.jclouds.scriptbuilder.domain.Statement
import org.jclouds.scriptbuilder.domain.StatementList
import com.google.common.collect.ImmutableList
import com.cisco.oss.foundation.orchestration.scope.provision.model.{ScopeProvisionModel, ProductRepoInfo, RoleInfo}

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/2/14
 * Time: 7:52 AM
 */
class ProvisionStatements(productRepoInfo: ProductRepoInfo , roleInfo: RoleInfo) extends Statement {
  private val productPuppetRepoPath = ScopeProvisionModel.PRODUCT_PUPPET_PATH
  private val basePuppetRepoPath = ScopeProvisionModel.FOUNDATION_PUPPET_PATH

  private val productPuppetRepoUrl = productRepoInfo.productPuppetRepoUrl
  private val basePuppetRepoUrl = productRepoInfo.basePuppetRepoUrl

  private val puppetRoleScript = roleInfo.puppetRoleScript
  private val puppetHieraRole = roleInfo.hieraRole
  private val puppetRoleName = roleInfo.roleName
  private val puppetEnvName = roleInfo.envName

  def functionDependencies(family: OsFamily): java.lang.Iterable[String] = {
    ImmutableList.of[String]
  }


  def render(family: OsFamily): String = {
    if (family eq OsFamily.WINDOWS) throw new UnsupportedOperationException("windows not yet implemented")
    val statements: ImmutableList.Builder[Statement] = ImmutableList.builder[Statement]

    statements.add(exec(s"sleep 10"))
    statements.add(exec(s"rm -rf $productPuppetRepoPath"))
    statements.add(exec(s"mkdir -p $productPuppetRepoPath"))
    statements.add(exec(s"cd $productPuppetRepoPath"))

    //statements.add(exec(s"git clone $repoPuppetUrl $instancePuppetRepoPath"))
    statements.add(exec(s"""wget --quiet -r -nH --level=100 --no-parent --cut-dirs=${ScopeUtils.getPathDepth(productPuppetRepoUrl)} --reject="index.html*" $productPuppetRepoUrl"""))
    statements.add(exec("success=$?"))
    statements.add(exec(s"find $productPuppetRepoPath -iname *.sh -exec chmod 700 '{}' \\;"))


    statements.add(exec(s"rm -rf $basePuppetRepoPath"))
    statements.add(exec(s"mkdir -p $basePuppetRepoPath"))
    statements.add(exec(s"cd $basePuppetRepoPath"))
    statements.add(exec(s"""wget --quiet -r -nH --level=100 --no-parent --cut-dirs=${ScopeUtils.getPathDepth(basePuppetRepoUrl)} --reject="index.html*" $basePuppetRepoUrl"""))
    statements.add(exec("success=$?"))
    statements.add(exec(s"find $basePuppetRepoPath -iname *.sh -exec chmod 700 '{}' \\;"))

    statements.add(exec(s"mv $basePuppetRepoPath/modules/role/manifests/$puppetRoleName.pp $basePuppetRepoPath/modules/role/manifests/${puppetRoleName}_`date +%d%m%Y_%H%M%S`.pp"))
    statements.add(exec(s"cat << ROLE_EOF > $basePuppetRepoPath/modules/role/manifests/$puppetRoleName.pp"))
    statements.add(exec(puppetRoleScript))
    statements.add(exec(s"ROLE_EOF"))
    statements.add(exec(s"mv $basePuppetRepoPath/hieradata/role/$puppetRoleName.json $basePuppetRepoPath/hieradata/role/${puppetRoleName}_`date +%d%m%Y_%H%M%S`.json"))
    statements.add(exec(s"cat << HIERA_EOF > $basePuppetRepoPath/hieradata/role/$puppetRoleName.json"))
    statements.add(exec(puppetHieraRole))
    statements.add(exec(s"HIERA_EOF"))
    statements.add(exec(s"chmod 600 $basePuppetRepoPath/hieradata/role/$puppetRoleName.json"))

    statements.add(exec(s"augtool set /files/etc/puppet/puppet.conf/main/environment $puppetEnvName"))
    statements.add(exec(s"augtool set /files/etc/puppet/puppet.conf/main/modulepath /etc/puppet/modules:/usr/share/puppet/modules:$productPuppetRepoPath/modules:$basePuppetRepoPath/modules:$basePuppetRepoPath/forge"))

    statements.add(exec(s"rm -f /etc/puppet/hiera.yaml"))
    statements.add(exec(s"ln -s $basePuppetRepoPath/hieradata/hiera.yaml /etc/puppet/hiera.yaml"))
    statements.add(exec(s"rm -f /etc/hieradata"))
    statements.add(exec(s"ln -s $basePuppetRepoPath/hieradata /etc/hieradata"))

    statements.add(exec(s"export FACTER_vmrole=$puppetRoleName"))
    statements.add(exec(s"export FACTER_fqdn=cisco"))
    statements.add(exec(s"[ -f /var/log/puppet/agent.log ] && mv /var/log/puppet/agent.log /var/log/puppet/agent_`date +%d%m%Y_%H%M%S`.log"))
    statements.add(exec(s"$basePuppetRepoPath/provisioning/puppet/puppet_check.sh role::$puppetRoleName"))
    statements.add(exec( "if [ $? -ne 0 ]; then"))
    statements.add(exec(s"   mv /var/log/puppet/agent.log /var/log/puppet/agent_`date +%d%m%Y_%H%M%S`.log"))
    statements.add(exec(s"   $basePuppetRepoPath/provisioning/puppet/puppet_check.sh role::$puppetRoleName"))
    statements.add(exec( "else"))
    statements.add(exec( "   exit 0"))
    statements.add(exec( "fi"))

    new StatementList(statements.build).render(family)
  }

}
