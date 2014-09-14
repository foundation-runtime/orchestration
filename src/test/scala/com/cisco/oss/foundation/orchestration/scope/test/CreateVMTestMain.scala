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

package com.cisco.oss.foundation.orchestration.scope.test

import com.cisco.oss.foundation.orchestration.scope.main.{ RunScope => RunScopeMain }
import net.liftweb.json._
import com.cisco.oss.foundation.orchestration.scope.utils.ScopeUtils
import com.cisco.oss.foundation.orchestration.scope.utils.Slf4jLogger
import com.cisco.oss.foundation.orchestration.scope.model._
import scala.concurrent.duration._
import com.mongodb.{MongoClient, Mongo}
import com.cisco.oss.foundation.orchestration.scope.model.AccessPoint
import com.cisco.oss.foundation.orchestration.scope.model.Product
import com.cisco.oss.foundation.orchestration.scope.model.Instance
import scala.Some
import com.cisco.oss.foundation.http.{HttpResponse, HttpMethod, HttpRequest}

object Main extends App with Slf4jLogger{

  implicit val formats = Serialization.formats(NoTypeHints)

  var mongodb : Mongo = new MongoClient("localhost")
  mongodb.getDB("scope").getCollection("instances").drop()
  //mongodb.getDB("scope").getCollection("systems").drop()
  mongodb.close()
  mongodb = null

  val run = new RunScopeMain
//  val httpClient = new HttpClient();
//  val host = ScopeUtils.configuration.getString(ScopeConstants.HOST, "localhost")
//  val port = ScopeUtils.configuration.getInt(ScopeConstants.PORT)
  //  httpClient.setBindAddress(new InetSocketAddress(host,port))
  //  val address = new Address(host, port)
//  httpClient.start()

  run.start
  Thread.sleep(2000)

//  var response : ContentResponse = null /*RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456")
//    .method(HttpMethod.POST)
//    .header("Accept", "test/plain")
//    .content(new BytesContentProvider("mypwd" getBytes()), "text/plain")
//    .send();
//  logInfo("response status:{} expected: 200",Integer.toString(response.getStatus()));*/

  //      contentExchange.setAddress(RestApiTest.address)
  //      contentExchange.setURI("/systems/123456");
  //      contentExchange.setMethod("post");
  //      contentExchange.setRequestHeader("Accept", "test/plain")
  //
  //      		contentExchange.setRequestContent(new ByteArrayBuffer("mypwd" getBytes()))
  //      contentExchange.setRequestHeader("Cookie: ", "Password=mypwd")

  //      RestApiTest.httpClient.send(contentExchange);
  //      contentExchange.waitForDone();

  //      var responseContent = contentExchange.getResponseContent();
  //      logInfo("response: {}", responseContent);


  val accessPoints = List[AccessPoint](AccessPoint("ndsConsole", "http://<vgc1c>:5015/console/app.html"),

                                       AccessPoint("upm", "http://<vgc1b>:5670/api/")
                                      )
              //Product(@(Id@field)val id:String, val productName: String, val productVersion: String, val productOptions: Option[Array[ProductOption]], val accessPoints:Option[Collection[AccessPoint]], val foundationMachine:FoundationMachine)
  //var product = Product("", "IdentityManagement", "1.23.4.8", None, Some(accessPoints), FoundationMachine(Some(HostDetails("","3ae40aaf-2bb8-4124-80b6-618063c60835","10.234.0.47")),""))
  var product = Product("", "IM", "3.48.0.0", List[ProductOption](), "http://10.45.37.14/scope-products/test-1.0.0.0/")
  println ("product is: " + ScopeUtils.mapper.writeValueAsString(product))


  val instance: Instance = Instance(null, "123456", "IzekIMTest", Some("completed"), None, product, Map(), accessPoints, None)
  println ("instance is: " +  ScopeUtils.mapper.writeValueAsString(instance))
  var body =  ScopeUtils.mapper.writeValueAsString(instance)
  println ("body is: " + body)

  var request = HttpRequest.newBuilder()
    .uri("http://localhost:6401/products/IM-3.48.0.0/instantiate/")
    .httpMethod(HttpMethod.POST)
    .header("Accept", "application/json")
    .header("Content-Type", "application/json")
    .entity(body)
    .contentType("application/json")
    .build();

  var response:HttpResponse = RestApiTest.httpClient.executeDirect(request)

//  response  = RestApiTest.httpClient.newRequest("http://localhost:6401/products/IM-3.48.0.0/instantiate/")
//    .method(HttpMethod.POST)
//    .header("Accept", "application/json")
//    .header("Content-Type", "application/json")
//    .content(new BytesContentProvider(body getBytes()), "application/json")
//    .send();

  var responseContent = response.getResponseAsString
  logInfo("response: {}", responseContent);
  logInfo("response status:{} expected: 200",Integer.toString(response.getStatus()));

  // get instance info
  var json = parse(responseContent)
  var instanceId = json \\ "instanceId"

  val instId = instanceId.values

  request = HttpRequest.newBuilder()
    .uri("http://localhost:6401/systems/123456/instances/" + instId)
    .httpMethod(HttpMethod.GET)
    .build();

  response = RestApiTest.httpClient.executeDirect(request)

//  response = RestApiTest.httpClient.newRequest("http://localhost:6401/systems/123456/instances/" + instId)
//    .method(HttpMethod.GET)
//    .send();

  responseContent = response.getResponseAsString
  logInfo("response status:{} expected: 200",Integer.toString(response.getStatus()));
  logInfo("response for get instance info: {}", responseContent);
  Thread sleep((1 hours).toMillis)


  run.stop


}




  /*
    @Test def testCreateVm() {

      val context = new ClassPathXmlApplicationContext("/META-INF/scopeServerApplicationContext.xml")

      val scope = context.getBean(classOf[ProductsResource])

      val accessPoints = new HashSet[AccessPoint]()
      accessPoints.add(AccessPoint("upm","http://<host>:6040/upm/households"))
      accessPoints.add(AccessPoint("ndsconsole","https://<host>:5015/ndsconsole/app.html"))

      var product = Product("IdentityManagement", "1.23.4.7", Some(
        Array(
          ProductOption("sampleKey1", None, "String sample key", "String", "defVal1", Some(Array[String]("defVal1", "defVal2", "defVal3"))),
          ProductOption("sampleKey2", None, "int sample key", "int", "123", None),
          ProductOption("sampleKey3", None, "boolean sample key", "boolean", "true", None))),
        Some(accessPoints)
      )

      scope.instantiateProdcut("","",Instance("","naama","yairTest",None, None,product))

     Thread sleep((1 hours).toMillis)

    }
    */





