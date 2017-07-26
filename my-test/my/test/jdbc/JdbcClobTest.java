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

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import org.h2.jdbc.JdbcClob;
import org.h2.jdbc.JdbcConnection;
import org.h2.util.IOUtils;
import org.h2.value.Value;
import org.junit.Assert;

import my.test.TestBase;

public class JdbcClobTest extends TestBase {

    public static void main(String[] args) throws Exception {
        new JdbcClobTest().crud();
    }

    public void crud() throws Exception {
        start();
        Connection conn = getConnection();
        Statement stmt = conn.createStatement();

        stmt.executeUpdate("DROP TABLE IF EXISTS JdbcClobTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS JdbcClobTest (f1 int, f2 long, f3 clob)");

        JdbcClob clob = (JdbcClob) conn.createClob();
        Value clobValue = ((JdbcConnection) conn).createClob(
                IOUtils.getBufferedReader(new FileInputStream("C:/TDDOWNLOAD/kotlin-compiler-1.1.2-2.zip")), -1);

        clob = new JdbcClob((JdbcConnection) conn, clobValue, 100);

        String clobStr = "clob-test";
        StringBuilder buff = new StringBuilder(5000 * clobStr.length());
        for (int i = 0; i < 5000; i++)
            buff.append(clobStr);

        clobStr = buff.toString();
        // 从1开始
        // clob.setString(1, clobStr);

        PreparedStatement ps = conn.prepareStatement("INSERT INTO JdbcClobTest(f1, f2, f3) VALUES(1, 2, ?)");
        ps.setClob(1, clob);
        ps.executeUpdate();

        ResultSet rs = stmt.executeQuery("SELECT f1, f2, f3 FROM JdbcClobTest");
        Assert.assertTrue(rs.next());
        Assert.assertEquals(1, rs.getInt(1));
        Assert.assertEquals(2, rs.getLong(2));

        clob = (JdbcClob) rs.getClob(3);
        Assert.assertNotNull(clob);
        clobStr = clob.getSubString(1, clobStr.length());
        System.out.println("f3=" + clobStr);

        stmt.executeUpdate("DELETE FROM JdbcClobTest WHERE f1 = 1");

        ps.close();
        stmt.close();
        conn.close();
    }
}
