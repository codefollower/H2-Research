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
import java.sql.Statement;

public class RightTest {

    public static void main(String[] args) throws Exception {
        Class.forName("org.h2.Driver");

        String user = "sa";
        String password = "";
        user = "test";
        password = "test";
        Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mydb", user, password);
        Statement stmt = conn.createStatement();
        // stmt.executeUpdate("CREATE USER IF NOT EXISTS test PASSWORD 'test'");
        // stmt.executeUpdate("drop table IF EXISTS RightTest");
        stmt.executeUpdate("create table IF NOT EXISTS RightTest(f1 int SELECTIVITY 10, f2 int, f3 int)");

        stmt.executeUpdate("create index IF NOT EXISTS RightTest_i1 on RightTest(f1)");

        stmt.close();
        conn.close();
    }

}
