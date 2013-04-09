package my.test.command.ddl;

import my.test.TestBase;

public class CreateUserDataTypeTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateUserDataTypeTest().start();
	}

	//测试org.h2.command.Parser.parseCreateUserDataType()
	//和org.h2.command.ddl.CreateUserDataType、org.h2.engine.UserDataType
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP DOMAIN IF EXISTS EMAIL");
		//VALUE是CREATE DOMAIN语句的默认临时列名
		stmt.executeUpdate("CREATE DOMAIN IF NOT EXISTS EMAIL AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");
		stmt.executeUpdate("CREATE TYPE IF NOT EXISTS EMAIL AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");
		stmt.executeUpdate("CREATE DATATYPE IF NOT EXISTS EMAIL AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");
		
		//stmt.executeUpdate("CREATE DATATYPE IF NOT EXISTS int AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");
		
		//从第二个名称开始的都是隐藏类型的，如下面的int
        //new String[]{"INTEGER", "INT", "MEDIUMINT", "INT4", "SIGNED"}
        //隐藏类型在用户在数据库中没有建表时可以覆盖
        //如CREATE DATATYPE IF NOT EXISTS int AS VARCHAR(255)
        //但是非隐藏类型就不能覆盖
        //如CREATE DATATYPE IF NOT EXISTS integer AS VARCHAR(255)
		//stmt.executeUpdate("CREATE DATATYPE IF NOT EXISTS integer AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");
	}
}
