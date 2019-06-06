package com.banorte.azure.blobstorage;

import java.sql.ResultSet;

public interface RowToByteArrayConverter {

	byte[] rowToByteArray(ResultSet resultSet);
}
