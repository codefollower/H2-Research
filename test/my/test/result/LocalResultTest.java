package my.test.result;

import my.test.TestBase;

public class LocalResultTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new LocalResultTest().start();
	}

	public void init() throws Exception {
		prop.setProperty("MAX_MEMORY_ROWS", "3");
		prop.setProperty("MAX_MEMORY_ROWS_DISTINCT", "3");
	}

	//测试org.h2.result.LocalResult
	//org.h2.result.ResultRemote
	//org.h2.command.dml.Select.queryWithoutCache(int, ResultTarget)
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS LocalResultTest CASCADE");
		stmt.executeUpdate("create table IF NOT EXISTS LocalResultTest(id int, name varchar(500), b boolean)");
		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS LocalResultTestIndex ON LocalResultTest(name)");

		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into LocalResultTest(id, name, b) values(70, 'b3', true)");

		sql = "select * from LocalResultTest";

		sql = "select * from LocalResultTest order by name";

		sql = "select * from LocalResultTest limit 5 offset 3";

		sql = "select distinct * from LocalResultTest limit 5 offset 2";
		sql = "select distinct * from LocalResultTest";

		sql = "select distinct * from LocalResultTest order by name";

		//子查询必须是单列
		//sql = "select * from LocalResultTest where id in (select id,name from LocalResultTest where id>30)";
		//sql = "select * from LocalResultTest where id in (select id from LocalResultTest where id>30)";
		stmt.setFetchSize(2);
		//stmt.setMaxRows(2);
		//executeQuery();

		rs = stmt.executeQuery(sql);
		int n = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
		}

		//下面两条查询虽一样，但是因为使用到了ResultExternal，所以没有缓存，还是不可以重复使用相同的结果集
		sql = "select id,name from LocalResultTest";
		//executeQuery();
		stmt.executeQuery(sql);

//		stmt = conn.createStatement();
//
//		sql = "select id,name from LocalResultTest";
//		executeQuery();
//
//		sql = "select id,name from LocalResultTest";
//		executeQuery();
	}
}