public class ConsoleUtil {
	
	static def debugEnabled = false
		
	static def debug = { message -> 
		if (debugEnabled) 
			println "[debug] [${new Date().format('dd/MM/yyyy HH:mm:ss')}] ${message}"
	}
	
	static def error = { message -> 
		println "[error] [${new Date().format('dd/MM/yyyy HH:mm:ss')}] ${message}"
	}
	
	static def info  = { message -> 
		println "[info ] [${new Date().format('dd/MM/yyyy HH:mm:ss')}] ${message}"
	}
}