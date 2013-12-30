package my.test.command.ddl;

import my.test.TestBase;

public class AlterIndexRenameTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new AlterIndexRenameTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS AlterIndexRenameTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS AlterIndexRenameTest (f1 int)");

		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx0 ON AlterIndexRenameTest(f1)");

		//stmt.executeUpdate("ALTER INDEX idx0 RENAME TO idx1");

		//stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS schema0 AUTHORIZATION sa");
		//stmt.executeUpdate("ALTER INDEX mydb.public.idx0 RENAME TO schema0.idx1");

		stmt.executeUpdate("ALTER INDEX mydb.public.idx0 RENAME TO idx1");

	}

}
