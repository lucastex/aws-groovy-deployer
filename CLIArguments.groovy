import org.apache.commons.cli.Option
import com.amazonaws.services.ec2.model.Tag
import com.amazonaws.auth.PropertiesCredentials

class CLIArguments {
	
	def arguments
	def cliBuilder
	def cliOptions
	
	//parse first arguments (version and help)
	//and stores the final clibuilder to use later
	public CLIArguments(args) {
		
		this.arguments = args
				
		cliBuilder = new CliBuilder(usage: 'groovy deploy.groovy -c <arg> -w <arg> [-hvdnikztgl] <args>')
		cliBuilder.with {
			h longOpt: 'help',            args: 0, required: false, 'usage information'
			v longOpt: 'version',         args: 0, required: false, 'show script version'
			d longOpt: 'debug',           args: 0, required: false, 'run the process with verbose mode on'
			c longOpt: 'credentials',     args: 1, required: false, 'file storing your AWS credentials'
			w longOpt: 'war',             args: 1, required: true,  'war file location'
			n longOpt: 'instances',       args: 1, required: false, 'Intance quantity, default is 1'
			i longOpt: 'ami',             args: 1, required: false, 'AMI to use, default is ami-8c1fece5'
			k longOpt: 'keypair',         args: 1, required: false, 'Keypar to use in launched instances, default is none'
			z longOpt: 'zone',            args: 1, required: false, 'Availability zone to launch instances, default is us-east-1a'
			t longOpt: 'instance-type',   args: 1, required: false, 'Instance type to launch, default is t1.micro'
			g longOpt: 'security-groups', args: Option.UNLIMITED_VALUES, required: true, valueSeparator: ',', 'Security Groups for the instances (comma separated if more than one)'
			l longOpt: 'tag',             args: Option.UNLIMITED_VALUES, 'Tags to add in the instances (correct format is tag_name=tag_value)'
		}
	
		def helpAndVersionCliBuilder = new CliBuilder()
		helpAndVersionCliBuilder.with {
			h longOpt: 'help',        'usage information'
			v longOpt: 'version',     'show script version'
		}		
				
		def helpAndVersionOptions = helpAndVersionCliBuilder.parse(arguments)
		if (helpAndVersionOptions.v) {
			println "/---------------------------------\\"
			println "| [AWS Java Deployer] version 0.1 |"
			println "|   Lucas Teixeira - @lucastex    |"
			println "\\---------------------------------/"
			System.exit 0
		}
		
		if (helpAndVersionOptions.h) {
			println cliBuilder.usage()
			System.exit 0
		}
	}
	
	//parse command line arguments
	def parseArguments() {
		
		def options = cliBuilder.parse(arguments)
		if (!options) {
			System.exit 0
		}
		this.cliOptions = options
		return options
	}
	
	//parse aws credentials
	def credentials() {

		def credentialsFile = new File(cliOptions.c)
		if (!credentialsFile?.exists() || !credentialsFile.isFile()) {
			ConsoleUtil.error "invalid credentials file [${cliOptions.c}]"
			ConsoleUtil.error "exiting now"
			System.exit 1
		} else {
			ConsoleUtil.debug "credentials loaded from file [${cliOptions.c}]"
			return new PropertiesCredentials(credentialsFile)
		}
	}
	
	def war() {
		
		def war = new File(cliOptions.w)
		if (!war?.exists() || !war.isFile()) {
			ConsoleUtil.error "invalid war file [${cliOptions.w}]"
			ConsoleUtil.error "exiting now"
			System.exit 1
		} else {
			ConsoleUtil.debug "war file set to [${cliOptions.w}]"
			return war
		}
	}
	
	def ec2Data() {
		
		def data = [:]
		data.imageId = cliOptions.i ?: "ami-8c1fece5"
		data.keyPairName = cliOptions.k ?: null
		data.instances = cliOptions.n ?: "1"
		data.zone = cliOptions.z ?: "us-east-1a"
		data.type = cliOptions.t ?: "t1.micro" 
		data.groups = cliOptions.gs
		def _tags = cliOptions.ls ?: []
		data.parsedTags = []
		_tags.each { 
			def splitted = it.split("=")
			data.parsedTags << new Tag(splitted[0], splitted[1])
		}
		
		return data
	}
}