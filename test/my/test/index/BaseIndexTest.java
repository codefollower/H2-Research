package my.test.index;

import my.test.TestBase;

public class BaseIndexTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new BaseIndexTest().start();
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
		stmt.executeUpdate("drop table IF EXISTS BaseIndexTest");
		//stmt.executeUpdate("create table IF NOT EXISTS BaseIndexTest(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("create table IF NOT EXISTS BaseIndexTest(id int not null, name varchar(500) not null, b boolean)");
		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS BaseIndexTestIndexId ON BaseIndexTest(id, name)");
		
		//stmt.executeUpdate("CREATE Unique INDEX IF NOT EXISTS BaseIndexTestIndexId ON BaseIndexTest(id, name)");
		
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS BaseIndexTestIndexId ON BaseIndexTest(id, name, b)");

		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS BaseIndexTestIndexName ON BaseIndexTest(name)");
		
		stmt.executeUpdate("CREATE PRIMARY KEY IF NOT EXISTS PK_ID ON BaseIndexTest(id)");
		stmt.executeUpdate("CREATE PRIMARY KEY IF NOT EXISTS PK_NAME ON BaseIndexTest(name)");

		stmt.executeUpdate("insert into BaseIndexTest(id, name, b) values(1, 'a1', true)");
		stmt.executeUpdate("insert into BaseIndexTest(id, name, b) values(1, 'b1', true)");
		stmt.executeUpdate("insert into BaseIndexTest(id, name, b) values(2, 'a2', false)");
		stmt.executeUpdate("insert into BaseIndexTest(id, name, b) values(2, 'b2', true)");
		stmt.executeUpdate("insert into BaseIndexTest(id, name, b) values(3, 'a3', false)");
		stmt.executeUpdate("insert into BaseIndexTest(id, name, b) values(3, 'b3', true)");

		//stmt.executeUpdate("SET OPTIMIZE_REUSE_RESULTS 0");

		sql = "delete top 3 from BaseIndexTest";
		sql = "delete top 3 from BaseIndexTest where name='a1'";
		sql = "delete top 3 from BaseIndexTest where 'a1'>name";
		sql = "delete top 3 from BaseIndexTest where name = null";
		sql = "delete top 3 from BaseIndexTest where name != null";
		sql = "delete top 3 from BaseIndexTest where name > null";

		sql = "delete from BaseIndexTest where name > 'b1'";
		sql = "delete from BaseIndexTest where id>2";
		sql = "delete from BaseIndexTest where 3<2";
		sql = "delete from BaseIndexTest where b";
		sql = "delete from BaseIndexTest where 3>2";
		sql = "delete from BaseIndexTest limit 0"; //limit 0不删除任何行
		sql = "delete from BaseIndexTest where id=2 and name>'a1'";
		sql = "delete from BaseIndexTest where name>'a1' and id=2 ";
		
		sql = "delete from BaseIndexTest where name='a1' and id=2 ";
		sql = "delete from BaseIndexTest where name='a1' and id=2 and b=true";
		
		sql = "delete from BaseIndexTest where 3<2 and id=1";
		
		sql = "delete from BaseIndexTest where id<1 and name<='a1'";
		
		sql = "delete from BaseIndexTest where id<1 and id<2";
		stmt.executeUpdate(sql);

	}

}
