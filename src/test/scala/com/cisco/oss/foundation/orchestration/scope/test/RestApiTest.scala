/*
 * Copyright 2014 Cisco Systems, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package com.cisco.oss.foundation.orchestration.scope.test

import com.cisco.oss.foundation.configuration.ConfigurationFactory
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory
import com.cisco.oss.foundation.http.{HttpMethod, HttpRequest, HttpResponse}
import com.cisco.oss.foundation.orchestration.scope.dblayer.SCOPeDB
import com.cisco.oss.foundation.orchestration.scope.main.{RunScope => RunScopeMain}
import com.cisco.oss.foundation.orchestration.scope.model.{AccessPoint, ControlStatusRequest, Instance, Product, ProductOption, ProvisionRequest, System, _}
import com.cisco.oss.foundation.orchestration.scope.utils.{ScopeUtils, Slf4jLogger}
import com.mongodb.casbah.Imports._
import com.novus.salat._
import com.novus.salat.global._
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.{MongodExecutable, MongodStarter}
import de.flapdoodle.embed.process.runtime.Network
import net.liftweb.json._
import org.junit.runner.RunWith
import org.junit.{AfterClass, BeforeClass, Test}
import org.scalatest.junit.{JUnitSuite, MustMatchersForJUnit, ShouldMatchersForJUnit}
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import scala.collection.JavaConversions._
import scala.collection.immutable.Map

object RestApiTest extends Slf4jLogger {
  ConfigurationFactory.getConfiguration.setProperty("scope-ui.isEnabled", false)
  val run = new RunScopeMain
  val httpClient = ApacheHttpClientFactory.createHttpClient("scopeClient", false)
  //  val host = ScopeUtils.configuration.getString(ScopeConstants.HOST, "localhost")
  //  val port = ScopeUtils.configuration.getInt(ScopeConstants.PORT)

  val mongoPort = Network.getFreeServerPort
  ScopeUtils.configuration.setProperty("mongodb.port", mongoPort.toString)
  ScopeUtils.configuration.setProperty("mongodb.host", "localhost")
  var mongodExecutable: MongodExecutable = null
  val productRepoUrl = "http://10.45.37.14/scope-products/test-1.0.0.0/"

  @BeforeClass def init() {


    val mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", mongoPort, false))
      .build()

    val runtime = MongodStarter.getDefaultInstance()


    try {
      mongodExecutable = runtime.prepare(mongodConfig)
      val mongod = mongodExecutable.start()

    } finally {

    }
    createScopeDB

    run.start()
    Thread.sleep(2000)
  }

  def createScopeDB() {
    val mongoConnection = MongoConnection("localhost", mongoPort)
    val scopedb = mongoConnection("scope")
    val systemsdb = scopedb("systems")
    val productsdb = scopedb("products")
    val servicesdb = scopedb("services")

    systemsdb.insert(grater[System].asDBObject(System("bento", "pwd", None)))
    systemsdb.insert(grater[System].asDBObject(System("dummysystem", "pwd", None)))

    var product = Product("MongoDB-1.48.0.0", "MongoDB", "1.48.0.0",
      List(
        ProductOption("sampleKey1", None, "String sample key", None, OptionType.STRING, "defVal1", Some(Array[String]("defVal1", "defVal2", "defVal3")), false),
        ProductOption("sampleKey2", None, "int sample key", None, OptionType.NUMBER, "123", None, false),
        ProductOption("sampleKey3", None, "boolean sample key", None, OptionType.BOOLEAN, "true", None, false),
        ProductOption("sampleKey4", None, "file sample key", None, OptionType.FILE, "", None, false)),
      productRepoUrl

    )


    productsdb insert (grater[Product].asDBObject(product))


    product = Product("IM-3.48.0.0", "IM", "3.48.0.0",
      List(
        ProductOption("sampleKey1", None, "String sample key", None, OptionType.STRING, "defVal1", Some(Array[String]("defVal1", "defVal2", "defVal3")), false),
        ProductOption("sampleKey2", None, "int sample key", None, OptionType.NUMBER, "123", None, false),
        ProductOption("sampleKey3", None, "boolean sample key", None, OptionType.BOOLEAN, "true", None, false),
        ProductOption("sampleKey4", None, "file sample key", None, OptionType.FILE, "", None, false)),
      productRepoUrl
    )
    productsdb insert (grater[Product].asDBObject(product))

    product = Product("Gluster-3.2.1.4", "Gluster", "3.2.1.4",
      List(
        ProductOption("sampleKey1", None, "String sample key", None, OptionType.STRING, "defVal1", Some(Array[String]("defVal1", "defVal2", "defVal3")), false),
        ProductOption("sampleKey2", None, "int sample key", None, OptionType.NUMBER, "123", None, false),
        ProductOption("sampleKey3", None, "boolean sample key", None, OptionType.BOOLEAN, "true", None, false),
        ProductOption("sampleKey4", None, "file sample key", None, OptionType.FILE, "", None, false)),
      "http://localhost/scope-products/Gluster-3.2.1.4/"
    )

    productsdb insert (grater[Product].asDBObject(product))

    product = Product("ScopeFoundation-1.50.0.0", "ScopeFoundation", "1.50.0.0",
      List(),
      productRepoUrl
    )

    productsdb insert (grater[Product].asDBObject(product))

    val service = Service("1283", "IM", "3.48.0.0")
    servicesdb.insert(grater[Service].asDBObject(service))
  }

  @AfterClass def stop() {
    logInfo("*** @AfterClass: Stopping mongo! ***")
    Thread.sleep(2500)
    run.stop
    if (mongodExecutable != null)
      mongodExecutable.stop()
  }
}

@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("classpath*:/META-INF/scopeServerApplicationContext.xml"))
class RestApiTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit {

  @Autowired var scopedb: SCOPeDB = _

  @Test def testCreateSystem() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .header("Accept", "test/plain")
      //    .entity("")
      .contentType("text/plain")
      .build()

    var response = RestApiTest.httpClient.executeDirect(request)

    try {

      request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/systems/123456")
        .httpMethod(HttpMethod.POST)
        .header("Accept", "test/plain")
        .entity("mypwd")
        .contentType("text/plain")
        .build()

      response = RestApiTest.httpClient.executeDirect(request)

      response getStatus() should equal(200)


      request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/systems/123456")
        .httpMethod(HttpMethod.POST)
        .header("Accept", "test/plain")
        .entity("mypwd")
        .contentType("text/plain")
        .build()

      response = RestApiTest.httpClient.executeDirect(request)

      response getStatus() should equal(409)

    } catch {
      case e: Exception =>
        logger.error("Error in {}", e.toString())
    } finally {


      request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/systems/123456")
        .httpMethod(HttpMethod.DELETE)
        .build()

      response = RestApiTest.httpClient.executeDirect(request)

      val responseStatus = response getStatus()
      responseStatus should equal(200)
    }

  }

  @Test def testCreateSystemAndFail() {

    val request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      .contentType("text/plain")
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    logInfo(response.getResponseAsString)
    response.getStatus() should equal(400)
  }

  @Test def testUpdateInstance() {
    val productRepoUrl = "http://localhost/scope-products/Gluster-3.2.1.4/"
    val product = Product("Gluster-3.2.1.4", "Gluster", "3.2.1.4",
      List(
        ProductOption("sampleKey1", None, "String sample key", None, OptionType.STRING, "defVal1", Some(Array[String]("defVal1", "defVal2", "defVal3")), false),
        ProductOption("sampleKey2", None, "int sample key", None, OptionType.NUMBER, "123", None, false),
        ProductOption("sampleKey3", None, "boolean sample key", None, OptionType.BOOLEAN, "true", None, false),
        ProductOption("sampleKey4", None, "file sample key", None, OptionType.FILE, "", None, false)),
      productRepoUrl
    )
    val instanceId = "wawaw"
    scopedb.createInstance(Instance(instanceId, "12345", "update", None, None, product, Map(), List(), None, Map("private" -> "aaa")))
    scopedb.createSystem(System("12345", "update", None))



    val data: UpdateInstanceData = UpdateInstanceData("Patch1", None, Map("step0" -> InstallModules(List(PuppetModule("Gluster", "3.2.1.4", None, None, None)))))
    val request = HttpRequest.newBuilder()
      .uri(s"http://localhost:6401/products/Gluster-3.2.1.4/instance/$instanceId")
      .httpMethod(HttpMethod.PUT)
      .entity(ScopeUtils.mapper.writeValueAsString(data))
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    println(response.getResponseAsString)
    val productFromDb: Product = scopedb.getProductDetails("Gluster", "3.2.1.4").get
    response.getStatus() should equal(200)
  }

  @Test def testSystemInstances() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build()

    RestApiTest.httpClient.executeDirect(request)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      .header("Accept", "test/plain")
      .entity("mypwd")
      .contentType("text/plain")
      .build()

    var response: HttpResponse = RestApiTest.httpClient.executeDirect(request)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances")
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    response.getStatus() should equal(200)
    logInfo("response: {}", response.getResponseAsString)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    response.getStatus() should equal(200)
  }

  @Test def getInstanceInfo() {

    val request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/456")
      .httpMethod(HttpMethod.GET)
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    logInfo("response: {}", response.getResponseAsString)
    response.getStatus() should equal(404)
  }

  @Test def testDeleteSystem() {

    val request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    logInfo("response: {}", response.getResponseAsString)
    response.getStatus() should equal(404)
  }

  @Test def testGetProducts() {

    val request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products")
      .httpMethod(HttpMethod.GET)
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    val responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent)
    val json = parse(responseContent)

    val products = json \\ "productName"
    products.children.length should be >= (2)

  }

  @Test def testGetProduct() {

    val request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products/IM-3.48.0.0/")
      .httpMethod(HttpMethod.GET)
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    val responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent)
    val responseStatus = response.getStatus()
    responseStatus should equal(200)
  }

  @Test def testGetProductAndFail() {

    val request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products/IDONTEXIST-3.48.0.0/")
      .httpMethod(HttpMethod.GET)
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    val responseStatus = response.getStatus()
    responseStatus should equal(404)
  }

  @Test def testCreateDeleteProduct() {
    {
      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.DELETE)
        .build()

      val response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/TestProduct-1.2.3.4/")
      //        .method(HttpMethod.DELETE)
      //        .send()

      val responseStatus = response.getStatus()
      responseStatus should equal(404)
    }

    val product = Product("TestProduct-1.2.3.4", "TestProduct", "1.2.3.4", List[ProductOption](), RestApiTest.productRepoUrl)
    val body = ScopeUtils.mapper.writeValueAsString(product)

    {
      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.PUT)
        .entity(body)
        .contentType("application/json")
        .build()

      val response = RestApiTest.httpClient.executeDirect(request)

      val responseStatus = response.getStatus()
      responseStatus should equal(201)
    }

    {
      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.PUT)
        .entity(body)
        .contentType("application/json")
        .build()

      val response = RestApiTest.httpClient.executeDirect(request)

      val responseStatus = response.getStatus()
      responseStatus should equal(409)
    }

    {
      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.GET)
        .build()

      val response = RestApiTest.httpClient.executeDirect(request)

      val responseStatus = response.getStatus()
      responseStatus should equal(200)
    }

    {
      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.DELETE)
        .build()

      val response = RestApiTest.httpClient.executeDirect(request)

      val responseStatus = response.getStatus()
      responseStatus should equal(200)
    }

    {

      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.GET)
        .build()

      val response = RestApiTest.httpClient.executeDirect(request)

      val responseStatus = response.getStatus()
      responseStatus should equal(404)
    }

  }


  @Test def testInstantiateProduct() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build()

    var response: HttpResponse = RestApiTest.httpClient.executeDirect(request)

    //create new system
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      .header("Accept", "test/plain")
      .entity("mypwd")
      .contentType("text/plain")
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    response.getStatus() should equal(200)

    // Add foundation service info to system record.
    val system = scopedb.findSystem("123456").get
    val dummyScopeFoundationProduct = Product("dummyFoundation-0.0.0.0", "dummyFoundation", "0.0.0.0", List[ProductOption](), RestApiTest.productRepoUrl)
    val dummyScopeFoundationInstance = Instance("dummyFoundation",
      system.systemId,
      system.systemId,
      Some("STARTED"),
      Some("Foundation product for all the system."),
      dummyScopeFoundationProduct,
      Map(),
      List(AccessPoint("Ccp-Server", "1.1.1.1")),
      Some(false))
    scopedb.updateSystem(System(system.systemId, system.password, Some(dummyScopeFoundationInstance)))


    var product = Product("IM-3.48.0.0", "IM", "3.48.0.0", List[ProductOption](), RestApiTest.productRepoUrl)
    var body = ScopeUtils.mapper.writeValueAsString(Instance(null, "123456", "myInstance_123", Some("completed"), None, product, Map(), List[AccessPoint](), None))
    println("body is: " + body)


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products/IM-3.48.0.0/instantiate/")
      .httpMethod(HttpMethod.POST)
      .entity(body)
      .contentType("application/json")
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    var responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent)
    response.getStatus() should equal(200)

    // get instance info
    var json = parse(responseContent)
    var instanceId = json \\ "instanceId"

    val instanceId1 = instanceId.values


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId1)
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response for get instance info: {}", responseContent)


    //add instance for system
    product = Product("IM-3.48.0.0", "IM", "3.48.0.0", List[ProductOption](), RestApiTest.productRepoUrl)
    body = ScopeUtils.mapper.writeValueAsString(Instance(null, "123456", "myInstance 456", Some("completed"), None, product, Map(), List[AccessPoint](), None))


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products/IM-3.48.0.0/instantiate/")
      .httpMethod(HttpMethod.POST)
      .header("Accept", "application/json")
      .header("Content-Type", "application/json")
      .entity(body)
      .contentType("application/json")
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response: {}", responseContent)

    json = parse(responseContent)
    instanceId = json \\ "instanceId"

    val instanceId2 = instanceId.values


    //get all system instances
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances")
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response all instances: {}", responseContent)

    json = parse(responseContent)
    instanceId = json \\ "instanceId"
    instanceId.children.length > (2)


    // delete instance
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId1)
      .httpMethod(HttpMethod.DELETE)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)

    //get all instances
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances")
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response all instances: {}", responseContent)

    json = parse(responseContent)
    responseContent should include(instanceId2.toString)

    //delete instance - cleanup code
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId2)
      .httpMethod(HttpMethod.DELETE)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    response.getStatus() should equal(200)
    logInfo("response: {}", response.getResponseAsString)

    //delete system - cleanup code
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)


    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)

  }

  //   @Ignore
  @Test def testStartBentoService() {

    //    val request = ProvisionRequest("bento", None)

    // Add foundation service info to system record.
    val system = scopedb.findSystem("bento").get
    val dummyScopeFoundationProduct = Product("dummyFoundation-0.0.0.0", "dummyFoundation", "0.0.0.0", List[ProductOption](), RestApiTest.productRepoUrl)
    val dummyScopeFoundationInstance = Instance("dummyFoundation",
      system.systemId,
      system.systemId,
      Some("STARTED"),
      Some("Foundation product for all the system."),
      dummyScopeFoundationProduct,
      Map(),
      List(AccessPoint("Ccp-Server", "1.1.1.1")),
      Some(false))
    scopedb.updateSystem(System(system.systemId, system.password, Some(dummyScopeFoundationInstance)))

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/1283/service")
      .httpMethod(HttpMethod.POST)
      .entity(ScopeUtils.mapper.writeValueAsString(ProvisionRequest("bento", None)))
      .contentType("application/json")
      .build()

    var response: HttpResponse = RestApiTest.httpClient.executeDirect(request)

    var responseStatus = response.getStatus()

    responseStatus should equal(202)

    var location = response.getHeaders.get("Location").toList.mkString
    var endPoint = location.replace(":8086", "")
    println("location: " + endPoint)


    request = HttpRequest.newBuilder()
      .uri(endPoint)
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseStatus = response.getStatus()
    logInfo("content response: " + response.getResponseAsString)
    responseStatus should equal(303)

    location = response.getHeaders.get("Location").toList.mkString
    endPoint = location.replace(":8086", "")
    logInfo("location: " + endPoint)

    request = HttpRequest.newBuilder()
      .uri(endPoint + "/control/status")
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseStatus = response.getStatus()
    responseStatus should equal(200)

    var reqCont = ControlStatusRequest("START")


    request = HttpRequest.newBuilder()
      .uri(endPoint + "/control/status")
      .httpMethod(HttpMethod.PUT)
      .entity(ScopeUtils.mapper.writeValueAsString(reqCont))
      .contentType("application/json")
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseStatus = response.getStatus()

    responseStatus should equal(200)

    Thread.sleep(1000)

    request = HttpRequest.newBuilder()
      .uri(endPoint + "/control/status")
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseStatus = response.getStatus()
    responseStatus should equal(200)

    val responseContent = response getResponseAsString

    responseContent should include("STARTING")

    reqCont = ControlStatusRequest("STOP")

    request = HttpRequest.newBuilder()
      .uri(endPoint + "/control/status")
      .httpMethod(HttpMethod.PUT)
      .entity(ScopeUtils.mapper.writeValueAsString(reqCont))
      .contentType("application/json")
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseStatus = response.getStatus()

    responseStatus should equal(501)


    request = HttpRequest.newBuilder()
      .uri(endPoint)
      .httpMethod(HttpMethod.DELETE)
      .contentType("application/json")
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseStatus = response.getStatus()

    responseStatus should equal(202)
  }

  @Test def testFoundationInstance() {
    // delete system in case it exists.

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build()

    RestApiTest.httpClient.executeDirect(request)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      .header("Accept", "test/plain")
      .entity("mypwd")
      .contentType("text/plain")
      .build()

    var response = RestApiTest.httpClient.executeDirect(request)

    response.getStatus() should equal(200)

    //instantiate foundation
    val product = Product("ScopeFoundation-1.50.0.0", "ScopeFoundation", "1.50.0.0", List[ProductOption](), RestApiTest.productRepoUrl)
    val body = ScopeUtils.mapper.writeValueAsString(Instance(null, "123456", "myInstance_123", Some("completed"), None, product, Map(), List[AccessPoint](), None))
    println("body is: " + body)


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/foundation")
      .httpMethod(HttpMethod.PUT)
      .entity(body)
      .contentType("application/json")
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    var responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent)
    response.getStatus() should equal(200)

    // get instance info
    var json = parse(responseContent)
    var instanceId = json \\ "instanceId"

    val instanceId1 = instanceId.values

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId1)
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/")
      .httpMethod(HttpMethod.GET)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/")
      .httpMethod(HttpMethod.DELETE)
      .build()

    response = RestApiTest.httpClient.executeDirect(request)

    response.getStatus() should equal(200)
  }

  @Test def testGetSystems() {

    val request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems")
      .httpMethod(HttpMethod.GET)
      .build()

    val response = RestApiTest.httpClient.executeDirect(request)

    val responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent)
    val json = parse(responseContent)

    val systems = json \\ "systemId"
    systems.children.length should be >= (2)

  }


}