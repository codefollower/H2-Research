package my.test.command.ddl;

import my.test.TestBase;
import my.test.expression.MedianString;

public class CreateAggregateTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateAggregateTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.execute("DROP AGGREGATE IF EXISTS MEDIAN");
		stmt.execute("CREATE FORCE AGGREGATE IF NOT EXISTS MEDIAN FOR \"" + MedianString.class.getName() + "\"");
		sql = "SELECT MEDIAN(X) FROM SYSTEM_RANGE(1, 9)";

		executeQuery();
	}
}
