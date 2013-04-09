package my.test.jdbc;

import java.sql.CallableStatement;
import java.sql.Types;

import my.test.TestBase;

public class JdbcCallableStatementTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new JdbcCallableStatementTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
	}

	//测试org.h2.result.LocalResult
	//org.h2.result.ResultRemote
	@Override
	public void startInternal() throws Exception {
		//		stmt.executeUpdate("drop table IF EXISTS JdbcCallableStatementTest CASCADE");
		//		stmt.executeUpdate("create table IF NOT EXISTS JdbcCallableStatementTest(id int, name varchar(500), b boolean)");
		//		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS JdbcCallableStatementTestIndex ON JdbcCallableStatementTest(name)");
		//
		//		stmt.executeUpdate("insert into JdbcCallableStatementTest(id, name, b) values(10, 'a1', true)");
		//		stmt.executeUpdate("insert into JdbcCallableStatementTest(id, name, b) values(20, 'b1', true)");
		//		stmt.executeUpdate("insert into JdbcCallableStatementTest(id, name, b) values(30, 'a2', false)");
		//		stmt.executeUpdate("insert into JdbcCallableStatementTest(id, name, b) values(40, 'b2', true)");
		//		stmt.executeUpdate("insert into JdbcCallableStatementTest(id, name, b) values(50, 'a3', false)");
		//		stmt.executeUpdate("insert into JdbcCallableStatementTest(id, name, b) values(60, 'b3', true)");
		//		stmt.executeUpdate("insert into JdbcCallableStatementTest(id, name, b) values(70, 'b3', true)");

//		stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS myschema AUTHORIZATION sa");
//		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS FunctionTest(id int primary key, name varchar(500))");
//		stmt.executeUpdate("CREATE ALIAS IF NOT EXISTS myschema.MY_SQRT FOR \"java.lang.Math.sqrt\"");

		callableStatement();
	}

	void callableStatement() throws Exception {
		sql = "?= CALL myschema.MY_SQRT(2.2)";
		sql = "?= CALL myschema.MY_SQRT(?)";
		//sql = "select id from JdbcPreparedStatementTest where id>?";
		
		//sql = "?2 = CALL myschema.MY_SQRT(2.2)";
		CallableStatement cs = conn.prepareCall(sql);
		//cs.registerOutParameter(1, Types.DOUBLE); //sqlType其实被忽略了，所以设什么都没用
		//cs.registerOutParameter("ID", Types.DOUBLE);
		//cs.registerOutParameter("2", Types.DOUBLE);
		//cs.registerOutParameter("MYSCHEMA.MY_SQRT(2.2)", Types.DOUBLE);
		cs.registerOutParameter("MYSCHEMA.MY_SQRT(?2)", Types.DOUBLE);
		cs.setDouble(2, 2.2);
		cs.execute();
		System.out.println(cs.getDouble(1));
	}
}
