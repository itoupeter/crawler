# crawler
Developed for School of International Education, South China University of Technology

This is a module of a news colletor system project. The other modules are GUI and a Apache Solr based indexing module. Modules work on different server machine and transfer data using HTTP request containing json string content.

A simple crawler used to collect pages from user specified news websites.<br>
-Used Apache's HttpClient to download and parse html files<br>
-Used Boilerpipe to extract news title and body from pages<br>
-Used Bloomfilter hashtable to avoid duplicate page caching<br>
-Used JsonObject to parse and construct json string to transfer content<br>
-Developed as Java servlets and deployed on Tomcat server<br>
