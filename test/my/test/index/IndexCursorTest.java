package my.test.index;

import my.test.TestBase;

//找断点条件
//table.getName().equalsIgnoreCase("IndexCursorTest");
public class IndexCursorTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new IndexCursorTest().start();
	}

	//重点测试find
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS IndexCursorTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexCursorTest(id int not null, name varchar(500) not null, address varchar(500))");
		stmt.executeUpdate("drop index IF EXISTS idx_name");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON IndexCursorTest(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON IndexCursorTest(name desc)");

		stmt.executeUpdate("drop index IF EXISTS idx_name2");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name2 ON IndexCursorTest(name, id)");

		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(10, '1000000001', 'a')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(20, '1000000002', 'b')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(30, '1000000003', 'c')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(40, '1000000004', 'd')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(50, '1000000005', 'e')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(60, '1000000006', 'f')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(70, '1000000007', 'g')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(80, '1000000008', 'h')");
		stmt.executeUpdate("insert into IndexCursorTest(id, name, address) SORTED values(90, '1000000009', 'i')");

		sql = "select * from IndexCursorTest where 2>3";
		sql = "select * from IndexCursorTest where name in ('1000000004', '1000000006')";
		sql = "select * from IndexCursorTest where name in (select '1000000004')";
		sql = "select * from IndexCursorTest where name > address"; //不会建立索引条件，因为两边都是字段
		sql = "select * from IndexCursorTest where name>='1000000004'";
		//下面这两条一样
		sql = "select * from IndexCursorTest where name>='1000000004' and name<='1000000006'";
		sql = "select * from IndexCursorTest where name between '1000000004' and '1000000006'";

		//索引条件只有一个name<='1000000006'
		//前面的因为是or，所以不建立索引条件
		sql = "select * from IndexCursorTest where (name>='1000000004' or name>'1000000002') and name<='1000000006'";

		//这个就有三个索引条件，不过前两个合并成name>'1000000004'
		sql = "select * from IndexCursorTest where name>='1000000002' and name>'1000000004' and name<='1000000006'";

		sql = "select * from IndexCursorTest where id in (10, 20)";
		//sql = "select * from IndexCursorTest";
		executeQuery();

	}
}