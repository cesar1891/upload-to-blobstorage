package com.banorte.azure.blobstorage;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.banorte.azure.blobstorage.BlobRemote.Builder;

import blob_storage.upload_fraom_db.App;

public class ResultSetAsInputStream extends InputStream{

	private static final Logger logger = Logger.getLogger(ResultSetAsInputStream.class);
	
	private  RowToByteArrayConverter converter;
    private  PreparedStatement statement;
    private  ResultSet resultSet;
    private  Builder build;

    private byte[] buffer;
    private int position;
    
    public static class Builder{
    	
    	private RowToByteArrayConverter converter;
    	private Connection connection;
    	private String sql;
    	private Object[] parameters;
    	
    	public static Builder newInstance() 
        { 
            return new Builder(); 
        }
		
		private Builder() {}

		public Builder setConverter(RowToByteArrayConverter converter) {
			this.converter = converter;
			return this;
		}

		public Builder setConnection(Connection connection) {
			this.connection = connection;
			return this;
		}

		public Builder setSql(String sql) {
			this.sql = sql;
			return this;
		}

		public Builder setParameters(Object ... parameters) {
			this.parameters = parameters;
			return this;
		}
		
		public ResultSetAsInputStream build() throws SQLException {
			ResultSetAsInputStream rsToInStr = new ResultSetAsInputStream();
			rsToInStr.converter = this.converter;
			rsToInStr.statement = createStatement(this.connection, this.sql, this.parameters);
			rsToInStr.build = this;
			try {
				rsToInStr.resultSet = rsToInStr.
						statement.
						executeQuery();
			} catch (SQLException e) {
				logger.error("Error construyendo ResultSetAsInputStream", e);
				System.exit(1);
			}
			
			return rsToInStr;
		}

		public RowToByteArrayConverter getConverter() {
			return converter;
		}

		public Connection getConnection() {
			return connection;
		}

		public String getSql() {
			return sql;
		}

		public Object[] getParameters() {
			return parameters;
		}
    	
		
    }

    private ResultSetAsInputStream() {
    	
    }
    

    private static PreparedStatement createStatement(final Connection connection, final String sql, final Object[] parameters) throws SQLException {
		PreparedStatement preparedStatement = connection.prepareStatement(sql);
		int i=0;
		
		if(parameters != null)
	    	for(Object obj : parameters) {
	    		i++;
	    		preparedStatement.setObject(i, obj);
	    	}
		return preparedStatement;
    }

    @Override
    public int read() throws IOException {
        try {
            if(buffer == null) {
                // first call of read method
                if(!resultSet.next()) {
                    return -1; // no rows - empty input stream
                } else {
                    buffer = converter.rowToByteArray(resultSet);
                    position = 0;
                    return buffer[position++] & (0xff);
                }
            } else {
                // not first call of read method
                if(position < buffer.length) {
                    // buffer already has some data in, which hasn't been read yet - returning it
                    return buffer[position++] & (0xff);
                } else {
                    // all data from buffer was read - checking whether there is next row and re-filling buffer
                    if(!resultSet.next()) {
                        return -1; // the buffer was read to the end and there is no rows - end of input stream
                    } else {
                        // there is next row - converting it to byte array and re-filling buffer
                        buffer = converter.rowToByteArray(resultSet);
                        position = 0;
                        return buffer[position++] & (0xff);
                    }
                }
            }
        } catch(final SQLException ex) {
            throw new IOException(ex);
        }
    }


    @Override
    public int available() throws IOException {
    	int rowcount = 0;
    	try {
    		statement = createStatement(build.connection, build.sql, build.parameters);
			ResultSet rs = statement.executeQuery();
			while(rs.next()) rowcount++;
			rs.close();
		} catch (SQLException e) {
			logger.error("Error contando los registros.", e);
			System.exit(1);
		}
    	return rowcount;
    }

    @Override
    public void close() throws IOException {
        try {
            statement.close();
        } catch(final SQLException ex) {
            throw new IOException(ex);
        }
    }
}
