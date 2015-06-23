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
package my.test.command.ddl;

import my.test.TestBase;

public class TruncateTableTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new TruncateTableTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        executeUpdate("drop table IF EXISTS TruncateTableTest");
        executeUpdate("create table IF NOT EXISTS TruncateTableTest(id int PRIMARY KEY, name varchar(500), b boolean, f1 int)");

        // executeUpdate("ALTER TABLE TruncateTableTest ADD CONSTRAINT myfk FOREIGN KEY (f1) REFERENCES TruncateTableTest(id)");

        executeUpdate("insert into TruncateTableTest(id, name, b) values(1, 'a1', true)");
        executeUpdate("insert into TruncateTableTest(id, name, b) values(2, 'b1', true)");
        executeUpdate("insert into TruncateTableTest(id, name, b) values(4, 'a2', false)");

        sql = "select count(*) from TruncateTableTest";
        executeQuery();

        executeUpdate("truncate table TruncateTableTest");
        sql = "select count(*) from TruncateTableTest";
        executeQuery();
    }
}
