package my.test.index;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class PageDataLeafTest {

	/**
	 * @param args
	 * @throws SQLException 
	 */
	public static void main(String[] args) throws SQLException {
		Properties prop = new Properties();
		prop.setProperty("user", "sa");
		prop.setProperty("password", "");

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

		String url = "jdbc:h2:tcp://localhost:9092/mydb";

		Connection conn = DriverManager.getConnection(url, prop);
		Statement stmt = conn.createStatement();
		stmt.executeUpdate("create table IF NOT EXISTS mytable(id int primary key, name varchar(500))");

		stmt.executeUpdate("delete top 3 from mytable where id>10");

		for (int i = 1; i <= 200; i++) {
			if (i != 2000)
				stmt.executeUpdate("insert into mytable(id, name) values(" + i + ", 'abcdef1234')");
			else
				stmt.executeUpdate("insert into mytable(id, name) values(" + i + ", '" + s + "')");
		}

		stmt.executeUpdate("update mytable set name='1234567890' where id>10");
		ResultSet rs = stmt.executeQuery("select id,name from mytable");
		while (rs.next()) {
			System.out.println(rs.getInt(1) + " " + rs.getString(2));
		}
		rs.close();
		//stmt.executeUpdate("delete from mytable");
		stmt.close();
		conn.close();
	}

}
