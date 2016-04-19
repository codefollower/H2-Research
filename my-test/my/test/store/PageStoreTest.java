package my.test.store;

import my.test.TestBase;
import org.h2.engine.ConnectionInfo;
import org.h2.engine.Constants;
import org.h2.engine.Database;
import org.h2.store.PageStore;
import org.h2.store.fs.FileUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

//找断点条件indexName.equalsIgnoreCase("idx_name"); getName().equalsIgnoreCase("idx_name");
//index.getName().equalsIgnoreCase("idx_name");
//tableFilter.getTable().getName().equalsIgnoreCase("PageStoreTest");
public class PageStoreTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new PageStoreTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("TRACE_LEVEL_FILE", "10");

		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");

		//prop.setProperty("ACCESS_MODE_DATA", "r");

		//PAGE_SIZE参数必须在64到32768(32K)之间，即64<=PAGE_SIZE<=32768，并且是2的n次幂(n>=0)
		//prop.setProperty("PAGE_SIZE", "32769");
		//prop.setProperty("PAGE_SIZE", "1024");

		//prop.setProperty("FILE_LOCK", "FS");
	}

	@Override
	public void startInternal() throws Exception {
		//stmt.executeUpdate("SET DB_CLOSE_DELAY -1");
		stmt.executeUpdate("DROP TABLE IF EXISTS PageStoreTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS PageStoreTest(id int not null, name varchar(500) not null, address varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON PageStoreTest(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON PageStoreTest(name desc)");

		sql = "select * from SYS";
		sql = "select id, type, sql from SYS";
		sql = "select sql from SYS";
		executeQuery();
		//stmt.executeUpdate("DROP ALL OBJECTS DELETE FILES");
	}
	/**
	 * @param args
	 */
	public static void main0(String[] args) {
		String databaseName = "mydb";
		//FileUtils.delete("E:/H2/eclipse-workspace-client/mydb.h2.db");
		FileUtils.delete("E:/H2/eclipse-workspace-server/mydb.h2.db");
		Database db = new Database(new ConnectionInfo(databaseName), null);

		PageStore ps = new PageStore(db, databaseName + Constants.SUFFIX_PAGE_FILE, "rw", 16384);
		//ps.setPageSize(16384);
		ps.setPageSize(1024);
		ps.setLogMode(2);
		ps.setLockFile(true);
		//ps.open();

		System.out.println("ddd");

	}

	//重点测试find
	public static void main1(String[] args) throws Exception {
		Properties prop = new Properties();
		prop.setProperty("user", "sa");
		prop.setProperty("password", "");

		//prop.setProperty("MODE", "DB2"); //支持SYSDUMMY1

		int len = 256;
		StringBuilder s = new StringBuilder(len);
		for (int i = 1; i <= len; i++) {
			s.append("a");
		}

		//		prop.setProperty("TRACE_LEVEL_FILE", "10");
		//		prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
		//		prop.setProperty("PAGE_SIZE", "1024");
		//		prop.setProperty("FILE_LOCK", "FS");

		prop.setProperty("ACCESS_MODE_DATA", "rw");
		prop.setProperty("CACHE_SIZE", "4096");
		prop.setProperty("PAGE_SIZE", "128");
		prop.setProperty("LOG", "0"); //0: LOG_MODE_OFF, 2: LOG_MODE_SYNC
		prop.setProperty("CACHE_TYPE", "TQ"); //只有两种: LRU、TQ或者加“SOFT_”前缀
		prop.setProperty("FILE_LOCK", "FS"); //有5种: NO、FILE、SOCKET、SERIALIZED、FS
		//prop.setProperty("MVCC", "true");

		String url = "jdbc:h2:tcp://localhost:9092/mydb6";

		Connection conn = DriverManager.getConnection(url, prop);
		conn.setAutoCommit(false);
		Statement stmt = conn.createStatement();

		ResultSet rs = null;
		//stmt.executeUpdate("DROP TABLE IF EXISTS PageStoreTest");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS PageStoreTest(id int not null, name varchar(500) not null, address varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON PageStoreTest(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON PageStoreTest(name desc)");
//		long ii = 1000000001L;
//		for (int i = 1; i <= 200; i++) {
//			//stmt.executeUpdate("insert into PageStoreTest(id, name) values(" + i + ", '" + s + "abcdef1234')");
//			//stmt.executeUpdate("insert into PageStoreTest(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
//			//stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
//			ii++;
//		}
		//				//stmt.executeUpdate("insert into PageStoreTest(id, name) values(" + 101 + ", '" + s + "abcdef1234')");

		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(10, '1000000001', 'a')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(20, '1000000002', 'b')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(30, '1000000003', 'c')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(40, '1000000004', 'd')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(50, '1000000005', 'e')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(60, '1000000006', 'f')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(70, '1000000007', 'g')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(80, '1000000008', 'h')");
		//			stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(90, '1000000009', 'i')");
		//
		//		//stmt.executeUpdate("insert into PageStoreTest(id, name, address) SORTED values(30, '1000000003', 'c')");
		//		//stmt.executeUpdate("delete from PageStoreTest where id=40");
		//		stmt.executeUpdate("delete from PageStoreTest where id=30");
		//conn.commit();

		//		stmt.executeUpdate("delete from PageStoreTest where id=2");
		//		stmt.executeUpdate("delete from PageStoreTest where id>300");
		//		stmt.executeUpdate("insert into PageStoreTest(id, name, address) values(" + 300 + ", '" + 300 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into PageStoreTest(id, name, address) values(" + 301 + ", '" + 302 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into PageStoreTest(id, name, address) values(" + 2 + ", '" + 2 + "abcdef1234', 'zzz')");
		//
		//		stmt.executeUpdate("update PageStoreTest set name='1234567890' where id>10");

		String sql = null;

		sql = "select name,id from PageStoreTest where name>='1000000004'";
		sql = "select name,id from PageStoreTest where name between '1000000004' and '1000000006'";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getString(1));
		}

		//stmt.executeUpdate("TRUNCATE TABLE PageStoreTest");
		//stmt.executeUpdate("drop index IF EXISTS idx_name");

		conn.commit();
		//conn.rollback();
		rs.close();
		stmt.close();
		conn.close();

	}
}
