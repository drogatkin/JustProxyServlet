JustProxyServlet
================

A proxy servlet for testing

The servlet redirect all requests to location specified as part of request following after
the servlet URL, for example
<br>
http://lt-dmitriy:8000/app/gateway/args/<strong>http://localhost/metricstream/systemi/Userlogin?locale=en_US&logoff=1&sessionInvalidated=no</strong>
<br>
if the proxy servlet configured for URL: http://lt-dmitriy:8000/app/gateway/args
<br>
then it will redirect request to: http://localhost/metricstream/systemi/Userlogin?locale=en_US&logoff=1&sessionInvalidated=no

