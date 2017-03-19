package my.test;

import java.sql.Connection;
import java.sql.Statement;

public class DeadlockTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new DeadlockTest().start();
    }

    @Override
    public void init() throws Exception {
        prop.setProperty("MVCC", "false");
    }

    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("set DEFAULT_LOCK_TIMEOUT 100000");
        stmt.executeUpdate("drop table IF EXISTS DeadlockTest1");
        stmt.executeUpdate("drop table IF EXISTS DeadlockTest2");
        stmt.executeUpdate("create table IF NOT EXISTS DeadlockTest1(id int, name varchar(500), b boolean)");
        stmt.executeUpdate("create table IF NOT EXISTS DeadlockTest2(id int, name varchar(500), b boolean)");

        stmt.executeUpdate("insert into DeadlockTest1(id, name, b) values(1, 'a1', true)");
        stmt.executeUpdate("insert into DeadlockTest2(id, name, b) values(1, 'a1', true)");
        Thread t1 = new Thread() {
            @Override
            public void run() {
                Connection conn;
                try {
                    conn = DeadlockTest.this.getConnection();
                    conn.setAutoCommit(false);
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate("update DeadlockTest1 set name = 'a2' where id = 1");
                    stmt.executeUpdate("update DeadlockTest2 set name = 'a2' where id = 1");
                    conn.commit();
                    stmt.close();
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        t1.start();

        Thread t2 = new Thread() {
            @Override
            public void run() {
                Connection conn;
                try {
                    conn = DeadlockTest.this.getConnection();
                    conn.setAutoCommit(false);
                    Statement stmt = conn.createStatement();
                    stmt.executeUpdate("update DeadlockTest2 set name = 'a2' where id = 1");
                    stmt.executeUpdate("update DeadlockTest1 set name = 'a2' where id = 1");
                    conn.commit();
                    stmt.close();
                    conn.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        t2.start();

        t1.join();
        t2.join();
    }
}
