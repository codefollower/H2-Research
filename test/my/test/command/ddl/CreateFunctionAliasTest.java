package my.test.command.ddl;

import my.test.TestBase;

public class CreateFunctionAliasTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateFunctionAliasTest().start();
	}

	public void init() throws Exception {
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP ALIAS IF EXISTS my_sqrt");
		stmt.executeUpdate("DROP ALIAS IF EXISTS my_reverse");
		
		//必须是static方法
		stmt.executeUpdate("CREATE ALIAS IF NOT EXISTS my_sqrt DETERMINISTIC FOR \"java.lang.Math.sqrt\"");

		stmt.executeUpdate("CREATE ALIAS IF NOT EXISTS my_reverse AS "
				+ "$$ String reverse(String s) { return new StringBuilder(s).reverse().toString(); } $$");
		
		sql = "select my_sqrt(4.0), my_reverse('abc')";
		executeQuery();
	}
}
