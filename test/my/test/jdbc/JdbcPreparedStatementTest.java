package my.test.jdbc;

import java.sql.ResultSetMetaData;

import my.test.TestBase;

public class JdbcPreparedStatementTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new JdbcPreparedStatementTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
	}

	//测试org.h2.result.LocalResult
	//org.h2.result.ResultRemote
	@Override
	public void startInternal() throws Exception {
		//		stmt.executeUpdate("drop table IF EXISTS JdbcPreparedStatementTest CASCADE");
		//		stmt.executeUpdate("create table IF NOT EXISTS JdbcPreparedStatementTest(id int, name varchar(500), b boolean)");
		//		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS JdbcPreparedStatementTestIndex ON JdbcPreparedStatementTest(name)");
		//
		//		stmt.executeUpdate("insert into JdbcPreparedStatementTest(id, name, b) values(10, 'a1', true)");
		//		stmt.executeUpdate("insert into JdbcPreparedStatementTest(id, name, b) values(20, 'b1', true)");
		//		stmt.executeUpdate("insert into JdbcPreparedStatementTest(id, name, b) values(30, 'a2', false)");
		//		stmt.executeUpdate("insert into JdbcPreparedStatementTest(id, name, b) values(40, 'b2', true)");
		//		stmt.executeUpdate("insert into JdbcPreparedStatementTest(id, name, b) values(50, 'a3', false)");
		//		stmt.executeUpdate("insert into JdbcPreparedStatementTest(id, name, b) values(60, 'b3', true)");
		//		stmt.executeUpdate("insert into JdbcPreparedStatementTest(id, name, b) values(70, 'b3', true)");

		stmt.executeUpdate("drop table IF EXISTS JdbcPreparedStatementTest CASCADE");
		stmt.executeUpdate("create table IF NOT EXISTS JdbcPreparedStatementTest(rowkey varchar(500), age int, name varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS JdbcPreparedStatementTestIndex ON JdbcPreparedStatementTest(name)");
		//		sql = "select * from JdbcPreparedStatementTest where id>? and b=?";
		//		//sql = "update JdbcPreparedStatementTest set b=true where id>? and b=?";
		//		ps = conn.prepareStatement(sql);
		//		ps.setFetchSize(2);
		//		ps.setInt(1, 50);
		//		ps.setBoolean(2, true);
		//
		//		rs = ps.executeQuery();

		sql = "insert into JdbcPreparedStatementTest values(?, ?, ?)";
		//		ps = conn.prepareStatement(sql);
		//		ps.setString(1, "20000");
		//		ps.setInt(2, 30);
		//		ps.setString(3, "zhh-2009");
		//		ps.addBatch();
		//		
		//		ps.setString(1, "20001");
		//		ps.setInt(2, 30);
		//		ps.setString(3, "zhh-2009");
		//		ps.addBatch();
		//		
		//		ps.setString(1, "20002");
		//		ps.setInt(2, 30);
		//		ps.setString(3, "zhh-2009");
		//		ps.addBatch();
		//		
		//		ps.setString(1, "20003");
		//		ps.setInt(2, 30);
		//		ps.setString(3, "zhh-2009");
		//		ps.addBatch();

		sql = "insert into JdbcPreparedStatementTest(rowkey, age) values(?, ?)";
		ps = conn.prepareStatement(sql);
		ps.setString(1, "20000");
		ps.setInt(2, 30);
		ps.addBatch();

		ps.setString(1, "20001");
		ps.setInt(2, 30);
		ps.addBatch();

		//ps.executeUpdate();
		ps.executeBatch();

		sql = "select * from JdbcPreparedStatementTest";
		executeQuery();

		//等价于ResultSet.getMetaData()，只不过PreparedStatement.getMetaData()不需要事先执行查询
		ResultSetMetaData rsmd = ps.getMetaData(); //如果不是查询sql，此方法会返回null
		int n = rsmd.getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
		}
	}
}
