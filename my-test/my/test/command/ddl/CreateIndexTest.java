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

public class CreateIndexTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new CreateIndexTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        executeUpdate("DROP TABLE IF EXISTS CreateIndexTest");
        executeUpdate("CREATE TABLE IF NOT EXISTS CreateIndexTest (f1 int NOT NULL, f2 int, f3 int)");

        for (int i = 1; i <= 5000; i++) {
            stmt.executeUpdate("insert into CreateIndexTest(f1, f2, f3) values(" + i + "," + i + "," + i + ")");
        }

        // executeUpdate("CREATE PRIMARY KEY HASH ON CreateIndexTest(f1)");
        executeUpdate("CREATE PRIMARY KEY HASH IF NOT EXISTS idx0 ON CreateIndexTest(f1)");

        executeUpdate("CREATE UNIQUE HASH INDEX IF NOT EXISTS idx1 ON CreateIndexTest(f2)");
        executeUpdate("CREATE INDEX IF NOT EXISTS idx2 ON CreateIndexTest(f3)");

        executeUpdate("ALTER INDEX idx2 RENAME TO idx22");

        executeUpdate("DROP INDEX IF EXISTS idx22");

        // executeUpdate("CREATE SCHEMA IF NOT EXISTS schema0 AUTHORIZATION sa");
        // executeUpdate("ALTER INDEX mydb.public.idx0 RENAME TO schema0.idx1");

        // executeUpdate("ALTER INDEX mydb.public.idx0 RENAME TO idx1");

        executeUpdate("ALTER TABLE CreateIndexTest ADD CONSTRAINT IF NOT EXISTS c0 PRIMARY KEY HASH(f1) INDEX idx0");
        executeUpdate("DROP INDEX IF EXISTS idx0");
    }
}
