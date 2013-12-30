package my.test.transaction;

import my.test.TestBase;
import static junit.framework.Assert.assertEquals;

import java.sql.SQLException;
import java.sql.Savepoint;

import org.junit.Assert;

public class TransactionTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new TransactionTest().start();
    }

    @Override
    public void startInternal() throws Exception {
//        create();
//        insert();
//        select();
//
//        testCommit();
//        testRollback();

        testSavepoint();
    }

    void create() throws Exception {
        //stmt.executeUpdate("DROP TABLE IF EXISTS IndexTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS IndexTest (f1 int NOT NULL, f2 int, f3 varchar)");
        stmt.executeUpdate("CREATE PRIMARY KEY HASH IF NOT EXISTS IndexTest_idx0 ON IndexTest(f1)");
        stmt.executeUpdate("CREATE UNIQUE HASH INDEX IF NOT EXISTS IndexTest_idx1 ON IndexTest(f2)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTest_idx2 ON IndexTest(f3, f2)");

        //indexFieldWithColumnFamilyPrefix();
    }

    void indexFieldWithColumnFamilyPrefix() throws Exception {
        //IndexTest_idx3与IndexTest_idx1一样，只不过索引字段前可加列族前缀，
        //用CREATE TABLE建立的表是单列族的静态表，列族名称是cf
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTest_idx3 ON IndexTest(cf.f2)");

        stmt.executeUpdate("CREATE HBASE TABLE IF NOT EXISTS IndexTest2 (" //
                + "COLUMN FAMILY cf(id int), COLUMN FAMILY cf2(id2 int))");

        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTest2_idx1 ON IndexTest2(cf.id)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS IndexTest2_idx2 ON IndexTest2(cf2.id2)");
    }

    void delete() throws Exception {
        stmt.executeUpdate("DELETE FROM IndexTest");
    }

    void insert() throws Exception {
        stmt.executeUpdate("DELETE FROM IndexTest");

        stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(100, 10, 'a')");
        stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(200, 20, 'b')");
        stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(300, 30, 'c')");
        try {
            stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(400, 20, 'd')");
            Assert.fail("insert duplicate key: 20");
        } catch (SQLException e) {
            //e.printStackTrace();
        }

        try {
            stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(500, 20, 'e')");
            Assert.fail("insert duplicate key: 20");
        } catch (SQLException e) {
            //e.printStackTrace();
        }

        try {
            stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(600, 20, 'f')");
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

        sql = "SELECT f1, f2, f3 FROM IndexTest";
        printResultSet();

        sql = "SELECT count(*) FROM IndexTest";
        assertEquals(3, getIntValue(1, true));

        sql = "DELETE FROM IndexTest";
        assertEquals(3, stmt.executeUpdate(sql));

        sql = "SELECT count(*) FROM IndexTest";
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

        sql = "SELECT count(*) FROM IndexTest";
        assertEquals(0, getIntValue(1, true));

    }

    void select() throws Exception {
        sql = "SELECT f1, f2, f3 FROM IndexTest";
        printResultSet();

        sql = "SELECT count(*) FROM IndexTest";
        assertEquals(3, getIntValue(1, true));

        sql = "SELECT f1, f2, f3 FROM IndexTest WHERE f1 >= 200";
        printResultSet();

        sql = "SELECT count(*) FROM IndexTest WHERE f1 >= 200";
        assertEquals(2, getIntValue(1, true));

        sql = "SELECT f1, f2, f3 FROM IndexTest WHERE f2 >= 20";
        printResultSet();

        sql = "SELECT count(*) FROM IndexTest WHERE f2 >= 20";
        assertEquals(2, getIntValue(1, true));

        sql = "SELECT f1, f2, f3 FROM IndexTest WHERE f3 >= 'b' AND f3 <= 'c'";
        printResultSet();

        sql = "SELECT count(*) FROM IndexTest WHERE f3 >= 'b' AND f3 <= 'c'";
        assertEquals(2, getIntValue(1, true));

        sql = "DELETE FROM IndexTest WHERE f2 >= 20";
        assertEquals(2, stmt.executeUpdate(sql));
    }

    void testSavepoint() throws Exception {
        stmt.executeUpdate("DELETE FROM IndexTest");
        try {
            conn.setAutoCommit(false);
            stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(100, 10, 'a')");
            stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(200, 20, 'b')");
            Savepoint savepoint = conn.setSavepoint();
            stmt.executeUpdate("INSERT INTO IndexTest(f1, f2, f3) VALUES(300, 30, 'c')");
            sql = "SELECT f1, f2, f3 FROM IndexTest";
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

        sql = "SELECT f1, f2, f3 FROM IndexTest";
        printResultSet();
    }
}