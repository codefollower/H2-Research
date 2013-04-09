package my.test.command.dml;

import my.test.TestBase;

public class BackupCommandTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new BackupCommandTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "2");
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS BackupCommandTest");
		stmt.executeUpdate("create table IF NOT EXISTS BackupCommandTest(id int, name varchar(500), b boolean)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS BackupCommandTestIndex ON BackupCommandTest(name)");

		stmt.executeUpdate("insert into BackupCommandTest(id, name, b) values(1, 'a1', true)");
		stmt.executeUpdate("insert into BackupCommandTest(id, name, b) values(1, 'b1', true)");
		stmt.executeUpdate("insert into BackupCommandTest(id, name, b) values(2, 'a2', false)");
		stmt.executeUpdate("insert into BackupCommandTest(id, name, b) values(2, 'b2', true)");
		stmt.executeUpdate("insert into BackupCommandTest(id, name, b) values(3, 'a3', false)");
		stmt.executeUpdate("insert into BackupCommandTest(id, name, b) values(3, 'b3', true)");

		sql = "BACKUP TO E:/H2/baseDir/myBackup"; //文件名要加单引号
		sql = "BACKUP TO 'E:/H2/baseDir/myBackup'";
		stmt.executeUpdate(sql);

		sql = "select * from BackupCommandTest";
		executeQuery();
	}
}