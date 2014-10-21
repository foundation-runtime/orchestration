Orchestration (Simple Cloud Orchestration/Provisioning/etc)
=============
The current unit of deployment for most solutions is an individual component.</br> 
This whitepaper outlines a solution to enable teams to produce fully integrated and assembled products,</br>  
without compromising individual component’s ability to manage their own roadmap and release cycle.</br> 

Background
============
‘Automatic product provisioning’ has been identified as being of pivotal importance in improving the way we develop and deliver software. In this background section, 
I’ll explore what ‘automatic product provisioning’ is in context of our organization, and why it is so crucially important. 

From Components to Product
============
There has been a growing sense of discomfort over the increasing number of components in product suite. Setting up a system, be it for customer install, end-to-end integration lab or even for an internal pre-integration activity is complicated, error prone and time consuming. 
So complicated and time consuming that people have to think very carefully before they attempt to install full system for whatever purpose. 
Component delineation is driven by architectural considerations: small, nimble components, or micro services, can be assembled in different fashions to support different customer requirements, 
on a single “trunk” of code. This is very good. 
The operational difficulties arise from the fact that because a micro component is the basic Lego block of the Product universe, 
it exposes configuration variance at its own resolution level. 
That means that each component has its own extensive configuration file(s), determining both its functional feature set and its own deployment model. 
It is impossible to express configuration choices at a higher level of abstraction, because this higher level of abstraction is not tangible in anyway.
What is the higher level of abstraction at which we’d like to be able to express variance? The answer is of course the ‘Product’. 
In Product is a segment of the overall offer that can be deployed as a single unit, that has clear and consistent APIs, and that can be monetized independently.

Where is the product?
============
A component is an RPM, possibly wrapped as a Puppet module. 
What is a Product? What is the tangible deliverable artifact that constitutes Product? Is it simply a DVD to which the RPMs have been copied? 
The answer is an equivocal “no”. A box full of car parts - engine, gears, wheels – is not a car. 
If you’re a car technician you may be able to assemble the car from it; but it’s not a car. 
And neither is a bag of RPMs a Product. If you’re an expert engineer you could assemble a Product from it, but it’s not a Product.
Product must be a tangible artifact that defines internally what happens when the bag of RPMs comes together with the underlying platform. 
Which RPMs to install, where, in which order, and with what initial configuration. The answers to these questions are a byproduct of the 
Product installer’s choices of high level variance, 
not the actual choices themselves. The high level choices need to be translated into answers for these low level decisions. 
The Product machinery needs to know how to perform this translation.

Product level variance
============
To be able to express variance at the product level, is to be able to define the Product feature setting and deployment model in one place. 
That expression of variance needs to propagate down to specific component level configuration and deployment settings. 
That propagation must be automatic and built into the ‘Product Artifact’.
There are two dimensions of Product Level variance – feature variance and deployment model variance. 
Feature variance is a simple concept – it refers to the functions supported by the Product and is expressed internally as component level configuration choices. 
Deployment model variance refers to the different deployment options available when installing the Product. An example would be helpful:
Let’s say our Product requires a MongoDB installation . 
The Product could expose deployment model configuration that dictates the number of shards required for MongoDB and the number of replica sets for each shard. 
Let’s say it does, and let’s say we choose 2 for the number of shards and 3 for the size of each replica set; 
what we are requiring in essence is for the existence of 6 mongo virtual machines instances, each installed with two process types – mongod and mongos.

Cloud
============
Where are these virtual machines going to come from? Will an operator, after choosing ‘2’ and ‘3’ as the MongoDB configuration, 
have to create virtual instances? Will he have to assign IPs? Will he have to configure VLANs?  
For Product Configuration to make sense, the answer must be a resounding NO.
The deployment model of a product is a byproduct of the high level configuration choices made by the operator of that product, not the configuration choice itself. 
What the concept of cloud adds to plain virtualization infrastructure is the notion of API. When the full extent of a virtualization infrastructure is governable through API, 
that virtualize infrastructure can be called ‘cloud’. 
With cloud it’s possible for software to create guest instances, provision storage and set up network. 
Such an API will allow us to define the MongoDB product at a high level of ‘2’ shards/’3’ replica-sets, 
which software will then translate into calls to the cloud API to create virtual machine instances. 
Without cloud, we could not effectively support Product level variance.

Building Products for Cloud
============
For our organization to be able to build Products that capitalize on cloud technology and paradigm, 
we need to define technology and supporting process by which components being developed by different teams can be assembled into Products. 
The following diagram provides a very high level capture of the objects and processes that are integral to Product Building. 
It contains references to new architectural blocks that will be explained in the sections to follow it.

<img src=https://github.com/foundation-runtime/orchestration/blob/master/images/Product_Deployment_Flowchart.png>

Architectural Building Blocks
============
<h2>Component Package (1)</h2>
As depicted in the illustration, a component package contains three parts:
<h2>Core Component (1a)</h2>
Core components represent the units of RPM software. A component maintains its own version tree, and may be part of more than one Products. 
<h2>CCPConfig.xml (1b)</h2>
Each Core component can contain a file that describing all the components configuration. 
This is true for Cisco components as well as for third party components, for which we’ll can use the configuration server artifacts. 
<h2>Puppet Modules (1c)</h2>
Each Core component will have a matching Puppet Module artifact, versioned in unison with it.  
<h2>Product (2)</h2> 
A Product is the sum total of the specific versions of each underlying component deliverable and Puppet Scripts. 
It includes some core secret ingredients that make it viable, here’s the breakdown of the main pieces:
<h2>Product Archive (2a)</h2>
Contains physical copies of the underlying RPMs, configuration file documents and Puppet Modules.
<h2>Product Variance Definition (2b)</h2>
This artifact contains a declarative description of all the exposed product level variance. 
<h2>Product Transformation Logic (2c)</h2>
The ‘Transformation Logic’ concept is of singular importance, and represents one of the key concepts of the Platform’s provisioning paradigm: 
The way in which high level configuration is translated into a deployment model and component configuration set, 
must be defined by the product itself. This ‘Transformation Logic’ is expressed as a software code, 
which receives as input the set of Product level configuration parameter choices, 
and produces as output a ‘deployment model capture’ document that describes the system in low level detail.
<h3>Deployment Model Capture</h3>

The ‘Transformation Logic’ block knows how to build a ‘deployment model capture’ based on high level parameter choices. What exactly is a ‘deployment model capture’?
‘Deployment Model Capture’ will be a document artifact in a predefined format that will describe system installation in terms of the following aspects:
<ol>
<li>Required physical resources (virtual machines, network, etc). For example: guests will be described in terms of their cpu, memory and disk needs.</li>
<li>Module to guest mapping:  which modules to install on which guests</li>
<li>Configuration bootstrap: any additional component configuration required by components in order to operate properly. This additional configuration would be referred to in terms of CCP namespace and configuration-parameter-names. Example of such configuration could be: user-names, service ports, etc. As a rule any CCP required configuration not provided as a default value, would need to be covered in the configuration bootstrap section.</li>
</ol>

<h2>Provisioning Solution (3)</h2>
The role of the Provisioning solution (“SCOPE”) is threefold:
<ol>
<li>Given a Product type, provide a web interface that exposes the product variance.</li>
<li>Translate the values chosen for product variance into a ‘Deployment Model Capture’ by invoking the ‘Product Transformation Logic’.</li>
<li>Provision the deployment using a cloud API to reflect the ‘Deployment Model Capture’.</li>
</ol>

<h2>Cloud Deployment (4)</h2>
A provisioned product will inhabit dedicated virtual machines on supported cloud infrastructure. 
There are two parts to the provisioned product:
<ol>
<li>Product’s local provisioning and operational ecosystem. This includes Puppet and YUM repositories as well as configuration Infrastructure.</li>
<li>The Product’s core components and third party dependencies, deployed across the Product’s dedicated virtual machines.</li>
</ol>
<h2>Puppet Repository (4a)</h2>
The idea is that SCOPE will dynamically create a Puppet repository based on the Puppet modules associated with the components contained within the Product.
<h2>YUM Repository (4b)</h2>
The YUM repository is really an implementation detail. The point is that SCOPE will dynamically create a YUM repository, placing all its component RPMs there.
<h2>Configuration Infrastructure (4c)</h2>
The configuration infrastructure is central to the provisioning activity. The configuration bootstrap data will be placed there by SCOPE.
This SCOPE-to-CCP-integration allows us to use the same configuration solution for the system installation and system operation use cases. 
This is of crucial importance; without it we’d have to manage two separate configuration silos – one for Puppet when installing components; 
the other for the components’ runtime configuration needs, using configuration infrastructure.
