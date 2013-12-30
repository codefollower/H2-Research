package my.test.index;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class IndexTest {

	/**
	 * @param args
	 */
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
		stmt.executeUpdate("drop table IF EXISTS IndexTestTable");
		//stmt.executeUpdate("create TEMPORARY table IF NOT EXISTS IndexTestTable(id int, name varchar(500),CONSTRAINT myindex INDEX (name)) TRANSACTIONAL");
		//stmt.executeUpdate("create table IF NOT EXISTS IndexTestTable(id int, name varchar(500) not null)");

		//stmt.executeUpdate("create LOCAL TEMPORARY table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null)");
		//stmt.executeUpdate("CREATE UNIQUE HASH INDEX idx_name ON IndexTestTable(name)");
		//stmt.executeUpdate("CREATE PRIMARY KEY HASH idx_name ON IndexTestTable(id)");

		//stmt.executeUpdate("create table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null)");
		//stmt.executeUpdate("CREATE primary key idx_name ON IndexTestTable(id)");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + 120 + ", '"+110 +"abcdef1234')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + 110 + ", '"+110 +"abcdef1234')");
		//		stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + 130 + ", '"+110 +"abcdef1234')");
		
		//测试TreeIndex
		//stmt.executeUpdate("create LOCAL TEMPORARY table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null) NOT PERSISTENT");
		//stmt.executeUpdate("CREATE index idx_name ON IndexTestTable(id)");
		
		//测试PageBtreeIndex
		//找断点条件indexName.equalsIgnoreCase("idx_name"); getName().equalsIgnoreCase("idx_name");
		//index.getName().equalsIgnoreCase("idx_name");
		stmt.executeUpdate("create table IF NOT EXISTS IndexTestTable(id int not null, name varchar(500) not null, address varchar(500) not null)");
		stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON IndexTestTable(name)");
		for (int i = 1; i <= 200; i++) {
			//stmt.executeUpdate("insert into IndexTestTable(id, name) values(" + i + ", '" + s + "abcdef1234')");
			stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + i + ", 'abcdef1234', 'zzz')");
		}

		stmt.executeUpdate("delete from IndexTestTable where id=2");
		stmt.executeUpdate("delete from IndexTestTable where id>300");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 300 + ", '" + 300 + "abcdef1234', 'zzz')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 301 + ", '" + 302 + "abcdef1234', 'zzz')");
		stmt.executeUpdate("insert into IndexTestTable(id, name, address) values(" + 2 + ", '" + 2 + "abcdef1234', 'zzz')");

		String sql = null;

		sql = "select name from IndexTestTable where id>198";
		rs = stmt.executeQuery(sql);
		while (rs.next()) {
			System.out.println(rs.getString(1));
		}
		conn.commit();
		rs.close();
		stmt.close();
		conn.close();

	}
}
