package my.test.command.dml;

import my.test.TestBase;

public class ScriptCommandTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ScriptCommandTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		sql = "BACKUP TO E:/H2/baseDir/myBackup"; //文件名要加单引号
		sql = "BACKUP TO 'E:/H2/baseDir/myBackup'";
		sql = "SCRIPT NODATA"; //生成各种Create SQL，此命令返回结果集，所以要用executeQuery
		executeQuery();
	}
}