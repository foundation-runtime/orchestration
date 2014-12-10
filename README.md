Product
============
<h2>Structure</h2>
<img src=https://github.com/foundation-runtime/orchestration/blob/master/images/scope_product.png>

<h2>Materializer</h2>
materializer directory will contain 'run.sh' or 'run.bat' script that will actually run the materializer. <br>
the materializer output would be the deployment capture of the current product instance.

The materializer input is:
<ol>
<li>Instance as json string</li>
<li>Product as json string</li>
</ol>
separated with ~;~


