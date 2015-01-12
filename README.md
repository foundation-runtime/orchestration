SCOPE Product
============
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