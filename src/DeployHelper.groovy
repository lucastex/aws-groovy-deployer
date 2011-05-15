import groovyx.net.http.HTTPBuilder

class DeployHelper {
	
	def checkApplication(instances) {
		
		ConsoleUtil.debug "Checking application in ${instances.size()} instances"
		
		def instanceIds = instances.collect { it.instanceId }
		
		while (instanceIds.size() > 0) {
			
			instances.each { instance -> 
			
				if (instanceIds.contains(instance.instanceId)) {
					
					try {
						def urlToCheck = "http://${instance.publicDnsName}:8080"
						ConsoleUtil.debug "Checking application in instance [${instance.instanceId}] - ${urlToCheck}/index.jsp"
			
						def http = new HTTPBuilder(urlToCheck)
						http.get( path : '/index.jsp', contentType : "text/plain") { resp, reader ->
							if (resp.status == 200) {
								ConsoleUtil.debug "\tStatus: [OK]"
								instanceIds.remove(instance.instanceId)
							}
						}
					} catch (Exception e) {
						//ConsoleUtil.debug "\tStatus: [NOT OK]"
					}		
				}
			}
		}
	}

}