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
package my.test.bugs;

import my.test.TestBase;

public class ColumnBugTest extends TestBase {

    public static void main(String[] args) throws Exception {
        new ColumnBugTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        validateConvertUpdateSequence();
    }

    void validateConvertUpdateSequence() throws Exception {
        sql = "DROP TABLE IF EXISTS ColumnBugTest";
        executeUpdate(sql);
        sql = "CREATE TABLE IF NOT EXISTS ColumnBugTest (f1 int, f2 int NULL_TO_DEFAULT)";
        executeUpdate(sql);
        executeUpdate("insert into ColumnBugTest(f2, f1) values(20, 10)");
    }

}
