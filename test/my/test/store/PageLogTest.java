package my.test.store;

import my.test.TestBase;

//ÕÒ¶ÏµãÌõ¼þindexName.equalsIgnoreCase("idx_name"); getName().equalsIgnoreCase("idx_name");
//index.getName().equalsIgnoreCase("idx_name");
//tableFilter.getTable().getName().equalsIgnoreCase("PageLogTest");
public class PageLogTest extends TestBase {
	public static void main(String[] args) throws Exception {
		PageLogTest test = new PageLogTest();
		test.start();
		test.stop();
	}
	@Override
	public void init() throws Exception {
		//prop.setProperty("PAGE_SIZE", "128");
	}
	@Override
	public void startInternal() throws Exception {
		conn.setAutoCommit(false);
		
		stmt.executeUpdate("DROP TABLE IF EXISTS PageLogTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS PageLogTest(id int not null, name varchar(500) not null, address varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON PageLogTest(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON PageLogTest(name desc)");
		
		stmt.executeUpdate("SET MAX_LOG_SIZE 10"); //10K
		
						long ii = 1000000001L;
						for (int i = 1; i <= 500; i++) {
							//stmt.executeUpdate("insert into PageLogTest(id, name) values(" + i + ", '" + s + "abcdef1234')");
							//stmt.executeUpdate("insert into PageLogTest(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
							stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
							if(i%100==0)
								stmt.executeUpdate("CHECKPOINT");
							
							if(i%200==0) {
								stmt.executeUpdate("PREPARE COMMIT newTransactionName");
								//conn.commit();
							}
								
							
							
							ii++;
						}
		//				//stmt.executeUpdate("insert into PageLogTest(id, name) values(" + 101 + ", '" + s + "abcdef1234')");
						//conn.rollback();
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(10, '1000000001', 'a')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(20, '1000000002', 'b')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(30, '1000000003', 'c')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(40, '1000000004', 'd')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(50, '1000000005', 'e')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(60, '1000000006', 'f')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(70, '1000000007', 'g')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(80, '1000000008', 'h')");
//		stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(90, '1000000009', 'i')");
		//
		//		//stmt.executeUpdate("insert into PageLogTest(id, name, address) SORTED values(30, '1000000003', 'c')");
		//		//stmt.executeUpdate("delete from PageLogTest where id=40");
		//		stmt.executeUpdate("delete from PageLogTest where id=30");
		//conn.commit();

		//		stmt.executeUpdate("delete from PageLogTest where id=2");
		//		stmt.executeUpdate("delete from PageLogTest where id>300");
		//		stmt.executeUpdate("insert into PageLogTest(id, name, address) values(" + 300 + ", '" + 300 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into PageLogTest(id, name, address) values(" + 301 + ", '" + 302 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into PageLogTest(id, name, address) values(" + 2 + ", '" + 2 + "abcdef1234', 'zzz')");
		//
		//		stmt.executeUpdate("update PageLogTest set name='1234567890' where id>10");

		

		sql = "select name,id from PageLogTest where name>='1000000004'";
		sql = "select name,id from PageLogTest where name between '1000000004' and '1000000006'";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getString(1));
		}

		//stmt.executeUpdate("TRUNCATE TABLE PageLogTest");
		//stmt.executeUpdate("drop index IF EXISTS idx_name");

		//conn.commit();
		//conn.rollback();
	}
}