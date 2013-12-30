package my.test.command.ddl;

import my.test.TestBase;

public class SetCommentTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new SetCommentTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS SetCommentTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS SetCommentTest (f1 int)");
		
		stmt.executeUpdate("DROP ROLE IF EXISTS myrole");
		stmt.executeUpdate("CREATE ROLE IF NOT EXISTS myrole");

		stmt.executeUpdate("COMMENT ON COLUMN mydb.public.SetCommentTest.f1 IS 'column comment'");
		stmt.executeUpdate("COMMENT ON TABLE public.SetCommentTest IS 'table comment'");
		
		stmt.executeUpdate("COMMENT ON ROLE myrole IS 'role comment'");
		
	}
}
