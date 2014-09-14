try:
    import json
except ImportError:
    import simplejson as json

from Base import materializerBase

class materializer(materializerBase):
    """

    """

    def buildCapture(self):
        """
        Builds the deployment capture base on input.
        """
        stepsCount = 2
        nodeCount = 2
        nodeNamePrefix = 'gluster'


        capture = self.createCaptureTemplate()
        nodes = capture['installNodes']['nodes']

        for i in range(0,nodeCount):
            node = self.createNode(self.createVmName(nodeNamePrefix + str(i), False))
            nodes.append(node)



        steps = capture['installModules']
        for i in range(0, stepsCount):
            step = dict()

            modules = list()
            if i == 0:
                gluster = self.createModule('gluster',
                                                '3.4.2.1',
                                                self.createNodelist(nodeNamePrefix, nodeCount, False)
                )

                modules.append(gluster)

            elif i == 1:
                gluster_configure = self.createModule('gluster_configure',
                                            '3.4.2.1',
                                            self.createNodelist(nodeNamePrefix, nodeCount, False),
                                            file=self.createFile([],
                                                                   self.createAdditionalValues(['glusterfs::peers'],
                                                                                               [';;'.join(self.createNodelist(nodeNamePrefix, nodeCount, False))]))
                )

                modules.append(gluster_configure)
            
            step['modules'] = modules

            steps['step' + str(i)] = step

            exposeAccessPoints = capture['exposeAccessPoints']

            accessPoints = []
            for i in range(0,nodeCount):
                nodeName = self.createVmName(nodeNamePrefix + str(i), False)
                accessPoints.append(self.createAccessPoint('node-' + str(i), 'https://<' + nodeName + '>:5015/ndsconsole/app.html'))


            exposeAccessPoints['accessPoints'] = accessPoints

        print(json.dumps(capture,indent=2))



def main():
    """

    """

    m = materializer()
    m.parseInput()
    m.buildCapture()

if __name__ == '__main__':
    main()


#print(result)