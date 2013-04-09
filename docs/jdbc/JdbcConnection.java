org.h2.jdbc.JdbcConnection.createBlob()
	=> org.h2.store.LobStorage.createBlob(InputStream, long)
		=> org.h2.value.ValueLobDb.createTempBlob(InputStream, long, DataHandler)