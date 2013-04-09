package my.test.constraint;

import my.test.TestBase;

public class ConstraintUniqueTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ConstraintUniqueTest().start();
	}

	public void init() throws Exception {
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS ConstraintUniqueTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ConstraintUniqueTest (f1 int not null)");
		
		sql = "ALTER TABLE ConstraintUniqueTest ADD CONSTRAINT IF NOT EXISTS c4 COMMENT IS 'haha4' " +
				"UNIQUE KEY INDEX myunique(f1) NOCHECK";
        stmt.executeUpdate(sql);
		
		//ÔÚEclipse´ò¶Ïµã:constraintName.equalsIgnoreCase("mypk")
		stmt.executeUpdate("ALTER TABLE ConstraintUniqueTest ADD CONSTRAINT mypk PRIMARY KEY HASH (f1)");
		stmt.executeUpdate("insert into ConstraintUniqueTest(f1) values(1)");
	}
}