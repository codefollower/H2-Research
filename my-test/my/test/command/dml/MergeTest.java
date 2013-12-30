package my.test.command.dml;

import my.test.TestBase;

public class MergeTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new MergeTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_FILE", "10");
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS MergeTest");
		//如果id是primary key，那么在MERGE语句中KEY子句更省，默认用primary key
		//stmt.executeUpdate("CREATE TABLE IF NOT EXISTS MergeTest(id int not null primary key, name varchar(500) not null)");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS MergeTest(id int, name varchar(500) as '123')");

		stmt.executeUpdate("DROP TABLE IF EXISTS tmpSelectTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tmpSelectTest(id int, name varchar(500))");
		stmt.executeUpdate("INSERT INTO tmpSelectTest VALUES(DEFAULT, DEFAULT),(10, 'a'),(20, 'b')");

		//从另一表查数据，然后插入此表
		sql = "MERGE INTO MergeTest KEY(id) (SELECT * FROM tmpSelectTest)";
		stmt.executeUpdate(sql);

		sql = "MERGE INTO MergeTest KEY(id) VALUES()"; //这里会抛异常，但是异常信息很怪，算是H2的一个小bug
		try {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			//Syntax error in SQL statement "UPDATE PUBLIC.MERGETEST SET  WHERE[*] ID=?"; expected "identifier"; 
			//SQL statement:UPDATE PUBLIC.MERGETEST SET  WHERE ID=? [42001-172]
			System.out.println(e.getMessage());
		}

		//这种语法可查入多条记录
		//30 null
		//10 a
		//20 b
		sql = "MERGE INTO MergeTest KEY(id) VALUES(30, DEFAULT),(10, 'a'),(20, 'b')";
		stmt.executeUpdate(sql);

		try {
			sql = "MERGE INTO MergeTest KEY(id) VALUES(DEFAULT, DEFAULT),(10, 'a'),(20, 'b')";
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			//org.h2.jdbc.JdbcSQLException: Column "ID" contains null values; 
			System.out.println(e.getMessage());
		}

		//列必须一样多，否则:org.h2.jdbc.JdbcSQLException: Column count does not match;
		sql = "MERGE INTO MergeTest(name) KEY(id) (SELECT * FROM tmpSelectTest)";
		try {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		//key字段必须出现在VALUES中
		sql = "MERGE INTO MergeTest(name) KEY(id) VALUES('abc')";
		try {
			stmt.executeUpdate(sql);
		} catch (Exception e) {
			//Column "ID" contains null values;
			System.out.println(e.getMessage());
		}

		sql = "MERGE INTO MergeTest(name, id) KEY(id) VALUES('abc', 10)";
		stmt.executeUpdate(sql);

		ps = conn.prepareStatement("MERGE INTO MergeTest(id, name) KEY(id) VALUES(?, ?)");
		ps.setInt(1, 30);
		ps.setString(2, "c");
		ps.executeUpdate();

		sql = "EXPLAIN MERGE INTO MergeTest(id, name) KEY(id) SELECT * FROM tmpSelectTest";
		executeQuery();

		sql = "select id,name from MergeTest";
		executeQuery();
	}

}
