package blob_storage.upload_fraom_db;

import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.banorte.azure.blobstorage.BlobRemote;
import com.banorte.azure.blobstorage.ResultSetAsInputStream;
import com.banorte.db.DbConnection;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

public class App 
{
	
	private static Properties props;
	private static String propFileLoc;
	private static String restore;
	private static final Logger logger = Logger.getLogger(App.class);
	static final Logger restoreLog = Logger.getLogger("restoreLogger");
	
    public static void main( String[] args )
    {
        if (args.length == 0) {
        	 System.err.println("Argumentr. Please check properties file location in startup script.");
        	 System.exit(1);;
        } else if (args.length > 1) {
        	propFileLoc = args[0];
       	 	restore = args[1];
        } else {
        	 propFileLoc = args[0];
        }
        System.out.println(propFileLoc);
        loadProperties(propFileLoc);
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
        LocalDateTime inicio = LocalDateTime.parse(restore != null ? restore : props.getProperty("application.start_date"), formatter);
        LocalDateTime fin = LocalDateTime.now();
       
        while(true) {
	         Connection conn = null;
	         try {
				if(conn == null || conn.isClosed()) {
					logger.debug("DB: Establishing connection...");
					conn = DbConnection.Builder.newInstance()
			 				.setDbms(props.getProperty("db.dbms"))
			 				.setServerName(props.getProperty("db.server_name"))
			 				.setPortNumber(props.getProperty("db.port"))
			 				.setDbname(props.getProperty("db.db_name"))
			 				.whitDriver(props.getProperty("db.driver"))
			 				.build();
					logger.debug("DB: Conected to " + conn.getSchema()); 
				 }
			 } catch (SQLException e1) {
				 logger.error("Error conectando con la BD.",e1);
				 System.exit(1);
			 }
	         
	          
	         CloudBlockBlob blob = null;
			 try {
				blob = BlobRemote.Builder.newInstance()
						 				.setAccountKey(props.getProperty("azure.blobstorage.account_key"))
						 				.setAccountName(props.getProperty("azure.blobstorage.account_name"))
						 				.setContainerReference(props.getProperty("azure.blobstorage.container"))
						 				.setRemoteFile(props.getProperty("azure.blobstorage.remote_file"))
						 				.build();
				while(blob != null && blob.exists() && "false".equals(props.getProperty("application.override_remote_file")) ){
					logger.debug("BLOB STORAGE: Waiting remote file...");
					Thread.sleep(Long.parseLong(props.getProperty("application.wait_remote_process")));
				}
			} catch (InvalidKeyException | URISyntaxException | StorageException | NumberFormatException | InterruptedException e1) {
				logger.error("Error obteniendo el blob de azure", e1);
				System.exit(1);
			}
	        
	         
	         
	         
	        ResultSetAsInputStream inpuntStream = null;
			try {
				inpuntStream = ResultSetAsInputStream.Builder.newInstance()
				 						.setConnection(conn)
				 						.setSql("select * from mock_date where 	date_time between ? and ?")
				 						.setParameters(new Timestamp(inicio.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()),
				 								       new Timestamp(fin.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()))
				 						.setConverter(rs -> { 
				 													StringBuilder sb = new StringBuilder();
				 													try {
																		return sb.append(rs.getInt(1))
																		.append("\t")
																		.append(rs.getString(2))
																		.append("\t")
																		.append(rs.getTimestamp(3))
																		.append("\n")
																		.toString()
																		.getBytes();
																	} catch (SQLException e) {
																		logger.error("Error convirtiendo RestultSet a InputStream",e);
																		System.exit(1);
																	}
				 													return null;
				 												
				 											})
				 						.build();
			} catch (SQLException e1) {
				logger.error("Error construllendo ResultSetAsInputStream",e1);
				System.exit(1);
			}
	         
	         try {
	        	if(inpuntStream.available() > 0) {
	        		logger.debug("BLOB STORAGE: Upload to blob storage..." + inpuntStream.available() +" rows availables");
	        		blob.upload(inpuntStream, -1);
	        		logger.debug("BLOB STORAGE: Finish upload.");
	        		logger.debug("RESTORING: [" + fin.format(formatter) + "]");
	        		restoreLog.debug("RESTORING: [" + fin.format(formatter) + "]");
	        	}else {
	        		logger.debug("BLOB STORAGE: Nothing to upload.");
	        		logger.debug("RESTORING: [" + fin.format(formatter) + "]");
	        	}
			} catch (StorageException | IOException e) {
				logger.error("Error subiendo archivo",e);
				System.exit(1);
			}
	         
	        try {
	        	if(props.getProperty("application.sleep",null) != null )
	        		Thread.sleep(Long.parseLong(props.getProperty("application.sleep")));
			} catch (NumberFormatException | InterruptedException e) {
				logger.error("Error pausando App",e);
				System.exit(1);
			}
	        inicio = fin; 
	        fin = LocalDateTime.now();
        }
    }
    
    private static void loadProperties( String propFile) {
    	props = new Properties(); 
        try { 
            FileReader fr = new FileReader(propFile);
            props.load(fr); 
            fr.close(); 
        } catch (IOException e) { 
            System.out.println("Error not laod configuration file "); 
            System.exit(1);
        } 
        
        LogManager.resetConfiguration(); 
        PropertyConfigurator.configure(props); 
    }
}
