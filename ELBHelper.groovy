import com.amazonaws.services.elasticloadbalancing.model.Instance as ELBInstance
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerResult
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest

class ELBHelper {
	
	def elbClient
	def credentials
	
	public ELBHelper(cred) {
		this.credentials = cred
		elbClient = new AmazonElasticLoadBalancingClient(credentials) 
	}
	
	def listAvailableElbsAndChooseOne() {
		
		def availableElbs = []
		def describeLoadBalancersResult = elbClient.describeLoadBalancers()
		describeLoadBalancersResult.loadBalancerDescriptions.each { elbInstance ->
			availableElbs << [name: elbInstance.loadBalancerName, dns: elbInstance.getDNSName(), az: elbInstance.availabilityZones.join(',')]
		}

		if (availableElbs.size() == 0) {
			ConsoleUtil.error "There are no ELBs in your environment, please create one first"
			return
		}
		
		ConsoleUtil.info "Select witch ELB to attach instances"
		availableElbs.eachWithIndex { aElb, index ->
			ConsoleUtil.info "[${index}] ${aElb.name} (${aElb.az}) - ${aElb.dns}"
		}

		print 'choose: '
		def elbOption = 0
		elbOption = System.console().readLine()
		
		def choosenElb = availableElbs[Integer.parseInt(elbOption)]
		
		ConsoleUtil.info "Reading ELB state"
		def oldInstancesInElb = []
		def describeLoadBalancersRequest = new DescribeLoadBalancersRequest()
		describeLoadBalancersRequest.withLoadBalancerNames(choosenElb.name)
		def describeLoadBalancersResultForOldInstances = elbClient.describeLoadBalancers(describeLoadBalancersRequest)
		describeLoadBalancersResultForOldInstances.loadBalancerDescriptions.each { elbDescription ->
			elbDescription.instances.each { instance ->
				oldInstancesInElb << instance
			}
		}
		
		return [choosenElb, oldInstancesInElb]
	}
	
	void attachInstances(elbName, instanceIds) {
		ConsoleUtil.debug "Attaching instances ${instanceIds} on ELB [${elbName}]"
		elbClient.registerInstancesWithLoadBalancer(new RegisterInstancesWithLoadBalancerRequest(elbName, instanceIds.collect { new ELBInstance(it) }))
	}
	
	void removeInstancesFromElb(elbName, oldInstancesInElb) {
		
		ConsoleUtil.debug "Removing instances ${oldInstancesInElb.collect { it.instanceId }} from ELB [${elbName}]"
		def deregisterInstancesFromLoadBalancerRequest = new DeregisterInstancesFromLoadBalancerRequest()
		deregisterInstancesFromLoadBalancerRequest.setLoadBalancerName(elbName)
		deregisterInstancesFromLoadBalancerRequest.setInstances(oldInstancesInElb)
		elbClient.deregisterInstancesFromLoadBalancer(deregisterInstancesFromLoadBalancerRequest)
	}
}