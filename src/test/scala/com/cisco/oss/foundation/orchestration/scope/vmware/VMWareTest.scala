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

package com.cisco.oss.foundation.orchestration.scope.vmware

import org.junit.Test
import com.vmware.vim25.mo._
import java.net.URL
import com.vmware.vim25._
import java.lang.String
import org.jclouds.route53.xml.GetHostedZoneResponseHandler
import scala.Predef._
import com.cisco.oss.foundation.orchestration.scope.vmware.G
import com.cisco.oss.foundation.orchestration.scope.vmware.H
import com.cisco.oss.foundation.orchestration.utils.ScopeUtils
import com.cisco.oss.foundation.orchestration.model.Module

/**
 * Created with IntelliJ IDEA.
 * User: igreenfi
 * Date: 2/9/14
 * Time: 9:38 AM
 */
class VMWareTest {

  val spec: HostConnectSpec = new HostConnectSpec()

  val compResSpec: ComputeResourceConfigSpec = new ComputeResourceConfigSpec()

  @Test
  def createMachineTest() {
    val si = new ServiceInstance(new URL("https://10.56.161.100/sdk"), "root", "master1234", true)

    val dcName: String = "NDS"
    val vmName: String = "vimasterVM"
    val memorySizeMB: Long = 500
    val cupCount: Int = 1
    val guestOsId: String = "rhel5Guest"
    val diskSizeKB: Long = 4.194e+7.toLong
    // mode: persistent|independent_persistent, independent_nonpersistent
    val diskMode: String = "persistent"
    val datastoreName: String = "CONDUCTOR2-local1 (1)"
    val netName: String = "VLAN860"
    val nicName: String = "Network Adapter 1"
    val rootFolder: Folder = si.getRootFolder
    val dc: Datacenter = new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", dcName).asInstanceOf[Datacenter]
    val rp: ResourcePool = new InventoryNavigator(dc).searchManagedEntities("ResourcePool")(0).asInstanceOf[ResourcePool]
    val vmFolder: Folder = dc.getVmFolder
    // create vm config spec
    val vmSpec: VirtualMachineConfigSpec = new VirtualMachineConfigSpec
    vmSpec.setName(vmName)
    vmSpec.setAnnotation("VirtualMachine Annotation")
    vmSpec.setMemoryMB(memorySizeMB)
    vmSpec.setNumCPUs(cupCount)
    vmSpec.setGuestId(guestOsId)
    // create virtual devices
    val cKey: Int = 1000
    val scsiSpec: VirtualDeviceConfigSpec = createScsiSpec(cKey)
    val diskSpec: VirtualDeviceConfigSpec = createDiskSpec(datastoreName, cKey, diskSizeKB, diskMode)
    val nicSpec: VirtualDeviceConfigSpec = createNicSpec(netName, nicName)
    vmSpec.setDeviceChange(Array[VirtualDeviceConfigSpec](scsiSpec, diskSpec, nicSpec))
    // create vm file info for the vmx file
    val vmfi: VirtualMachineFileInfo = new VirtualMachineFileInfo
    vmfi.setVmPathName("[" + datastoreName + "]")
    vmSpec.setFiles(vmfi)

    // call the createVM_Task method on the vm folder
    val task: Task = vmFolder.createVM_Task(vmSpec, rp, null)
    val result: String = task.waitForMe
    if (result eq Task.SUCCESS) {
      System.out.println("VM Created Sucessfully")
    }
    else {
      System.out.println("VM could not be created. ")
    }


  }

  private def createScsiSpec(cKey: Int): VirtualDeviceConfigSpec = {
    val scsiSpec: VirtualDeviceConfigSpec = new VirtualDeviceConfigSpec
    scsiSpec.setOperation(VirtualDeviceConfigSpecOperation.add)
    val scsiCtrl: VirtualLsiLogicController = new VirtualLsiLogicController
    scsiCtrl.setKey(cKey)
    scsiCtrl.setBusNumber(0)
    scsiCtrl.setSharedBus(VirtualSCSISharing.noSharing)
    scsiSpec.setDevice(scsiCtrl)
    return scsiSpec
  }

  private def createDiskSpec(dsName: String, cKey: Int, diskSizeKB: Long, diskMode: String): VirtualDeviceConfigSpec = {
    val diskSpec: VirtualDeviceConfigSpec = new VirtualDeviceConfigSpec
    diskSpec.setOperation(VirtualDeviceConfigSpecOperation.add)
    diskSpec.setFileOperation(VirtualDeviceConfigSpecFileOperation.create)
    val vd: VirtualDisk = new VirtualDisk
    vd.setCapacityInKB(diskSizeKB)
    diskSpec.setDevice(vd)
    vd.setKey(0)
    vd.setUnitNumber(0)
    vd.setControllerKey(cKey)
    val diskfileBacking: VirtualDiskFlatVer2BackingInfo = new VirtualDiskFlatVer2BackingInfo
    val fileName: String = "[" + dsName + "]"
    diskfileBacking.setFileName(fileName)
    diskfileBacking.setDiskMode(diskMode)
    diskfileBacking.setThinProvisioned(true)
    vd.setBacking(diskfileBacking)
    return diskSpec
  }

  private def createNicSpec(netName: String, nicName: String): VirtualDeviceConfigSpec = {
    val nicSpec: VirtualDeviceConfigSpec = new VirtualDeviceConfigSpec
    nicSpec.setOperation(VirtualDeviceConfigSpecOperation.add)
    val nic: VirtualEthernetCard = new VirtualPCNet32
    val nicBacking: VirtualEthernetCardNetworkBackingInfo = new VirtualEthernetCardNetworkBackingInfo
    nicBacking.setDeviceName(netName)
    val info: Description = new Description
    info.setLabel(nicName)
    info.setSummary(netName)
    nic.setDeviceInfo(info)
    nic.setAddressType("generated")
    nic.setBacking(nicBacking)
    nic.setKey(0)
    nicSpec.setDevice(nic)
    return nicSpec
  }



  @Test
  def generatedCode() {
    //val datacenterName = "CONDUCTOR2-local1 (1)"
    val datacenterName = "NDS"
    val cloneName = "Junit-test"

    val si = new ServiceInstance(new URL("https://10.56.161.100/sdk"), "root", "master1234", true)
    val sc = si.getServerConnection()

    val rootFolder = si.getRootFolder()
    val baseTemplate :VirtualMachine = new InventoryNavigator(rootFolder).searchManagedEntity("VirtualMachine", "Cisco Centos 6.5").asInstanceOf[VirtualMachine]
    val resourcePool: Array[ManagedEntity] = new InventoryNavigator(rootFolder).searchManagedEntities("ResourcePool")

    // Start: CloneVM_Task
    val virtualMachine2 = new VirtualMachine(sc, baseTemplate.getMOR)

    //val dc = si.getSearchIndex().findByInventoryPath(datacenterName).asInstanceOf[Datacenter]
    val dc = new InventoryNavigator(rootFolder).searchManagedEntity("Datacenter", datacenterName).asInstanceOf[Datacenter]
   // dc = si.getSearchIndex().findByInventoryPath(s"[$datacenterName]").asInstanceOf[Datacenter]


    if(baseTemplate==null || dc ==null)
    {
      System.out.println("VirtualMachine or Datacenter path is NOT correct. Pls double check. ")
      return
    }


    //val networkShaper = new VirtualMachineNetworkShaperInfo
    val net0 = createNicSpec("VLAN859","eth0")
    val net1 = createNicSpec("VLAN860","eth1")

    val vmFolder = dc.getVmFolder()
    val newConfig = new VirtualMachineConfigSpec()
    newConfig.setNumCPUs(4)
    newConfig.setMemoryMB(4000)
    newConfig.setDeviceChange(Array(net0,net1))
    //newConfig.setNetworkShaper(networkShaper)

    val cloneSpec = new VirtualMachineCloneSpec()
    val virtualMachineRelocateSpec1: VirtualMachineRelocateSpec = new VirtualMachineRelocateSpec()
    virtualMachineRelocateSpec1.setPool(resourcePool.head.getMOR)
    cloneSpec.setLocation(virtualMachineRelocateSpec1)
    cloneSpec.setPowerOn(true)
    cloneSpec.setTemplate(false)
    cloneSpec.setConfig(newConfig)

    val task = baseTemplate.cloneVM_Task(vmFolder, cloneName, cloneSpec)
    System.out.println("Launching the VM clone task. It might take a while. Please wait for the result ...")

    val status = 	task.waitForMe()

    val runScript = new RunScriptAction()
    runScript.setScript("cat 'tttt' > test.txt")

    val alarmSpec = new  AlarmSpec
    val action = new AlarmAction
    //action.s

    alarmSpec.setAction(action)
    si.getAlarmManager.createAlarm(task.getAssociatedManagedEntity, alarmSpec)

     /*
    val managedObjectReference2 = new ManagedObjectReference()
    managedObjectReference2.`type` = "Folder"
    managedObjectReference2.`val` = "group-v22"
    val folder4 = new Folder(sc, managedObjectReference2)

    val vmName = "Test-Izek"
    val virtualMachineCloneSpec = new VirtualMachineCloneSpec()
    val virtualMachineRelocateSpec = new VirtualMachineRelocateSpec()
    virtualMachineCloneSpec.setLocation(virtualMachineRelocateSpec)
    val datastore8 = new ManagedObjectReference()
    virtualMachineRelocateSpec.datastore = datastore8
    datastore8.`type` = "Datastore"
    datastore8.`val` = "datastore-51"
    val pool9 = new ManagedObjectReference()
    virtualMachineRelocateSpec.pool = pool9
    pool9.`type` = "ResourcePool"
    pool9.`val` = "resgroup-85"
    val host10 = new ManagedObjectReference()
    virtualMachineRelocateSpec.host = host10
    host10.`type` = "HostSystem"
    host10.`val` = "host-50"
    val disks11 = new Array[VirtualMachineRelocateSpecDiskLocator](1)
    val disk12 = new VirtualMachineRelocateSpecDiskLocator()
    disks11(0) = disk12
    disk12.diskId = 2000
    val datastore13 = new ManagedObjectReference()
    disk12.datastore = datastore13
    datastore13.`type` = "Datastore"
    datastore13.`val` = "datastore-51"
    virtualMachineCloneSpec.template = false
    val customization14 = new CustomizationSpec()
    virtualMachineCloneSpec.setCustomization(customization14)
    //spec6 = customization14
    val options15 = new CustomizationLinuxOptions()
    customization14.setOptions(options15)
    //customization14 = options15
    val identity16 = new CustomizationLinuxPrep()
    customization14.setIdentity(identity16)
    //customization14 = identity16

    val hostName17 = new CustomizationFixedName()
    identity16.setHostName(hostName17)
    hostName17.name = "Izek-Test"

    identity16.domain = "il.nds.com"
    identity16.timeZone = "Asia/Jerusalem"
    identity16.hwClockUTC = true

    val globalIPSettings18 = new CustomizationGlobalIPSettings()
    customization14.setGlobalIPSettings(globalIPSettings18)
    //customization14 = globalIPSettings18
    globalIPSettings18.dnsSuffixList = List("il.nds.com").toArray
    globalIPSettings18.dnsServerList = List("10.63.3.60").toArray
    val nicSettingMaps19 = new Array[CustomizationAdapterMapping](1)
    val nicSettingMap20 = new CustomizationAdapterMapping()
    nicSettingMaps19(0) = nicSettingMap20
    val adapter21 = new CustomizationIPSettings()
    nicSettingMap20.setAdapter(adapter21)
    //nicSettingMap20 = adapter21
    val ip22 = new CustomizationDhcpIpGenerator()
    adapter21.setIp(ip22)
    //adapter21 = ip22
    adapter21.primaryWINS = ""
    adapter21.secondaryWINS = ""
    virtualMachineCloneSpec.powerOn = true
    virtualMachine2.cloneVM_Task(folder4, vmName, virtualMachineCloneSpec)
    // End: CloneVM_Task
                                     */
    si.getServerConnection().logout()
  }

  @Test
  def testJ(){
    val f = ScopeUtils.mapper.readValue("{\n            \"name\": \"nds_umsui\",\n            \"version\": \"3.46.0-1\",\n            \"nodes\": [\"ndsconsoles\"],\n            \"file\": {\n                \"baseConfigProperties\":[\n                    \"config.properties\"\n                ],\n                \"additionalValues\":[\n                       {\n                          \"key\" : \"upm::host\",\n                          \"value\" : \"upm1\"\n                       }\n                       ,{\n                            \"key\" : \"ndsconsole::version\",\n                            \"value\" : \"3.45.1-SNAPSHOT\"\n                        }\n                ]\n            }\n        }",
      classOf[Module])


    println(f)

  }
}


import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import com.fasterxml.jackson.annotation.JsonSubTypes.Type


@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  include = JsonTypeInfo.As.PROPERTY,
  property = "type")
@JsonSubTypes( Array (
  new Type(value = classOf[G], name = "puppet"),
  new Type(value = classOf[H], name = "plugin")
)                     )
trait FFF {
  val `type` : String = "puppet"
  val name: String = ""
  val version: String = ""
}

case class G( val ccp: String, val length: Int) extends FFF
case class H( val size: Int, val value: Option[String]) extends FFF




