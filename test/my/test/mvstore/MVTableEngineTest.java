package my.test.mvstore;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;

import my.test.TestBase;

public class MVTableEngineTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new MVTableEngineTest().start();
	}
	
	public void init() throws Exception {
		//url = "jdbc:h2:mem:mydb";
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS MVTableEngineTest CASCADE");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS MVTableEngineTest(id int, name varchar(500), b boolean, b2 clob) "
				+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"");
		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS MVTableEngineTestIndex ON MVTableEngineTest(name)");

		conn.setAutoCommit(false);
//		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(10, 'a1', true)");
//		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(20, 'b1', true)");
//		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(30, 'a2', false)");
//		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(40, 'b2', true)");
//		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(50, 'a3', false)");
//		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(60, 'b3', true)");
//		stmt.executeUpdate("insert into MVTableEngineTest(id, name, b) values(70, 'b3', true)");
//		
		//for(int i=0;i<5000;i++) {
		PreparedStatement prep = conn.prepareStatement("insert into MVTableEngineTest(id, b2) values(1, ?)");
        prep.setAsciiStream(1,  new ByteArrayInputStream(new byte[4097]));
        prep.execute();
        prep.close();
        
        stmt.executeUpdate("delete from MVTableEngineTest where id=1");
		//}

		conn.commit();
		sql = "select * from MVTableEngineTest";

		//见org.h2.index.Index.findNext(Session, SearchRow, SearchRow)中的注释
		sql = "select distinct name from MVTableEngineTest";
		executeQuery();
	}
}
