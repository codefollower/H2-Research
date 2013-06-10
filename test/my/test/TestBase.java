package my.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public abstract class TestBase {
	protected Properties prop = new Properties();
	protected Connection conn;
	protected Statement stmt;
	protected PreparedStatement ps;
	protected ResultSet rs;
	protected String sql;
	protected String url;

	private void initDefaults() throws Exception {
		prop.setProperty("user", "sa");
		prop.setProperty("password", "");
		//		prop.setProperty("TRACE_LEVEL_FILE", "10");
		//		prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
		//		prop.setProperty("PAGE_SIZE", "1024");
		//		prop.setProperty("FILE_LOCK", "FS");

		//prop.setProperty("PAGE_SIZE", "128");
		//prop.setProperty("MVCC", "true");

		//prop.setProperty("MODE", "DB2"); //支持SYSDUMMY1

	}

	public Connection getConnection() throws Exception {
		return DriverManager.getConnection(url, prop);
	}

	public void init() throws Exception {
	}

	public void start() throws Exception {
		initDefaults();
		init();
		if (url == null)
			url = "jdbc:h2:tcp://localhost:9092/mydb";

		//		int len = 256;
		//		StringBuilder s = new StringBuilder(len);
		//		for (int i = 1; i <= len; i++) {
		//			s.append("a");
		//		}

		conn = DriverManager.getConnection(url, prop);
		//conn.setAutoCommit(false);
		stmt = conn.createStatement();

		startInternal();

		stop();
	}

	public void stop() throws Exception {
		if (rs != null)
			rs.close();
		if (ps != null)
			ps.close();
		if (stmt != null)
			stmt.close();
		if (conn != null)
			conn.close();
	}

	public abstract void startInternal() throws Exception;

	public void executeQuery() throws Exception {
		rs = stmt.executeQuery(sql);
		int n = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
			//System.out.println(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " + rs.getString(4));
		}
	}

	public void tryExecuteQuery() {
		try {
			executeQuery();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}

	public void printResultSet(ResultSet rs) throws Exception {
		int n = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
			//System.out.println(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " + rs.getString(4));
		}
	}

	public void printResultSet() throws Exception {
		rs = stmt.executeQuery(sql);

		int n = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
		}
		rs.close();
		rs = null;
		//System.out.println();
	}

	public int executeUpdate() throws Exception {
		return stmt.executeUpdate(sql);
	}

}
