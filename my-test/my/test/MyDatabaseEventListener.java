package my.test;

import java.sql.SQLException;

public class MyDatabaseEventListener implements org.h2.api.DatabaseEventListener {

	@Override
	public void init(String url) {
		System.out.println("my.test.MyDatabaseEventListener.init(String) => " + url);
	}

	@Override
	public void opened() {
		System.out.println("my.test.MyDatabaseEventListener.opened()");
	}

	@Override
	public void exceptionThrown(SQLException e, String sql) {
		System.out.println("my.test.MyDatabaseEventListener.exceptionThrown(SQLException, String) => " + sql);
		e.printStackTrace();
	}

	@Override
	public void setProgress(int state, String name, long x, long max)  {
		System.out.println("my.test.MyDatabaseEventListener.setProgress(int, String, int, int) => " + name);
	}

	@Override
	public void closingDatabase() {
		System.out.println("my.test.MyDatabaseEventListener.closingDatabase()");
	}

}
