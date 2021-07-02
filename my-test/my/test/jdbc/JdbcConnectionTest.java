package my.test.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.h2.jdbc.JdbcConnection;

public class JdbcConnectionTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		Properties prop = new Properties();
		prop.setProperty("user", "sa");
		prop.setProperty("password", "");
		String url = "jdbc:h2:tcp://localhost:9092/test9";
		
		System.setProperty("h2.baseDir", "E:\\H2\\baseDir");
		
		//TRACE_LEVEL_FILE参数放System中无效，放在url中非法，只能放在prop中
		//System.setProperty("TRACE_LEVEL_FILE", "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE");
		//url +="; TRACE_LEVEL_FILE=E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE";
		
		//prop.setProperty("TRACE_LEVEL_FILE", "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE.txt"); //只能是数字
		
		//prop.setProperty("TRACE_LEVEL_FILE", "10");
		
		
		prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
		
		url = "jdbc:h2:tcp://localhost:9092,localhost:9093/test9";
		url = "jdbc:h2:mem:memdb";
		
		//prop.setProperty("AUTO_SERVER", "true"); //AUTO_SERVER为true时url中不能指定多个server
		
		prop.setProperty("AUTO_RECONNECT", "true");
		
		//prop.setProperty("DATABASE_EVENT_LISTENER", "org.h2.samples.ShowProgress");
		//prop.setProperty("DATABASE_EVENT_LISTENER", "my.test.JdbcConnectionTest$MyDatabaseEventListener");
		
		//prop.setProperty("CIPHER", "my_cipher");
		//prop.setProperty("password", "my_password1 my_password2");
		
		
		JdbcConnection conn = new JdbcConnection(url, prop, null, null);
		
		PreparedStatement ps = conn.prepareStatement("insert into t values(?,?)");
		ps.setInt(2, 20);
		ps.setString(1, "aaa");
		ps.executeUpdate();
		
		ps.close();
		//stmt.close();
		conn.close();

	}
	
	public static class MyDatabaseEventListener implements org.h2.api.DatabaseEventListener {

		@Override
		public void init(String url) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void opened() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void exceptionThrown(SQLException e, String sql) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setProgress(int state, String name, long x, long max) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void closingDatabase() {
			// TODO Auto-generated method stub
			
		}
		
	}

}
