package my.test.engine;

import java.sql.ResultSetMetaData;

import my.test.TestBase;

public class SessionRemoteTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new SessionRemoteTest().start();
	}

	@Override
	public void init() throws Exception {
		prop.setProperty("user", "sa3");
		//prop.setProperty("password", "22");
		
		//url = "jdbc:h2:mem:memdb";
		//注意: localhost:9092,localhost:9093，说明是集群环境，但是并不是XA，
		//也就是说可能有一台server更新成功了，可以允许另一台更新不成功
		//url = "jdbc:h2:tcp://localhost:9092,localhost:9093/mydb_SessionRemoteTest";
		//url = "jdbc:h2:tcp://localhost:9092/mydb_SessionRemoteTest";
		url = "jdbc:h2:tcp://localhost:9092/mydb_SessionRemoteTest5";
		//prop.setProperty("AUTO_SERVER", "true"); //AUTO_SERVER为true时url中不能指定多个server
		prop.setProperty("AUTO_RECONNECT", "true");
		
		

		//TRACE_LEVEL_FILE参数放System中无效，放在url中非法，只能放在prop中
		//System.setProperty("TRACE_LEVEL_FILE", "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE");
		//url +="; TRACE_LEVEL_FILE=E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE";

		//prop.setProperty("TRACE_LEVEL_FILE", "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE.txt"); //只能是数字

		//prop.setProperty("TRACE_LEVEL_FILE", "10");
		//prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");

		prop.setProperty("DATABASE_EVENT_LISTENER", "my.test.MyDatabaseEventListener");

		//同一个数据库第一次打开时如果没使用CIPHER，那么接下来打开也不能用CIPHER了，
		//如果第一次用了CIPHER，那么就一直要用CIPHER连它
		//prop.setProperty("CIPHER", "my_cipher"); //只支持XTEA、AES、FOG
		prop.setProperty("CIPHER", "AES");
		//如果配置了CIPHER，则密码包含两部份，用空格分开，第一部份是filePassword，第二部份是用户密码
		//prop.setProperty("password", "myFilePassword myUserPassword");
		prop.setProperty("password", "123456 654321");
		//prop.setProperty("PASSWORD_HASH", "true"); //如果为true那么不需要再对filePassword和userPassword进行hash了
	}

	//测试org.h2.engine.SessionRemote.connectEmbeddedOrServer(boolean)
	@Override
	public void startInternal() throws Exception {
		conn.setAutoCommit(true); //如果是集群环境，设置这个参数其实没用
						stmt.executeUpdate("drop table IF EXISTS SessionRemoteTest CASCADE");
						stmt.executeUpdate("create table IF NOT EXISTS SessionRemoteTest(id int, name varchar(500), b boolean)");
						stmt.executeUpdate("CREATE INDEX IF NOT EXISTS SessionRemoteTestIndex ON SessionRemoteTest(name)");
				
						stmt.executeUpdate("insert into SessionRemoteTest(id, name, b) values(10, 'a1', true)");
						stmt.executeUpdate("insert into SessionRemoteTest(id, name, b) values(20, 'b1', true)");
						stmt.executeUpdate("insert into SessionRemoteTest(id, name, b) values(30, 'a2', false)");
						stmt.executeUpdate("insert into SessionRemoteTest(id, name, b) values(40, 'b2', true)");
						stmt.executeUpdate("insert into SessionRemoteTest(id, name, b) values(50, 'a3', false)");
						stmt.executeUpdate("insert into SessionRemoteTest(id, name, b) values(60, 'b3', true)");
						stmt.executeUpdate("insert into SessionRemoteTest(id, name, b) values(70, 'b3', true)");

		new StatementCancelThread().start();
		//stmt.executeUpdate("CREATE USER IF NOT EXISTS SA3 IDENTIFIED BY abc"); // 密码不加引号

		stmt.executeUpdate("drop table IF EXISTS tempSessionRemoteTest");
		//下面这条SQL可以测试org.h2.engine.SessionWithState.readSessionState()
		stmt.executeUpdate("create LOCAL TEMPORARY table IF NOT EXISTS tempSessionRemoteTest(i int)");
		sql = "select * from SessionRemoteTest where id>? and b=?";
		sql = "select * from SessionRemoteTest where id>? and b=? for update";
		//sql = "select * from SessionRemoteTest where id>? and b=? and id<RAND()";
		//sql = "update SessionRemoteTest set b=true where id>? and b=?";
		ps = conn.prepareStatement(sql);
		ps.setFetchSize(2);
		ps.setInt(1, 50);
		ps.setBoolean(2, true);

		rs = ps.executeQuery();
		//等价于ResultSet.getMetaData()，只不过PreparedStatement.getMetaData()不需要事先执行查询
		ResultSetMetaData rsmd = ps.getMetaData(); //如果不是查询sql，此方法会返回null
		int n = rsmd.getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
		}
	}

	private class StatementCancelThread extends Thread {
		public void run() {
			try {
				//在eclipse debug模式下调试到这里暂停，然后执行stmt.executeUpdate，在stmt.executeUpdate中又暂停，在转到这里继续
				//stmt.cancel();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
