package my.test.command.ddl;

import java.sql.Connection;
import java.sql.SQLException;

import my.test.TestBase;

//找断点条件
//table.getName().equalsIgnoreCase("CreateTriggerTest");
//getName().equalsIgnoreCase("MyTrigger1")
public class CreateTriggerTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateTriggerTest().start();
	}

	public static class MyTrigger implements org.h2.api.Trigger {

		@Override
		public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type)
				throws SQLException {
			System.out.println("schemaName=" + schemaName + " tableName=" + tableName);

		}

		@Override
		public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
			System.out.println("oldRow=" + oldRow + " newRow=" + newRow);
		}

		@Override
		public void close() throws SQLException {
			System.out.println("my.test.command.ddl.CreateTriggerTest.MyInsertTrigger.close()");
		}

		@Override
		public void remove() throws SQLException {
			System.out.println("my.test.command.ddl.CreateTriggerTest.MyInsertTrigger.remove()");
		}

	}
	@Override
	public void init() throws Exception {
		//prop.setProperty("FILE_LOCK", "SERIALIZED");
		prop.setProperty("MULTI_THREADED", "true");
		//prop.setProperty("MVCC", "true");
	}

	//测试org.h2.command.Parser.parseCreateTrigger(boolean)和org.h2.command.ddl.CreateTrigger
	//和org.h2.schema.TriggerObject
	@Override
	public void startInternal() throws Exception {
		conn.setAutoCommit(false);

		stmt.executeUpdate("DROP TABLE IF EXISTS CreateTriggerTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS CreateTriggerTest(id int, name varchar(500))");
		
		//如果是CREATE FORCE TRIGGER，那么在加载触发器类出错时不抛异常，
	    //见org.h2.schema.TriggerObject.setTriggerClassName(Session, String, boolean)
		stmt.executeUpdate("CREATE FORCE TRIGGER IF NOT EXISTS MyTrigger1"
				+ " BEFORE INSERT,UPDATE,DELETE,SELECT,ROLLBACK ON CreateTriggerTest"
				+ " QUEUE 10 NOWAIT CALL \"my.test.command.ddl.CreateTriggerTest$MyTrigger\"");

		stmt.executeUpdate("CREATE TRIGGER IF NOT EXISTS MyTrigger2"
				+ " AFTER INSERT,UPDATE,DELETE,SELECT,ROLLBACK ON CreateTriggerTest FOR EACH ROW"
				+ " QUEUE 10 NOWAIT CALL \"my.test.command.ddl.CreateTriggerTest$MyTrigger\"");
		
		//INSTEAD OF也是BEFORE类型
		stmt.executeUpdate("CREATE TRIGGER IF NOT EXISTS MyTrigger3"
				+ " INSTEAD OF INSERT,UPDATE,DELETE,SELECT,ROLLBACK ON CreateTriggerTest FOR EACH ROW"
				+ " QUEUE 10 NOWAIT CALL \"my.test.command.ddl.CreateTriggerTest$MyTrigger\"");

		//这种语法可查入多条记录
		//null null
		//10 a
		//20 b
		stmt.executeUpdate("INSERT INTO CreateTriggerTest VALUES(DEFAULT, DEFAULT),(10, 'a'),(20, 'b')");

		sql = "select id,name from CreateTriggerTest";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getString(1) + " " + rs.getString(2));
		}

		conn.commit();
	}
}