package my.test.transaction;

import my.test.TestBase;

public class TransactionCommandTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new TransactionCommandTest().start();
	}

	public void init() throws Exception {
		//prop.setProperty("LARGE_TRANSACTIONS", "false");
		prop.setProperty("user", "root");
		prop.setProperty("password", "zhh");
		url = "jdbc:mysql://localhost:3306/test";
	}

	@Override
	public void startInternal() throws Exception {
		//stmt.executeUpdate("drop table IF EXISTS TransactionCommandTest");
		stmt.executeUpdate("create table IF NOT EXISTS TransactionCommandTest(id int, name varchar(4))");
		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS TransactionCommandTestIndex ON TransactionCommandTest(name)");

		//stmt.executeUpdate("SET AUTOCOMMIT true");
		//stmt.executeUpdate("SET AUTOCOMMIT false");
		//stmt.executeUpdate("BEGIN");
		try {
			conn.setAutoCommit(false);
			stmt.executeUpdate("insert into TransactionCommandTest(id, name) values(1, 'a1')");
			stmt.executeUpdate("insert into TransactionCommandTest(id, name) values(1, 'b1')");
			stmt.executeUpdate("insert into TransactionCommandTest(id, name) values(2, 'a22')");
			stmt.executeUpdate("insert into TransactionCommandTest(id, name) values(2, 'b2')");
			stmt.executeUpdate("insert into TransactionCommandTest(id, name) values(3, 'a3')");
			stmt.executeUpdate("insert into TransactionCommandTest(id, name) values(3, 'b3')");

			stmt.executeUpdate("insert into TransactionCommandTest(id, name) values(3, 'b3 too long')");
		} catch (Exception e) {
			//conn.rollback();
		} finally {
			conn.setAutoCommit(true);
			sql = "select * from TransactionCommandTest";
			executeQuery();
		}
	}
}