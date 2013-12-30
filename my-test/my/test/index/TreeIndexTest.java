package my.test.index;

import my.test.TestBase;

public class TreeIndexTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new TreeIndexTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("MVCC", "true");
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS TreeIndexTest");
		stmt.executeUpdate("CREATE LOCAL TEMPORARY TABLE IF NOT EXISTS TreeIndexTest(id int not null, name varchar(500) not null, address varchar(500))");
		stmt.executeUpdate("DROP INDEX IF EXISTS idx_name");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON TreeIndexTest(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON TreeIndexTest(name desc)");

		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(10, '1000000001', 'a')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(20, '1000000002', 'b')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(30, '1000000003', 'c')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(40, '1000000004', 'd')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(50, '1000000005', 'e')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(60, '1000000006', 'f')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(70, '1000000007', 'g')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(80, '1000000008', 'h')");
		stmt.executeUpdate("insert into TreeIndexTest(id, name, address) SORTED values(90, '1000000009', 'i')");

		sql = "select * from TreeIndexTest";
		executeQuery();
	}
}
