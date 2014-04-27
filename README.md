JustProxyServlet
================

A proxy servlet for testing

The servlet redirects all requests to a location specified as a part of the request following after
the servlet URL, for example
<br>
http://lt-dmitriy:8000/app/gateway/args/<strong>http://localhost/metricstream/systemi/Userlogin?locale=en_US&logoff=1&sessionInvalidated=no</strong>
<br>
if the proxy servlet is configured for URL: http://lt-dmitriy:8000/app/gateway/args
<br>
then it will redirect the request to: http://localhost/metricstream/systemi/Userlogin?locale=en_US&logoff=1&sessionInvalidated=no
<br>
You can consider a diffrent algorithm of redirection just modifying the servlet source.

