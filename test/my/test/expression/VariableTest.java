package my.test.expression;

import my.test.TestBase;

public class VariableTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new VariableTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("SET @topVariableName=3");
		sql = "select @topVariableName";
		sql = "select @topVariableName:=2"; //这样就变成SET函数了
		
		sql = "select @nullVariableName"; //不存在的变量名，此时值为null
		executeQuery();
	}
}
