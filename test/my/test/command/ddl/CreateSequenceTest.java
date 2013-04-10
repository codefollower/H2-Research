package my.test.command.ddl;

import my.test.TestBase;

public class CreateSequenceTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new CreateSequenceTest().start();
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP SEQUENCE IF EXISTS myseq");
		//加了BELONGS_TO_TABLE就删不掉了
		//stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS myseq START WITH 1000 INCREMENT BY 1 CACHE 20 BELONGS_TO_TABLE");
		stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS myseq START WITH 1000 INCREMENT BY 1 CACHE 20");

		sql = "select myseq.CURRVAL, myseq.NEXTVAL";
		executeQuery();
	}
}
