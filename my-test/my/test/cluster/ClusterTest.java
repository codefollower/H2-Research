package my.test.cluster;

import static junit.framework.Assert.assertEquals;

import java.sql.SQLException;
import java.sql.Savepoint;

import my.test.TestBase;

import org.junit.Assert;

public class ClusterTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new ClusterTest().start();
    }

    @Override
    public void init() throws Exception {
        url = "jdbc:h2:tcp://localhost:9092,localhost:9093/mydb";
    }

    @Override
    public void startInternal() throws Exception {
        //create();
        insert();
        //        select();
        //
        testCommit();
        //testRollback();
        //testSavepoint();
    }

    void create() throws Exception {
        stmt.executeUpdate("DROP TABLE IF EXISTS ClusterTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ClusterTest (f1 int NOT NULL, f2 int, f3 varchar)");
        stmt.executeUpdate("CREATE PRIMARY KEY HASH IF NOT EXISTS ClusterTest_idx1 ON ClusterTest(f1)");
        stmt.executeUpdate("CREATE UNIQUE HASH INDEX IF NOT EXISTS ClusterTest_idx2 ON ClusterTest(f2)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS ClusterTest_idx3 ON ClusterTest(f3, f2)");
    }

    void delete() throws Exception {
        stmt.executeUpdate("DELETE FROM ClusterTest");
    }

    void insert() throws Exception {
        stmt.executeUpdate("DELETE FROM ClusterTest");

        stmt.executeUpdate("INSERT INTO ClusterTest(f3, f2, f1) VALUES('d', 40, 400)");
        stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(100, 10, 'a')");
        stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(200, 20, 'b')");
        stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(300, 30, 'c')");
        try {
            stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(400, 20, 'd')");
            Assert.fail("insert duplicate key: 20");
        } catch (SQLException e) {
            //e.printStackTrace();
        }

        try {
            stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(500, 20, 'e')");
            Assert.fail("insert duplicate key: 20");
        } catch (SQLException e) {
            //e.printStackTrace();
        }

        try {
            stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(600, 20, 'f')");
            Assert.fail("insert duplicate key: 20");
        } catch (SQLException e) {
            //e.printStackTrace();
        }
    }

    void testCommit() throws Exception {
        try {
            conn.setAutoCommit(false);
            insert();
            conn.commit();
        } finally {
            conn.setAutoCommit(true);
        }

        sql = "SELECT f1, f2, f3 FROM ClusterTest";
        printResultSet();

        sql = "SELECT count(*) FROM ClusterTest";
        assertEquals(4, getIntValue(1, true));

        sql = "DELETE FROM ClusterTest";
        assertEquals(4, stmt.executeUpdate(sql));

        sql = "SELECT count(*) FROM ClusterTest";
        assertEquals(0, getIntValue(1, true));
    }

    void testRollback() throws Exception {
        try {
            conn.setAutoCommit(false);
            insert();
            conn.rollback();
        } finally {
            conn.setAutoCommit(true);
        }

        sql = "SELECT count(*) FROM ClusterTest";
        assertEquals(0, getIntValue(1, true));

    }

    void select() throws Exception {
        sql = "SELECT f1, f2, f3 FROM ClusterTest";
        printResultSet();

        sql = "SELECT count(*) FROM ClusterTest";
        assertEquals(3, getIntValue(1, true));

        sql = "SELECT f1, f2, f3 FROM ClusterTest WHERE f1 >= 200";
        printResultSet();

        sql = "SELECT count(*) FROM ClusterTest WHERE f1 >= 200";
        assertEquals(2, getIntValue(1, true));

        sql = "SELECT f1, f2, f3 FROM ClusterTest WHERE f2 >= 20";
        printResultSet();

        sql = "SELECT count(*) FROM ClusterTest WHERE f2 >= 20";
        assertEquals(2, getIntValue(1, true));

        sql = "SELECT f1, f2, f3 FROM ClusterTest WHERE f3 >= 'b' AND f3 <= 'c'";
        printResultSet();

        sql = "SELECT count(*) FROM ClusterTest WHERE f3 >= 'b' AND f3 <= 'c'";
        assertEquals(2, getIntValue(1, true));

        sql = "DELETE FROM ClusterTest WHERE f2 >= 20";
        assertEquals(2, stmt.executeUpdate(sql));
    }

    void testSavepoint() throws Exception {
        stmt.executeUpdate("DELETE FROM ClusterTest");
        try {
            conn.setAutoCommit(false);
            stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(100, 10, 'a')");
            stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(200, 20, 'b')");
            Savepoint savepoint = conn.setSavepoint();
            stmt.executeUpdate("INSERT INTO ClusterTest(f1, f2, f3) VALUES(300, 30, 'c')");
            sql = "SELECT f1, f2, f3 FROM ClusterTest";
            printResultSet();
            conn.rollback(savepoint);
            //调用rollback(savepoint)后还是需要调用commit
            conn.commit();
            //或调用rollback也能撤消之前的操作
            //conn.rollback();
        } finally {
            //这个内部也会触发commit
            conn.setAutoCommit(true);
        }

        sql = "SELECT f1, f2, f3 FROM ClusterTest";
        printResultSet();
    }
}
