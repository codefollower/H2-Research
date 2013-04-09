package my.test.command.dml;

import my.test.TestBase;

public class ExecuteProcedureTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ExecuteProcedureTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "2");
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS ExecuteProcedureTest");
		stmt.executeUpdate("create table IF NOT EXISTS ExecuteProcedureTest(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS ExecuteProcedureTestIndex ON ExecuteProcedureTest(name)");

		stmt.executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(1, 'a1', true)");
		stmt.executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(1, 'b1', true)");
		stmt.executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(2, 'a2', false)");
		stmt.executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(2, 'b2', true)");
		stmt.executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(3, 'a3', false)");
		stmt.executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(3, 'b3', true)");

		sql = "PREPARE mytest (int, varchar2, boolean) AS insert into ExecuteProcedureTest(id, name, b) values(?, ?, ?)";
		stmt.executeUpdate(sql);

		sql = "EXECUTE mytest(4, 'b4', true)";
		stmt.executeUpdate(sql);

		sql = "select * from ExecuteProcedureTest";
		executeQuery();
	}
}
