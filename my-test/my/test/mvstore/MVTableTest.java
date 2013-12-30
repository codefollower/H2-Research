package my.test.mvstore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import my.test.TestBase;

public class MVTableTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new MVTableTest().start();
	}

	@Override
	public void init() throws Exception {
		prop.setProperty("MVCC", "true");
	}

	@Override
	public void startInternal() throws Exception {
		//test();

		//testConcurrentUpdate();
		testConcurrentUpdate2();

		//testTwoPhaseCommit();
	}

	public void testTwoPhaseCommit() throws Exception {
		//		JdbcDataSource ds = new JdbcDataSource();
		//		ds.setURL("jdbc:h2:tcp://localhost:9092/mydb");
		//		ds.setUser("sa");
		//		ds.setPassword("");
		//		
		//		XAConnection xacon1 = ds.getXAConnection();
		//		XAConnection xacon2 = ds.getXAConnection();
		//		
		//		XAResource resource1 = xacon1.getXAResource();
		//		//resource1.
		//		XAResource resource2 = xacon2.getXAResource();
		stmt.executeUpdate("DROP TABLE IF EXISTS MVTableTest CASCADE");
		sql = "CREATE TABLE IF NOT EXISTS MVTableTest(id int not null, name varchar(500), b boolean) "
		+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"";

		stmt.executeUpdate(sql);
		conn.setAutoCommit(false);
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(10, 'a1', true)");
		conn.commit();

		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(20, 'b1', true)");
		stmt.execute("PREPARE COMMIT myxa");
		stmt.execute("COMMIT TRANSACTION myxa");

		sql = "select * from MVTableTest";
		executeQuery();
	}


	public void test() throws Exception {
		stmt.executeUpdate("DROP TABLE IF EXISTS MVTableTest CASCADE");
		sql = "CREATE TABLE IF NOT EXISTS MVTableTest(id int not null, name varchar(500), b boolean) "
		+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"";

		//sql = "CREATE TABLE IF NOT EXISTS MVTableTest(id int, name varchar(500), b boolean) ";

		stmt.executeUpdate(sql);
		//也可建立与table.getName() + "_DATA"同名的索引，但是不会冲突，
		//因为内部做了特殊处理，MVPrimaryIndex是不可见的，也不放入Meta表中
		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS MVTableTest_DATA ON MVTableTest(name)");
		//stmt.executeUpdate("DROP INDEX IF EXISTS MVTableTest_DATA");

		//stmt.executeUpdate("CREATE INDEX IF NOT EXISTS MVTableTest_name ON MVTableTest(name desc)");
		stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS MVTableTest_name ON MVTableTest(name desc)");
		//		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS MVTableTest_name ON MVTableTest(name, b)");
		//		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS MVTableTest_name ON MVTableTest(b, name)");
		//		stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx_MVTableTest_id ON MVTableTest(id)");

		stmt.executeUpdate("CREATE PRIMARY KEY IF NOT EXISTS idx_MVTableTest_id ON MVTableTest(id)");

		conn.setAutoCommit(false);
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(8, null, true)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(9, null, true)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(10, 'a1', true)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(20, 'b1', true)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(30, 'a2', false)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(40, 'b2', true)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(50, 'a3', false)");
		//
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(60, 'b3', true)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(70, 'b3', true)");
		//		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(-1, 'a1', true)");

		//stmt.executeUpdate("insert into MVTableTest(id, name, b) values(-1, 'a1', true)");
		//stmt.executeUpdate("insert into MVTableTest(id, name, b) values(10, 'a1', true)");

		//for(int i=90;i<200;i++)
		//	stmt.executeUpdate("insert into MVTableTest(id, name, b) values("+i+", 'a1', true)");
		stmt.executeUpdate("insert into MVTableTest(id, name, b) values(70, 'b4', true)");

		//测试org.h2.mvstore.db.MVTable.addRow(Session, Row)中的rollbackToSavepoint
		//同时测org.h2.mvstore.db.TransactionStore.rollbackTo(Transaction, long, long)
		//MVPrimaryIndex对应的map会rollback
		//idx_name对应的MVSecondaryIndex的map也会rollback
		//stmt.executeUpdate("insert into MVTableTest(id, name, b) values(70, 'b3', true)");

		//System.out.println(stmt.executeUpdate("delete from MVTableTest where id=70"));

		conn.commit();
		sql = "select * from MVTableTest where id=1";
		sql = "select * from MVTableTest where id>1";
		sql = "select * from MVTableTest where name>'a1' and name<'b3'";

		sql = "select * from MVTableTest where name='a1'";

		sql = "select * from MVTableTest where name>='b3' and name<'a1'";

		sql = "select min(name) from MVTableTest";
		sql = "select max(name) from MVTableTest";
		sql = "select count(name) from MVTableTest"; //如果字段name的值是null，是不算在内的
		//sql = "select count(id) from MVTableTest";
		executeQuery();

	}

	//	mysql> show variables where variable_name ='innodb_lock_wait_timeout';
	//	+--------------------------+-------+
	//	| Variable_name            | Value |
	//	+--------------------------+-------+
	//	| innodb_lock_wait_timeout | 50    |
	//	+--------------------------+-------+

	//	Exception in thread "main" java.sql.SQLException: Lock wait timeout exceeded; try restarting transaction
	//		at com.mysql.jdbc.SQLError.createSQLException(SQLError.java:1074)
	//		at com.mysql.jdbc.MysqlIO.checkErrorPacket(MysqlIO.java:4096)
	//		at com.mysql.jdbc.MysqlIO.checkErrorPacket(MysqlIO.java:4028)
	//		at com.mysql.jdbc.MysqlIO.sendCommand(MysqlIO.java:2490)
	//		at com.mysql.jdbc.MysqlIO.sqlQueryDirect(MysqlIO.java:2651)
	//		at com.mysql.jdbc.ConnectionImpl.execSQL(ConnectionImpl.java:2728)
	//		at com.mysql.jdbc.StatementImpl.executeUpdate(StatementImpl.java:1811)

	void testConcurrentUpdate2() throws Exception {
		Connection conn1 = DriverManager.getConnection(url, prop);
		Connection conn2 = DriverManager.getConnection(url, prop);
		Statement stmt1 = conn1.createStatement();
		Statement stmt2 = conn2.createStatement();
		try {
			stmt.executeUpdate("delete from MVTableTest where id=80");
			stmt.executeUpdate("insert into MVTableTest(id, name, b) values(80, 'b8', true)");
			stmt.executeUpdate("delete from MVTableTest where id=80");
			stmt.executeUpdate("insert into MVTableTest(id, name, b) values(80, 'b8', true)");
			
			conn1.setAutoCommit(false);
			conn2.setAutoCommit(false);

			//stmt1.executeUpdate("update MVTableTest set name='a' where id=70");
			stmt1.executeUpdate("delete from MVTableTest where id=80");
//			ResultSet rs = stmt1.executeQuery("select * from MVTableTest where id=70");
//			printResultSet(rs);
//			rs.close();
			//stmt2.executeUpdate("update MVTableTest set name='b3' where id=70");
			stmt2.executeUpdate("delete from MVTableTest where id=80");

			conn1.commit();
			conn2.commit();

			//sql = "select * from MVTableTest";

			//executeQuery();
		} finally {
			stmt1.close();
			conn1.close();
			stmt2.close();
			conn2.close();
		}
	}

	void testConcurrentUpdate() throws Exception {
		T1 t1 = new T1();
		t1.prop = prop;
		t1.url = url;

		T2 t2 = new T2();
		t2.prop = prop;
		t2.url = url;

		t1.start();
		t2.start();
	}

	public static class T1 extends Thread {
		Properties prop;
		String url;

		public void run() {
			Connection conn1 = null;
			Statement stmt1 = null;
			try {
				conn1 = DriverManager.getConnection(url, prop);
				stmt1 = conn1.createStatement();
				conn1.setAutoCommit(false);
				ResultSet rs = stmt1.executeQuery("select * from MVTableTest where id=70");
				printResultSet(rs);
				rs.close();
				conn1.commit();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					stmt1.close();
					conn1.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
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
	}

	public static class T2 extends Thread {
		Properties prop;
		String url;

		public void run() {
			Connection conn2 = null;
			Statement stmt2 = null;
			try {
				conn2 = DriverManager.getConnection(url, prop);
				stmt2 = conn2.createStatement();
				conn2.setAutoCommit(false);
				stmt2.executeUpdate("update MVTableTest set name='b5' where id=70");
				conn2.commit();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					stmt2.close();
					conn2.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}