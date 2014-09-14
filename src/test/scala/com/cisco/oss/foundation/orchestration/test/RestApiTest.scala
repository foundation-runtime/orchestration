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

package com.cisco.oss.foundation.orchestration.test

import org.junit.{AfterClass, BeforeClass, Test}
import org.scalatest.junit.JUnitSuite
import org.scalatest.junit.MustMatchersForJUnit
import org.scalatest.junit.ShouldMatchersForJUnit
import com.cisco.oss.foundation.orchestration.main.{RunScope => RunScopeMain}
import net.liftweb.json._
import com.cisco.oss.foundation.orchestration.utils.ScopeUtils
import com.cisco.oss.foundation.orchestration.utils.Slf4jLogger
import com.cisco.oss.foundation.orchestration.model._
import scala.collection.immutable.Map
import com.cisco.oss.foundation.orchestration.dblayer.SCOPeDB
import org.springframework.beans.factory.annotation.Autowired
import org.junit.runner.RunWith
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import de.flapdoodle.embed.mongo.distribution.Version
import de.flapdoodle.embed.mongo.config.{MongodConfigBuilder, Net}
import de.flapdoodle.embed.mongo.{MongodExecutable, MongodStarter}
import de.flapdoodle.embed.process.runtime.Network
import com.mongodb.casbah.Imports._
import com.novus.salat.global._
import scala.collection.JavaConversions._

import com.novus.salat._
import com.cisco.oss.foundation.orchestration.model.ControlStatusRequest
import com.cisco.oss.foundation.orchestration.model.Instance
import scala.Some
import com.cisco.oss.foundation.orchestration.model.ProductOption
import com.cisco.oss.foundation.orchestration.model.AccessPoint
import com.cisco.oss.foundation.orchestration.model.Product
import com.cisco.oss.foundation.orchestration.model.System
import com.cisco.oss.foundation.orchestration.model.ProvisionRequest
import com.cisco.oss.foundation.http.apache.ApacheHttpClientFactory
import com.cisco.oss.foundation.http.{HttpResponse, HttpMethod, HttpRequest}
import com.cisco.oss.foundation.flowcontext.FlowContextFactory

object RestApiTest extends Slf4jLogger {

  val run = new RunScopeMain
  val httpClient = ApacheHttpClientFactory.createHttpClient("scopeClient", false);
  //  val host = ScopeUtils.configuration.getString(ScopeConstants.HOST, "localhost")
  //  val port = ScopeUtils.configuration.getInt(ScopeConstants.PORT)

  val mongoPort = Network.getFreeServerPort;
  ScopeUtils.configuration.setProperty("mongodb.port", mongoPort.toString)
  ScopeUtils.configuration.setProperty("mongodb.host", "localhost")
  //  httpClient.setBindAddress(new InetSocketAddress(host,port))
  //  val address = new Address(host, port)
  //  httpClient.start()
  var mongodExecutable: MongodExecutable = null;
  val productRepoUrl = "http://10.45.37.14/scope-products/test-1.0.0.0/"

  @BeforeClass def init() {


    val mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net("localhost", mongoPort, false))
      .build();

    val runtime = MongodStarter.getDefaultInstance();


    try {
      mongodExecutable = runtime.prepare(mongodConfig);
      val mongod = mongodExecutable.start();

    } finally {

    }
    createScopeDB

    run.start()
    Thread.sleep(2000)
  }

  def createScopeDB() {
    val mongodbConnetion = MongoConnection("localhost", mongoPort)
    val scopedb = mongodbConnetion("scope")
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

    product = Product("ScopeFoundation-1.50.0.0", "ScopeFoundation", "1.50.0.0",
      List(),
      productRepoUrl
    )

    productsdb insert (grater[Product].asDBObject(product))

    val service = Service("1283", "IM", "3.48.0.0")
    servicesdb.insert(grater[Service].asDBObject(service))
  }

  @AfterClass def stop() {
    logInfo("*** @AfterClass: Stoping mongo! ***")
    Thread.sleep(2500)
    run.stop
    if (mongodExecutable != null)
      mongodExecutable.stop();
  }
}


@RunWith(classOf[SpringJUnit4ClassRunner])
@ContextConfiguration(locations = Array("classpath*:/META-INF/scopeServerApplicationContext.xml"))
class RestApiTest extends Slf4jLogger with JUnitSuite with ShouldMatchersForJUnit with MustMatchersForJUnit {

  //implicit val formats = Serialization.formats(NoTypeHints)

  @Autowired var scopedb: SCOPeDB = _


  @Test def testCreateSystem() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .header("Accept", "test/plain")
      //    .entity("")
      .contentType("text/plain")
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();



    try {

      request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/systems/123456")
        .httpMethod(HttpMethod.POST)
        .header("Accept", "test/plain")
        .entity("mypwd")
        .contentType("text/plain")
        .build();

      //      var response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
      //        .method(HttpMethod.POST)
      //        .header("Accept", "test/plain")
      //        .content(new BytesContentProvider("mypwd" getBytes()), "text/plain")
      //        .send();

      response = RestApiTest.httpClient.executeDirect(request)



      response getStatus() should equal(200)


      request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/systems/123456")
        .httpMethod(HttpMethod.POST)
        .header("Accept", "test/plain")
        .entity("mypwd")
        .contentType("text/plain")
        .build();

      response = RestApiTest.httpClient.executeDirect(request)


      //      contentExchange.reset()
      //      response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
      //        .method(HttpMethod.POST)
      //        .header("Accept", "test/plain")
      //        .content(new BytesContentProvider("mypwd" getBytes()), "text/plain")
      //        .send();

      //      logInfo("response: {}", responseContent);
      response getStatus() should equal(409)

    } catch {
      case e: Exception =>
        logger.error("Error in {}", e.toString())
    } finally {


      request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/systems/123456")
        .httpMethod(HttpMethod.DELETE)
        //        .header("Accept", "test/plain")
        //        .entity("")
        //        .contentType("text/plain")
        .build();

      response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
      //        .method(HttpMethod.DELETE)
      //        .send();
      //      contentExchange.setMethod("delete")
      //      RestApiTest.httpClient.send(contentExchange);
      //      contentExchange.waitForDone();

      //      val responseContent = contentExchange.getResponseContent();
      val responseStatus = response getStatus()
      //      logInfo("response: {}", responseContent);
      responseStatus should equal(200)
    }

  }

  @Test def testCreateSystemAndFail() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      //      .header("Accept", "test/plain")
      //    .entity("")
      .contentType("text/plain")
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    val response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.POST)
    //      .send();

    println(response.getResponseAsString)
    response.getStatus() should equal(400)
  }

  @Test def testSystemInstances() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      //      .header("Accept", "test/plain")
      //    .entity("")
      //      .contentType("text/plain")
      .build();

    RestApiTest.httpClient.executeDirect(request)


    // delete system in case it exists.
    //    RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      .header("Accept", "test/plain")
      .entity("mypwd")
      .contentType("text/plain")
      .build();

    var response: HttpResponse = RestApiTest.httpClient.executeDirect(request)


    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.POST)
    //      .header("Accept", "test/plain")
    //      .content(new BytesContentProvider("mypwd" getBytes()), "text/plain")
    //      .send();
    //
    //    response.getStatus() should equal(200)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances")
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)


    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances")
    //      .method(HttpMethod.GET)
    //      .send();

    response.getStatus() should equal(200)
    logInfo("response: {}", response.getResponseAsString);


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();

    response.getStatus() should equal(200)
  }

  @Test def getInstanceInfo() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/456")
      .httpMethod(HttpMethod.GET)
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)


    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances/4567")
    //      .method(HttpMethod.GET)
    //      .send();

    response.getStatus() should equal(404)

  }

  @Test def testDeleteSystem() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    val response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();

    response.getStatus() should equal(404)
    logInfo("response: {}", response.getResponseAsString);
  }

  @Test def testGetProducts() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products")
      .httpMethod(HttpMethod.GET)
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/products")
    //      .method(HttpMethod.GET)
    //      .send();


    val responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent);
    val json = parse(responseContent)

    val products = json \\ "productName"
    products.children.length should be >= (2)

  }

  @Test def testGetProduct() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products/IM-3.48.0.0/")
      .httpMethod(HttpMethod.GET)
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/IM-3.48.0.0/")
    //      .method(HttpMethod.GET)
    //      .send();


    val responseContent = response.getResponseAsString;
    logInfo("response: {}", responseContent);
    val responseStatus = response.getStatus()
    responseStatus should equal(200)
    //    		val json = parse(responseContent)
    //		
    //		val products = json \\ "name"
    //		products.children.length should equal (2)

  }

  @Test def testGetProductAndFail() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/products/IDONTEXIST-3.48.0.0/")
      .httpMethod(HttpMethod.GET)
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/IDONTEXIST-3.48.0.0/")
    //      .method(HttpMethod.GET)
    //      .send();


    val responseStatus = response.getStatus()
    responseStatus should equal(404)

  }

  @Test def testCreateDeleteProduct() {
    // Uses ScalaTest matchers

    {
      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.DELETE)
        .build();

      val response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/TestProduct-1.2.3.4/")
      //        .method(HttpMethod.DELETE)
      //        .send();

      val responseStatus = response.getStatus()
      responseStatus should equal(404)
    }

    val product = Product("TestProduct-1.2.3.4", "TestProduct", "1.2.3.4", List[ProductOption](), RestApiTest.productRepoUrl)
    val body = ScopeUtils.mapper.writeValueAsString(product)

    {

      var request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.PUT)
        .entity(body)
        .contentType("application/json")
        .build();

      var response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/TestProduct-1.2.3.4/")
      //        .method(HttpMethod.PUT)
      //        .content(new BytesContentProvider(body getBytes()), "application/json")
      //        .send();

      val responseStatus = response.getStatus()
      responseStatus should equal(201)
    }

    {

      var request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.PUT)
        .entity(body)
        .contentType("application/json")
        .build();

      var response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/TestProduct-1.2.3.4/")
      //        .method(HttpMethod.PUT)
      //        .content(new BytesContentProvider(body getBytes()), "application/json")
      //        .send();

      val responseStatus = response.getStatus()
      responseStatus should equal(409)
    }

    {
      var request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.GET)
        .build();

      var response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/TestProduct-1.2.3.4/")
      //        .method(HttpMethod.GET)
      //        .send();

      val responseStatus = response.getStatus()
      responseStatus should equal(200)
    }

    {
      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.DELETE)
        .build();

      var response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/TestProduct-1.2.3.4/")
      //        .method(HttpMethod.DELETE)
      //        .send();

      val responseStatus = response.getStatus()
      responseStatus should equal(200)
    }

    {

      val request = HttpRequest.newBuilder()
        .uri("http://localhost:6401/products/TestProduct-1.2.3.4/")
        .httpMethod(HttpMethod.GET)
        .build();

      val response = RestApiTest.httpClient.executeDirect(request)

      //      val response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/TestProduct-1.2.3.4/")
      //        .method(HttpMethod.GET)
      //        .send();

      val responseStatus = response.getStatus()
      responseStatus should equal(404)
    }

  }


  @Test def testInstantiateProdcut() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build();

    var response: HttpResponse = RestApiTest.httpClient.executeDirect(request)

    //    RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();


    //create new system
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      .header("Accept", "test/plain")
      .entity("mypwd")
      .contentType("text/plain")
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.POST)
    //      .header("Accept", "test/plain")
    //      .content(new BytesContentProvider("mypwd" getBytes()), "text/plain")
    //      .send();


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
      .build();

    response = RestApiTest.httpClient.executeDirect(request)


    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/IM-3.48.0.0/instantiate/")
    //      .method(HttpMethod.POST)
    //      //      .header("Accept", "application/json")
    //      //      .header("Content-Type", "application/json")
    //      .content(new BytesContentProvider(body getBytes()), "application/json")
    //      .send();

    var responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent);
    response.getStatus() should equal(200)

    // get instance info
    var json = parse(responseContent)
    var instanceId = json \\ "instanceId"

    val instanceId1 = instanceId.values


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId1)
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances/" + instanceId1)
    //      .method(HttpMethod.GET)
    //      .send();

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response for get instance info: {}", responseContent);


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
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/products/IM-3.48.0.0/instantiate/")
    //      .method(HttpMethod.POST)
    //      .header("Accept", "application/json")
    //      .header("Content-Type", "application/json")
    //      .content(new BytesContentProvider(body getBytes()), "application/json")
    //      .send();



    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response: {}", responseContent);

    json = parse(responseContent)
    instanceId = json \\ "instanceId"

    val instanceId2 = instanceId.values


    //get all system instances

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances")
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances")
    //      .method(HttpMethod.GET)
    //      .send();


    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response all instances: {}", responseContent);

    json = parse(responseContent)
    instanceId = json \\ "instanceId"
    instanceId.children.length > (2)


    // delete instance
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId1)
      .httpMethod(HttpMethod.DELETE)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances/" + instanceId1)
    //      .method(HttpMethod.DELETE)
    //      .send();


    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    //    logInfo("response for get instance info: {}", responseContent);


    //get all instances

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances")
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances")
    //      .method(HttpMethod.GET)
    //      .send();
    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)
    logInfo("response all instances: {}", responseContent);

    json = parse(responseContent)
    responseContent should include(instanceId2.toString)

    //delete instance - cleanup code
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId2)
      .httpMethod(HttpMethod.DELETE)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances/" + instanceId2)
    //      .method(HttpMethod.DELETE)
    //      .send();

    response.getStatus() should equal(200)
    logInfo("response: {}", response.getResponseAsString);

    //delete system - cleanup code
    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();

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
      .build();

    var response: HttpResponse = RestApiTest.httpClient.executeDirect(request)

    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/1283/service")
    //      .method(HttpMethod.POST)
    //      .content(new BytesContentProvider((ScopeUtils.mapper.writeValueAsString(request)) getBytes()), "application/json")
    //      .send();


    var responseStatus = response.getStatus()

    responseStatus should equal(202)

    var location = response.getHeaders.get("Location").toList.mkString
    var endPoint = location.replace(":8086", "")
    println("location: " + endPoint)


    request = HttpRequest.newBuilder()
      .uri(endPoint)
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest(endPoint)
    //      .method(HttpMethod.GET)
    //      .followRedirects(false)
    //      .send();

    responseStatus = response.getStatus()
    println("content response: " + response.getResponseAsString)
    responseStatus should equal(303)

    location = response.getHeaders.get("Location").toList.mkString
    endPoint = location.replace(":8086", "")
    println("location: " + endPoint)

    request = HttpRequest.newBuilder()
      .uri(endPoint + "/control/status")
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest(endPoint + "/control/status")
    //      .method(HttpMethod.GET)
    //      .followRedirects(false)
    //      .send();

    responseStatus = response.getStatus()
    responseStatus should equal(200)

    var reqCont = ControlStatusRequest("START")


    request = HttpRequest.newBuilder()
      .uri(endPoint + "/control/status")
      .httpMethod(HttpMethod.PUT)
      .entity(ScopeUtils.mapper.writeValueAsString(reqCont))
      .contentType("application/json")
      .build();

    response = RestApiTest.httpClient.executeDirect(request)


    //    response = RestApiTest.httpClient.newRequest(endPoint + "/control/status")
    //      .method(HttpMethod.PUT)
    //      .content(new BytesContentProvider((ScopeUtils.mapper.writeValueAsString(reqCont)) getBytes()), "application/json")
    //      .send();

    responseStatus = response.getStatus()

    responseStatus should equal(200)

    Thread.sleep(1000)

    request = HttpRequest.newBuilder()
      .uri(endPoint + "/control/status")
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest(endPoint + "/control/status")
    //      .method(HttpMethod.GET)
    //      .followRedirects(false)
    //      .send();

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
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest(endPoint + "/control/status")
    //      .method(HttpMethod.PUT)
    //      .content(new BytesContentProvider((ScopeUtils.mapper.writeValueAsString(reqCont)) getBytes()), "application/json")
    //      .send();

    responseStatus = response.getStatus()

    responseStatus should equal(501)


    request = HttpRequest.newBuilder()
      .uri(endPoint)
      .httpMethod(HttpMethod.DELETE)
      //.entity(ScopeUtils.mapper.writeValueAsString(ProvisionRequest("bento", None)))
      .contentType("application/json")
      .build();

    response = RestApiTest.httpClient.executeDirect(request)


    //    response = RestApiTest.httpClient.newRequest(endPoint)
    //      .method(HttpMethod.DELETE)
    //      .content(new BytesContentProvider((ScopeUtils.mapper.writeValueAsString(request)) getBytes()), "application/json")
    //      .send();

    responseStatus = response.getStatus()

    responseStatus should equal(202)
  }

  @Test def testFoundationInstance() {
    // delete system in case it exists.

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.DELETE)
      .build();

    RestApiTest.httpClient.executeDirect(request)

    //    RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456")
      .httpMethod(HttpMethod.POST)
      .header("Accept", "test/plain")
      .entity("mypwd")
      .contentType("text/plain")
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.POST)
    //      .header("Accept", "test/plain")
    //      .content(new BytesContentProvider("mypwd" getBytes()), "text/plain")
    //      .send();

    response.getStatus() should equal(200)

    //instantiate foundation
    var product = Product("ScopeFoundation-1.50.0.0", "ScopeFoundation", "1.50.0.0", List[ProductOption](), RestApiTest.productRepoUrl)
    var body = ScopeUtils.mapper.writeValueAsString(Instance(null, "123456", "myInstance_123", Some("completed"), None, product, Map(), List[AccessPoint](), None))
    println("body is: " + body)


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/foundation")
      .httpMethod(HttpMethod.PUT)
      .entity(body)
      .contentType("application/json")
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/foundation")
    //      .method(HttpMethod.PUT)
    //      .content(new BytesContentProvider(body getBytes()), "application/json")
    //      .send();


    var responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent);
    response.getStatus() should equal(200)

    // get instance info
    var json = parse(responseContent)
    var instanceId = json \\ "instanceId"

    val instanceId1 = instanceId.values

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/instances/" + instanceId1)
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances/" + instanceId1)
    //      .method(HttpMethod.GET)
    //      .send();


    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/")
      .httpMethod(HttpMethod.GET)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //     response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/")
    //      .method(HttpMethod.GET)
    //      .send();

    responseContent = response.getResponseAsString
    response.getStatus() should equal(200)

    request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems/123456/")
      .httpMethod(HttpMethod.DELETE)
      .build();

    response = RestApiTest.httpClient.executeDirect(request)

    //    response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
    //      .method(HttpMethod.DELETE)
    //      .send();

    response.getStatus() should equal(200)
  }

  @Test def testGetSystems() {

    var request = HttpRequest.newBuilder()
      .uri("http://localhost:6401/systems")
      .httpMethod(HttpMethod.GET)
      .build();

    var response = RestApiTest.httpClient.executeDirect(request)

    //    var response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems")
    //      .method(HttpMethod.GET)
    //      .send();

    val responseContent = response.getResponseAsString
    logInfo("response: {}", responseContent);
    val json = parse(responseContent)

    val systems = json \\ "systemId"
    systems.children.length should be >= (2)

  }


}