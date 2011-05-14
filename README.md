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