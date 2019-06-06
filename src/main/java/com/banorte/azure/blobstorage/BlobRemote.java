package com.banorte.azure.blobstorage;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobContainerPublicAccessType;
import com.microsoft.azure.storage.blob.BlobRequestOptions;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

public class BlobRemote {

	private String storageConnectionString;
	private CloudStorageAccount storageAccount;
	private CloudBlobClient blobClient;
	private CloudBlobContainer container;
	
	public static class Builder{
		
		private String accountName;
		private String accountKey;
		private String containerReference;
		private String cloudBlockBlob;
		
		public static Builder newInstance() 
        { 
            return new Builder(); 
        }
		
		private Builder() {}
		
		public Builder setAccountName(String accountName) {
			this.accountName = accountName;
			return this;
		}
		
		public Builder setAccountKey(String accountKey) {
			this.accountKey = accountKey;
			return this;
		}
		
		public Builder setContainerReference(String containerReference) {
			this.containerReference = containerReference;
			return this;
		}
		
		public Builder setRemoteFile(String remoteFile) {
			this.cloudBlockBlob = remoteFile;
			return this;
		}
		
		
		public CloudBlockBlob build() throws InvalidKeyException, URISyntaxException, StorageException {
			BlobRemote conn = new BlobRemote();
			conn.storageConnectionString = 
					        "DefaultEndpointsProtocol=https;" +
							"AccountName=" + this.accountName +
							";AccountKey=" + this.accountKey;
			conn.storageAccount = CloudStorageAccount.parse(conn.storageConnectionString);
			conn.blobClient = conn.storageAccount.createCloudBlobClient();
			conn.container = conn.blobClient.getContainerReference(this.containerReference);
			conn.container.createIfNotExists(BlobContainerPublicAccessType.CONTAINER, new BlobRequestOptions(), new OperationContext());		    
			return conn.container.getBlockBlobReference(this.cloudBlockBlob);
		}
			
	}
}
