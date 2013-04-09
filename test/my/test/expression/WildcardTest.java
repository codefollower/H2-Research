package my.test.expression;

import my.test.TestBase;

public class WildcardTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new WildcardTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS WildcardTest");
		stmt.executeUpdate("create table IF NOT EXISTS WildcardTest(id int, name varchar(500))");

		stmt.executeUpdate("insert into WildcardTest(id, name) values(1, 'a1')");
		stmt.executeUpdate("insert into WildcardTest(id, name) values(1, 'b1')");
		stmt.executeUpdate("insert into WildcardTest(id, name) values(2, 'a2')");
		stmt.executeUpdate("insert into WildcardTest(id, name) values(2, 'b2')");
		stmt.executeUpdate("insert into WildcardTest(id, name) values(3, 'a3')");
		stmt.executeUpdate("insert into WildcardTest(id, name) values(3, 'b3')");

		sql = "select * from WildcardTest";
		sql = "select WildcardTest.* from WildcardTest";
		sql = "select public.WildcardTest.* from WildcardTest";
		executeQuery();

		System.out.println(quoteIdentifier("abc"));
		System.out.println(quoteIdentifier("a\"bc"));
	}

	public static String quoteIdentifier(String s) {
		int length = s.length();
		StringBuilder buff = new StringBuilder(length + 2);
		buff.append('\"');
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			if (c == '"') {
				buff.append(c);
			}
			buff.append(c);
		}
		return buff.append('\"').toString();
	}

}
