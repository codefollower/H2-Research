package my.test.expression;

import my.test.TestBase;

public class SubqueryTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new SubqueryTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS SubqueryTest");
		stmt.executeUpdate("create table IF NOT EXISTS SubqueryTest(id int, name varchar(500))");

		stmt.executeUpdate("insert into SubqueryTest(id, name) values(1, 'a1')");
		stmt.executeUpdate("insert into SubqueryTest(id, name) values(1, 'b1')");
		stmt.executeUpdate("insert into SubqueryTest(id, name) values(2, 'a2')");
		stmt.executeUpdate("insert into SubqueryTest(id, name) values(2, 'b2')");
		stmt.executeUpdate("insert into SubqueryTest(id, name) values(3, 'a3')");
		stmt.executeUpdate("insert into SubqueryTest(id, name) values(3, 'b3')");

		//严格来说这种sql才算Subquery，上面的in，ALL，ANY，SOME都只是普通的select
		//Subquery包含的行数不能大于1，而in，ALL，ANY，SOME没限制，
		//想一下也理解，比如id> (select id from SubqueryTest where id>1)如果这个Subquery大于1行，那么id就不知道和谁比较
		//sql = "select * from SubqueryTest where id > (select id from SubqueryTest where id>1)";
		//但是Subquery可以有多例
		sql = "select * from SubqueryTest where id > (select id, name from SubqueryTest where id=1 and name='a1')";

		executeQuery();
	}

}
