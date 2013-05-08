package my.test.mvstore;

import my.test.TestBase;

public class MVTableTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new MVTableTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS MVTableTest CASCADE");
		sql = "CREATE TABLE IF NOT EXISTS MVTableTest(id int, name varchar(500), b boolean) "
				+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"";

		//sql = "CREATE TABLE IF NOT EXISTS MVTableTest(id int, name varchar(500), b boolean) ";

		stmt.executeUpdate(sql);
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_MVTableTest_name ON MVTableTest(name)");
		stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_MVTableTest_id ON MVTableTest(id)");

		conn.setAutoCommit(false);
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(70, 'b3', true)");

		//测试org.h2.mvstore.db.MVTable.addRow(Session, Row)中的rollbackToSavepoint
		//同时测org.h2.mvstore.db.TransactionStore.rollbackTo(Transaction, long, long)
		//MVPrimaryIndex对应的map会rollback
		//idx_name对应的MVSecondaryIndex的map也会rollback
		//stmt.executeUpdate("insert into MVTableTest(id, name, b) values(70, 'b3', true)");

		System.out.println(stmt.executeUpdate("delete from MVTableTest where id=70"));

		conn.commit();
		sql = "select * from MVTableTest";

		executeQuery();
	}
}