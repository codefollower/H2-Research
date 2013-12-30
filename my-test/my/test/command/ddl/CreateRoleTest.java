package my.test.command.ddl;

import my.test.TestBase;

public class CreateRoleTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateRoleTest().start();
	}

	//测试org.h2.command.Parser.parseCreateRole()
	//和org.h2.command.ddl.CreateRole、org.h2.engine.Role
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS CreateRoleTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateRoleTest (f1 int)");

		stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole1");

		stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole0");
		stmt.executeUpdate("GRANT INSERT ON CreateRoleTest TO myrole0");

		stmt.executeUpdate("GRANT SELECT,DELETE ON CreateRoleTest TO myrole1");
		stmt.executeUpdate("GRANT myrole1 TO myrole0");

		stmt.executeUpdate("CREATE USER IF NOT EXISTS sa1 PASSWORD 'abc' ADMIN");

		stmt.executeUpdate("GRANT myrole1 TO sa1");

		stmt.executeUpdate("DROP ROLE IF EXISTS myrole1");

		stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole3");
		stmt.executeUpdate("DROP ROLE IF EXISTS myrole3");
		stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole4");
		stmt.executeUpdate("DROP ROLE IF EXISTS myrole4");

	}

}
