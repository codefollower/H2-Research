package my.test.mvstore;

import my.test.TestBase;

public class MVTableEngineTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new MVTableEngineTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS MVTableEngineTest CASCADE");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS MVTableEngineTest(id int, name varchar(500), b boolean) "
				+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS MVTableEngineTestIndex ON MVTableEngineTest(name)");

		//conn.setAutoCommit(false);
		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(70, 'b3', true)");

		//conn.commit();
		sql = "select * from MVTableEngineTest";
		executeQuery();
	}
}
