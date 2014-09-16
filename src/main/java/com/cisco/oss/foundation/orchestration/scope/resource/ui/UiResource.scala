package com.cisco.oss.foundation.orchestration.scope.resource.ui

import java.net.InetAddress
import javax.servlet.http.HttpServletRequest

import com.cisco.oss.foundation.orchestration.scope.utils.{ScopeUtils, Slf4jLogger}
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import scala.io.Source

/**
 * Created by igreenfi on 14/09/2014.
 */
@Controller
@RequestMapping(value = Array[String]("/scope-ui"))
class UiResource extends Slf4jLogger {

  private val baseDir = ScopeUtils.configuration.getString("scope-ui.baseDir", "/opt/cisco/scope/ui")
  private val inetAddress = InetAddress.getLocalHost.getHostAddress

  @RequestMapping(value = Array[String]("/{fileName}.html"), method = Array[RequestMethod](RequestMethod.GET), produces = Array[String]("text/html"))
  @ResponseBody
  def getUiPage(request: HttpServletRequest, @PathVariable("fileName") fileName: String): String = {

    ScopeUtils.time(logger, "getUiPage") {
      Source.fromFile(s"$baseDir/$fileName.html").getLines().mkString
    }

  }

  @RequestMapping(value = Array[String]("/scripts/{fileName:.*}"), method = Array[RequestMethod](RequestMethod.GET), produces = Array[String]("application/javascript", "text/javascript"))
  @ResponseBody
  def getJsScript(@PathVariable("fileName") fileName: String): String = {

    ScopeUtils.time(logger, "getJsScript") {
      Source.fromFile(s"$baseDir/scripts/$fileName").getLines().mkString.replace("<scope_ip>", inetAddress)
    }

  }

  @RequestMapping(value = Array[String]("/styles/{fileName:.*}"), method = Array[RequestMethod](RequestMethod.GET), produces = Array[String] ("text/css"))
  @ResponseBody
  def getCssScript(@PathVariable("fileName") fileName: String): String = {

    ScopeUtils.time(logger, "getCssScript") {
      Source.fromFile(s"$baseDir/styles/$fileName").getLines().mkString
    }

  }

  @RequestMapping(method = Array[RequestMethod](RequestMethod.GET), produces = Array[String]("text/html"))
  @ResponseBody
  def getIndexPage(request: HttpServletRequest): String = {

    ScopeUtils.time(logger, "getIndexPage") {
      Source.fromFile(s"$baseDir/index.html").getLines().mkString
    }

  }
}
