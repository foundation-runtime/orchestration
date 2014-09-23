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

package com.cisco.oss.foundation.orchestration.scope.main

import java.lang.Thread.UncaughtExceptionHandler
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap
import java.util.{Collections, EventListener}
import javax.servlet.{Filter, Servlet}

import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory
import com.cisco.oss.foundation.orchestration.scope.utils.{ScopeUtils, Slf4jLogger}
import com.google.common.collect.{ArrayListMultimap, ListMultimap}
import org.apache.log4j.PropertyConfigurator
import org.apache.log4j.helpers.Loader
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.{ContextHandler, ResourceHandler, ContextHandlerCollection}
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.context.support.XmlWebApplicationContext
import org.springframework.web.servlet.DispatcherServlet

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

    val webConfig = new XmlWebApplicationContext()
    webConfig.setConfigLocation("classpath*:/META-INF/scopeServerApplicationContext.xml")
    webConfig.registerShutdownHook()



    val servletMap: ArrayListMultimap[String, Servlet] = ArrayListMultimap.create()
    servletMap.put("/*", new DispatcherServlet(webConfig))

    val filterMap: ListMultimap[String, Filter] = ArrayListMultimap.create()
    val uiFilterMap: ListMultimap[String, Filter] = ArrayListMultimap.create()

    JettyHttpServerFactory.INSTANCE.startHttpServer("scope", servletMap, filterMap, Collections.singletonList[EventListener](new ContextLoaderListener(webConfig)))

    if (ScopeUtils.configuration.getBoolean("scope-ui.isEnabled", false)) {
      val uiWebConfig = new XmlWebApplicationContext()
      uiWebConfig.setConfigLocation("classpath*:/META-INF/scopeUIApplicationContext.xml")
      uiWebConfig.registerShutdownHook()
      val uiServletMap: ArrayListMultimap[String, Servlet] = ArrayListMultimap.create()
      //uiServletMap.put("/*", new DispatcherServlet(uiWebConfig));
      JettyHttpServerFactory.INSTANCE.startHttpServer("scope-ui", uiServletMap, uiFilterMap, Collections.singletonList[EventListener](new ContextLoaderListener(uiWebConfig)))
      staticFileServer()
    }
  }

  def staticFileServer(): Unit = {
    val serversField: Field = JettyHttpServerFactory.INSTANCE.getClass.getDeclaredField("servers")
    serversField.setAccessible(true)
    val serverMap: ConcurrentHashMap[String, Server] =  serversField.get(JettyHttpServerFactory.INSTANCE).asInstanceOf[ConcurrentHashMap[String, Server]]

    val puppetBaseDir = ScopeUtils.configuration.getString("scope-ui.scope-puppet.puppetBaseDir", "/opt/cisco/scopeData/scope-puppet")
    val yumBaseDir = ScopeUtils.configuration.getString("scope-ui.scope-base.yum.baseDir", "/opt/cisco/scopeData/scope-base/")
    val productsBaseDir = ScopeUtils.configuration.getString("scope-ui.scope-products.yum.baseDir", "/opt/cisco/scopeData/products/")
    val uiBaseDir = ScopeUtils.configuration.getString("scope-ui.baseDir", "/opt/cisco/scope/ui")

    val scopeBasePuppetContext = new ContextHandler();
    val scopeBaseYumContext = new ContextHandler();
    val scopeUiContext = new ContextHandler();
    val productsBaseContext = new ContextHandler();

    scopeBasePuppetContext.setContextPath("/scope-base/puppet/")
    scopeUiContext.setContextPath("/scope-ui/")
    scopeBaseYumContext.setContextPath("/scope-base/yum/")
    productsBaseContext.setContextPath("/scope-products/")

    val scopeBasePuppetResourceHandler: ResourceHandler = new ResourceHandler
    scopeBasePuppetResourceHandler.setDirectoriesListed(true)
    scopeBasePuppetResourceHandler.setResourceBase(puppetBaseDir)
    scopeBasePuppetContext.setHandler(scopeBasePuppetResourceHandler)

    val scopeUiResourceHandler: ResourceHandler = new ResourceHandler
    scopeUiResourceHandler.setDirectoriesListed(true)
    scopeUiResourceHandler.setResourceBase(uiBaseDir)
    scopeUiResourceHandler.setDirectoriesListed(false)
    scopeUiContext.setHandler(scopeUiResourceHandler)

    val scopeBaseYumResourceHandler: ResourceHandler = new ResourceHandler
    scopeBaseYumResourceHandler.setDirectoriesListed(true)
    scopeBaseYumResourceHandler.setResourceBase(yumBaseDir)
    scopeBaseYumContext.setHandler(scopeBaseYumResourceHandler)

    val productsBaseResourceHandler: ResourceHandler = new ResourceHandler
    productsBaseResourceHandler.setDirectoriesListed(true)
    productsBaseResourceHandler.setResourceBase(productsBaseDir)
    productsBaseContext.setHandler(productsBaseResourceHandler)


    val contextHandlerCollection: ContextHandlerCollection = serverMap.get("scope-ui").getHandler().asInstanceOf[ContextHandlerCollection]
    contextHandlerCollection.addHandler(scopeBasePuppetContext)
    contextHandlerCollection.addHandler(scopeUiContext)
    contextHandlerCollection.addHandler(scopeBaseYumContext)
    contextHandlerCollection.addHandler(productsBaseContext)
  }

  def stop() {
    JettyHttpServerFactory.INSTANCE.stopHttpServer("scope")
    if (ScopeUtils.configuration.getBoolean("scope-ui.isEnabled", false)) {
      JettyHttpServerFactory.INSTANCE.stopHttpServer("scope-ui")
    }
  }
}