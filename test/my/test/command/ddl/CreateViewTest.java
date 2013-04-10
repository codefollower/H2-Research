package my.test.command.ddl;

import my.test.TestBase;

public class CreateViewTest extends TestBase {
	public static void main(String[] args) throws Exception {

		new CreateViewTest().start();
		System.out.println(Integer.bitCount(3));
		System.out.println(Integer.bitCount(1));
		System.out.println(Integer.bitCount(2));
		System.out.println(Integer.bitCount(7));
	}

	//测试org.h2.command.Parser.parseCreateView(boolean, boolean)
	//org.h2.command.ddl.CreateView
	@Override
	public void startInternal() throws Exception {
		//stmt.executeUpdate("drop table IF EXISTS CreateViewTest CASCADE");
		stmt.executeUpdate("create table IF NOT EXISTS CreateViewTest(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS CreateViewTestIndex ON CreateViewTest(name)");

		stmt.executeUpdate("insert into CreateViewTest(id, name, b) values(1, 'a1', true)");
		stmt.executeUpdate("insert into CreateViewTest(id, name, b) values(1, 'b1', true)");
		stmt.executeUpdate("insert into CreateViewTest(id, name, b) values(2, 'a2', false)");
		stmt.executeUpdate("insert into CreateViewTest(id, name, b) values(2, 'b2', true)");
		stmt.executeUpdate("insert into CreateViewTest(id, name, b) values(3, 'a3', false)");
		stmt.executeUpdate("insert into CreateViewTest(id, name, b) values(3, 'b3', true)");

		//stmt.executeUpdate("DROP VIEW IF EXISTS my_view");
		sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view COMMENT IS 'my view'(f1,f2) " //
				+ "AS SELECT id,name FROM CreateViewTest";

		sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1,f2) " //
				+ "AS SELECT id,name FROM CreateViewTest";

		//select字段个数比view字段多的情况，多出来的按select字段原来的算
		//这里实际是f1、name
		sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1) " //
				+ "AS SELECT id,name FROM CreateViewTest";

		//select字段个数比view字段少的情况，view中少的字段被忽略
		//这里实际是f1，而f2被忽略了，也不提示错误
		sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
				+ "AS SELECT id FROM CreateViewTest";

		//不管加不加FORCE，跟上面也一样
		sql = "CREATE OR REPLACE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
				+ "AS SELECT id FROM CreateViewTest";

		sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1,f2) " //
				+ "AS SELECT id,name FROM CreateViewTest";

		stmt.executeUpdate("CREATE OR REPLACE FORCE VIEW view1 AS SELECT f1 FROM my_view");
		stmt.executeUpdate("CREATE OR REPLACE FORCE VIEW view2 AS SELECT f2 FROM my_view");

		//		sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1,f2) " //
		//			+ "AS SELECT top 2 id,name FROM CreateViewTest order by id";
		//		
		//如果是这种情况，接下来要查视图时也要按CreateViewTest的字段名来查
		//sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view " //
		//		+ "AS SELECT id,name FROM CreateViewTest";

		//目前不支持参数:
		//org.h2.jdbc.JdbcSQLException: Feature not supported: "parameters in views"; SQL statement:
		//sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view (f1,f2) AS SELECT id,name FROM CreateViewTest where id=?";
		//		ps = conn.prepareStatement(sql);
		//		ps.setInt(1, 2);
		//		ps.executeUpdate();
		stmt.executeUpdate(sql);

		sql = "select * from my_view where f1 > 2";
		sql = "select * from my_view where f2 > 'b1'";
		sql = "select * from my_view where f2 between 'b1' and 'b2'";

		sql = "select * from my_view where f1=2 and f2 between 'b1' and 'b2'";

		//		sql = "select name from (select id,name from CreateViewTest where id=? and name=?) where name='b2'";
		//		ps = conn.prepareStatement(sql);
		//		ps.setInt(1, 2);
		//		ps.setString(2, "b2");
		//		ps.executeQuery();

		//sql = "select * from CreateViewTest";

		//测试org.h2.command.Parser.parserWith()
		//stmt.executeUpdate("CREATE LOCAL TEMPORARY TABLE IF NOT EXISTS my_tmp_table(f1 int)");
		//stmt.executeUpdate("DROP VIEW IF EXISTS my_tmp_table");
		//stmt.executeUpdate("CREATE OR REPLACE FORCE VIEW my_tmp_table AS SELECT f2 FROM my_view");
		//sql = "WITH RECURSIVE my_tmp_table(f1,f2) AS(select id,name from CreateViewTest) select f1, f2 from my_tmp_table";
		//sql = "WITH my_tmp_table(f1,f2) AS(select id,name from CreateViewTest) select f1, f2 from my_tmp_table";

		//AS里面必须是UNION ALL
		sql = "WITH RECURSIVE my_tmp_table(f1,f2) AS(select id,name from CreateViewTest UNION ALL select 1, 2)"
				+ "select f1, f2 from my_tmp_table";

		sql = "WITH RECURSIVE my_tmp_table(f1,f2) AS(select id,name from CreateViewTest UNION ALL select id,name from CreateViewTest)"
				+ "select f1, f2 from my_tmp_table";

		//必须在from后面加括号，此时from后面的被认为是一个临时视图
		sql = "select f1, f2 from (select id,name from CreateViewTest)"; //f1,f2找不到
		sql = "select id,name from (select id,name from CreateViewTest)";
		
		//这条不会使得parameters.size>0
		sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view2(f1,f2) " //
				+ "AS select id,name from (select id,name from CreateViewTest) where id=? and name=?";
		
		//这条可以
//		sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view2(f1,f2) " //
//			+ "AS select id,name from (select id,name from CreateViewTest where id=? and name=?)";
//
//		ps = conn.prepareStatement(sql);
//		ps.setInt(1, 2);
//		ps.setString(2, "b2");
//		ps.executeUpdate();
		
		sql = "select id,name from (select id,name from CreateViewTest where id=? and name=?)";
		ps = conn.prepareStatement(sql);
		ps.setInt(1, 2);
		ps.setString(2, "b2");
		rs = ps.executeQuery();
		printResultSet(rs);

		sql = "select * from my_view2";
		sql = "select * from my_view2 where f1=2 and f2 between 'b1' and 'b2'";
		
		
		//stmt.setFetchSize(2);
		//executeQuery();
	}
}