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
package my.test.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.h2.jdbc.JdbcBlob;

import my.test.TestBase;

public class JdbcBlobTest extends TestBase {

    public static void main(String[] args) throws Exception {
        System.setProperty("h2.lobClientMaxSizeMemory", "8192");
        new JdbcBlobTest().crud();
    }

    public void crud() throws Exception {
        start();
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();

        stmt.executeUpdate("DROP TABLE IF EXISTS test");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS test (f1 int, f2 long, f3 blob)");

        JdbcBlob blob = (JdbcBlob) conn.createBlob();

        String blobStr = "blob test";
        StringBuilder buff = new StringBuilder(1000 * blobStr.length());
        for (int i = 0; i < 1000; i++)
            buff.append(blobStr);

        blobStr = buff.toString();
        // 从1开始
        blob.setBytes(1, blobStr.getBytes());

        PreparedStatement ps = conn.prepareStatement("INSERT INTO test(f1, f2, f3) VALUES(2, 1, ?)");
        ps.setBlob(1, blob);
        ps.executeUpdate();

        ResultSet rs = stmt.executeQuery("SELECT f1, f2, f3 FROM test");
        rs.next();
        System.out.println("f1=" + rs.getInt(1) + " f2=" + rs.getLong(2));

        blob = (JdbcBlob) rs.getBlob(3);
        blobStr = new String(blob.getBytes(1, blobStr.length()));
        System.out.println("f3=" + blobStr);

        ps.close();
        stmt.close();
        conn.close();
    }
}
