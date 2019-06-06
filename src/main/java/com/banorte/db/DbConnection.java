package com.banorte.db;

import java.sql.Connection;
import java.sql.DriverManager;

import com.banorte.azure.blobstorage.BlobRemote.Builder;

public class DbConnection {

	private String url;
	private String driver;
	private Connection conn;
	
	public static class Builder{
		private String driver;
		private String username;
		private String password;
		private String dbms;
		private String serverName;
		private String portNumber;
		private String dbname;
		
		public static Builder newInstance() 
        { 
            return new Builder(); 
        }
		
		public Builder(){
		}
		
		
		public Builder setDbms(String dbms) {
			this.dbms = dbms;
			return this;
		}

		public Builder setServerName(String serverName) {
			this.serverName = serverName;
			return this;
		}

		public Builder setPortNumber(String portNumber) {
			this.portNumber = portNumber;
			return this;
		}

		public Builder setDbname(String dbname) {
			this.dbname = dbname;
			return this;
		}

		public Builder whitDriver(String driver) {
			this.driver = driver;
			return this;
		}
		
		public Builder setPassword(String password) {
			this.password = password;
			return this;
		}
		
		public Builder setUser(String user) {
			this.username = user;
			return this;
		}
		
		public Connection build() {
			DbConnection conn = new DbConnection();
			conn.url = "jdbc:" + this.dbms + "://" +
	                   this.serverName +
	                   ":" + this.portNumber + "/" +
	                   this.dbname;
			conn.driver = this.driver;
			if( this.username != null ) {
				conn.url += ";user=" + this.username;
			}
			if( this.password != null ) {
				conn.url += ";password=" + this.password;
			}
			conn.createConnection();
			return conn.conn;
		}
	}
	
	private DbConnection() {
	}
	
	private void createConnection() {
		try
        {
            Class.forName(this.driver).newInstance();
            conn = DriverManager.getConnection(this.url); 
        }
        catch (Exception except)
        {
            except.printStackTrace();
            System.exit(1);
        }
		
	}
	
	
}
