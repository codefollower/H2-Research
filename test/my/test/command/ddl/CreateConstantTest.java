package my.test.command.ddl;

import my.test.TestBase;

public class CreateConstantTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateConstantTest().start();
	}

	public void init() throws Exception {
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP CONSTANT IF EXISTS myconstant");
		stmt.executeUpdate("CREATE CONSTANT IF NOT EXISTS myconstant VALUE 10");
		
		sql = "select myconstant";
		executeQuery();
	}
}
