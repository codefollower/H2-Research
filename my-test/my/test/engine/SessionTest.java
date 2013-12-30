package my.test.engine;

import my.test.TestBase;

public class SessionTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new SessionTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("PAGE_SIZE", "128");
	}

	@Override
	public void startInternal() throws Exception {
		conn.setAutoCommit(false);
		//stmt.executeUpdate("SET DB_CLOSE_DELAY -1");
		stmt.executeUpdate("DROP TABLE IF EXISTS SessionTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS SessionTest(id int not null, name varchar(500) not null, address varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON SessionTest(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON SessionTest(name desc)");

		//stmt.executeUpdate("SET MAX_LOG_SIZE 10"); //10K

		long ii = 1000000001L;
		for (int i = 1; i <= 50; i++) {
			//stmt.executeUpdate("insert into SessionTest(id, name) values(" + i + ", '" + s + "abcdef1234')");
			//stmt.executeUpdate("insert into SessionTest(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
			stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
			//			if (i % 100 == 0)
			//				stmt.executeUpdate("CHECKPOINT");
			//
			//			if (i % 200 == 0) {
			//				stmt.executeUpdate("PREPARE COMMIT newTransactionName");
			//				//conn.commit();
			//			}

			if (i % 10 == 0) {
				conn.commit();
			}

			ii++;
		}
		// stmt.executeUpdate("insert into SessionTest(id, name) values(" + 101 + ", '" + s + "abcdef1234')");
		// conn.rollback();
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(10, '1000000001', 'a')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(20, '1000000002', 'b')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(30, '1000000003', 'c')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(40, '1000000004', 'd')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(50, '1000000005', 'e')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(60, '1000000006', 'f')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(70, '1000000007', 'g')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(80, '1000000008', 'h')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(90, '1000000009', 'i')");
		//
		//		//stmt.executeUpdate("insert into SessionTest(id, name, address) SORTED values(30, '1000000003', 'c')");
		//		//stmt.executeUpdate("delete from SessionTest where id=40");
		//		stmt.executeUpdate("delete from SessionTest where id=30");
		//conn.commit();

		//		stmt.executeUpdate("delete from SessionTest where id=2");
		//		stmt.executeUpdate("delete from SessionTest where id>300");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) values(" + 300 + ", '" + 300 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) values(" + 301 + ", '" + 302 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into SessionTest(id, name, address) values(" + 2 + ", '" + 2 + "abcdef1234', 'zzz')");
		//
		//		stmt.executeUpdate("update SessionTest set name='1234567890' where id>10");

		sql = "select name,id from SessionTest where name>='1000000004'";
		sql = "select name,id from SessionTest where name between '1000000004' and '1000000006'";
		executeQuery();

		//stmt.executeUpdate("TRUNCATE TABLE SessionTest");
		//stmt.executeUpdate("drop index IF EXISTS idx_name");

		//conn.commit();
		//conn.rollback();
	}
}