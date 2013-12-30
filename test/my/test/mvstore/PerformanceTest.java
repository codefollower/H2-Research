package my.test.mvstore;

import java.util.Random;

import my.test.TestBase;

public class PerformanceTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new PerformanceTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		run(null);
		run("org.h2.mvstore.db.MVTableEngine");
	}

	public void run(String engine) throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS PerformanceTest CASCADE");
		sql = "CREATE TABLE IF NOT EXISTS PerformanceTest(id int, name varchar(500), b boolean)";
		if (engine != null)
			sql += " ENGINE \"" + engine + "\"";

		if (engine == null)
			engine = "[PageStore]";
		else
			engine = "[MVStore  ]";

		stmt.executeUpdate(sql);
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_PerformanceTest_id ON PerformanceTest(id)");

		int count = 5000;
		long t1 = System.currentTimeMillis();
		for (int i = 0, size = count / 5; i < size; i++) {
			stmt.executeUpdate("insert into PerformanceTest(id, name, b) values(10, 'a1', true)");
			stmt.executeUpdate("insert into PerformanceTest(id, name, b) values(20, 'b1', true)");
			stmt.executeUpdate("insert into PerformanceTest(id, name, b) values(30, 'a2', false)");
			stmt.executeUpdate("insert into PerformanceTest(id, name, b) values(40, 'b2', true)");
			stmt.executeUpdate("insert into PerformanceTest(id, name, b) values(50, 'a3', false)");
			stmt.executeUpdate("insert into PerformanceTest(id, name, b) values(60, 'b3', true)");
			stmt.executeUpdate("insert into PerformanceTest(id, name, b) values(70, 'b3', true)");
		}
		long t2 = System.currentTimeMillis();
		System.out.println(engine + " insert: " + (t2 - t1) + " ms");

		t1 = System.currentTimeMillis();
		Random r = new Random();
		for (int i = 0; i < count; i++) {
			stmt.executeQuery("select * from PerformanceTest where id=" + r.nextInt(count));
		}
		t2 = System.currentTimeMillis();
		System.out.println(engine + " select: " + (t2 - t1) + " ms");
	}

}