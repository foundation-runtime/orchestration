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

package com.cisco.oss.foundation.orchestration.scope.scripting.scala

import java.io.File

import com.cisco.oss.foundation.orchestration.scripting.ScalaScriptEngineWrapper
import org.junit.Test
//import opt.cisco.scope.plugins.{WaitForSocketPlugin}

/**
 * CISCO LTD.
 * User: igreenfi
 * Date: 08/06/2014 2:48 PM
 * Package: com.cisco.oss.foundation.orchestration.scope.scripting.scala
 */
class SSETest {

  @Test
  def sseTest() ={
    val engine: ScalaScriptEngineWrapper = new ScalaScriptEngineWrapper(new File("d:/opt/cisco/scope/plugins"))
//    val pl = engine.getPlugin("opt.cisco.scope.plugins.InstallCopPlugin")
//    val pl = engine.getPlugin("opt.cisco.scope.plugins.WaitForSocketPlugin")
    val pl = engine.getPlugin("opt.cisco.scope.plugins.SaslPlugin")
    pl match {
//      case Some(p) => p.run("user = admin\npassword = roZes123\nhost = 10.45.38.3\nremoteHost = 10.45.37.10\nremoteUser = copuser\nremotePassword = master\ncopFilePath = /opt/cisco/scopeData/products/Conductor-3.1.0.0/materializer/resources\ncopFilename = cisco.conductor-cmc-3.1-0-1877.cop.sgn")
//      case Some(p) => p.run("user = admin\npassword = roZes123\nhost = 10.45.38.3\nremoteHost = 10.45.37.10\nremoteUser = copuser\nremotePassword = master\ncopFilePath = /opt/cisco/scopeData/products/Conductor-3.1.0.0/materializer/resources\ncopFilename = cisco.conductor-rootaccess-2.1.1-0.cop.sgn")
      case Some(p) => p.run("seleniumRemoteServer = 10.45.37.221\ncmcHost = 10.45.38.3\nremoteHost = 10.45.37.10\nremoteUser = root\nremotePassword = master\nremotePath = /opt/cisco/scopeData/products")
      case None =>
    }
  }

  //@Test
  def copPluginTest(){
//    val p = new WaitForSocketPlugin()
//    p.run("timeout = 80000\nhost = 10.45.38.3\nport = 7300\n")
//    val p = new InstallCopPlugin()
//    p.run("user = admin\npassword = roZes123\nhost = 10.45.38.3\nremoteHost = 10.45.37.10\nremoteUser = copuser\nremotePassword = master\ncopFilePath = /opt/cisco/scopeData/products/Conductor-3.1.0.0/materializer/resources\ncopFilename = cisco.conductor-cmc-3.1-0-1877.cop.sgn")
  }


  @Test
  def saslPluginTest(){
    //val p = new SaslPlugin()
    //p.run("seleniumRemoteServer = 10.45.37.221\ncmcHost = 10.45.38.3\nremoteHost = 10.45.37.10\nremoteUser = root\nremotePassword = master\nremotePath = /opt/cisco/scopeData/products")
    //    val p = new InstallCopPlugin()
    //    p.run("user = admin\npassword = roZes123\nhost = 10.45.38.3\nremoteHost = 10.45.37.10\nremoteUser = copuser\nremotePassword = master\ncopFilePath = /opt/cisco/scopeData/products/Conductor-3.1.0.0/materializer/resources\ncopFilename = cisco.conductor-cmc-3.1-0-1877.cop.sgn")
  }
}
