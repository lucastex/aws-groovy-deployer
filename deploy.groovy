@GrabResolver(name='central', root='http://repo1.maven.org/maven2')
@GrabResolver(name='javanet', root='http://download.java.net/maven/2')
@Grab(group='com.amazonaws', module='aws-java-sdk', version='1.1.9')
@Grab(group='commons-logging', module='commons-logging', version='1.1.1')
@Grab(group='commons-httpclient', module='commons-httpclient', version='3.1')
@Grab(group='javax.mail', module='mail', version='1.4.3')
@Grab(group='org.codehaus.jackson', module='jackson-core-asl', version='1.4.3')
@Grab(group='stax', module='stax', version='1.2.0')
@Grab(group='stax', module='stax-api', version='1.0.1')

import org.apache.commons.logging.LogFactory

//turnin log off for aws sdk
def logAttribute = "org.apache.commons.logging.Log"
def logValue = "org.apache.commons.logging.impl.NoOpLog"
LogFactory.getFactory().setAttribute(logAttribute, logValue)

def cliArguments         = new CLIArguments(args)
def cliOptions           = cliArguments.parseArguments()
ConsoleUtil.debugEnabled = cliOptions.d

ConsoleUtil.debug "Initializing script"

def credentials = cliArguments.credentials()

ConsoleUtil.info "uploading war file to S3"

def s3Helper = new S3Helper(credentials)
def signedWarURL = s3Helper.upload(cliArguments.war())

ConsoleUtil.info "Signer URL for file is [${signedWarURL}]"

ConsoleUtil.info "Connecting to EC2 to launch instances"
def ec2Helper = new EC2Helper(credentials)
def instanceIds = ec2Helper.runInstances(cliArguments.ec2Data(), signedWarURL)

ConsoleUtil.info "Tagging instances"
ec2Helper.tagInstances(instanceIds)

ConsoleUtil.info "Connecting to ELB console"
def elbHelper = new ELBHelper(credentials)
def (elb, oldInstancesInElb) = elbHelper.listAvailableElbsAndChooseOne()

if (elb) {

	ConsoleUtil.info "Attaching instances to ELB [${elb.name}]"
	elbHelper.attachInstances(elb.name, instanceIds)
	
	ConsoleUtil.info "Would you like to remove old instances from ELB and terminate them?"
	print 'choose [y/n]: '
	def removeOldInstances = "y"
	removeOldInstances = System.console().readLine()
	
	if (removeOldInstances == "y") {
		ConsoleUtil.info "Removing old instances from ELB"
		elbHelper.removeInstancesFromElb(elb.name, oldInstancesInElb)
		ec2Helper.terminateInstances(oldInstancesInElb.collect { it.instanceId })
	}
}

s3Helper.deleteWarAndBucket()
s3Helper.shutdown()
System.exit 0

def instanceDetails = ec2Helper.getInstanceDetails(instanceIds)
ConsoleUtil.info "Done! Look at your new environment: "
if (elb) {
	ConsoleUtil.info "\tELB: ${elb.name} - ${elb.dns}"
}

instanceDetails.eachWithIndex { instance, idx ->	
	ConsoleUtil.info "\tInstance ${idx+1} - [${instance.instanceId}] - Public DNS: ${instance.publicDnsName}"
}