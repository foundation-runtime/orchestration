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

import org.jclouds.scriptbuilder.domain.{StatementList, OsFamily, Statement}
import com.google.common.collect.ImmutableList
import org.jclouds.scriptbuilder.domain.Statements._

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/6/14
 * Time: 9:01 AM
 * To change this template use File | Settings | File Templates.
 */
class MapDiskStatements(val partitionSize: List[Int], volumeGroupNames: List[String]) extends Statement {

  private val statements: ImmutableList.Builder[Statement] = ImmutableList.builder[Statement]

  def addStatement(element: Statement) {
    statements.add(element)
  }

  def functionDependencies(family: OsFamily): java.lang.Iterable[String] = {
    ImmutableList.of[String]
  }

  def render(family: OsFamily): String = {
    if (family eq OsFamily.WINDOWS) throw new UnsupportedOperationException("windows not yet implemented")

    statements.add(exec("sudo -s"))
    statements.add(exec("cd /etc/"))

    statements.add(exec("ram_size=`head -1 /proc/meminfo | awk '{print $2;}'`;"))
    statements.add(exec("if [ $ram_size -le 2097152 ]; then"))
    statements.add(exec("   swap_size=$((ram_size*2));"))
    statements.add(exec("else"))
    statements.add(exec("   swap_size=$((ram_size+2097152));"))
    statements.add(exec("fi"))




    statements.add(exec("echo 'fdisk /dev/sdb <<EOF"))
    statements.add(exec("\np"))

    val pvcreateStatement: ImmutableList.Builder[Statement] = ImmutableList.builder()
    val vgextendStatement: ImmutableList.Builder[Statement] = ImmutableList.builder()
    var index = 1
    (partitionSize, volumeGroupNames).zipped.foreach {
      case (size, name) => {
        statements.add(exec("\nn"))
        statements.add(exec("\np"))
        statements.add(exec(s"\n$index"))
        statements.add(exec("\n"))
        statements.add(exec(s"\n+${size}G"))
        statements.add(exec(s"\nt"))
        statements.add(exec(s"\n$index"))
        statements.add(exec(s"\n8e"))
        pvcreateStatement.add(exec(s"vgextend $name /dev/sdb$index"))

        //vgdisplay VolGroup >> /tmp/volgroup 2>&1;");
        //ethScript.append("\nawk 'BEGIN { free=0; alloc=0; } /Alloc/ { alloc=\\$7 } /Free/ { free=\\$7 } END { print \\\"-L+\\\" free - alloc \\\"G\\\" }' /tmp/volgroup | xargs lvextend /dev/VolGroup/lv_root >> /tmp/jclouds-init.log 2>&1;");
        vgextendStatement.add(exec(s"vgdisplay $name >> /tmp/$name"))
        vgextendStatement.add(exec("awk 'BEGIN { free=0; alloc=0; } /Alloc/ { alloc=\\$7 } /Free/ { free=\\$7 } END { print \\\"-L+\\\" free - alloc \\\"G\\\" }' /tmp/" + name + s" | xargs lvextend `ls /dev/" + name + "`"))
        vgextendStatement.add(exec("resize2fs `ls /dev/" + name + "`"))
        index += 1
      }
    }
    statements.add(exec("\nw"))
    statements.add(exec("\nEOF' > /tmp/fdisk.sh"))
    statements.add(exec("chmod 0700 /tmp/fdisk.sh "))
    statements.add(exec("./tmp/fdisk.sh"))
    statements.add(exec("rm -f /tmp/fdisk.sh"))
    statements.add(exec("rm -f /tmp/vg_*"))
    statements.addAll(pvcreateStatement.build())
    statements.addAll(vgextendStatement.build())


    new StatementList(statements.build).render(family)
  }
}
