/*
 * Copyright Lealone Database Group.
 * Licensed under the Server Side Public License, v 1.
 * Initial Developer: zhh
 */
package my.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class JdbcTest50 {
    public static void main2(String[] args) throws Exception {
        String url = "jdbc:h2:tcp://localhost:9092/mydb";
        Connection conn = DriverManager.getConnection(url, "sa", "");
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("select count(*) from test where f1+f2>1");
        rs.next();
        System.out.println(rs.getString(1));
        for (int i = 0; i < 10; i++)
            rs = stmt.executeQuery("select sum(f1) from test where f1+f2>1");
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 20; i++) {t1 = System.currentTimeMillis();
            rs = stmt.executeQuery("select sum(f1) from test where f1+f2>1");
            long t2 = System.currentTimeMillis();System.out.println("time:" + ((t2 - t1) ));
        }
        long t2 = System.currentTimeMillis();
        rs.next();
        System.out.println(rs.getString(1));
        System.out.println("time:" + ((t2 - t1) / 20));
        stmt.close();
        conn.close();
    }
    public static void main (String[] args) throws Exception {

        String url = "jdbc:h2:tcp://localhost:9092/mydb";
        Connection conn = DriverManager.getConnection(url, "sa", "");
        Statement stmt = conn.createStatement(); 
        stmt.executeUpdate("DROP TABLE IF EXISTS test");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test(name varchar(10), f1 int,f2 int)");
        // stmt.close();
        PreparedStatement ps = conn.prepareStatement("insert into test(name, f1,f2) values(?,?,?)");

        long t1 = System.currentTimeMillis();

        int loop = 5 * 10;
        for (int i = 0; i < loop; i++) {
            for (int j = 1; j <= 1000; j++) {
                ps.setString(1, "abc");
                ps.setInt(2, i * 1000 + j);
                ps.setInt(3, (i * 1000 + j) * 10);
                ps.addBatch();
            }
            ps.executeBatch();
            // ps.clearBatch();
        }

        // int count = 5 * 10000;
        // for (int i = 1; i <= count; i++) {
        // ps.setString(1, "abc" + i);
        // ps.setInt(2, i);
        // ps.setInt(3, i * 10);
        // ps.executeUpdate();
        // ps.clearParameters();
        // }

        long t2 = System.currentTimeMillis();
        System.out.println(t2 - t1);

        ps.close();

        ResultSet rs = stmt.executeQuery("select count(*) from test where f1+f2>1");
        rs.next();
        System.out.println(rs.getString(1));
        t1 = System.currentTimeMillis();
        rs = stmt.executeQuery("select sum(f1) from test where f1+f2>1");
        t2 = System.currentTimeMillis();
        rs.next();
        System.out.println(rs.getString(1));
        System.out.println("time:" + (t2 - t1));
        stmt.close();
        conn.close();
    }
}
