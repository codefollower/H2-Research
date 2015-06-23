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

public class CreateTableTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new CreateTableTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        executeUpdate("DROP TABLE IF EXISTS mytable3");
        executeUpdate("DROP TABLE IF EXISTS mytable2");
        executeUpdate("DROP TABLE IF EXISTS mytable1");

        sql = "CREATE MEMORY LOCAL TEMPORARY TABLE IF NOT EXISTS t1";
        // 以下三者等价
        sql = "CREATE MEMORY GLOBAL TEMPORARY TABLE IF NOT EXISTS t2";
        sql = "CREATE MEMORY TEMP TABLE IF NOT EXISTS t3";
        sql = "CREATE MEMORY TEMPORARY TABLE IF NOT EXISTS t4";

        sql = "CREATE TABLE IF NOT EXISTS mytable1 COMMENT IS 'my table'";

        executeUpdate("CREATE SCHEMA IF NOT EXISTS myschema AUTHORIZATION SA");
        // sql = "DROP TABLE IF EXISTS myschema.mytable";
        executeUpdate("DROP TABLE IF EXISTS mytable");

        // executeUpdate(sql);

        // executeUpdate("CREATE TABLE IF NOT EXISTS mytable1 (f1 int,PRIMARY KEY(f1), f2 int not null)");
        // executeUpdate("CREATE INDEX IF NOT EXISTS myindex ON mytable1(f2)");
        //
        // executeUpdate("CREATE TABLE IF NOT EXISTS mytable2 (f1 int PRIMARY KEY, f2 int REFERENCES(f1))");
        //
        // executeUpdate("CREATE TABLE IF NOT EXISTS mytable3 (f1 int REFERENCES mytable1(f2) INDEX myindex ON DELETE CASCADE)");

        // parseAlterTableAddConstraintIf();
        // parseColumnWithType();
        // parseColumnForTable();
        parseCreateTable(); // 测试Parser.parseCreateTable在调用完parseColumnForTable之后的代码

        sql = "CREATE TABLE IF NOT EXISTS mytable3 ("
                + "f1 int CONSTRAINT c1 PRIMARY KEY HASH AUTO_INCREMENT(1000, 10), " + // 此时CONSTRAINT名无用
                "f2 int CONSTRAINT c2 UNIQUE, " + //
                "f3 int CONSTRAINT c3 NOT NULL, " + //
                "f4 int CONSTRAINT c4 NULL , " + //
                "f5 int CONSTRAINT c5 NOT NULL CHECK f5>0, " + //
                "f6 int CONSTRAINT c6 NOT NULL REFERENCES(f1) , " + // 自引用
                "f7 int CONSTRAINT c7 NOT NULL REFERENCES mytable1(f1) , " + //
                "f8 int CONSTRAINT c8 NOT NULL REFERENCES mytable1(f1) ON DELETE CASCADE " + //
                ")";
        // executeUpdate(sql);

    }

    void parseAlterTableAddConstraintIf() throws Exception {
        sql = "CREATE TABLE IF NOT EXISTS mytable3 (f1 int, CONSTRAINT c1 PRIMARY KEY(f1), f2 int, CONSTRAINT c2 PRIMARY KEY(f2))";

        sql = "CREATE TABLE IF NOT EXISTS mytable3 (f1 int, CONSTRAINT c1 PRIMARY KEY(f1), f2 int)";

        sql = "CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint0 COMMENT IS 'haha0' PRIMARY KEY HASH(f1,f2) INDEX myindex," //
                // +
                // "CONSTRAINT IF NOT EXISTS myschema.my_constraint8 COMMENT IS 'haha0' PRIMARY KEY HASH(f1) INDEX myindex,"

                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint1 COMMENT IS 'haha1' INDEX int," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint7 COMMENT IS 'haha7' INDEX myindex(f1,f2)," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint2 COMMENT IS 'haha2' INDEX(f1,f2)," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint3 COMMENT IS 'haha3' CHECK f1>0 and f2<10 CHECK," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint4 COMMENT IS 'haha4' UNIQUE KEY INDEX myunique(f1,f2) NOCHECK," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint5 COMMENT IS 'haha5' FOREIGN KEY(f1) REFERENCES(f2)," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint6 COMMENT IS 'haha6' FOREIGN KEY(f1) REFERENCES myschema.mytable(f2) " //
                + "ON DELETE CASCADE ON UPDATE RESTRICT ON DELETE NO ACTION ON UPDATE SET NULL ON DELETE SET DEFAULT NOT DEFERRABLE)";

        executeUpdate(sql);
    }

    void parseColumnForTable() throws Exception {
        stmt.executeUpdate("DROP SEQUENCE IF EXISTS mytable_myseq");
        stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS mytable_myseq START WITH 1000 INCREMENT BY 1 CACHE 20");

        sql = "CREATE TABLE IF NOT EXISTS mytable (" + //
                // "f1 IDENTITY, " + //
                "f2 IDENTITY(1, 10), " + //
                "f3 BIGSERIAL BIGSERIAL(1, 10), " + //
                "f4 SERIAL(1, 10) , " + //
                "f5 int NOT NULL, " + //
                "f6 int NULL , " + //
                "f7 int AS 10 , " + //
                "f8 int DEFAULT 10 " + //
                "f9 int GENERATED BY DEFAULT AS IDENTITY (START WITH 1, INCREMENT BY 1)," + //
                "f10 int AUTO_INCREMENT, " + //
                "f11 int IDENTITY, " + //
                "f12 int NULL_TO_DEFAULT, " + //
                "f13 int SEQUENCE mytable_myseq, " + //
                "f14 int SELECTIVITY 10 COMMENT IS 'a column comment', " + //
                "f99 int" + //
                ")";

        executeUpdate(sql);
    }

    void parseCreateTable() throws Exception {
        sql = "CREATE TABLE IF NOT EXISTS mytable (" + //
                "f2 IDENTITY(1, 10) CONSTRAINT pk PRIMARY KEY HASH AUTO_INCREMENT, " + //
                "f5 int NOT NULL UNIQUE NOT NULL, " + //
                "f6 int NULL CHECK f6>10, " + //
                "f7 int CONSTRAINT c8 NOT NULL REFERENCES mytable(f2) ON DELETE CASCADE, " + //
                "f99 int" + //
                ")";

        executeUpdate(sql);
    }

    void parseColumnForTable0() throws Exception {

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int)";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 IDENTITY)";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 IDENTITY(1,10))";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 SERIAL(1,10)))";

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 IDENTITY(1,10),PRIMARY KEY(f1))";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int,PRIMARY KEY(f1))";

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int, f2 int AS 10 )";

        executeUpdate("DROP SEQUENCE IF EXISTS myseq");
        executeUpdate("CREATE SEQUENCE IF NOT EXISTS myseq START WITH 1000 INCREMENT BY 1 CACHE 20");

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int, f2 int NULL_TO_DEFAULT SEQUENCE myseq) ";
        // sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int, f2 int NOT NULL NULL_TO_DEFAULT SEQUENCE myseq) ";

        executeUpdate(sql);

        executeUpdate("insert into mytable(f1) values(1)");
        executeUpdate("insert into mytable(f1) values(2)");
        sql = "select * from mytable";
        executeQuery();
    }

    void parseColumnWithType() throws Exception {
        executeUpdate("DROP DOMAIN IF EXISTS EMAIL");
        // VALUE是CREATE DOMAIN语句的默认临时列名
        executeUpdate("CREATE DOMAIN IF NOT EXISTS EMAIL "
                + "AS VARCHAR(255) default 'abc' SELECTIVITY 10 CHECK (POSITION('@', VALUE) > 1)");

        // executeUpdate("SET IGNORECASE 1");

        sql = "CREATE TABLE IF NOT EXISTS mytable (" + //
                "f1 LONG RAW, " + //
                "f2 DOUBLE PRECISION, " + //
                "f3 CHARACTER VARYING, " + //
                "f4 EMAIL , " + //
                // "f5 int CONSTRAINT c5 NOT NULL CHECK f5>0, " + //
                // "f6 int CONSTRAINT c6 NOT NULL REFERENCES(f1) , " + //自引用
                // "f7 int CONSTRAINT c7 NOT NULL REFERENCES mytable1(f1) , " + //
                // "f8 int CONSTRAINT c8 NOT NULL REFERENCES mytable1(f1) ON DELETE CASCADE " + //
                "f9 int, " + //
                "f10 DECIMAL(2m, 3), " + //
                "f11 TIMESTAMP(23, 5), " + //
                "f12 TIMESTAMP(5), " + // 相当于TIMESTAMP(23, 5)
                "f13 VARCHAR FOR BIT DATA, " + //
                "f99 int" + //
                ")";

        executeUpdate(sql);

        // executeUpdate("insert into mytable(f1) values(1)");
        // executeUpdate("insert into mytable(f1) values(2)");
        // sql = "select * from mytable";
        // executeQuery();
    }

    // @Override
    public void startInternal0() throws Exception {

        // executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' INDEX int)");

        // executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,f2 int,CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' INDEX my_int(f1,f2))");
        // executeUpdate("CREATE TABLE IF NOT EXISTS TEST9.public.mytable (f1 int,f2 int,"
        // + "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' CHECK f1>0)");
        // executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,f2 int,"
        // +
        // "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' UNIQUE KEY INDEX my_constraint2(f1,f2) INDEX myi)");
        // executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,f2 int,"
        // + "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' FOREIGN KEY(f1,f2))  INDEX my-i REFERENCES(f1)");

        // executeUpdate("CREATE CACHED GLOBAL TEMPORARY TABLE IF NOT EXISTS mytable (f1 int)");
        // executeUpdate("CREATE CACHED GLOBAL TEMPORARY TABLE IF NOT EXISTS TEST9.SESSION.mytable (f1 int)");

        // 表名schema和约束schema必须一样
        // executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int,"
        // + "CONSTRAINT IF NOT EXISTS public.my_constraint COMMENT IS 'haha' PRIMARY KEY HASH(f1,f2))");

        // executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int,"
        // +
        // "CONSTRAINT IF NOT EXISTS myschema.my_constraint COMMENT IS 'haha' PRIMARY KEY HASH(f1,f2) INDEX myindex)");

        // executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int,"
        // + "CONSTRAINT IF NOT EXISTS myschema.my_constraint1 COMMENT IS 'haha1' INDEX myindex(f1,f2),"
        // + "CONSTRAINT IF NOT EXISTS myschema.my_constraint2 COMMENT IS 'haha2' INDEX(f1,f2),"
        // + "CONSTRAINT IF NOT EXISTS myschema.my_constraint3 COMMENT IS 'haha3' CHECK f1>0 and f2<10 CHECK,"
        // +
        // "CONSTRAINT IF NOT EXISTS myschema.my_constraint4 COMMENT IS 'haha4' UNIQUE KEY INDEX myunique(f1,f2) NOCHECK,"
        // + "CONSTRAINT IF NOT EXISTS myschema.my_constraint5 COMMENT IS 'haha5' FOREIGN KEY(f1) REFERENCES(f2),"
        // +
        // "CONSTRAINT IF NOT EXISTS myschema.my_constraint6 COMMENT IS 'haha6' FOREIGN KEY(f1) REFERENCES myschema.mytable(f2) "
        // +
        // "ON DELETE CASCADE ON UPDATE RESTRICT ON DELETE NO ACTION ON UPDATE SET NULL ON DELETE SET DEFAULT NOT DEFERRABLE)");

        executeUpdate("set IGNORECASE true");
        executeUpdate("CREATE DATATYPE IF NOT EXISTS EMAIL AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");
        executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (" //
                + "f0 varchar(MAX),f00 varchar(100K CHAR),f01 DECIMAL(100K CHAR, 90)," //
                + "f1 LONG RAW, f2 DOUBLE PRECISION, f3 CHARACTER VARYING, f4 EMAIL, f5 varchar, f6 varchar FOR BIT DATA)");
    }
}
