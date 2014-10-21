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

<img src=