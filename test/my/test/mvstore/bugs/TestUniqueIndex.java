package my.test.mvstore.bugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestUniqueIndex {
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
		run(null); //all tests pass
	}

	@Test
	public void testMVTable() throws Exception {
		run("org.h2.mvstore.db.MVTableEngine");
	}

	private void run(String engine) throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS TestUniqueIndex");
		String sql = "CREATE TABLE IF NOT EXISTS TestUniqueIndex(id int not null PRIMARY KEY, name varchar(20))";
		if (engine != null)
			sql += " ENGINE \"" + engine + "\"";

		stmt.executeUpdate(sql);

		stmt.executeUpdate("DROP INDEX IF EXISTS idx_name");
		stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_name ON TestUniqueIndex(name)");

		try {
			stmt.executeUpdate("insert into TestUniqueIndex(id, name) values(-1, 'a')");
			stmt.executeUpdate("insert into TestUniqueIndex(id, name) values(-2, 'a')");
			stmt.executeUpdate("insert into TestUniqueIndex(id, name) values(1, 'a')");

			Assert.fail("insert duplicate keys into unique index"); //MVTable failed
		} catch (SQLException e) {
		}
	}
}
/*
[MVStore] Insert duplicate keys into unique index if the value of primary key is negative

H2 Version: svn trunk

Test:
==========================================
package my.test.mvstore.bugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestUniqueIndex {
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
		run(null); //all tests pass
	}

	@Test
	public void testMVTable() throws Exception {
		run("org.h2.mvstore.db.MVTableEngine");
	}

	private void run(String engine) throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS TestUniqueIndex");
		String sql = "CREATE TABLE IF NOT EXISTS TestUniqueIndex(id int not null PRIMARY KEY, name varchar(20))";
		if (engine != null)
			sql += " ENGINE \"" + engine + "\"";

		stmt.executeUpdate(sql);

		stmt.executeUpdate("DROP INDEX IF EXISTS idx_name");
		stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_name ON TestUniqueIndex(name)");

		try {
			stmt.executeUpdate("insert into TestUniqueIndex(id, name) values(-1, 'a')");
			stmt.executeUpdate("insert into TestUniqueIndex(id, name) values(-2, 'a')");
			stmt.executeUpdate("insert into TestUniqueIndex(id, name) values(1, 'a')");

			Assert.fail("insert duplicate keys into unique index"); //MVTable failed
		} catch (SQLException e) {
		}
	}
}


Analyze:
===============
The org.h2.mvstore.db.MVSecondaryIndex.add(Session, Row) method using 0 as the key,
then map.ceilingKey(['a', 0]) always return null


Fix:
===============
Using Long.MIN_VALUE as the key may be better

*/
