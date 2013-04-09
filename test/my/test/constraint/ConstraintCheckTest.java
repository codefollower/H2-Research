package my.test.constraint;

import my.test.TestBase;

public class ConstraintCheckTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ConstraintCheckTest().start();
	}

	public void init() throws Exception {
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS ConstraintCheckTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ConstraintCheckTest (f1 int)");
		
		stmt.executeUpdate("ALTER TABLE ConstraintCheckTest ADD CONSTRAINT mycheck CHECK (f1 > 1)");
		stmt.executeUpdate("insert into ConstraintCheckTest(f1) values(1)");
	}
}