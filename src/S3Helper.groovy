import java.text.DecimalFormat
import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSCredentials
import org.codehaus.groovy.runtime.TimeCategory
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.transfer.TransferManagerConfiguration

class S3Helper {
	
	def s3client
	def credentials
	
	def bucketName
	def objectKey
	
	public S3Helper(cred) {
		this.credentials = cred
		s3client = new AmazonS3Client(credentials) 
	}
	
	def upload(war) {
		
		bucketName = "bkt-${UUID.randomUUID()}"
		objectKey  = war.name

		ConsoleUtil.debug "creating a temporary bucket [${bucketName}] to hold application file"

		def fileSize = war.size()
		def bucket = s3client.createBucket(bucketName)

		def metadata = new ObjectMetadata()
		metadata.setContentLength(fileSize) 

		def transferManager = new TransferManager(credentials)
		def tmConfiguration = new TransferManagerConfiguration()
		tmConfiguration.setMinimumUploadPartSize(fileSize) 
		transferManager.setConfiguration(tmConfiguration) 

		war.withInputStream { warInputStream -> 

			def decimalFormat = new DecimalFormat("##.#")
			def putObjectRequest = new PutObjectRequest(bucketName, objectKey, warInputStream, metadata)
			putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead)

			def warUpload = transferManager.upload(putObjectRequest)

			def doneForPercentages = []
			while (!warUpload.done) {

				def percent = (warUpload.progress.bytesTransfered * 100) / warUpload.progress.totalBytesToTransfer
				def percentToShow = decimalFormat.format(percent)
				if (percent > 0) {
					ConsoleUtil.debug "${warUpload.description}: [${warUpload.state}] - ${warUpload.progress.bytesTransfered} of ${warUpload.progress.totalBytesToTransfer} (${percentToShow}%) "
					if (percent.intValue() % 5 == 0) {
						if (!doneForPercentages.contains(percent.intValue())) {
							doneForPercentages << percent.intValue()
							ConsoleUtil.info "${warUpload.description}: [${warUpload.state}] - ${warUpload.progress.bytesTransfered} of ${warUpload.progress.totalBytesToTransfer} (${percentToShow}%) "
						}
					}
				}
				Thread.sleep(500)
			}	
		}

		ConsoleUtil.info "Finished uploading war"
		ConsoleUtil.debug "File stored in [http://${bucketName}.s3.amazonaws.com/${objectKey}]"

		return "http://${bucketName}.s3.amazonaws.com/${objectKey}"
	}
	
	def deleteWarAndBucket() {
		
		ConsoleUtil.debug "Deleting file from S3"
		s3client.deleteObject(bucketName, objectKey)

		ConsoleUtil.debug "Deleting bucket ${bucketName}"
		s3client.deleteBucket(bucketName) 
	}
	
	def shutdown() {
		ConsoleUtil.debug "shutting down S3 connection"
		s3client.shutdown()
	}
}