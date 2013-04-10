package my.test.index;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

//找断点条件indexName.equalsIgnoreCase("idx_name"); getName().equalsIgnoreCase("idx_name");
//index.getName().equalsIgnoreCase("idx_name");
public class PageBtreeLeafTest {

	/**
	 * @param args
	 */
	//重点测试org.h2.index.PageBtreeLeaf.addRow(SearchRow, boolean)
	public static void main0(String[] args) throws Exception {
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

		prop.setProperty("PAGE_SIZE", "128");
		prop.setProperty("MVCC", "true");

		String url = "jdbc:h2:tcp://localhost:9092/mydb";

		Connection conn = DriverManager.getConnection(url, prop);
		conn.setAutoCommit(false);
		Statement stmt = conn.createStatement();

		ResultSet rs = null;
		stmt.executeUpdate("DROP TABLE IF EXISTS IndexTestTable");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null, address varchar(500) not null)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON IndexTestTable(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON IndexTestTable(name desc)");
		//		long ii = 1000000001L;
		//		for (int i = 1; i <= 100; i++) {
		//			//stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + i + ", '" + s + "abcdef1234')");
		//			//stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
		//			stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
		//			ii++;
		//		}

		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(10, '1000000001', 'a')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(30, '1000000003', 'c')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(20, '1000000002', 'b')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(20, '1000000002', 'b')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(40, '1000000004', 'd')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(0, '1000000000', '0')");

		//stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(30, '1000000003', 'c')");
		stmt.executeUpdate("delete from IndexTestTable where id=40");
		stmt.executeUpdate("delete from IndexTestTable where id=30");

		//		stmt.executeUpdate("delete from IndexTestTable where id=2");
		//		stmt.executeUpdate("delete from IndexTestTable where id>300");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 300 + ", '" + 300 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 301 + ", '" + 302 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 2 + ", '" + 2 + "abcdef1234', 'zzz')");
		//
		//		stmt.executeUpdate("update IndexTestTable set name='1234567890' where id>10");

		String sql = null;

		sql = "select name,id from IndexTestTable where name>'1000000005'";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getString(1));
		}

		//stmt.executeUpdate("TRUNCATE TABLE IndexTestTable");
		//stmt.executeUpdate("drop index IF EXISTS idx_name");

		//conn.commit();
		conn.rollback();
		rs.close();
		stmt.close();
		conn.close();

	}

	//重点测试find
	public static void main(String[] args) throws Exception {
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

		prop.setProperty("PAGE_SIZE", "128");
		prop.setProperty("MVCC", "true");

		String url = "jdbc:h2:tcp://localhost:9092/mydb";

		Connection conn = DriverManager.getConnection(url, prop);
		conn.setAutoCommit(false);
		Statement stmt = conn.createStatement();

		ResultSet rs = null;
		stmt.executeUpdate("DROP TABLE IF EXISTS IndexTestTable");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null, address varchar(500) not null)");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON IndexTestTable(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON IndexTestTable(name desc)");
		//		long ii = 1000000001L;
		//		for (int i = 1; i <= 100; i++) {
		//			//stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + i + ", '" + s + "abcdef1234')");
		//			//stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + i * 10 + ", 'abcdef1234', 'zzz')");
		//			stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
		//			ii++;
		//		}

		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(10, '1000000001', 'a')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(20, '1000000002', 'b')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(30, '1000000003', 'c')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(40, '1000000004', 'd')");
		//stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(40, '"+s+"', 'd')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(50, '1000000005', 'e')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(60, '1000000006', 'f')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(70, '1000000007', 'g')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(80, '1000000008', 'h')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(90, '1000000009', 'i')");

		//stmt.executeUpdate("insert into IndexTestTable(id, name, address) SORTED values(30, '1000000003', 'c')");
		//stmt.executeUpdate("delete from IndexTestTable where id=40");
		stmt.executeUpdate("delete from IndexTestTable where id=30");
		//conn.commit();

		//		stmt.executeUpdate("delete from IndexTestTable where id=2");
		//		stmt.executeUpdate("delete from IndexTestTable where id>300");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 300 + ", '" + 300 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 301 + ", '" + 302 + "abcdef1234', 'zzz')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 2 + ", '" + 2 + "abcdef1234', 'zzz')");
		//
		//		stmt.executeUpdate("update IndexTestTable set name='1234567890' where id>10");

		String sql = null;

		sql = "select name,id from IndexTestTable where name>='1000000004'";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getString(1));
		}

		//stmt.executeUpdate("TRUNCATE TABLE IndexTestTable");
		//stmt.executeUpdate("drop index IF EXISTS idx_name");

		conn.commit();
		//conn.rollback();
		rs.close();
		stmt.close();
		conn.close();

	}
}
