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

import java.lang.Iterable

import com.cisco.oss.foundation.orchestration.model.ScopeStatement
import com.google.common.collect.ImmutableList
import org.apache.commons.lang3.StringUtils
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.scriptbuilder.domain.{OsFamily, Statement, StatementList}

import scala.io.Source

/**
 * Created by igreenfi on 12/08/2014.
 */
class ShellScriptLoader(baseDir: String, scriptName: String) extends ScopeStatement {
  private val statements: ImmutableList.Builder[Statement] = ImmutableList.builder[Statement]

  if (StringUtils.isNotEmpty(scriptName))
    Source.fromFile(s"$baseDir/$scriptName").getLines().foreach(line => statements.add(exec(line)))

  def addStatement(element: Statement) {
    statements.add(element)
  }

  override def functionDependencies(p1: OsFamily): Iterable[String] = ImmutableList.of[String]

  override def render(family: OsFamily): String = {
    if (family eq OsFamily.WINDOWS)
      throw new UnsupportedOperationException("windows not yet implemented")
    new StatementList(statements.build).render(family)
  }
}
