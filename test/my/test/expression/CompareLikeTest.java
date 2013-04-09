package my.test.expression;

import my.test.TestBase;

public class CompareLikeTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CompareLikeTest().start();
	}

	//测试org.h2.expression.CompareLike
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS CompareLikeTest");
		stmt.executeUpdate("create table IF NOT EXISTS CompareLikeTest(id int, name varchar(500))");

		stmt.executeUpdate("insert into CompareLikeTest(id, name) values(1, 'a1')");
		stmt.executeUpdate("insert into CompareLikeTest(id, name) values(1, 'b1')");
		stmt.executeUpdate("insert into CompareLikeTest(id, name) values(2, 'a2')");
		stmt.executeUpdate("insert into CompareLikeTest(id, name) values(2, 'b2')");
		stmt.executeUpdate("insert into CompareLikeTest(id, name) values(3, 'a3')");
		stmt.executeUpdate("insert into CompareLikeTest(id, name) values(3, 'b3')");
		
		//ESCAPE后只能接一个字符
		//org.h2.jdbc.JdbcSQLException: Error in LIKE ESCAPE: "%kk";
		sql = "SELECT id,name FROM CompareLikeTest where name like '%2%' ESCAPE '%kk'";
		
		sql = "SELECT id,name FROM CompareLikeTest where name like '%2%' ESCAPE '%'";
		executeQuery();
	}
}
