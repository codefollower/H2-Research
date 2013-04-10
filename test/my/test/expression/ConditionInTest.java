package my.test.expression;

import my.test.TestBase;

public class ConditionInTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ConditionInTest().start();
	}

	//测试org.h2.expression.ConditionIn
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS ConditionInTest");
		stmt.executeUpdate("create table IF NOT EXISTS ConditionInTest(id int, name varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS ConditionInTestIndex ON ConditionInTest(name)");

		stmt.executeUpdate("insert into ConditionInTest(id, name) values(1, 'a1')");
		stmt.executeUpdate("insert into ConditionInTest(id, name) values(1, 'b1')");
		stmt.executeUpdate("insert into ConditionInTest(id, name) values(2, 'a2')");
		stmt.executeUpdate("insert into ConditionInTest(id, name) values(2, 'b2')");
		stmt.executeUpdate("insert into ConditionInTest(id, name) values(3, 'a3')");
		stmt.executeUpdate("insert into ConditionInTest(id, name) values(3, 'b3')");

		sql = "select count(*) from ConditionInTest where id in()";
		sql = "select count(*) from ConditionInTest where not id in()";

		//如id in(+(select id from SubqueryTest where id=1 and name='a1'))
		//Subquery的记录不能多于1条，所以在前面放+号就可以绕过isSelect()，此时返回的就是一个Subquery
		//但是放减号是不行的，会得到一个Operation
		//+号会转成ConditionInSelect，而不再使用ConditionIn
		sql = "select count(*) from ConditionInTest where id in(+(select id from SubqueryTest where id=1 and name='a1'))";
		sql = "select count(*) from ConditionInTest where id in(-(select id from SubqueryTest where id=1 and name='a1'))";
		sql = "select count(*) from ConditionInTest where id in(3, (select id from SubqueryTest where id=1 and name='a1'))";

		sql = "select count(*) from ConditionInTest where null in(1,2)";
		//		sql = "select count(*) from ConditionInTest where 2 in(1,2)";
		//		sql = "select count(*) from ConditionInTest where id in(2)";
		//		sql = "select count(*) from ConditionInTest where id in(30,40,null)";
		sql = "select count(*) from ConditionInTest where id in(1,2,2)"; //值列表允许重复

		//跟ConditionInSelect不一样，这里没有ALL、ANY
		//sql = "select count(*) from ConditionInTest where id > ALL(1,2)";
		//ANY和SOME一样
		//sql = "select count(*) from ConditionInTest where id > ANY(1,2)";
		//sql = "select count(*) from ConditionInTest where id > SOME(1,2)";

		//sql = "select count(*) from ConditionInTest";
		executeQuery();
	}
}
