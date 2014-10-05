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

import java.nio.file.{Files, Paths}

import com.cisco.oss.foundation.orchestration.scope.model.ScopeStatement
import com.google.common.collect.ImmutableList
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.scriptbuilder.domain.{OsFamily, Statement, StatementList}

import scala.io.Source

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/6/14
 * Time: 9:01 AM
 */
class ExistsMachineBootstrapStatements(val baseRepoUrl: String, val osVersion: String) extends ScopeStatement {

  private val statements: ImmutableList.Builder[Statement] = ImmutableList.builder[Statement]
  private val baseDir: String = "/opt/cisco/scope/scripts"

  def addStatement(element: Statement) {
    statements.add(element)
  }

  def functionDependencies(family: OsFamily): java.lang.Iterable[String] = {
    ImmutableList.of[String]
  }

  def render(family: OsFamily): String = {
    if (family eq OsFamily.WINDOWS) throw new UnsupportedOperationException("windows not yet implemented")

    statements.add(exec("cd /etc/"))
    statements.add(exec("sudo mkdir -p /etc/yum.repos.d/save"))
    statements.add(exec("sudo mv /etc/yum.repos.d/*.repo /etc/yum.repos.d/save/"))

    statements.add(exec("sudo /bin/bash -c \"cat >> /etc/yum.repos.d/scope.repo << EOF"))
    statements.add(exec("[scope]"))
    statements.add(exec("name=scope_repo"))
    statements.add(exec(s"baseurl=${baseRepoUrl}/scope-base/yum/$osVersion/" + "\\\\\\$basearch"))
    statements.add(exec("enabled=1"))
    statements.add(exec("gpgcheck=0"))
    statements.add(exec("EOF"))
    statements.add(exec("\""))

    statements.add(exec("sudo yum -y install ruby"))
    statements.add(exec("sudo yum -y install libselinux-ruby"))
    if (osVersion.equals("5"))
      statements.add(exec("sudo yum -y install puppet-2.7.21-1"))
    else
      statements.add(exec("sudo yum -y install puppet-2.7.21-1.el6"))
    statements.add(exec("sudo yum -y install hiera"))
    statements.add(exec("sudo yum -y install hiera-puppet"))
    statements.add(exec("sudo yum -y install augeas"))
    statements.add(exec("sudo yum -y install wget"))
    statements.add(exec("sudo yum -y install expect"))
    statements.add(exec("sudo yum -y install sysstat"))
    statements.add(exec("sudo yum -y install strace"))
    statements.add(exec("sudo yum -y install dos2unix"))

    if (Files.exists(Paths.get(s"$baseDir/exists_machine_bootstrap.sh")))
      Source.fromFile(s"$baseDir/exists_machine_bootstrap.sh").getLines().foreach(line => statements.add(exec(line)))

    new StatementList(statements.build).render(family)
  }
}
