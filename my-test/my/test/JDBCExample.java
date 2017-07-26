/*
 * Copyright 2011 The Apache Software Foundation
 *
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class JDBCExample {

    static Connection getConnection() throws Exception {

        String url = "jdbc:h2:tcp://localhost:9092/mydb";
        url = "jdbc:h2:file:./target/mytest/baseDir/mydb2";
        url = "jdbc:h2:mem:mydb3";

        Connection conn = DriverManager.getConnection(url, "sa", "");
        return conn;
    }

    static Random random = new Random();

    // private static final ConcurrentNavigableMap<Value, String> rows = new ConcurrentSkipListMap<Value, String>();

    static class Value implements Comparable<Value> {
        int v;

        Value(int v) {
            this.v = v;
        }

        @Override
        public int compareTo(Value o) {
            return v - o.v;
        }

    }

    static CountDownLatch latch;

    static class MyThread extends Thread {
        Statement stmt;
        Connection conn;
        long time;
        int start;
        int end;

        MyThread(int start, int count) throws Exception {
            super("MyThread-" + start);
            conn = getConnection();
            stmt = conn.createStatement();
            this.start = start;
            this.end = start + count;
        }

        @Override
        public void run() {
            try {
                // long t1 = System.currentTimeMillis();
                // for (int i = start; i < end; i++) {
                // String sql = "INSERT INTO test(f1, f2) VALUES(" + i + "," + i * 10 + ")";
                // stmt.executeUpdate(sql);
                //
                // //rows.put(new Value(i), sql);
                //
                // }
                long t1 = System.currentTimeMillis();
                for (int i = start; i < end; i++) {
                    // ResultSet rs = stmt.executeQuery("SELECT * FROM test where f1 <= 3");
                    ResultSet rs = stmt.executeQuery("SELECT * FROM test where f1 = " + random.nextInt(end));

                    while (rs.next()) {
                        // System.out.println("f1=" + rs.getInt(1) + " f2=" + rs.getLong(2));
                        // System.out.println();
                    }
                }
                long t2 = System.currentTimeMillis();

                time = t2 - t1;
                System.out.println(getName() + " end, time=" + time + " ms");
                stmt.close();
                conn.close();
                latch.countDown();
            } catch (SQLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        // //System.out.println("Create MemoryTable: ");
        // //String url = "jdbc:lealone:tcp://localhost:9092/hbasedb?default_table_engine=memory";
        // //url = "jdbc:lealone:embed:hbasedb?default_table_engine=cbase";
        // stmt.executeUpdate("DROP TABLE IF EXISTS test");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test (f1 int primary key, f2 long)"); //engine cbase");
        // // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test2 (f1 int primary key, f2 long) engine hbase");
        // // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test3 (f1 int primary key, f2 long) engine cassandra");
        // //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test3 (f1 int primary key, f2 long) engine memory");
        // // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test4 (f1 int primary key, f2 long)"); //memory store
        // long t1 = System.currentTimeMillis();
        // for (int i = 1; i < 100000; i++) {
        // stmt.executeUpdate("INSERT INTO test(f1, f2) VALUES(" + i + "," + i * 10 + ")");
        // }
        // long t2 = System.currentTimeMillis();
        // System.out.println("time: " + (t2 - t1));
        // stmt.executeUpdate("UPDATE test SET f2 = 1 where f1 = 1");
        // ResultSet rs = stmt.executeQuery("SELECT * FROM test where f1 <= 3");
        // while (rs.next()) {
        // System.out.println("f1=" + rs.getInt(1) + " f2=" + rs.getLong(2));
        // //System.out.println();
        // }
        // stmt.executeUpdate("DELETE FROM test WHERE f1 = 1");

        Connection conn = getConnection();
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS test");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test (f1 int primary key, f2 long)");
        stmt.executeUpdate("set MULTI_THREADED 1");

        PreparedStatement ps = conn.prepareStatement("delete from test where f1=$1");
        ps.setInt(1, 3);
        ps.executeUpdate();

        for (int i = 1; i < 50000; i++) {
            String sql = "INSERT INTO test(f1, f2) VALUES(" + i + "," + i * 10 + ")";
            stmt.executeUpdate(sql);

            // rows.put(new Value(i), sql);

        }
        // stmt.close();
        // conn.close();

        int count = 10;
        latch = new CountDownLatch(count);

        MyThread[] threads = new MyThread[count];
        int loop = 30000;
        for (int i = 0; i < count; i++) {
            threads[i] = new MyThread(i * loop, loop);
        }

        for (int i = 0; i < count; i++) {
            threads[i].start();
        }

        latch.await();

        long sum = 0;
        for (int i = 0; i < count; i++) {
            sum += threads[i].time;
        }

        System.out.println("sum=" + sum + ", count=" + count + ", avg=" + (sum / count) + " ms");
    }
}
