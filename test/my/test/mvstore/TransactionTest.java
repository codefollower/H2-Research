package my.test.mvstore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Savepoint;
import java.sql.Statement;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TransactionTest {
	protected static Connection conn;
	protected static Statement stmt;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mydb", "sa", "");
		stmt = conn.createStatement();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		if (stmt != null)
			stmt.close();
		if (conn != null)
			conn.close();
	}

	@Test
	public void testRegularTable() throws Exception {
		//run(null); //all tests pass
	}

	@Test
	public void testMVTable() throws Exception {
		run("org.h2.mvstore.db.MVTableEngine");
	}

	private void run(String engine) throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS TransactionTest");
		String sql = "CREATE TABLE IF NOT EXISTS TransactionTest(id int, name varchar(20))";
		if (engine != null)
			sql += " ENGINE \"" + engine + "\"";

		stmt.executeUpdate(sql);

		conn.setAutoCommit(false);
		stmt.executeUpdate("insert into TransactionTest(id, name) values(10, 'a')");
		stmt.executeUpdate("insert into TransactionTest(id, name) values(20, 'b')");

		count(2); //MVTable failed
		selectAll(); //MVTable failed

		conn.commit();

		count(2);
		selectAll();

		stmt.executeUpdate("insert into TransactionTest(id, name) values(30, 'c')");
		Savepoint sp = conn.setSavepoint();
		stmt.executeUpdate("insert into TransactionTest(id, name) values(40, 'd')");
		conn.rollback(sp);

		count(3); //MVTable failed
	}

	private void count(int expected) throws Exception {
		ResultSet rs = stmt.executeQuery("select count(*) from TransactionTest");
		rs.next();
		Assert.assertEquals(expected, rs.getInt(1));
		rs.close();
	}

	private void selectAll() throws Exception {
		ResultSet rs = stmt.executeQuery("select * from TransactionTest");
		int n = rs.getMetaData().getColumnCount();
		Assert.assertEquals(2, n);
		Assert.assertTrue(rs.next());
		do {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
		} while (rs.next());
		rs.close();
	}
}
