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

package com.cisco.oss.foundation.orchestration.scope.jclouds

import com.google.common.util.concurrent.MoreExecutors.sameThreadExecutor;
import com.cisco.oss.foundation.orchestration.scope.utils._
import org.scalatest.junit.{MustMatchersForJUnit, ShouldMatchersForJUnit, JUnitSuite}
import org.junit.{Ignore, Test}
import org.jclouds.ContextBuilder

import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.concurrent.config.ExecutorServiceModule
import com.google.common.collect.ImmutableSet
import org.jclouds.compute.domain.{ExecResponse, NodeMetadata}
import org.jclouds.openstack.nova.v2_0.NovaApi
import scala.concurrent.duration._
import scala.collection.JavaConversions._
import scala.io.Source
import org.jclouds.domain.LoginCredentials
import org.jclouds.sshj.config.SshjSshClientModule
import org.jclouds.compute.options.TemplateOptions.Builder._
import org.jclouds.scriptbuilder.ScriptBuilder
import org.jclouds.scriptbuilder.domain.Statements._
import org.jclouds.scriptbuilder.domain.OsFamily
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule
import org.jclouds.ssh.SshKeys
import scala.Some
import java.util.Properties

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 12/31/13
 * Time: 8:58 AM
 * To change this template use File | Settings | File Templates.
 */
@Ignore
class JcouldesTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit {


  val NAME = "vcs.com"

  //private var  computeService: ComputeService = null
  //private final ServerApi serverApi;


  @Test
  def testConnect() {
    val modules = ImmutableSet.of(new ExecutorServiceModule(sameThreadExecutor(), sameThreadExecutor()), new SshjSshClientModule(), new SLF4JLoggingModule())

    val p = new Properties
    p.setProperty("jclouds.trust-all-certs","true")
    val clazz: Class[NovaApi] = classOf[NovaApi]
//    val context = ContextBuilder.newBuilder("rackspace-cloudservers-us")
    val context = ContextBuilder.newBuilder("vcloud")
   // val context = ContextBuilder.newBuilder("stub")
      //val context = ContextBuilder.newBuilder(new NovaApiMetadata())
//      .credentials(ScopeUtils.configuration.getString("cloud.provider.rackspace.user"), ScopeUtils.configuration.getString("cloud.provider.rackspace.password"))
  .endpoint("https://10.56.161.100/api")
      .credentials("root", "master1234")
      .modules(modules)
  .overrides(p)
      //.buildApi(clazz)
      .buildView(classOf[ComputeServiceContext])

    /* val serverOptions: CreateServerOptions = CreateServerOptions.
                                               Builder.adminPass("master1").
                                                       networks(Lists.newArrayList("ecfe7c06-0b29-435e-99a1-0b6d115544c0",
                                                                                   "00000000-0000-0000-0000-000000000000")
                                                               )
 */

    val images = context.getComputeService.listImages()
    val internalSubnet = "10.234.0.0:255.255.255.0"

    /*
    save=$IFS
IFS='
'
for line in `ip -4 addr | grep inet | grep eth`
do
ip=`echo $line | awk '{ print $2 }'`
network_ip=`ipcalc -n $ip`
if echo $network_ip | grep -q '10.234.0.0' ; then
dev=`echo $line | awk '{ print $7 }'`
touch /etc/sysconfig/network-scripts/route-$dev
echo ADDRESS0=10.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev
echo NETMASK0=255.0.0.0 >> /etc/sysconfig/network-scripts/route-$dev
echo GATEWAY0=10.234.0.1 >> /etc/sysconfig/network-scripts/route-$dev
#service network restart
fi
done

IFS=$save

     */


    val builder = NetworkUtils.getNetworkInitScript("10.234.0.0", "10.234.0.1", "10.234.0.29", "5", false)

    val keyPair1 = SshKeys.generate()
    // val keyPair2 = SshKeys.generate()
    val publicKey = keyPair1.get("public")
    val privateKey = keyPair1.get("private")
    val loc  = context.getComputeService.listAssignableLocations()
    val img  = context.getComputeService.listImages()
    val template = context.getComputeService.templateBuilder
      //.imageId("DFW/b68b7322-1451-40be-9e21-9c30ba80631e")   //5.9
      .imageNameMatches("CISCO CentoOS 5.9")
      .minRam(1048)
      .locationId("DFW")
      .os64Bit(true)
      .hardwareId("DFW/performance1-4")
      .options(inboundPorts(22)
      .tags(List("Izek Machine"))
      //.networks(List("00000000-0000-0000-0000-000000000000","ecfe7c06-0b29-435e-99a1-0b6d115544c0"))
      .networks(List("ecfe7c06-0b29-435e-99a1-0b6d115544c0"))
      .nodeNames(ImmutableSet.of("test81"))
      .authorizePublicKey(publicKey)
      .installPrivateKey(privateKey).runScript(builder.render(OsFamily.UNIX)))
      .build()


    //val newMachines = context.getComputeService.createNodesInGroup("izek-im", 1, template)
    logInfo("Finish Create Machine.")
    val nodes = context.getComputeService.listNodes()
    logInfo("Finish List nodes.")
    nodes.foreach {
      case node: NodeMetadata => {
        val nodeId = node.getId
        val publicIP = node.getPublicAddresses.head
        var result: ExecResponse = null;
        if (nodeId.toLowerCase.endsWith("abfe9312-2ef3-45a5-ae54-fa622909a0e4")) {

          val builder = new ScriptBuilder()
          builder.addStatement(exec(s"iptables -A INPUT -p tcp --tcp-flags ALL NONE -j DROP"))

          context.getComputeService.runScriptOnNode(nodeId, builder.render(OsFamily.UNIX), overrideLoginCredentials(LoginCredentials.builder().user("root").password("").build()))
          //result = context.getComputeService.runScriptOnNode(nodeId, "ip -4 addr | grep inet | grep eth", overrideLoginCredentials(getLoginForCommandExecution())
          //  .runAsRoot(true))
          println(result.getOutput)

        }
      }
    }
    //val serverApi = context.getServerApiForZone("DFW")
    //val computeService = context.unwrap()
    //val nova : RestContext[NovaApi, NovaAsyncApi] = computeService.
    // val compute = serverApi.create("test_jclouds","00ca0594-6d44-458b-a9f3-f623d1fd242d","5",serverOptions)

    Thread sleep ((1 hour).toMillis)
  }

  @Test
  def configureHosts() {
    val builder = new ScriptBuilder()

    builder.addStatement(exec("echo 1234"))

    val vmUtils = new VMUtils

    vmUtils.runScriptOnMatchingNodes(builder.render(OsFamily.UNIX), "configureHosts", Some("izek-izek"), Some("ccp"), Some(List("ScopeFoundation")), "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEArsXS3WV8SSSF0zc3ZxtA7eXzBXEpc9erRLrskaoK2lBp7sqb\nAoOE7POcMvWDfHhl1XHsGqfz6iGfi0fRqXAoIYMAazacuz3QovELao3cmAy9QyjW\nRBr3GvFYlSYiT9Wm7R8ouiAa3QtWD29lscuZYS5qcm2MYaZZ0zc1xS7Tet+eAQ4L\nSHJGcjV/PzWX7vnBzeWxBK6kQcXpU+YdR3KCyp2buwO8tYLj7ZtpLQaNqFU97Oob\nUX7hFJ2+ca4+NPXtff+KDqLvE8b8sZVvaESBGOEARyUhQQsQ0+1IsJVfXlDyJuK7\n/ktFHE7AYkvUB6CpAzDquw+kxx4HLTmWJewtIwIDAQABAoIBAQCam/ZGVRj1u5GH\noDkqxTlTOzEZh1ocWJXyX0oYMk0XhDuyOxmVx7M3yupLSlXfLsMnZ3huvFVSshvp\noscfFVDrFHCyZ8WO/sgq3QI0aPrUp7BmUSH7bb9b3lV23B1OcrsQ2Ze1z+Dz2qtv\nTCSyfGJbTixsveyKFhcweo7euOXss59apP6vdiLxEroOFyiX6OzLXjKXaeOO/klM\nTB3uYKmgNwHFEbkABXVgRk0cf6dQ7oYzZY1F7FthEZtmaOavhf1UeOMxNqWG8yhy\nGeVHJe5/q4vkNp5yQoqLypCzRr+sGh+SKxHorIA0PTdCSPOIR/7CTY5O5Rdd3KJG\nXkFYQpIBAoGBANT1utbiU7AXhvS0gFkeJvPbDl0PdHMMUNbUN8iy65xhuUomU1M1\n3udeBS2frlT3EGCxrACyLXyU929nTxNUOWlTNq6ifYMbL96+4YOD0lQtADHCF86i\n7AjY0CxS+w17YyOHZhzFtXAVrikOTxTf5M6BUfFXCh6koZpBDK1dXfThAoGBANIY\nVznRu7zKkPXa/wuHKglPl10MBwdPPIQFkvGWsB/PFY2LayX8Ie556pmlPRM1yE/s\nT1/8NyPvm5Xm/hJuXmyqR77w9b0WeOGpE0QqMtkkJEwHx1UNG55m5yZWOOp6SdkG\nYHMwichOPg3qipYIESrlkwXXPJFj7O9UPYxzVZ6DAoGAayn4ynd764n7VJqcV7wv\nl9js4nGLhPV2nJSQOcOngrs+dYzRs3bRY5ZRfsLzBDiLmkzuJC+FakLAPOQ8y5o/\nhbvlMX39MQN2VF2zt+2W4tr+VQu51TbxFgacwyFCymcD3XYeMW5gPjX6vRhBPVzp\nk+1JoevN4NuxoqEHUxo/1KECgYB1416efQdhKjEpI5dVOqTl0JlYhrUy7s1AL3iC\nw1FUp/iqEf8vs0i+7f4r9MJOkzExV6I6c+Xk7kBZXuJWKUQmqW3UwJTDY2a1CRBc\ngWlC2rrbRsoc+Vv0CD3QDzWkrLXfZ2qbeDL4CJ9dY6wb/67SaTuUXXAJDQ8YRCa9\nNsJmTQKBgQC99ap/wiaT+8zL0u2F8hmd5dCYYrtrUVSZxlKg4e/2zS+7Ck/Ugfrw\nLJUus7BW9BKVACbrj3fiiJd/QCbJuE7ojJpNj7gwWp9DHeYNApFlhnRxa/Ez5U+d\ndfMrAvz60xbro6+tFagdSUJ9YLtWWl2j9Rc7pfyHokcdHC0KO0NPnQ==\n-----END RSA PRIVATE KEY-----")
  }

  @Test
  def openstackTest() {
   // JcloudsFactory.computeServiceContext().getComputeService.createNodesInGroup()
  }


  private def getLoginForCommandExecution(): LoginCredentials = {
    try {
      val user = "root"
      //val privateKey = Source.fromFile(ScopeUtils.configuration.getString("service.scope.ssh.privatekey"))
      val privateKey = Source.fromFile("D:/SVN/scopeServer/id_rsa").getLines().mkString("\n")
      return LoginCredentials.builder().
        user(user).privateKey(privateKey).build();
    } catch {
      case e: Throwable => return null;
    }
  }
}
