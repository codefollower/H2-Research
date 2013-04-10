package my.test.table;

import java.sql.Connection;

import my.test.TestBase;

public class RegularTableTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new RegularTableTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
		
		prop.setProperty("MVCC", "true");
		//prop.setProperty("ANALYZE_AUTO", "3");
	}

	//测试org.h2.result.LocalResult
	//org.h2.result.ResultRemote
	//org.h2.command.dml.Select.queryWithoutCache(int, ResultTarget)
	@Override
	public void startInternal() throws Exception {
		test();
		//lock();
	}

	void test() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS RegularTableTest CASCADE");
		stmt.executeUpdate("create table IF NOT EXISTS RegularTableTest(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS RegularTableTestIndex ON RegularTableTest(name)");

		stmt.executeUpdate("insert into RegularTableTest(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into RegularTableTest(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into RegularTableTest(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into RegularTableTest(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into RegularTableTest(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into RegularTableTest(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into RegularTableTest(id, name, b) values(70, 'b3', true)");
		stmt.execute("insert into RegularTableTest(id, name, b) values(70, 'b3', true)");
		
		
		stmt.executeUpdate("create table IF NOT EXISTS RegularTableTest1(id int,  primary key(id))");
		stmt.executeUpdate("create table IF NOT EXISTS RegularTableTest2(id int," +
				" FOREIGN KEY(id) REFERENCES RegularTableTest1(id))");
		
		stmt.executeUpdate("TRUNCATE TABLE RegularTableTest1");

		sql = "select * from RegularTableTest";
		stmt.setFetchSize(2);
		//executeQuery();

		rs = stmt.executeQuery(sql);
		int n = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
		}
	}

	void lock() throws Exception {
		//stmt.executeUpdate("SET MULTI_THREADED 1");
		new MyThread().start();
		new MyThread2().start();
	}

	class MyThread extends Thread {
		public void run() {
			Connection conn;
			try {
				conn = getConnection();
				conn.createStatement().executeUpdate("insert into RegularTableTest(id, name, b) values(70, 'b3', true)");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	class MyThread2 extends Thread {
		public void run() {
			Connection conn;
			try {
				conn = getConnection();
				conn.createStatement().executeQuery("select * from RegularTableTest");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}
}
