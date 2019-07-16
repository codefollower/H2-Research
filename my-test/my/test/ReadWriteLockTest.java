/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package my.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

//因为update语句会转成removeRow和addRow操作，所以不会跟delete语句产生死锁
public class ReadWriteLockTest {
    static Connection getConnection(String url) throws Exception {
        Properties prop = new Properties();
        prop.setProperty("MULTI_THREADED", "true");
        return DriverManager.getConnection(url, prop);
    }

    static String url = "jdbc:h2:mem:mydb";;

    public static void main(String[] args) throws Exception {
        Connection conn = getConnection(url);
        Statement stmt = conn.createStatement();

        stmt.executeUpdate("DROP TABLE IF EXISTS DeadLockTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS DeadLockTest (f1 int primary key, f2 long, f3 long)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS DeadLockIndexTest1 ON DeadLockTest(f2)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS DeadLockIndexTest2 ON DeadLockTest(f3)");
        stmt.executeUpdate("INSERT INTO DeadLockTest(f1, f2, f3) VALUES(10, 20, 30)");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS DeadLockTest2 (f1 int primary key, f2 long, f3 long)");
        stmt.executeUpdate("INSERT INTO DeadLockTest2(f1, f2, f3) VALUES(10, 20, 30)");

        new Thread(() -> {
            try {
                Connection conn2 = getConnection(url);
                conn2.setAutoCommit(false);
                Statement stmt2 = conn2.createStatement();
                stmt2.executeUpdate("delete from DeadLockTest where f1=10");
                conn2.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                Connection conn1 = getConnection(url);
                conn1.setAutoCommit(false);
                conn1.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
                Statement stmt1 = conn1.createStatement();
                ResultSet rs = stmt1.executeQuery("select * from DeadLockTest where f2=20");
                if (rs.next()) {
                    System.out.println(rs.getInt(1));
                }
                rs.close();
                rs = stmt1.executeQuery("select * from DeadLockTest where f3=30");
                if (rs.next()) {
                    System.out.println(rs.getInt(1));
                } else {
                    System.out.println("empty");
                }
                rs.close();
                conn1.commit();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        conn.close();
    }

    public static void main2(String[] args) throws Exception {
        Connection conn = getConnection(url);
        Statement stmt = conn.createStatement();

        stmt.executeUpdate("DROP TABLE IF EXISTS DeadLockTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS DeadLockTest (f1 int primary key, f2 long)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS DeadLockIndexTest ON DeadLockTest(f2)");
        stmt.executeUpdate("INSERT INTO DeadLockTest(f1, f2) VALUES(10, 12)");

        new Thread(() -> {
            try {
                Connection conn1 = getConnection(url);
                Statement stmt1 = conn1.createStatement();
                stmt1.executeUpdate("UPDATE DeadLockTest SET f2=13 where f1=10");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                Connection conn2 = getConnection(url);
                Statement stmt2 = conn2.createStatement();
                stmt2.executeUpdate("delete from DeadLockTest where f1=10");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        conn.close();
    }
}
