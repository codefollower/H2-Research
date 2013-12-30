package my.test.expression;

import my.test.TestBase;

public class JavaAggregateTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new JavaAggregateTest().start();
	}

	//测试org.h2.expression.JavaAggregate
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS JavaAggregateTest");
		stmt.executeUpdate("create table IF NOT EXISTS JavaAggregateTest(id int, name varchar(500))");

		stmt.executeUpdate("insert into JavaAggregateTest(id, name) values(1, 'a1')");
		stmt.executeUpdate("insert into JavaAggregateTest(id, name) values(1, 'b1')");
		stmt.executeUpdate("insert into JavaAggregateTest(id, name) values(2, 'a2')");
		stmt.executeUpdate("insert into JavaAggregateTest(id, name) values(2, 'b2')");
		stmt.executeUpdate("insert into JavaAggregateTest(id, name) values(3, 'a3')");
		stmt.executeUpdate("insert into JavaAggregateTest(id, name) values(3, 'b3')");

		stmt.execute("DROP AGGREGATE IF EXISTS MEDIAN");
		stmt.execute("CREATE AGGREGATE IF NOT EXISTS MEDIAN FOR \"" + MedianString.class.getName() + "\"");
		sql = "SELECT MEDIAN(X) FROM SYSTEM_RANGE(1, 9)";
		//sql = "SELECT MEDIAN(id) FROM JavaAggregateTest";

		executeQuery();
	}
}
