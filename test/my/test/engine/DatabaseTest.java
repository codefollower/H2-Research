package my.test.engine;

import java.sql.Connection;

import my.test.TestBase;

public class DatabaseTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new DatabaseTest().start();
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
		multiThreaded();
		//setPageSize(1);
		sql = "select * from SYS";
		sql = "select id, type, sql from SYS";
		sql = "select sql from SYS";
		executeQuery();
		//stmt.executeUpdate("DROP ALL OBJECTS DELETE FILES");
	}

	void multiThreaded() throws Exception {
		//stmt.executeUpdate("SET MULTI_THREADED 1");
		new MyThread().start();
		new MyThread().start();
	}

	class MyThread extends Thread {
		public void run() {
			Connection conn;
			try {
				conn = getConnection();
				conn.createStatement().executeUpdate("drop table IF EXISTS ttt");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}
	}

	static void setPageSize(int size) {

		boolean good = false;
		int shift = 0;
		for (int i = 1; i <= size;) {
			if (size == i) {
				good = true;
				break;
			}
			shift++;
			i += i;

			System.out.println(i);
		}

		System.out.println(good);
		System.out.println(shift);
	}
}
