<img src=https://github.com/foundation-runtime/orchestration/blob/master/images/ScopeLogo_noSlogan.png>
============
<H1>What is Scope?</H1>
When deploying applications on the cloud, creating machines, installing, configuring and running applications becomes a daily task. Scope comes to the aid, by providing the ability to provition a system, install a range of applications on it and make them run together.

All that, in a single click, without having to go through any cloud-dashboard, without copying any files and without medling with configurations. 

<h2>Scope product</h2>
A SCOPE deployment consists of a "product". It defines the structure of the final deployment, the number of VMs, which application will we be installed on which VM and many other details.

Once a SCOPE product is defined, it can be deployed again and again on each of the virtualization enviroments that SCOPE supports, with no required changes to the product. The product will be installed on the virtualization environment that Scope is configured to work with.

<a href=https://github.com/foundation-runtime/orchestration/tree/master/src/main/rpm/products/Gluster-3.4.2.1>GlusterFS Product</a> is an example of a product that installs <a href="http://www.gluster.org/">GlasterFS</a>, a distributed filesystem, on two VMs.
<h2>Supported Enviroments</h2>
Scope uses <a href=https://jclouds.apache.org/ >jclouds</a> as its orchestration abstraction layer and has been tested with the following providers:
<a href="http://aws.amazon.com/">AWS</a>, <a href="http://www.vmware.com/products/vsphere">VMware vSphere</a>, <a href="www.rackspace.com/">Rackspace</a> and <a href="https://www.openstack.org/">OpenStack</a>.



<h2>Structure</h2>
<img src=https://github.com/foundation-runtime/orchestration/blob/master/images/scope_product.png>

<h2>Materializer</h2>
materializer directory will contain 'run.sh' or 'run.bat' script that will actually run the materializer. <br>
the materializer output would be the deployment capture of the current product instance.

<h3>Materializer input</h3>
The materializer input is:
<ol>
<li><a href=https://github.com/foundation-runtime/orchestration/blob/master/jsonSchema/instance.json>Instance</a> as json string</li>
<li><a href=https://github.com/foundation-runtime/orchestration/blob/master/jsonSchema/product.json>Product</a> as json string</li>
</ol>
separated with ~;~

Product Json sample:

```json
{
    "_id" : "Gluster-3.4.2.1",
    "productName" : "Gluster",
    "productOptions" : [{
        "key" : "nodecount",
        "label" : "Number of gluster nodes to build",
        "optionType" : "string",
        "defaultValue" : "2",
        "enumeration" : ["2"]
    }],
    "productVersion" : "3.4.2.1",
    "repoUrl" : "http://<products-server>/scope-products/Gluster-3.4.2.1/"
}
```

<h3>Materializer output</h3>
The materializer output has this schema:

TBD

<h3>run.bat/sh</h3>
The materializer directory should contain "run.sh" or "run.bat" depends on the operating system scope server run on.
This script should actually run the materializer application. so you can write it in any language you want.

<h2>prodpuppet</h2>
This directory should contains the puppet for each component the product should install.
<br/>
<img src=https://github.com/foundation-runtime/orchestration/blob/master/images/scope_prodpuppet.png>

<h2>yum</h2>
This directory should contain all the RPMs the product needs, includes all dependencies.
