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

package com.cisco.oss.foundation.orchestration.scope.selenium

import org.junit.{BeforeClass, Test}
//import org.openqa.selenium._
//import org.openqa.selenium.remote.{CapabilityType, DesiredCapabilities, RemoteWebDriver}
import java.net.URL
import com.cisco.oss.foundation.orchestration.scope.utils.Slf4jLogger
//import org.openqa.selenium.support.ByIdOrName
//import org.openqa.selenium.interactions.Actions
//import org.openqa.selenium.support.ui.{Select, ExpectedConditions, WebDriverWait}
import org.apache.commons.lang3.StringUtils
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * CISCO LTD.
 * User: igreenfi
 * Date: 27/05/2014 1:03 PM
 * Package: com.cisco.oss.foundation.orchestration.scope.selenium
 */
object SeleniumTest {
//  var driver: RemoteWebDriver = null
//
//  @BeforeClass
//  def beforeClass() {
//    val desiredCapabilities: DesiredCapabilities = DesiredCapabilities.firefox()
//    //webdriver.firefox.firefox /usr/bin/firefox
//    //desiredCapabilities.setCapability("webdriver.firefox.firefox", "/usr/bin/firefox")
//    desiredCapabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true)
//    driver = new RemoteWebDriver(new URL("http://10.45.37.221:4444/wd/hub"), desiredCapabilities)
//    //    driver.get("http://www.google.com")
//    driver.get("https://10.45.38.20/")
//
//    val username = (new WebDriverWait(SeleniumTest.driver, 60))
//      .until(ExpectedConditions.presenceOfElementLocated(new ByIdOrName("username")))
//    username.sendKeys("root")
//    val password = driver.findElement(new ByIdOrName("password"))
//    password.sendKeys("Public123")
//    val login = driver.findElement(new ByIdOrName("loginPage_loginSubmit"))
//    login.click()
//  }
}

class SeleniumTest extends Slf4jLogger {

//
//  @Test
//  def seleniumDeploySaslTest() {
//    val messageInfra = (new WebDriverWait(SeleniumTest.driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_menu_infra > span:nth-child(1) > span:nth-child(3)")))
//    val action: Actions = new Actions(SeleniumTest.driver)
//    action.moveToElement(messageInfra)
//    action.perform()
//
//    val clientFacingConfig: WebElement = (new WebDriverWait(SeleniumTest.driver, 10))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_menu_infrasasldeploy > span:nth-child(1) > a:nth-child(1)")))
//    (new WebDriverWait(SeleniumTest.driver, 60))
//      .until(ExpectedConditions.visibilityOf(clientFacingConfig))
//    clientFacingConfig.click()
//
//    val deployButton = (new WebDriverWait(SeleniumTest.driver, 20)).until(ExpectedConditions.titleContains("Deploy"))
//
//
//
//    val infraRadioButton = (new WebDriverWait(SeleniumTest.driver, 20))
//      .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@type='radio' and ../../../../*/*[contains(text(), 'Infra')]]")))
//
//    infraRadioButton.click()
//
//    var file1 = SeleniumTest.driver.getScreenshotAs(OutputType.FILE)
//    FileUtils.copyFile(file1, new File("d:/temp/1.png"))
//
//    val saslPluginRadioButton = (new WebDriverWait(SeleniumTest.driver, 20))
//      .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@type='radio' and ../../../../*/*[contains(text(), 'SASLexternhttp.tar')] and ../../../../*/*[contains(text(), 'SASL')] and ../../../../../../../../../../..[@id='fileTable']]")))
//
//    saslPluginRadioButton.click()
//
//
//    //val downloadButton = (new WebDriverWait(SeleniumTest.driver, 10)).until(ExpectedConditions.visibilityOfElementLocated(By.id("downloadButton")))
//    file1 = SeleniumTest.driver.getScreenshotAs(OutputType.FILE)
//    FileUtils.copyFile(file1, new File("d:/temp/2.png"))
//
//    (new WebDriverWait(SeleniumTest.driver, 20)).until(ExpectedConditions.elementToBeClickable(By.id("downloadButton"))).click()
//
//
//    val alert: Alert = (new WebDriverWait(SeleniumTest.driver, 10))
//      .until(ExpectedConditions.alertIsPresent());
//    logInfo(alert.getText)
//    alert.accept();
//
//    file1 = SeleniumTest.driver.getScreenshotAs(OutputType.FILE)
//    FileUtils.copyFile(file1, new File("d:/temp/3.png"))
//
//  }
//
//  @Test
//  def seleniumSetSftpUserAccountTest() {
//    val messageInfra = (new WebDriverWait(SeleniumTest.driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_menu_administration > span:nth-child(1) > span:nth-child(3)")))
//    val action: Actions = new Actions(SeleniumTest.driver)
//    action.moveToElement(messageInfra)
//    action.perform()
//
//    val changePassword: WebElement = (new WebDriverWait(SeleniumTest.driver, 10))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_admin_accounts_and_aaa > span:nth-child(1) > span:nth-child(1) > b:nth-child(2) > a:nth-child(1)")))
//    (new WebDriverWait(SeleniumTest.driver, 60))
//      .until(ExpectedConditions.visibilityOf(changePassword))
//    changePassword.click()
//
//    (new WebDriverWait(SeleniumTest.driver, 20)).until(ExpectedConditions.titleContains("Change Password"))
//
//    val sftpLink = SeleniumTest.driver.findElement(By.xpath("//a[contains(@href,'sftpusercreation_pageId')]"))
//
//    sftpLink.click()
//
//    val username = (new WebDriverWait(SeleniumTest.driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.id("userName")))
//
//    if (StringUtils.isEmpty(username.getText)) {
//      val userPassword = (new WebDriverWait(SeleniumTest.driver, 20))
//        .until(ExpectedConditions.presenceOfElementLocated(By.id("userPassword")))
//
//      username.sendKeys("cisco123")
//      userPassword.sendKeys("cisco123")
//
//      val submitButton = SeleniumTest.driver.findElement(By.xpath("//input[..[@id='sftpuser_button']]"))
//      submitButton.click()
//
//      val alert: Alert = (new WebDriverWait(SeleniumTest.driver, 10))
//        .until(ExpectedConditions.alertIsPresent());
//      logInfo(alert.getText)
//      alert.accept();
//    }
//
//  }
//
//  @Test
//  def seleniumConfigureFacingTest() {
//    val desiredCapabilities: DesiredCapabilities = DesiredCapabilities.firefox()
//    //webdriver.firefox.firefox /usr/bin/firefox
//    //desiredCapabilities.setCapability("webdriver.firefox.firefox", "/usr/bin/firefox")
//    desiredCapabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true)
//    val driver: WebDriver = new RemoteWebDriver(new URL("http://10.45.37.221:4444/wd/hub"), desiredCapabilities)
//    //    driver.get("http://www.google.com")
//    driver.get("https://10.45.38.20/")
//    val username = driver.findElement(new ByIdOrName("username"))
//    username.sendKeys("root")
//    val password = driver.findElement(new ByIdOrName("password"))
//    password.sendKeys("Public123")
//    val login = driver.findElement(new ByIdOrName("loginPage_loginSubmit"))
//    login.click()
//
//
//    val messageInfra = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_menu_infra > span:nth-child(1) > span:nth-child(3)")))
//    val action: Actions = new Actions(driver)
//    action.moveToElement(messageInfra)
//    action.perform()
//
//    val clientFacingConfig: WebElement = (new WebDriverWait(driver, 10))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_menu_infracfconfig > span:nth-child(1) > a:nth-child(1)")))
//    (new WebDriverWait(driver, 60))
//      .until(ExpectedConditions.visibilityOf(clientFacingConfig))
//    clientFacingConfig.click()
//
//    (new WebDriverWait(driver, 20)).until(ExpectedConditions.titleContains("Client Facing Configuration"))
//
//    val logLevelCombo = driver.findElement(By.xpath("//table[@widgetid='logLevel']"))
//    logLevelCombo.click()
//
//    val debugLogLevel = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[contains(text(), 'Debug')]")))
//    debugLogLevel.click()
//
//    val removefromMeclist = driver.findElement(By.id("removefromMeclist"))
//    val meclistbox = new Select(driver.findElement(By.id("meclistbox")))
//    while (meclistbox.getOptions.size() > 0) {
//      meclistbox.selectByIndex(0)
//      removefromMeclist.click()
//    }
//
//    val saslCombo = driver.findElement(By.xpath("//table[@widgetid='saslFileName']"))
//    saslCombo.click()
//
//    val saslMethod = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//td[contains(text(), 'SASLexternhttp.tar')]")))
//    saslMethod.click()
//    val saslMecName = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.visibilityOfElementLocated(By.id("saslMecName")))
//    saslMecName.clear()
//    saslMecName.sendKeys("PLAIN")
//    val saslLib = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.visibilityOfElementLocated(By.id("saslLib")))
//    saslLib.clear()
//    saslLib.sendKeys("externhttp.so")
//    val saslLoad = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.visibilityOfElementLocated(By.id("saslLoad")))
//    saslLoad.clear()
//    saslLoad.sendKeys("sasl_externhttp")
//
//    val addtoMeclist = driver.findElement(By.id("addtoMeclist"))
//    addtoMeclist.click()
//
//    val saveButton = driver.findElement(By.id("saveBtn"))
//    saveButton.click()
//
//    val okButton = (new WebDriverWait(driver, 30))
//      .until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//button[contains(@id, 'TextButton') and contains(*/text(), 'OK') and ../../../../../../*/*/*[contains(text(), 'The configuration will take effect after approximate')]]")))
//
//    okButton.click()
//
//    logInfo(driver.getCurrentUrl)
//  }
//
//
//  @Test
//  def seleniumImportSaslPluginTest() {
//    val desiredCapabilities: DesiredCapabilities = DesiredCapabilities.firefox()
//    //webdriver.firefox.firefox /usr/bin/firefox
//    //desiredCapabilities.setCapability("webdriver.firefox.firefox", "/usr/bin/firefox")
//    desiredCapabilities.setCapability(CapabilityType.SUPPORTS_JAVASCRIPT, true)
//    val driver: WebDriver = new RemoteWebDriver(new URL("http://10.45.37.221:4444/wd/hub"), desiredCapabilities)
//    //    driver.get("http://www.google.com")
//    driver.get("https://10.45.38.20/")
//    val username = driver.findElement(new ByIdOrName("username"))
//    username.sendKeys("root")
//    val password = driver.findElement(new ByIdOrName("password"))
//    password.sendKeys("Public123")
//    val login = driver.findElement(new ByIdOrName("loginPage_loginSubmit"))
//    login.click()
//
//    val messageInfra = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_menu_infra > span:nth-child(1) > span:nth-child(3)")))
//    val action: Actions = new Actions(driver)
//    action.moveToElement(messageInfra)
//    action.perform()
//
//    val uploadLink: WebElement = (new WebDriverWait(driver, 10))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#wcsuishell_node_id_menu_infrasaslupload > span:nth-child(1) > a:nth-child(1)")))
//    (new WebDriverWait(driver, 60))
//      .until(ExpectedConditions.visibilityOf(uploadLink))
//    uploadLink.click()
//
//    val importButton = (new WebDriverWait(driver, 10))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#downloadButton")))
//    importButton.click()
//
//    val sftpRadioButton = (new WebDriverWait(driver, 20))
//      .until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("#collectionSource4")))
//    sftpRadioButton.click()
//
//    val sftpServer = driver.findElement(By.cssSelector("#fServer"))
//    val sftpUser = driver.findElement(By.cssSelector("#fUserid"))
//    val sftpPassword = driver.findElement(By.cssSelector("#fPassword"))
//    val sftpDir = driver.findElement(By.cssSelector("#fDirectory"))
//    val sftpFile = driver.findElement(By.cssSelector("#fFile"))
//    val sftpSubbmit = driver.findElement(By.cssSelector("#scheduleCollectBtn"))
//
//    sftpServer.clear()
//    sftpServer.sendKeys("10.45.37.10")
//    sftpUser.clear()
//    sftpUser.sendKeys("root")
//    sftpPassword.clear()
//    sftpPassword.sendKeys("master")
//    sftpDir.clear()
//    sftpDir.sendKeys("/opt/cisco/scopeData/products")
//    sftpFile.clear()
//    sftpFile.sendKeys("SASLexternhttp.tar")
//    sftpSubbmit.click()
//
//    val alert: Alert = (new WebDriverWait(driver, 10))
//      .until(ExpectedConditions.alertIsPresent());
//    logInfo(alert.getText)
//    alert.accept();
//
//    (new WebDriverWait(driver, 60 * 4))
//      .until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(text(), 'SASLexternhttp.tar')]")))
//
//    logInfo(driver.getCurrentUrl)
//    logInfo(driver.getTitle)
//    driver.quit()
//  }

}
