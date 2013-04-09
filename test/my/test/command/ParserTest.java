package my.test.command;

import my.test.TestBase;

public class ParserTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new ParserTest().start();
	}

	@Override
	public void init() throws Exception {
		prop.setProperty("DATABASE_TO_UPPER", "false");
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS ParserTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ParserTest(id int, name varchar(500) as '123')");

		//stmt.executeUpdate("set @[set] 10");
		sql = "INSERT INTO ParserTest VALUES(DEFAULT, DEFAULT),(10, 'a'),(20, 'b')";
		stmt.executeUpdate(sql);

		sql = "select id,name from ParserTest";

		initialize();
		read();
		//readTerm();

		//sql = "select id,name from ParserTest;select id,name from ParserTest;select id,name from ParserTest";
		executeQuery();

	}

	void initialize() throws Exception {
		//("DROP /*TA");
		//stmt.executeUpdate("DROP /*TA*/ t");
		//stmt.executeUpdate("CREATE TABLE IF NOT EXISTS A(id int, name varchar(500) as '123')");
		//stmt.executeUpdate("DROP //TA");
		//stmt.executeUpdate("DROP TABLE //TA");
		//		stmt.executeUpdate("DROP TABLE //T\n\rA");
		//		
		//		PreparedStatement ps = conn.prepareStatement("delete top ?2 from ParserTest where id>10 and name=?1");
		//		ps.setString(1, "abc");
		//		ps.setInt(2, 3);
		//		ps.executeUpdate();

		//stmt.executeUpdate("delete from ParserTest where name='abc' and id>0");

		//stmt.executeUpdate("DROP TABLE //single line comment, drop table t");

		//stmt.executeUpdate("CREATE TABLE IF NOT != EXISTS t (name VARCHAR(20));");

		//stmt.executeUpdate("`CREATE TABLE IF NOT``aaaa` != EXISTS t (name VARCHAR(20));");

		//stmt.executeUpdate("90770000000005555559999999999999 CREATE TABLE IF NOT aaaa  != EXISTS t (name VARCHAR(20));");

		//stmt.executeUpdate("0x13ABCDEFabcdef CREATE TABLE IF NOT aaaa  != EXISTS t (name VARCHAR(20));");
	}

	void read() throws Exception {
		//如果DATABASE_TO_UPPER是false，那么0x是错的
    	//如sql = "select id,name from ParserTest where id > 0x2";
    	//只能用大写0X，并且也只能用A-F，
    	//否则像where id > 0X2ab，实际是where id > 0X2，但是ab没有读到，
    	//当判断org.h2.command.Parser.prepareCommand(String)时，(currentTokenType != END)为false就出错
		sql = "select id,name from ParserTest where id > 0x2";
		
		sql = "select id,name from ParserTest where id > 0X2ab";
		
		sql = "select id,name from ParserTest where id > 0X7FFFFFFF123";
		
		sql = "select id,name from ParserTest where id > 123.01";
		
		sql = "select id,name from ParserTest where id > 123.0e-11";
		
		
		sql = "select id,name from ParserTest where id > .123";
		//ALLOW_LITERALS_ALL=2 都允许
    	//ALLOW_LITERALS_NONE=0 说明不允许出现字面值
    	//ALLOW_LITERALS_NUMBERS=1 只允许数字字面值
		stmt.executeUpdate("SET ALLOW_LITERALS 1"); //只允许数字字面值
		sql = "select id,name from ParserTest where name = 'abc'"; //这时就不允许出现字符串字面值了
	}

	public static void testPG_GET_OID(String str) {
		System.out.println("testPG_GET_OID: " + str);
	}

	void readTerm() throws Exception {
		//stmt.executeUpdate("SET @topVariableName=3");
		//stmt.executeUpdate("SET @topVariableName to 3");
		sql = "SELECT @topVariableName";

		sql = "SELECT @topVariableName:=4";
		sql = "SELECT ('Hello', 'World')[2]"; //null, 下标要从0开始，内部会加1
		sql = "SELECT ('Hello', 'World')[1]"; //World

		stmt.executeUpdate("CREATE ALIAS IF NOT EXISTS PG_GET_OID FOR \"my.test.ParserTest.testPG_GET_OID\"");

		sql = "SELECT 'ddd'::REGCLASS";

		//如sql = "SELECT 12::varchar";，表示CAST(12 AS varchar)，把12转成varchar类型
		//这里的r先是12，然后变成CAST函数
		sql = "SELECT 'ddd'::varchar f"; //f是另名了，是('ddd'::varchar)这一项的别名
		sql = "SELECT 'ddd'::varchar";
		sql = "SELECT 12::varchar";

		sql = "SELECT CASE WHEN END CASE"; //错
		sql = "SELECT SET(@v, 11), CASE WHEN @v<10 THEN 'Low' END CASE";
		sql = "SELECT SET(@v, 11), CASE WHEN @v<10 THEN 'Low' ELSE 'High' END";
		sql = "SELECT SET(@v, 1), CASE @v WHEN 0 THEN 'No' WHEN 1 THEN 'One' ELSE 'Some' END";

		sql = "SELECT -1.4e-10";

		//stmt.executeUpdate("delete top @topVariableName from ParserTest where id>10");
		//stmt.executeUpdate("delete top 3 from ParserTest where id>10");
		//stmt.executeUpdate("delete top ?1 from ParserTest where id>10");

		//stmt.executeUpdate("delete top true from ParserTest where id>10");
		//stmt.executeUpdate("update ParserTest set name='1234567890' where id>10");
		//stmt.executeUpdate("delete top rownum from ParserTest where id>10");
		//stmt.executeUpdate("delete top CURRENT_TIME from ParserTest where id>10");
		//stmt.executeUpdate("delete top MAX(id) from ParserTest where id>10");
	}

}
