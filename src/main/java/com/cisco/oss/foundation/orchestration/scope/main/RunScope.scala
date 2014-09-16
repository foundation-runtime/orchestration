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
import java.util.{Collections, EventListener}
import javax.servlet.{Filter, Servlet}

import com.cisco.oss.foundation.flowcontext.FlowContextFactory
import com.cisco.oss.foundation.http.server.jetty.JettyHttpServerFactory
import com.cisco.oss.foundation.orchestration.scope.utils.Slf4jLogger
import com.google.common.collect.{ArrayListMultimap, ListMultimap}
import org.apache.log4j.PropertyConfigurator
import org.apache.log4j.helpers.Loader
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

    val uiWebConfig = new XmlWebApplicationContext()
    uiWebConfig.setConfigLocation("classpath*:/META-INF/scopeUIApplicationContext.xml")
    uiWebConfig.registerShutdownHook()
    val uiServletMap: ArrayListMultimap[String, Servlet] = ArrayListMultimap.create()
    uiServletMap.put("/*", new DispatcherServlet(uiWebConfig));
    JettyHttpServerFactory.INSTANCE.startHttpServer("scope-ui", uiServletMap, uiFilterMap, Collections.singletonList[EventListener](new ContextLoaderListener(uiWebConfig)))


  }

  def stop() {
    JettyHttpServerFactory.INSTANCE.stopHttpServer("scope")
  }
}