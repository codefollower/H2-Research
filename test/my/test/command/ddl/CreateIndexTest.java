package my.test.command.ddl;

import my.test.TestBase;

public class CreateIndexTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateIndexTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS CreateIndexTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateIndexTest (f1 int NOT NULL, f2 int, f3 int)");

		
		//stmt.executeUpdate("CREATE PRIMARY KEY HASH ON CreateIndexTest(f1)");
		stmt.executeUpdate("CREATE PRIMARY KEY HASH IF NOT EXISTS idx0 ON CreateIndexTest(f1)");
		
		stmt.executeUpdate("CREATE UNIQUE HASH INDEX IF NOT EXISTS idx1 ON CreateIndexTest(f2)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx2 ON CreateIndexTest(f3)");

		stmt.executeUpdate("ALTER INDEX idx2 RENAME TO idx22");
		
		stmt.executeUpdate("DROP INDEX IF EXISTS idx22");

		//stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS schema0 AUTHORIZATION sa");
		//stmt.executeUpdate("ALTER INDEX mydb.public.idx0 RENAME TO schema0.idx1");

		//stmt.executeUpdate("ALTER INDEX mydb.public.idx0 RENAME TO idx1");

	}

}
