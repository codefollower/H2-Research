package my.test.command.ddl;

import my.test.TestBase;

public class AlterSchemaRenameTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new AlterSchemaRenameTest().start();
	}

	@Override
	public void startInternal() throws Exception {
//		stmt.executeUpdate("DROP TABLE IF EXISTS AlterSchemaRenameTest");
//		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS AlterSchemaRenameTest (f1 int)");
//
//		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx0 ON AlterSchemaRenameTest(f1)");

		//stmt.executeUpdate("ALTER INDEX idx0 RENAME TO idx1");

		stmt.executeUpdate("DROP SCHEMA IF EXISTS schema0");
		stmt.executeUpdate("DROP SCHEMA IF EXISTS schema1");
		stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS schema0 AUTHORIZATION sa");

		stmt.executeUpdate("ALTER SCHEMA mydb.public.schema0 RENAME TO schema1");

	}

}
