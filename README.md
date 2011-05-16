aws-groovy-deployer
-------------------

Script to publish new versions of an war application. it is summarized by the following steps:

	1) Upload your war to s3 in a temporary bucket
	2) Start X instances
	3) Install tomcat on them and deploy your war file
	4) Attach the instances in your ELB in place of your old ones with the older version
	5) (if you want) remove old instances (handling the older version of your app) and terminate them
	6) Remove your war file from s3

Usage:

	groovy deploy.groovy -h to see the script usage
	
Dependencies: 

The script depends on several third party libraries that will be downloaded using grape. 
So, the first time you run the script, you'll download all of them, causing some time to wait, run the script with the *groovy.grape.report.downloads* property set to *true* to see how the download is going.

	groovy -Dgroovy.grape.report.downloads=true deploy.groovy -h
	
The download feedback will be shown in your console

	Resolving dependency: org.codehaus.groovy.modules.http-builder#http-builder;0.5.1 {default=[default]}
	Resolving dependency: stax#stax-api;1.0.1 {default=[default]}
	Resolving dependency: stax#stax;1.2.0 {default=[default]}
	Resolving dependency: org.codehaus.jackson#jackson-core-asl;1.4.3 {default=[default]}
	Resolving dependency: javax.mail#mail;1.4.3 {default=[default]}
	Resolving dependency: commons-httpclient#commons-httpclient;3.1 {default=[default]}
	Resolving dependency: commons-logging#commons-logging;1.1.1 {default=[default]}
	Resolving dependency: com.amazonaws#aws-java-sdk;1.1.9 {default=[default]}
