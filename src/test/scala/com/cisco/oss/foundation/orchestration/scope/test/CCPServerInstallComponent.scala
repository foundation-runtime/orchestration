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

import com.cisco.oss.foundation.orchestration.scope.utils.Slf4jLogger
import com.cisco.oss.foundation.orchestration.scope.configuration.IComponentInstallation
import javax.annotation.Resource

/**
 * Test CCP machine creation and deployment
 */
object CCPServerInstallComponent extends App with Slf4jLogger {

  @Resource(name = "componentInstallationImpl") val c: IComponentInstallation = null

  var configSchema: String =
    """|<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
      |<NamespaceDefinitions xsi:noNamespaceSchemaLocation="http://ch-infra.il.nds.com/cabResources/CCP_XML.xsd" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      |    <NamespaceDefinition>
      |        <NamespaceIdentifier version="3.49.1-0" name="emm"/>
      |        <NamespaceDependency>
      |            <NamespaceIdentifier version="3.49.0-2" name="embeddedMongoManager"/>
      |        </NamespaceDependency>
      |        <NamespaceDependency>
      |            <NamespaceIdentifier version="3.46.0-1" name="cabConfiguration"/>
      |        </NamespaceDependency>
      |        <Parameter base="mongoCluster.base" requiresRestart="false" required="false" description="Generic Mongo Cluster configuration" instantiationLevel="GROUP" type="STRUCTURE" name="generic.mongodb">
      |            <StructureDefinition>
      |                <StructureMemberDefinition description="List of hosts in mongo cluster." isArray="true" type="STRING" name="host"/>
      |                <StructureMemberDefinition type="INTEGER" name="shards">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="2"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition type="INTEGER" name="replicaSize">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="3"/>
      |                    </DefaultValue>
      |                    <Range>
      |                        <ValueRange max="5" min="1"/>
      |                    </Range>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" description="Size of opLog DB. if empty no opLog DB." type="INTEGER" name="opLogSize"/>
      |                <StructureMemberDefinition description="How mach time to wait until consider process as failed to laod." type="INTEGER" name="timeout">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="4"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="true" type="STRING" name="replicaName">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="rs"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition type="INTEGER" name="startingPort">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="27000"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition description="MongoDB version." type="STRING" name="version">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="2.4.5"/>
      |                    </DefaultValue>
      |                    <Range>
      |                        <StringEnum value="2.4.5"/>
      |                        <StringEnum value="2.4.6"/>
      |                    </Range>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" description="Type of the URL. (file, http, etc)." type="STRING" name="downloadPathType">
      |                    <Range>
      |                        <StringEnum value="file"/>
      |                        <StringEnum value="http"/>
      |                    </Range>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" description="Url to mongodb files." type="STRING" name="downloadPath"/>
      |            </StructureDefinition>
      |        </Parameter>
      |        <Parameter base="mongoCluster.authentication.base" required="false" description="Generic Mongo Cluster authentication configuration" instantiationLevel="GROUP" type="STRUCTURE" name="generic.mongodb.authentication">
      |            <StructureDefinition>
      |                <StructureMemberDefinition description="Full path to key file." advanced="true" type="STRING" name="keyFile"/>
      |                <StructureMemberDefinition description="mongo username." advanced="true" type="STRING" name="username"/>
      |                <StructureMemberDefinition description="mongo password." advanced="true" type="STRING" name="password"/>
      |            </StructureDefinition>
      |        </Parameter>
      |        <Parameter base="mongoCluster.js.base" required="false" description="Generic Mongo Cluster js configuration" instantiationLevel="GROUP" type="STRUCTURE" name="generic.mongodb.js">
      |            <StructureDefinition>
      |                <StructureMemberDefinition required="false" description="Mongo Cluster js directory." type="STRING" name="directory">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="/opt/nds/emm/js/"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" isArray="true" type="STRUCTURE" name="script">
      |                    <StructureDefinition>
      |                        <StructureMemberDefinition description="Script name to run." advanced="true" type="STRING" name="scriptName"/>
      |                        <StructureMemberDefinition description="Cron string." advanced="true" type="STRING" name="cronExpression"/>
      |                        <StructureMemberDefinition description="List of vars. example: var num = 5;" advanced="true" isArray="true" type="STRUCTURE" name="parameters">
      |                            <StructureDefinition>
      |                                <StructureMemberDefinition type="STRING" name="name"/>
      |                                <StructureMemberDefinition type="STRING" name="value"/>
      |                            </StructureDefinition>
      |                        </StructureMemberDefinition>
      |                    </StructureDefinition>
      |                </StructureMemberDefinition>
      |            </StructureDefinition>
      |        </Parameter>
      |    </NamespaceDefinition>
      |    <NamespaceDefinition>
      |        <NamespaceIdentifier version="3.49.0-2" name="embeddedMongoManager"/>
      |        <ParameterType description="Mongo Cluster configuration" type="STRUCTURE" name="mongoCluster.base">
      |            <StructureDefinition>
      |                <StructureMemberDefinition description="List of hosts in mongo cluster." isArray="true" type="STRING" name="host"/>
      |                <StructureMemberDefinition type="INTEGER" name="shards">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="2"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition type="INTEGER" name="replicaSize">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="3"/>
      |                    </DefaultValue>
      |                    <Range>
      |                        <ValueRange max="5" min="1"/>
      |                    </Range>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" description="Size of opLog DB. if empty no opLog DB." type="INTEGER" name="opLogSize"/>
      |                <StructureMemberDefinition description="How mach time to wait until consider process as failed to laod." type="INTEGER" name="timeout">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="4"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="true" type="STRING" name="replicaName">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="rs"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition type="INTEGER" name="startingPort">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="27000"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition description="MongoDB version." type="STRING" name="version">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="2.4.5"/>
      |                    </DefaultValue>
      |                    <Range>
      |                        <StringEnum value="2.4.5"/>
      |                        <StringEnum value="2.4.6"/>
      |                    </Range>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" description="Type of the URL. (file, http, etc)." type="STRING" name="downloadPathType">
      |                    <Range>
      |                        <StringEnum value="file"/>
      |                        <StringEnum value="http"/>
      |                    </Range>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" description="Url to mongodb files." type="STRING" name="downloadPath"/>
      |            </StructureDefinition>
      |        </ParameterType>
      |        <ParameterType required="false" description="Mongo Cluster authentication configuration" type="STRUCTURE" name="mongoCluster.authentication.base">
      |            <StructureDefinition>
      |                <StructureMemberDefinition description="Full path to key file." advanced="true" type="STRING" name="keyFile"/>
      |                <StructureMemberDefinition description="mongo username." advanced="true" type="STRING" name="username"/>
      |                <StructureMemberDefinition description="mongo password." advanced="true" type="STRING" name="password"/>
      |            </StructureDefinition>
      |        </ParameterType>
      |        <ParameterType required="false" description="Mongo Cluster js configuration" type="STRUCTURE" name="mongoCluster.js.base">
      |            <StructureDefinition>
      |                <StructureMemberDefinition required="false" description="Mongo Cluster js directory." type="STRING" name="directory">
      |                    <DefaultValue>
      |                        <PrimitiveValue value="/opt/nds/emm/js/"/>
      |                    </DefaultValue>
      |                </StructureMemberDefinition>
      |                <StructureMemberDefinition required="false" isArray="true" type="STRUCTURE" name="script">
      |                    <StructureDefinition>
      |                        <StructureMemberDefinition description="Script name to run." advanced="true" type="STRING" name="scriptName"/>
      |                        <StructureMemberDefinition description="Cron string." advanced="true" type="STRING" name="cronExpression"/>
      |                        <StructureMemberDefinition description="List of vars. example: var num = 5;" advanced="true" isArray="true" type="STRUCTURE" name="parameters">
      |                            <StructureDefinition>
      |                                <StructureMemberDefinition type="STRING" name="name"/>
      |                                <StructureMemberDefinition type="STRING" name="value"/>
      |                            </StructureDefinition>
      |                        </StructureMemberDefinition>
      |                    </StructureDefinition>
      |                </StructureMemberDefinition>
      |            </StructureDefinition>
      |        </ParameterType>
      |    </NamespaceDefinition>
      |    <NamespaceDefinition>
      |        <NamespaceIdentifier version="3.46.0-1" name="cabConfiguration"/>
      |        <Parameter hidden="true" description="set to true if you want to have dyanmic supoprt enabled in your process" instantiationLevel="GLOBAL" type="BOOLEAN" name="dynamicConfigReload.enabled">
      |            <DefaultValue>
      |                <PrimitiveValue value="false"/>
      |            </DefaultValue>
      |        </Parameter>
      |        <Parameter unit="MILLISECONDS" advanced="true" description="define the perios of time to check for new config changes." instantiationLevel="GLOBAL" type="INTEGER" name="dynamicConfigReload.refreshDelay">
      |            <DefaultValue>
      |                <PrimitiveValue value="30000"/>
      |            </DefaultValue>
      |        </Parameter>
      |        <Parameter description="when set to true will throw an exception if a validation violation was found" type="BOOLEAN" name="throwErrorOnValidationViolation">
      |            <DefaultValue>
      |                <PrimitiveValue value="false"/>
      |            </DefaultValue>
      |        </Parameter>
      |    </NamespaceDefinition>
      |</NamespaceDefinitions>""".stripMargin

  var properties: String =
    """|generic.mongodb.downloadPathType = file
      |generic.mongodb.downloadPath = /usr/mongodb/
      |generic.mongodb.startingPort = 27017""".stripMargin

  c.install("localhost", 7890, "123456", "test", "3.49.1-0", "vgc1a", "/opt/nds/installed/emm-3.49.1-0", "emm", configSchema, properties, false)

}
