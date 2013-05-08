package my.test.mvstore;

import my.test.TestBase;

public class MVPrimaryIndexTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new MVPrimaryIndexTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS MVPrimaryIndexTest CASCADE");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS MVPrimaryIndexTest(id int not null, name varchar(500), b boolean) "
				+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS MVPrimaryIndexTestIndex ON MVPrimaryIndexTest(name)");

		stmt.executeUpdate("CREATE PRIMARY KEY IF NOT EXISTS idx_id ON MVPrimaryIndexTest(id)");

		//conn.setAutoCommit(false);
		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(70, 'b3', true)");

		stmt.executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(70, 'b3', true)");

		//当更新记录时，会删除原来的记录，再插入新记录，但是新记录的rowKey是0，不会沿用原来的rowKey
		stmt.executeUpdate("update MVPrimaryIndexTest set b=false where id=70");

		//conn.commit();
		sql = "select * from MVPrimaryIndexTest";

		//见org.h2.index.Index.findNext(Session, SearchRow, SearchRow)中的注释
		sql = "select distinct name from MVPrimaryIndexTest";
		executeQuery();
	}
}
