package my.test.command.dml;

import my.test.TestBase;

public class ExplainTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ExplainTest().start();
	}

	//²âÊÔorg.h2.command.Parser.parseExplain()
	//org.h2.command.dml.Explain
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS ExplainTest");
		stmt.executeUpdate("create table IF NOT EXISTS ExplainTest(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS ExplainTestIndex ON ExplainTest(name)");

		stmt.executeUpdate("insert into ExplainTest(id, name, b) values(1, 'a1', true)");
		stmt.executeUpdate("insert into ExplainTest(id, name, b) values(1, 'b1', true)");
		stmt.executeUpdate("insert into ExplainTest(id, name, b) values(2, 'a2', false)");
		stmt.executeUpdate("insert into ExplainTest(id, name, b) values(2, 'b2', true)");
		stmt.executeUpdate("insert into ExplainTest(id, name, b) values(3, 'a3', false)");
		stmt.executeUpdate("insert into ExplainTest(id, name, b) values(3, 'b3', true)");

		sql = "delete top 3 from ExplainTest";
		sql = "delete top 3 from ExplainTest where name='a1'";
		sql = "delete top 3 from ExplainTest where 'a1'>name";
		sql = "delete top 3 from ExplainTest where name = null";
		sql = "delete top 3 from ExplainTest where name != null";
		sql = "delete top 3 from ExplainTest where name > null";

		sql = "delete from ExplainTest where name > 'b1'";
		sql = "delete from ExplainTest where id>2";
		sql = "delete from ExplainTest where 3<2";
		sql = "delete from ExplainTest where b";
		sql = "delete from ExplainTest where 3>2";
		
		sql = "select * from ExplainTest where name > 'b1'";
		sql = "select count(name) from ExplainTest where name > 'b1' group by name";
		sql = "select name, count(name) from ExplainTest group by name";
		
		sql = "WITH RECURSIVE myTempViewName(f1,f2,f3) AS(select * from ExplainTest) select (f1, f2 from myTempViewName)";
		//sql = "WITH myTempViewName(f1,f2,f3) AS(select * from ExplainTest) select f1, f2 from myTempViewName";
		
		//sql = "EXPLAIN " + sql;
		sql = "EXPLAIN ANALYZE " + sql;
		executeQuery();
	}
}