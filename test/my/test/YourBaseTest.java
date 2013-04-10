package my.test;


public class YourBaseTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new YourBaseTest().start();
	}
	
	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
	}

	//测试org.h2.result.LocalResult
	//org.h2.result.ResultRemote
	//org.h2.command.dml.Select.queryWithoutCache(int, ResultTarget)
	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("drop table IF EXISTS YourBaseTest CASCADE");
//		stmt.executeUpdate("create table IF NOT EXISTS YourBaseTest(id int, name varchar(500), b boolean)");
//		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS YourBaseTestIndex ON YourBaseTest(name)");
//
//		stmt.executeUpdate("insert into YourBaseTest(id, name, b) values(10, 'a1', true)");
//		stmt.executeUpdate("insert into YourBaseTest(id, name, b) values(20, 'b1', true)");
//		stmt.executeUpdate("insert into YourBaseTest(id, name, b) values(30, 'a2', false)");
//		stmt.executeUpdate("insert into YourBaseTest(id, name, b) values(40, 'b2', true)");
//		stmt.executeUpdate("insert into YourBaseTest(id, name, b) values(50, 'a3', false)");
//		stmt.executeUpdate("insert into YourBaseTest(id, name, b) values(60, 'b3', true)");
//      stmt.executeUpdate("insert into YourBaseTest(id, name, b) values(70, 'b3', true)");
//		stmt.execute("insert into YourBaseTest(id, name, b) values(70, 'b3', true)");
//
//		sql = "select * from YourBaseTest";
//		stmt.setFetchSize(2);
//		//executeQuery();
//
//		rs = stmt.executeQuery(sql);
//		int n = rs.getMetaData().getColumnCount();
//		while (rs.next()) {
//			for (int i = 1; i <= n; i++) {
//				System.out.print(rs.getString(i) + " ");
//			}
//			System.out.println();
//		}
	}
}