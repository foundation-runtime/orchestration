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

package com.cisco.oss.foundation.orchestration.main

import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.context.support.XmlWebApplicationContext
import java.util.{Collections, EventListener}
import org.springframework.web.context.ContextLoaderListener
import com.cisco.oss.foundation.orchestration.utils.Slf4jLogger
import org.apache.log4j.helpers.Loader
import org.apache.log4j.PropertyConfigurator
import java.lang.Thread.UncaughtExceptionHandler
import com.cisco.oss.foundation.http.server.jetty.{HttpServerUtil, JettyHttpServerFactory}
import com.google.common.collect.{ListMultimap, ArrayListMultimap}
import javax.servlet.{Filter, Servlet}
import com.cisco.oss.foundation.flowcontext.FlowContextFactory

object RunScope extends App {

  val resource = Loader.getResource("log4j.properties");
  PropertyConfigurator.configureAndWatch(resource.getFile, 10000)

  FlowContextFactory.createFlowContext
  val run = new RunScope()
  run start
}

class RunScope extends Slf4jLogger {


  def start() {

    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler {
      override def uncaughtException(t: Thread, e: Throwable): Unit = {
        logError("thread: [{}] failed on uncaught exception: {}", t.getName, e, e)
      }
    })

    sys.ShutdownHookThread {
      stop
    }



//    val sh = new ServletContextHandler
    val webConfig = new XmlWebApplicationContext()
    webConfig.setConfigLocation("classpath*:/META-INF/scopeServerApplicationContext.xml")
    webConfig.registerShutdownHook()
//    sh.setEventListeners(Array[EventListener](new ContextLoaderListener(webConfig)))
//    sh.addServlet(new ServletHolder(new DispatcherServlet(webConfig)), "/*")

    val servletMap:ArrayListMultimap[String,Servlet] = ArrayListMultimap.create()
    servletMap.put("/*", new DispatcherServlet(webConfig))

    val filterMap: ListMultimap[String, Filter] = ArrayListMultimap.create()

    try {

      //      dumpConfiguration

      //      val port = setConnectors
//      val handler = createHandler
//
//      //      val resourceHandler = new ResourceHandler()
//      //      resourceHandler.setDirectoriesListed(true)
//      //      resourceHandler.setResourceBase(".")
//      //      resourceHandler.setWelcomeFiles(Array("index.html"))
//      //
//      //      val handlerList = new HandlerList
//      //      handlerList.addHandler(resourceHandler)
//      //      handlerList.addHandler(handler)
//      //
//      //      server.setHandler(handlerList)
//
//      server setHandler (handler)
//
//      server.start()

//      logInfo("Http SCOPE server started on {}", port.toString)

    } catch {
      case e: Exception => {
        logInfo("Failed to start SCOPE server: {}", Array(e, e))
        System exit (-1)
      }
    }
    JettyHttpServerFactory.INSTANCE.startHttpServer("scope", servletMap, filterMap, Collections.singletonList[EventListener](new ContextLoaderListener(webConfig)))


    //    val threadPool = HttpServerUtil.getDefaultThreadPool("scope")

    //    val server = new Server(threadPool)


  }

  def stop() {
    JettyHttpServerFactory.INSTANCE.stopHttpServer("scope")
  }

  //  private def getThreadPool: QueuedThreadPool = {
  //
  //    val threadPool = new QueuedThreadPool()
  //    val minThreads = ScopeUtils.configuration.getInt(ScopeConstants.MIN_THREADS)
  //    val maxThreads = ScopeUtils.configuration.getInt(ScopeConstants.MAX_THREADS)
  //    threadPool.setMaxThreads(maxThreads)
  //    threadPool.setMinThreads(minThreads)
  //
  //    threadPool
  //  }

//  private def setConnectors: Int = {
//
//    val host = ScopeUtils.configuration.getString(ScopeConstants.HOST)
//    val port = ScopeUtils.configuration.getInt(ScopeConstants.PORT)
//    val connectionIdleTime = ScopeUtils.configuration.getInt(ScopeConstants.CONNECTION_IDLE_TIME)
//    val isBlockingChannelConnector = ScopeUtils.configuration.getBoolean(ScopeConstants.IS_BLOCKING_CHANNEL_CONNECTOR)
//    val numberOfAcceptors = ScopeUtils.configuration.getInt(ScopeConstants.NUMBER_OF_ACCEPTORS)
//    val acceptQueueSize = ScopeUtils.configuration.getInt(ScopeConstants.ACCEPT_QUEUE_SIZE)
//
//    val http_config = new HttpConfiguration()
//    //    http_config.setSecureScheme("https")
//    //    http_config.setSecurePort(8443)
//    //    http_config.setOutputBufferSize(32768)
//    val numberOfListeners = 0
//    val http = new ServerConnector(server, numberOfAcceptors, numberOfListeners);
//    http.setPort(port);
//    http.setHost(host)
//    http.setIdleTimeout(connectionIdleTime);
//    http.setAcceptQueueSize(acceptQueueSize)
//
//
//    //    val connector: AbstractConnector = if (isBlockingChannelConnector) new BlockingChannelConnector() else new SelectChannelConnector()
//    //    connector.setAcceptQueueSize(acceptQueueSize)
//    //    connector.setAcceptors(numberOfAcceptors)
//    //    connector.setPort(port)
//    //    connector.setHost(host)
//    //    connector.setMaxIdleTime(connectionIdleTime)
//    //    connector.setRequestHeaderSize(connector.getRequestHeaderSize)
//    var connectors: Array[Connector] = Array(http)
//    server.setConnectors(connectors)
//    port
//  }

//  private def createHandler: org.eclipse.jetty.servlet.ServletContextHandler = {
//
//    //    val threadPool = HttpServerUtil.getDefaultThreadPool("scope")
//    //    server.setThreadPool(threadPool)
//
//    val sh = new ServletContextHandler
//    val webConfig = new XmlWebApplicationContext()
//    webConfig.setConfigLocation("classpath*:/META-INF/scopeServerApplicationContext.xml")
//    webConfig.registerShutdownHook()
//    sh.setEventListeners(Array[EventListener](new ContextLoaderListener(webConfig)))
//    sh.addServlet(new ServletHolder(new DispatcherServlet(webConfig)), "/*")
//    HttpServerUtil.addFiltersToServletContextHandler("scope", threadPool, sh)
//    sh
//  }

//  private def dumpConfiguration() {
//    val keys: List[String] = ScopeUtils.configuration.getKeys().asInstanceOf[java.util.Iterator[String]].toList
//    val builder = new StringBuilder
//    keys foreach ((key) => builder.append(key + "=" + ScopeUtils.configuration.getString(key) + "\n"))
//    logInfo("The properties loaded are:\n{}", builder)
//  }

}