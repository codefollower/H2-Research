package my.test.constraint;

import my.test.TestBase;

public class ConstraintReferentialTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ConstraintReferentialTest().start();
	}

	public void init() throws Exception {
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS ConstraintReferentialTest2");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ConstraintReferentialTest2 (f1 int not null)");
		
		stmt.executeUpdate("DROP TABLE IF EXISTS ConstraintReferentialTest1");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ConstraintReferentialTest1 (f1 int PRIMARY KEY not null)");
		
		//在Eclipse打断点:constraintName.equalsIgnoreCase("myfk")
		stmt.executeUpdate("ALTER TABLE ConstraintReferentialTest2 "
				+"ADD CONSTRAINT myfk FOREIGN KEY (f1) REFERENCES ConstraintReferentialTest1(f1)");
		
		//当主表存在主键时，引用表可以不指定主表的主键字段
		stmt.executeUpdate("ALTER TABLE ConstraintReferentialTest2 "
                +"ADD CONSTRAINT myfk FOREIGN KEY (f1) REFERENCES ConstraintReferentialTest1");
		stmt.executeUpdate("insert into ConstraintReferentialTest1(f1) values(1)");
		stmt.executeUpdate("insert into ConstraintReferentialTest2(f1) values(1)");
		stmt.executeUpdate("insert into ConstraintReferentialTest2(f1) values(2)");
		
		sql = "select * from ConstraintReferentialTest2";
		executeQuery();
	}
}