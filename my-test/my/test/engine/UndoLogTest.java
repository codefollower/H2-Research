package my.test.engine;

import java.sql.Savepoint;

import my.test.TestBase;

public class UndoLogTest extends TestBase {
	public static void main(String[] args) throws Exception {
		new UndoLogTest().start();
	}

	@Override
	public void init() throws Exception {
		//prop.setProperty("LARGE_TRANSACTIONS", "false");
		
		//当MVCC为true时，哪怕memoryUndo > database.getMaxMemoryUndo()了，也不把undo日志存到临时文件
		//见org.h2.engine.UndoLog.add(UndoLogRecord)
		prop.setProperty("MVCC", "true");
	}

	@Override
	public void startInternal() throws Exception {
		stmt.executeUpdate("SET MAX_MEMORY_UNDO 20");
		//stmt.executeUpdate("SET UNDO_LOG 0"); //禁用撤消日志，此时所有rollback都无效
		conn.setAutoCommit(false);
		//stmt.executeUpdate("SET DB_CLOSE_DELAY -1");
		stmt.executeUpdate("DROP TABLE IF EXISTS UndoLogTest");

		//MVStore还不支持rollback到保存点
		//stmt.executeUpdate("CREATE TABLE IF NOT EXISTS UndoLogTest(id int primary key not null, name varchar(500) not null, address varchar(500))"
		//		+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"");

		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS UndoLogTest(id int primary key not null, name varchar(500) not null, address varchar(500))");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON UndoLogTest(name asc)");
		//stmt.executeUpdate("CREATE index IF NOT EXISTS idx_name ON UndoLogTest(name desc)");

		//stmt.executeUpdate("SET MAX_LOG_SIZE 10"); //10K

		long ii = 1000000001L;

		Savepoint sp10 = null;
		for (int i = 1; i <= 50; i++) {
			stmt.executeUpdate("insert into UndoLogTest(id, name, address) SORTED values(" + i + ", '" + ii + "', 'zzz')");
			//			if (i % 100 == 0)
			//				stmt.executeUpdate("CHECKPOINT");
			//
			//			if (i % 200 == 0) {
			//				stmt.executeUpdate("PREPARE COMMIT newTransactionName");
			//				//conn.commit();
			//			}

			if (i % 10 == 0) {
				//conn.commit();
			}

			if (i == 10) {
				sp10 = conn.setSavepoint("sp10");
			}

			if (i == 35) {
				conn.setSavepoint("sp35");
			}

			ii++;
		}
		//conn.commit();
		//conn.rollback();
		conn.rollback(sp10);

		sql = "select * from UndoLogTest";
		executeQuery();

		//stmt.executeUpdate("TRUNCATE TABLE UndoLogTest");
		//stmt.executeUpdate("drop index IF EXISTS idx_name");

		//conn.commit();
		//conn.rollback();
	}
}