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

public class AlterSequenceTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new AlterSequenceTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        executeUpdate("DROP TABLE IF EXISTS AlterSequenceTest");
        executeUpdate("DROP SEQUENCE IF EXISTS myseq");
        executeUpdate("CREATE SEQUENCE IF NOT EXISTS myseq START WITH 1000 INCREMENT BY 1 CACHE 20");

        sql = "CREATE TABLE IF NOT EXISTS AlterSequenceTest (f1 int, f2 int NULL_TO_DEFAULT SEQUENCE myseq) ";
        executeUpdate(sql);

        sql = "ALTER TABLE AlterSequenceTest ALTER COLUMN f2 RESTART WITH 2000";
        executeUpdate();

        // f1没有Sequence
        sql = "ALTER TABLE AlterSequenceTest ALTER COLUMN f1 RESTART WITH 2000";
        tryExecuteUpdate();

        executeUpdate("ALTER SEQUENCE  myseq RESTART WITH 2000 INCREMENT BY 1 MINVALUE 100 MAXVALUE 10000 CYCLE CACHE 20");
    }

}
