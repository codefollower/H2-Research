package my.test.command.dml;

import my.test.TestBase;

public class DeleteTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new DeleteTest().start();
	}
	@Override
	public void init() throws Exception {
		//见org.h2.message.TraceSystem
		//0: OFF， 1: ERROR，2: INFO，3: DEBUG，4: ADAPTER
		//值越大，那么能跟踪的信息就越详细
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "2");
	}
	//测试org.h2.command.Parser.parseDelete()
	//org.h2.command.dml.Delete
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS DeleteTest");
		stmt.executeUpdate("create table IF NOT EXISTS DeleteTest(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS DeleteTestIndex ON DeleteTest(name)");

		stmt.executeUpdate("insert into DeleteTest(id, name, b) values(1, 'a1', true)");
		stmt.executeUpdate("insert into DeleteTest(id, name, b) values(1, 'b1', true)");
		stmt.executeUpdate("insert into DeleteTest(id, name, b) values(2, 'a2', false)");
		stmt.executeUpdate("insert into DeleteTest(id, name, b) values(2, 'b2', true)");
		stmt.executeUpdate("insert into DeleteTest(id, name, b) values(3, 'a3', false)");
		stmt.executeUpdate("insert into DeleteTest(id, name, b) values(3, 'b3', true)");

		sql = "delete top 3 from DeleteTest";
		sql = "delete top 3 from DeleteTest where name='a1'";
		sql = "delete top 3 from DeleteTest where 'a1'>name";
		sql = "delete top 3 from DeleteTest where name = null";
		sql = "delete top 3 from DeleteTest where name != null";
		sql = "delete top 3 from DeleteTest where name > null";
		
		sql = "delete from DeleteTest where name > 'b1'";
		sql = "delete from DeleteTest where id>2";
		sql = "delete from DeleteTest where 3<2";
		sql = "delete from DeleteTest where b";
		sql = "delete from DeleteTest where 3>2";
		sql = "delete from DeleteTest limit 0"; //limit 0不删除任何行
		stmt.executeUpdate(sql);

//		sql = "delete top 3 from DeleteTest where name > ?";
//		ps = conn.prepareStatement(sql);
//		ps.setString(1, "b1");
//		ps.executeUpdate();

		sql = "select * from DeleteTest";
		executeQuery();
	}
}
