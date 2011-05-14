import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.Placement
import com.amazonaws.services.ec2.model.Reservation
import com.amazonaws.services.ec2.model.InstanceState
import com.amazonaws.services.ec2.model.CreateTagsRequest
import com.amazonaws.services.ec2.model.RunInstancesResult
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.DescribeInstancesResult
import com.amazonaws.services.ec2.model.DescribeInstancesRequest
import com.amazonaws.services.ec2.model.TerminateInstancesRequest

class EC2Helper {
	
	def tags
	def ec2client
	def credentials
	
	public EC2Helper(cred) {
		this.credentials = cred
		ec2client = new AmazonEC2Client(credentials)
	}
	
	public runInstances(data, warUrl) {
		
		//store instance tags
		this.tags = data.parsedTags
		
		def runInstancesRequest = new RunInstancesRequest()
		runInstancesRequest.with {
			keyName               = data.keyPairName
			imageId               = data.imageId
			maxCount              = Integer.parseInt(data.instances)
			minCount              = Integer.parseInt(data.instances)
			userData              = getUserDataScript(warUrl)
			placement             = new Placement(data.zone)
			instanceType          = data.type
			securityGroups        = data.groups
			disableApiTermination = false
		}
		
		ConsoleUtil.debug "Starting ${data.instances} instance(s): "
		ConsoleUtil.debug "\tAMI................${data.imageId}"
		ConsoleUtil.debug "\tTYPE...............${data.type}"
		ConsoleUtil.debug "\tKEYPAR NAME........${data.keyPairName}"
		ConsoleUtil.debug "\tSECURITY GROUPS....${data.groups.join(',')}"
		ConsoleUtil.debug "\tAVAILABILITY ZONE..${data.zone}"

		//store instance ids
		def instanceIds = []
		def remainingInstanceIdsToLaunch = []
		def runInstancesResult = ec2client.runInstances(runInstancesRequest) 
		runInstancesResult.reservation.instances.each { instance ->
			instanceIds << instance.instanceId
			remainingInstanceIdsToLaunch << instance.instanceId
		}

		//wait instances to start running
		ConsoleUtil.debug "waiting for all instances to launch"
		while (true) {

			def describeInstancesRequest = new DescribeInstancesRequest()
			describeInstancesRequest.setInstanceIds(remainingInstanceIdsToLaunch) 

			def allStarted = true
			def describeInstancesResult = ec2client.describeInstances(describeInstancesRequest)
			describeInstancesResult.reservations.each { reservation ->
				reservation.instances.each { instance ->

					if (instance.state.code == 16) {
						remainingInstanceIdsToLaunch.remove(instance.instanceId)
						ConsoleUtil.info "[${instanceIds.size()-remainingInstanceIdsToLaunch.size()}/${instanceIds.size()}] Instance ${instance.instanceId} is running. Public DNS: ${instance.publicDnsName}"
					} else {
						allStarted = false
					}
				}
			}

			if (allStarted) break
			Thread.sleep(2000)
		}
		
		return instanceIds	
	}
	
	def tagInstances(instanceIds) {
		
		if (this.tags.size() > 0) {
			ConsoleUtil.debug "Tagging ${instanceIds.size()} instance(s) with tags: [${this.tags.collect { it.key.concat('=').concat(it.value)}.join(',')}]..."
			ec2client.createTags(new CreateTagsRequest(instanceIds, this.tags))
		}
	}

	def terminateInstances(instanceIds) {
		
		ConsoleUtil.debug "Terminating instances ${instanceIds}"
		def terminateInstancesRequest = new TerminateInstancesRequest()
		terminateInstancesRequest.setInstanceIds(instanceIds)
		ec2client.terminateInstances(terminateInstancesRequest)
	}
	
	def getInstanceDetails(instanceIds) {
		
		def instanceDetails = []
		
		def describeInstancesRequest = new DescribeInstancesRequest()
		describeInstancesRequest.setInstanceIds(instanceIds) 

		def describeInstancesResult = ec2client.describeInstances(describeInstancesRequest)
		describeInstancesResult.reservations.each { reservation ->
			reservation.instances.each { instance ->
				instanceDetails << instance
			}
		}
		
		return instanceDetails
	}

	def getUserDataScript = { warUrl ->
		
	    def lines = []
	    lines << "#! /bin/bash"
	
		lines << "mkdir /root/startscript"
		lines << "echo 'Iniciando script' >> /root/startscript/init.log"
		lines << ""
		lines << "#atualiza yum"
		lines << "yum -y update > /root/startscript/yum-update.log"
		lines << "echo 'Atualizou YUM' >> /root/startscript/init.log"
		lines << ""
		lines << "#instala tomcat"
		lines << "yum -y install tomcat6  > /root/startscript/tomcat-install.log"
		lines << "echo 'Instalou tomcat' >> /root/startscript/init.log"
		lines << ""	
		lines << "#baixa arquivo da aplicacao"
		lines << "cd /usr/share/tomcat6/webapps/"
		lines << "wget ${warUrl} -O ROOT.war"
		lines << "echo 'Fez download da app' >> /root/startscript/init.log"
		lines << ""
		lines << "#seta vars pro tomcat"
		lines << "chmod 666 /etc/tomcat6/tomcat6.conf"
		lines << "echo 'JAVA_OPTS=\"-server -Xms384m -Xmx384m -XX:PermSize=96m -XX:MaxPermSize=96m -Djava.awt.headless=true -XX:NewRatio=3 -XX:SurvivorRatio=6 -XX:+UseParallelGC -XX:+CMSClassUnloadingEnabled\"' >> /etc/tomcat6/tomcat6.conf"
		lines << "echo 'Setou vars para o tomcat' >> /root/startscript/init.log"
		lines << ""	
		lines << "#inicia tomcat"
		lines << "/etc/init.d/tomcat6 start > /root/startscript/tomcat-start.log"
		lines << "echo 'Startou tomcat' >> /root/startscript/init.log"
		lines << ""
		lines << "echo 'Finalizou script' >> /root/startscript/init.log"
	
		def scriptData = lines.join('\n').bytes.encodeBase64().toString()
	    ConsoleUtil.debug "Script data to instances: ${scriptData}"
	
		ConsoleUtil.debug "Start of script: ----------------------------"
		lines.each { line -> 
			ConsoleUtil.debug "\t\t${line}"
		}
		ConsoleUtil.debug "End of script: ------------------------------"
		
		return scriptData
	}
}