package my.test.expression;

import my.test.TestBase;

public class ConditionInSelectTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ConditionInSelectTest().start();
	}

	//测试org.h2.expression.ConditionInSelect
	@Override
	public void startInternal() throws Exception {
	    //stmt.executeUpdate("set BATCH_JOINS true");
		stmt.executeUpdate("drop table IF EXISTS ConditionInSelectTest");
		stmt.executeUpdate("create table IF NOT EXISTS ConditionInSelectTest(id int, name varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS ConditionInSelectTestIndex ON ConditionInSelectTest(name)");

		stmt.executeUpdate("insert into ConditionInSelectTest(id, name) values(1, 'a1')");
		stmt.executeUpdate("insert into ConditionInSelectTest(id, name) values(1, 'b1')");
		stmt.executeUpdate("insert into ConditionInSelectTest(id, name) values(2, 'a2')");
		stmt.executeUpdate("insert into ConditionInSelectTest(id, name) values(2, 'b2')");
		stmt.executeUpdate("insert into ConditionInSelectTest(id, name) values(3, 'a3')");
		stmt.executeUpdate("insert into ConditionInSelectTest(id, name) values(3, 'b3')");

		sql = "delete top 3 from ConditionInSelectTest where id in(select id from ConditionInSelectTest where id=3)";
		//子查询不能多于1个列
		sql = "delete from ConditionInSelectTest where (id,name) in(select id,name from ConditionInSelectTest where id=3)";
		executeUpdate();
		sql = "delete from ConditionInSelectTest where id in(select id from ConditionInSelectTest where id>2)";
		executeUpdate();
		//sql = "delete from ConditionInSelectTest where id > ALL(select id from ConditionInSelectTest where id>10)";
		//ANY和SOME一样
		//sql = "delete from ConditionInSelectTest where id > ANY(select id from ConditionInSelectTest where id>1)";
		//sql = "delete from ConditionInSelectTest where id > SOME(select id from ConditionInSelectTest where id>10)";
		
		//严格来说这种sql才算Subquery，上面的in，ALL，ANY，SOME都只是普通的select
		//Subquery包含的行数不能大于1，而in，ALL，ANY，SOME没限制，
		//想一下也理解，比如id> (select id from ConditionInSelectTest where id>1)如果这个Subquery大于1行，那么id就不知道和谁比较
		//sql = "delete from ConditionInSelectTest where id > (select id from ConditionInSelectTest where id>1)";
	    //但是Subquery可以有多例
		//sql = "delete from ConditionInSelectTest where id > (select id, name from ConditionInSelectTest where id=1 and name='a1')";
		stmt.executeUpdate(sql);

		//		sql = "delete top 3 from ConditionInSelectTest where name > ?";
		//		ps = conn.prepareStatement(sql);
		//		ps.setString(1, "b1");
		//		ps.executeUpdate();

		sql = "select * from ConditionInSelectTest";
		executeQuery();
	}
}
