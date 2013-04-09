package my.test.command.ddl;

import my.test.TestBase;

public class CreateSchemaTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateSchemaTest().start();
	}

	public void init() throws Exception {
		//prop.setProperty("user", "SA2");
		//prop.setProperty("password", "78");
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP SCHEMA IF EXISTS schema0");
		stmt.executeUpdate("DROP SCHEMA IF EXISTS schema1");
		stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS schema0 AUTHORIZATION sa");

		stmt.executeUpdate("ALTER SCHEMA mydb.public.schema0 RENAME TO schema1");

		stmt.executeUpdate("DROP SCHEMA IF EXISTS public");
	}
}
