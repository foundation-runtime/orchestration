package com.cisco.oss.foundation.orchestration.scope.resource.ui

import java.io.FileInputStream
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.cisco.oss.foundation.orchestration.scope.utils.{ScopeUtils, Slf4jLogger}
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._

import scala.io.Source

/**
 * Created by igreenfi on 14/09/2014.
 */
@Controller
class YumResource extends Slf4jLogger {

  private val baseDir = ScopeUtils.configuration.getString("scope-ui.scope-base.yum.baseDir", "/opt/cisco/scopeData/scope-base/")
  private val productsBaseDir = ScopeUtils.configuration.getString("scope-ui.scope-products.yum.baseDir", "/opt/cisco/scopeData/products/")

  @RequestMapping(value = Array[String]("/scope-{repoType}/**/yum/**/{fileName:.*}.{ext}"), method = Array[RequestMethod](RequestMethod.GET))
  def getFile(request: HttpServletRequest, response: HttpServletResponse, @PathVariable("repoType") repoType: String, @PathVariable("fileName") fileName: String, @PathVariable("ext") ext: String): Unit = {
    ScopeUtils.time(logger, "getFile") {
      val baseUrl = repoType match {
        case "base" => baseDir
        case "products" => productsBaseDir
      }

      var uri = StringUtils.replace(request.getRequestURI, "/yum/", "/")
      uri = StringUtils.replace(uri, s"/scope-$repoType", "/")

      ext match {
        case "rpm" | "bz2" => readAndReturnBinaryFile(response, s"$baseDir/$uri")
        case "xml" => readAndReturnTextFile(response, s"$baseDir/$uri")
      }
    }
  }

  private def readAndReturnTextFile(response: HttpServletResponse, fullPathToFile: String) {
    IOUtils.copy(Source.fromFile(fullPathToFile).reader(), response.getOutputStream())
    response.flushBuffer()
  }


  private def readAndReturnBinaryFile(response: HttpServletResponse, fullPathToFile: String) {
    val fileInputStream: FileInputStream = new FileInputStream(fullPathToFile)
    IOUtils.copy(fileInputStream, response.getOutputStream())
    response.flushBuffer()
    fileInputStream.close()
  }
}
