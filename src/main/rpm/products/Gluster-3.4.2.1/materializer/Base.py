try:
    import json
except ImportError:
    import simplejson as json

try:
    from builtins import input as read_stdin
except:
    from __builtin__ import raw_input as read_stdin

class materializerBase(object):
    '''

    '''

    product = None
    productOptions = None
    instance = None
    tenantId = None
    instanceName = None

    def readInput(self):
        inputString = read_stdin()

        return inputString

    def parseInput(self):
        '''
        Reads the input from STDIN and convert it to JSON object.
        '''
        inputData = self.readInput()
        product, instance = inputData.split(';~;')
        self.product = json.loads(product)
        self.productOptions = self.product["productOptions"]
        self.instance = json.loads(instance)
        self.tenantId = self.instance['systemId']
        self.instanceName = self.instance['instanceName']

    def createAdditionalValues2(self, d):
        listOfValues = list()
        for key,value in d.iteritems():
            m = dict()
            m['key'] = key
            m['value'] = value
            listOfValues.append(m)
        return listOfValues

    def createAdditionalValues(self, keys, values):
        listOfValues = list()
        for key,value in zip(keys,values):
            m = dict()
            m['key'] = key
            m['value'] = value
            listOfValues.append(m)
        return listOfValues

    def createNodelist(self, prefix, count):
        nodes = []
        for i in range(0,count):
            nodes.append(self.createVmName(prefix + str(i)))
        return nodes

    def createNodelist(self, prefix, count, includeInstanceName = True):
        nodes = []
        for i in range(0,count):
            nodes.append(self.createVmName(prefix + str(i), includeInstanceName))
        return nodes

    def createFile(self, baseConfigProperties = list(), additionalValues = dict()):
        '''
        Sample:
        {
            "baseConfigProperties":[
                "config.properties"
            ],
            "additionalValues":[
                {
                    "key":"upm::web_port",
                    "value":"6040"
                },
                {
                    "key":"upm::topic",
                    "value":"UPM-CD"
                },
                {
                    "key":"upm::dbname",
                    "value":"mongodb"
                },
                {
                    "key":"upm::host",
                    "value":"%{::ipaddress}"
                },
                {
                    "key":"flavor",
                    "value":"canal-digital"
                },
                {
                    "key":"mongodb::host",
                    "value":"mongo1"
                },
                {
                    "key":"mongodb::port",
                    "value":"27017"
                },
                {
                    "key":"fileSection::enabled",
                    "value":"false"
                },
                {
                    "key":"upm::hornetq::is_enabled",
                    "value":"false"
                },
                {
                    "key":"upm::log4j_mongo",
                    "value":"set log4j.rootLogger error,logfile,MongoDB;;set log4j.appender.MongoDB.port 27017;;set log4j.appender.MongoDB.hostname mongo1;;set log4j.appender.MongoDB com.nds.cab.infra.logging.appender.CABLoggingMongoDBAppender;;set log4j.appender.MongoDB.databaseName logview;;set log4j.appender.MongoDB.collectionName logs;;set log4j.appender.MongoDB.layout com.nds.cab.infra.logging.CABLoggingPatternLayout;;set log4j.appender.MongoDB.layout.ConversionPattern %j;;set log4j.appender.MongoDB.numOfDBPerDay 1"
                },
                {
                    "key":"upm::config_props",
                    "value":"set passwordQuality.checkNumericCharacterIncluded  false;;set passwordQuality.checkNonAlphaNumericIncluded  false;;set passwordQuality.checkNoVowelsIncluded  false;;set passwordQuality.checkMinimumPasswordLength  false;;set passwordQuality.minimumPasswordLength 5;;set upm.cdplugin.defaults.countryToPopulation.SWE 5;;set upm.cdplugin.defaults.countryToPopulation.FIN 5;;set upm.defaults.household.devices.device.isConnected.boolean.deviceFullType.map {\\"GATEWAY-11.7\\":false,\\"IP_ZAPPER\\":false,\\"STANDALONE_STB\\":false,\\"STANDALONE_STB_PVR\\":false};;set upm.defaults.household.devices.device.deviceType.string.deviceFullType.map {\\"GATEWAY-11.7\\":\\"GATEWAY\\",\\"IP_ZAPPER\\":\\"ZAPPER\\",\\"STANDALONE_STB\\":\\"ZAPPER\\",\\"STANDALONE_STB_PVR\\":\\"ZAPPER\\",\\"IPAD_6\\":\\"IPAD\\",\\"IPAD_5\\":\\"IPAD\\",\\"IPAD_4\\":\\"IPAD\\"};;set upm.defaults.household.devices.device.deviceFeatures.array.deviceFullType.map {\\"GATEWAY-11.7\\":[\\"STB\\",\\"FUSION\\",\\"PVR\\",\\"DVD\\",\\"GATEWAY\\"],\\"IP_ZAPPER\\":[\\"STB\\",\\"FUSION\\"],\\"STANDALONE_STB\\":[\\"STB\\",\\"FUSION\\",\\"DVD\\"],\\"STANDALONE_STB_PVR\\":[\\"STB\\",\\"FUSION\\",\\"PVR\\",\\"DVD\\"],\\"IPAD_6\\":[\\"COMPANION\\",\\"IOS\\",\\"IPAD\\",\\"IOS:6\\"],\\"IPAD_5\\":[\\"COMPANION\\",\\"IOS\\",\\"IPAD\\",\\"IOS:5\\"],\\"IPAD_4\\":[\\"COMPANION\\",\\"IOS\\",\\"IPAD\\",\\"IOS:4\\"]};;set upm.defaults.household.deviceQuota.ALL.integer 4;;set upm.defaults.household.deviceQuota.IPAD.integer 2;;set upm.defaults.household.deviceQuota.COMPANION.integer 1;;set upm.defaults.household.deviceQuota.PC.integer 3;;set upm.defaults.household.deviceQuota.IOS.integer 3;;set upm.defaults.household.authorizations.subscriptions.array [{\\"authorizationId\\":\\"70002\\",\\"authorizationType\\":\\"SUBSCRIPTION\\"},{\\"authorizationId\\":\\"70016\\",\\"authorizationType\\":\\"SUBSCRIPTION\\"},{\\"authorizationId\\":\\"66666\\",\\"authorizationType\\":\\"SUBSCRIPTION\\"}]"
                }
            ]
        }
        '''
        fileSection = dict()

        fileSection['baseConfigProperties'] = baseConfigProperties
        fileSection['additionalValues'] = additionalValues

        return fileSection

    def createCaptureTemplate(self):
        '''

        '''
        installNodes = dict()
        installNodes['nodes'] = list()
        sections = dict()
        sections['sections'] = list()

        schema = dict()

        schema['schemaVersion'] = '0.1'
        schema['installNodes'] = installNodes
        schema['setupProvisioningEnv'] = True
        schema['announceHostNames'] = True
        schema['installModules'] = dict()

        schema['preDeleteNodesScript'] = sections
        schema['exposeAccessPoints'] = dict()

        return schema

    def createPreDeleteNodesScript(self, nodes, script):
        scriptSection = dict()
        scriptSection['nodes'] = nodes
        scriptSection['script'] = script
        return scriptSection
        pass

    def createExistingInstance(self, user, password, ip=''):
        existingInstance = dict()
        existingInstance['ip'] = ip
        existingInstance['user'] = user
        existingInstance['password'] = password
        return existingInstance
        pass

    def createNode(self, name, dnsRoles=None, internal=True, public=False, openPorts=None, minDisk='10', minRam='1024',
                   minCores='2', osVersion='6', imageName=None, internalId=None, existingInstance=None):
        """
        sample:
        {
            "id":"",
            "name":"mongo1",
            "arch":"x86-64",
            "osType":"redHat",
            "osVersion":"6.0",
            "region":"US-EAST",
            "minDisk":10,
            "minRam":256,
            "minCores":4,
            "network":[
                {
                    "nicType":"internal",
                    "nicAlias":"this value will be set in hosts files"
                }
            ]
        }
        """
        node = dict()
        node['id'] = ''
        node['name'] = name
        node['arch'] = 'x86-64'
        node['osType'] = 'RedHat'
        node['osVersion'] = osVersion
        node['region'] = 'US-EAST'
        if imageName:
            node['image'] = imageName
        node['minDisk'] = minDisk
        node['minRam'] = minRam
        node['minCores'] = minCores
        node['postConfiguration'] = True
        node['folder'] = self.systemId.upper()
        networks = []
        if internal:
            network = dict()
            network['nicType'] = 'internal'
            network['nicAlias'] = name
            if internalId:
                network['networkId'] = internalId
            networks.append(network)

        if public:
            network = dict()
            network['nicType'] = 'public'
            network['nicAlias'] = name + "_public"
            if dnsRoles:
                network['dnsServices'] = dnsRoles
            if openPorts:
                network['openPorts'] = openPorts
            networks.append(network)

        node['network'] = networks
        node['existingInstance'] = existingInstance
        return node

    def createConfigurationServer(self, processName, baseConfigProperties = list(), additionalValues = dict()):
        '''
        Sample:
        {
            "processName":"clp",
            "baseConfigProperties":[
                "config.properties"
            ],
            "additionalValues":[
                {
                    "key":"logview.mongodb.host.1",
                    "value":"clp1"
                },
                {
                    "key":"logview.mongodb.host.2",
                    "value":"clp2"
                }
            ]
        }
        '''
        ccp = dict()

        ccp['processName'] = processName
        ccp['baseConfigProperties'] = baseConfigProperties
        ccp['additionalValues'] = additionalValues

        return ccp

    def createModule(self, name, version, nodes, configurationServer = None, file = None):
        '''
        Sample:
        {
            "name": "nds_umsui",
            "version": "3.46.0-1",
            "nodes": ["ndsconsoles"],
            "file": {
                "baseConfigProperties":[
                    "config.properties"
                ],
                "additionalValues":[
                       {
                          "key" : "upm::host",
                          "value" : "upm1"
                       }
                       ,{
                            "key" : "ndsconsole::version",
                            "value" : "3.45.1-SNAPSHOT"
                        }
                ]
            }
        }
        '''
        module = dict()

        module['name'] = name
        module['version'] = version
        module['nodes'] = nodes
        if configurationServer:
            module['configurationServer'] = configurationServer
        if file:
            module['file'] = file

        return module


    def createVmName(self, suffix, includeInstanceName=True):
        '''
        if includeInstanceName = True
            Return full vm name: <tenantId>-<instanceName>-<suffix>
        if includeInstanceName = False
            Return full vm name: <tenantId>-<suffix>
        '''
        if includeInstanceName:
            return self.tenantId + '-' + self.instanceName + '-' + suffix
        else:
            return self.tenantId + '-' + suffix

    def createAccessPoint(self, name, url):
        '''
        Sample:
        {
            "name": "mongo",
            "url": "http://<mongo1>:28000/"
        }
        '''
        accessPoint = dict()

        accessPoint['name'] = name
        accessPoint['url'] = url

        return accessPoint