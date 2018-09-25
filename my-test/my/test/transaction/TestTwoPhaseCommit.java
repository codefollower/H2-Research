/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package my.test.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import my.test.TestBase;


/**
 * Tests for the two-phase-commit feature.
 */
public class TestTwoPhaseCommit extends TestBase {
    public static void main(String[] args) throws Exception {
        new TestTwoPhaseCommit().start();
    }

    public Connection getConnection(String db) throws Exception {
        Properties prop = new Properties();
        String url = "jdbc:h2:tcp://localhost:9092/" + db;
        prop.setProperty("user", "sa");
        prop.setProperty("password", "");
        return DriverManager.getConnection(url, prop);
    }

    @Override
    public void startInternal() throws Exception {

        prepare();
        openWith(true);
        test(true);
        //
        //        prepare();
        //        openWith(false);
        //        test(false);

        //        testLargeTransactionName();
    }

    void testLargeTransactionName() throws Exception {
        Connection conn = getConnection("twoPhaseCommit");
        Statement stat = conn.createStatement();
        conn.setAutoCommit(false);
        stat.execute("DROP TABLE IF EXISTS TEST2");
        stat.execute("CREATE TABLE TEST2(ID INT) ENGINE \"org.h2.mvstore.db.MVTableEngine\"");
        String name = "tx12345678";
        try {
            while (true) {
                stat.execute("INSERT INTO TEST2 VALUES(1)");
                name += "x";
                stat.execute("PREPARE COMMIT " + name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        conn.close();
    }

    private void test(boolean rolledBack) throws Exception {
        Connection conn = getConnection("twoPhaseCommit");
        Statement stat = conn.createStatement();
        stat.execute("SET WRITE_DELAY 0");
        ResultSet rs = stat.executeQuery("SELECT * FROM TEST ORDER BY ID");
        rs.next();
        assertEquals(1, rs.getInt(1));
        assertEquals("Hello", rs.getString(2));
        if (!rolledBack) {
            rs.next();
            assertEquals(2, rs.getInt(1));
            assertEquals("World", rs.getString(2));
        }
        assertFalse(rs.next());
        conn.close();
    }

    private void openWith(boolean rollback) throws Exception {
        Connection conn = getConnection("twoPhaseCommit");
        Statement stat = conn.createStatement();
        ArrayList<String> list = new ArrayList<>();
        ResultSet rs = stat.executeQuery("SELECT * FROM INFORMATION_SCHEMA.IN_DOUBT");
        while (rs.next()) {
            list.add(rs.getString("TRANSACTION"));
        }
        for (String s : list) {
            if (rollback) {
                stat.execute("ROLLBACK TRANSACTION " + s);
            } else {
                stat.execute("COMMIT TRANSACTION " + s);
            }
        }
        conn.close();
    }

    private void prepare() throws Exception {
        //deleteDb("twoPhaseCommit");
        Connection conn = getConnection("twoPhaseCommit");
        Statement stat = conn.createStatement();
        stat.execute("SET WRITE_DELAY 0");
        conn.setAutoCommit(false);
        stat.execute("DROP TABLE IF EXISTS TEST");
        stat.execute("CREATE TABLE TEST(ID INT PRIMARY KEY, NAME VARCHAR) ENGINE \"org.h2.mvstore.db.MVTableEngine\"");
        stat.execute("INSERT INTO TEST VALUES(1, 'Hello')");
        conn.commit();
        stat.execute("INSERT INTO TEST VALUES(2, 'World')");
        stat.execute("PREPARE COMMIT XID_TEST_TRANSACTION_WITH_LONG_NAME");
        //crash(conn);

        //conn.close(); //调用了close之后，未提交的事务就自动rollback了，INFORMATION_SCHEMA.IN_DOUBT里就查不到了
    }
}
