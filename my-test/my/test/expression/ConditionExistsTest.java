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
package my.test.expression;

import my.test.TestBase;

public class ConditionExistsTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new ConditionExistsTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("drop table IF EXISTS ConditionExistsTest");
        stmt.executeUpdate("create table IF NOT EXISTS ConditionExistsTest(id int, name varchar(500))");

        stmt.executeUpdate("insert into ConditionExistsTest(id, name) values(1, 'a1')");
        stmt.executeUpdate("insert into ConditionExistsTest(id, name) values(1, 'b1')");
        stmt.executeUpdate("insert into ConditionExistsTest(id, name) values(2, 'a2')");
        stmt.executeUpdate("insert into ConditionExistsTest(id, name) values(2, 'b2')");
        stmt.executeUpdate("insert into ConditionExistsTest(id, name) values(3, 'a3')");
        stmt.executeUpdate("insert into ConditionExistsTest(id, name) values(3, 'b3')");

        sql = "select * from ConditionExistsTest where name>'b' and EXISTS(select name from ConditionExistsTest where id=1)";
        sql = "select * from ConditionExistsTest where name>'b' or EXISTS(select name from ConditionExistsTest where id=1)";
        sql = "select * from ConditionExistsTest where name>'c' or "
                + "EXISTS(select count(*) from ConditionExistsTest where id=4)";

        executeQuery();
    }
}
