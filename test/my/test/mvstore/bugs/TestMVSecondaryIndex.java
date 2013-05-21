package my.test.mvstore.bugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMVSecondaryIndex {
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
		stmt.executeUpdate("DROP TABLE IF EXISTS TestMVSecondaryIndex");
		String sql = "CREATE TABLE IF NOT EXISTS TestMVSecondaryIndex(id int not null PRIMARY KEY, name varchar(20))";
		if (engine != null)
			sql += " ENGINE \"" + engine + "\"";

		stmt.executeUpdate(sql);

		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON TestMVSecondaryIndex(name)");

		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(-1, 'a')");
		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(-2, 'a')");
		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(1, 'b')");
		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(2, 'b')");

		count(2, "name='b'");
		count(2, "name='a'"); //MVTable failed
	}

	private void count(int expected, String where) throws Exception {
		ResultSet rs = stmt.executeQuery("select count(*) from TestMVSecondaryIndex where " + where);
		rs.next();
		Assert.assertEquals(expected, rs.getInt(1));
		rs.close();
	}
}
/*
[MVStore] Can not find the records from the secondary index if the value of primary key is negative and the type of index condition is EQUAL

H2 Version: svn trunk

Test:
==========================================
package my.test.mvstore.bugs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestMVSecondaryIndex {
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
		stmt.executeUpdate("DROP TABLE IF EXISTS TestMVSecondaryIndex");
		String sql = "CREATE TABLE IF NOT EXISTS TestMVSecondaryIndex(id int not null PRIMARY KEY, name varchar(20))";
		if (engine != null)
			sql += " ENGINE \"" + engine + "\"";

		stmt.executeUpdate(sql);

		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON TestMVSecondaryIndex(name)");

		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(-1, 'a')");
		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(-2, 'a')");
		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(1, 'b')");
		stmt.executeUpdate("insert into TestMVSecondaryIndex(id, name) values(2, 'b')");

		count(2, "name='b'");
		count(2, "name='a'"); //MVTable failed
	}

	private void count(int expected, String where) throws Exception {
		ResultSet rs = stmt.executeQuery("select count(*) from TestMVSecondaryIndex where " + where);
		rs.next();
		Assert.assertEquals(expected, rs.getInt(1));
		rs.close();
	}
}


Analyze:
===============
The org.h2.mvstore.db.MVSecondaryIndex.find(Session, SearchRow, SearchRow) method using 0 as the key If the parameter 'first' is not null,
then ['a', 0] > ['a', -1]


Fix:
===============
Using Long.MIN_VALUE as the key may be better

*/
