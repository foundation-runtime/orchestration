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

package com.cisco.oss.foundation.orchestration.scope.utils

import java.io.{ByteArrayInputStream, ObjectInputStream, ObjectOutputStream}
import java.lang.System
import java.nio.file.{Files, Path}

import com.cisco.oss.foundation.orchestration.scope.model._
import com.cisco.oss.foundation.orchestration.scope.provision.model.ProductRepoInfo
import com.fasterxml.jackson.databind.`type`.TypeFactory
import org.apache.commons.io.FileUtils
import org.apache.commons.io.output.ByteArrayOutputStream

import scala.collection.JavaConversions._
import scala.collection.immutable.HashMap

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 11/14/13
 * Time: 2:52 PM
// */
object ModelUtils extends Slf4jLogger {

  def materialize(product: Product, instance: Instance): String = {
    var materializerPath: Path = null
    try {
      materializerPath = Files.createTempDirectory(s"materializer-${product.id}")
      val productRepoInfo = new ProductRepoInfo(product.repoUrl)
      downloadMaterializer(materializerPath, productRepoInfo.productMaterializerUrl)

      val cmd = if (isWindows) Array[String]("cmd", "/C", "run.bat")
      else Array[String]("./run.sh")

      var input = ScopeUtils.mapper.writeValueAsString(product)
      input += ";~;" + ScopeUtils.mapper.writeValueAsString(instance.copy(rsaKeyPair = Map()))
      logInfo("Materializer input is: {}", input)

      //run process and set the working directory to the materializerPath
      val jsonString = sys.process.Process(cmd, materializerPath.toFile.getAbsoluteFile()) #< new ByteArrayInputStream(input.getBytes("UTF-8")) !!

      logInfo("Result from materializer function is: {}", jsonString)
      jsonString
    } finally {
      if (materializerPath != null)
        FileUtils.deleteQuietly(materializerPath.toFile)
    }
  }

  private def isWindows: Boolean = {
    System.getProperty("os.name").toLowerCase().startsWith("windows")
  }

  private def downloadMaterializer(path: Path, materializerUrl: String) {
    logInfo("Downloading materializer from {}", materializerUrl)
    val pathDepth = ScopeUtils.getPathDepth(materializerUrl)
    val downloadCmd = Seq("wget", "--quiet", "-r", "-nH", "--level=100", "--no-parent", s"--cut-dirs=${pathDepth}", "--reject=\"index.html*\"", materializerUrl)
    if (sys.process.Process(downloadCmd, path.toFile.getAbsoluteFile).! != 0)
      throw new IllegalArgumentException(s"Can NOT download materializer from ${materializerUrl}")

    if (!isWindows) {
      val chmodCmd = Seq("chmod", "+x", "-R", ".")
      if (sys.process.Process(chmodCmd, path.toFile.getAbsoluteFile).! != 0)
        throw new IllegalArgumentException(s"Can NOT execute materializer")
    }
  }

  def StringToPojo(deploymentModel: String) = {
    try {
      ScopeUtils.mapper.readValue(deploymentModel, TypeFactory.defaultInstance().constructCollectionLikeType(classOf[List[DeploymentModelCapture]], classOf[DeploymentModelCapture]))
    } catch {
      case e: Exception => List(ScopeUtils.mapper.readValue(deploymentModel, classOf[DeploymentModelCapture]))
    }
  }

  def processDeploymentModel(deploymentModel: DeploymentModelCapture, productRepoInfo: ProductRepoInfo, puppetScriptName: String) = {
    ScopeUtils.time(logger, "processDeploymentModel") {
      var hosts = new HashMap[String, InstallationPart]
      var steps = new HashMap[String, HashMap[String, InstallationPart]]

      deploymentModel.installNodes.nodes.foreach {
        node => {
          val nodeName = node.name.toLowerCase
          val scopeBase: String =
            """
              |class role::scopeBase {
              |       require admin::ssh_disable_host_key_check
              |       require admin::augeas
              |       require admin::ntp
              |       require admin::yum
              |}
            """.stripMargin
          val puppetPrefix: String = scopeBase +
            s"""class role::$puppetScriptName inherits role::scopeBase {
                  #####################
                  # hostname $nodeName
                  #####################\n"""

          hosts = hosts + (nodeName -> InstallationPart(Some(PuppetRole(puppetPrefix.replace("\r", ""),
            Map[String, String](
              ("bin_repo::url" -> (productRepoInfo.productYumRepoUrl)),
              ("bin_repo::components::url" -> (productRepoInfo.productYumRepoUrl))),
            List[String]())), None)
            )
        }
      }


      deploymentModel.installModules.foreach {
        case (step, modules) => {
          var localhostMap: HashMap[String, InstallationPart] = deepCloneAnyRef(hosts).asInstanceOf[HashMap[String, InstallationPart]]
          modules.modules.foreach {
            case module => {
              module.nodes.foreach {
                case node => {
                  val lowerCaseNode = node.toLowerCase
                  val role = localhostMap.get(lowerCaseNode).getOrElse(new InstallationPart(Some(PuppetRole("", Map[String, String](), List[String]())), None))
                  module match {
                    case m: PuppetModule => {

                      m.deployFile match {
                        case Some(deployFile) => {
                          role.puppet.get.script +=
                            s"""
                          |  file { '${deployFile.destinationPath}':
                          |    content => base64('decode', '${deployFile.content}'),
                          |    owner => '${deployFile.owner}',
                          |    group => '${deployFile.group}',
                          |    mode => '${deployFile.mode}',
                          |  }
                        """.stripMargin
                        }
                        case None => {
                          val puppetModuleName: String = module.asInstanceOf[PuppetModule].name
                          role.puppet.get.modulesName += puppetModuleName
                          role.puppet.get.script += s"include $puppetModuleName\n"
                          role.puppet.get.configuration += (s"$puppetModuleName::version" -> module.asInstanceOf[PuppetModule].version)
                          module.asInstanceOf[PuppetModule].file match {
                            case Some(configurationServerSection) => {
                              configurationServerSection.additionalValues.foreach(parameter => {
                                role.puppet.get.configuration += (parameter.key -> parameter.value)
                              })
                            }
                            case None =>
                          }
                        }
                      }
                      role.puppet.get.hostname ::= lowerCaseNode
                      role.puppet.get.hostname = role.puppet.get.hostname.sorted

                    }
                    case m: PluginModule => {
                      role.plugin = Some(PluginRole(m.className, m.configuration))
                    }
                  }
                  localhostMap = localhostMap + (lowerCaseNode -> role)
                }
              }
            }
          }
          localhostMap.foreach {
            case (host, part) => {
              if (part.puppet.get.script.endsWith("#####################\n")) {
                part.plugin match {
                  case None => localhostMap -= host
                  case Some(_) => part.puppet = None
                }
              }
              else if (!part.puppet.get.script.endsWith("}")) {
                part.puppet.get.script += "}"
                localhostMap = localhostMap + (host -> part)
              }
            }
          }
          steps = steps + (step -> localhostMap)
        }
      }

      if (logger.isDebugEnabled())
        steps.foreach {
          case (step, map) => {
            map.foreach {
              case (host, role) => {
                logDebug("Step: {}; Hostname: {}; Puppet: {}; plugin: {}", step, host, role.puppet, role.plugin)
              }
            }
          }
        }

      (steps, deploymentModel.resources)
    }
  }

  private def deepClone(orig: HashMap[String, InstallationPart]) = {
    var copy = new HashMap[String, InstallationPart]
    orig.foreach {
      case (host, part) => {
        copy = copy + (host -> deepCloneAnyRef(part).asInstanceOf[InstallationPart])
      }
    }
    copy
  }

  /**
   * This method makes a "deep clone" of any Java object it is given.
   */
  private def deepCloneAnyRef(obj: AnyRef) = {
    try {
      val baos = new ByteArrayOutputStream()
      val oos = new ObjectOutputStream(baos)
      oos.writeObject(obj)
      val bais = new ByteArrayInputStream(baos.toByteArray())
      val ois = new ObjectInputStream(bais)
      ois.readObject()
    }
    catch {
      case e: Exception => {
        logError(e.getMessage, e)
        throw new IllegalStateException("Failed to deepClone object.")
      }
    }
  }
}
