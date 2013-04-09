package my.test.index;

import my.test.TestBase;

public class ScanIndexTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ScanIndexTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
		prop.setProperty("MVCC", "true");
	}

	@Override
	public void startInternal() throws Exception {
		conn.setAutoCommit(false);
		stmt.executeUpdate("drop table IF EXISTS ScanIndexTest CASCADE");
		stmt.executeUpdate("create LOCAL TEMPORARY table IF NOT EXISTS ScanIndexTest(id int, name varchar(50)) NOT PERSISTENT");

		stmt.executeUpdate("insert into ScanIndexTest(id, name) values(10, 'a1')");
		stmt.executeUpdate("insert into ScanIndexTest(id, name) values(20, 'b1')");
		stmt.executeUpdate("insert into ScanIndexTest(id, name) values(30, 'a2')");
		stmt.executeUpdate("delete from ScanIndexTest where id = 20");
		stmt.executeUpdate("delete from ScanIndexTest where id = 30");

		stmt.executeUpdate("insert into ScanIndexTest(id, name) values(40, 'b2')");
		stmt.executeUpdate("insert into ScanIndexTest(id, name) values(50, 'a3')");
		//		stmt.executeUpdate("insert into ScanIndexTest(id, name) values(60, 'b3')");
		//		stmt.executeUpdate("insert into ScanIndexTest(id, name) values(70, 'b3')");

		sql = "select * from ScanIndexTest";
		executeQuery();
		conn.commit();
	}
}
