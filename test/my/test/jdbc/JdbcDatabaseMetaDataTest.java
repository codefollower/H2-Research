package my.test.jdbc;

import java.sql.DatabaseMetaData;

import my.test.TestBase;

public class JdbcDatabaseMetaDataTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new JdbcDatabaseMetaDataTest().start();
	}

	@Override
	public void init() throws Exception {
	}

	@Override
	public void startInternal() throws Exception {
		DatabaseMetaData dbmd = conn.getMetaData();
		rs = dbmd.getTables(null, null, null, null);

		printResultSet(rs);
	}
}