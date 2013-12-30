package my.test.value;

import my.test.TestBase;

public class ValueDateTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ValueDateTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS ValueDateTest");
		stmt.executeUpdate("create table IF NOT EXISTS ValueDateTest(id int, name date, f2 time)");

		stmt.executeUpdate("insert into ValueDateTest(id, name, f2) values(1, CURRENT_DATE, CURRENT_TIME)");
		sql = "select * from ValueDateTest";
		executeQuery();
	}

}
