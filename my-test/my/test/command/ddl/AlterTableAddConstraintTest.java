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

public class AlterTableAddConstraintTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new AlterTableAddConstraintTest().start();
    }

    // 在eclipse中打断点条件table.getName().equalsIgnoreCase("mytable")
    @Override
    public void startInternal() throws Exception {
        executeUpdate("DROP TABLE IF EXISTS mytable");
        executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int not null default 10, f2 int not null, f3 int, ch varchar(10))");

        // ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY();
        // ALTER_TABLE_ADD_CONSTRAINT_INDEX();
        // ALTER_TABLE_ADD_CONSTRAINT_CHECK();
        // ALTER_TABLE_ADD_CONSTRAINT_UNIQUE();
        ALTER_TABLE_ADD_CONSTRAINT_REFERENTIAL();
    }

    void ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY() throws Exception {
        executeUpdate("CREATE PRIMARY KEY HASH IF NOT EXISTS myindex ON mytable(f1, f2)");

        // executeUpdate("CREATE INDEX IF NOT EXISTS myindex ON mytable(f1, f2)");

        // 指定INDEX
        // myindex其实无用，见org.h2.command.ddl.AlterTableAddConstraint.tryUpdate()中
        // 在ALTER_TABLE_ADD_CONSTRAINT_PRIMARY_KEY那的注释
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 COMMENT IS 'haha0' PRIMARY KEY HASH(f1,f2) INDEX myindex";

        executeUpdate(sql);

        executeUpdate("DROP INDEX IF EXISTS myindex");

        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c000 COMMENT IS 'haha0' PRIMARY KEY HASH(f1) INDEX myindex";

        // executeUpdate(sql);

        // sql =
        // "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 COMMENT IS 'haha0' PRIMARY KEY HASH(f1)";
        // sql =
        // "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 COMMENT IS 'haha0' PRIMARY KEY HASH(f2, f1)";

    }

    void ALTER_TABLE_ADD_CONSTRAINT_INDEX() throws Exception {
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c1 COMMENT IS 'haha0' INDEX int";
        executeUpdate(sql);
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c1 COMMENT IS 'haha0' INDEX myindex(f1,f2)";
        executeUpdate(sql);
    }

    void ALTER_TABLE_ADD_CONSTRAINT_CHECK() throws Exception {
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c3 COMMENT IS 'haha3' CHECK f1>0 and f2<10 CHECK";
        executeUpdate(sql);
    }

    void ALTER_TABLE_ADD_CONSTRAINT_UNIQUE() throws Exception {
        executeUpdate("DROP TABLE IF EXISTS mytable2");
        executeUpdate("CREATE TABLE IF NOT EXISTS mytable2 (f1 int not null, f2 int not null)");

        executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx0 ON mytable2(f1, f2)");
        executeUpdate("CREATE INDEX IF NOT EXISTS idx1 ON mytable(f1, f2)");
        executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx2 ON mytable(f1, f2, f3)");
        executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx3 ON mytable(f2, f3)");
        executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx4 ON mytable(f1, f2)");
        executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS idx5 ON mytable(f1, f2)");

        // idx0索引是mytable2表的，不是mytable，所以INDEX idx0无效，但是不报错
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f2) INDEX idx0 NOCHECK";

        // idx1索引不是UNIQUE索引，所以INDEX idx1无效，但是不报错
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f2) INDEX idx1 NOCHECK";

        // idx2是UNIQUE索引，但是它的索引字段是f1, f2, f3, 而这里只有f1, f2，所以INDEX idx2无效，但是不报错
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f2) INDEX idx2 NOCHECK";

        // idx3是UNIQUE索引，索引字段个数与myunique一样，
        // 但是它的索引字段是f2, f3, 而这里是f1, f2，所以INDEX idx3无效，但是不报错
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f2) INDEX idx3 NOCHECK";

        // idx4是正确的
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f2) INDEX idx4 NOCHECK";

        // idx5也是正确的，因为idx5中的索引字段只有f1,f2，因为f1,f2保证唯一了，那么f1,f2,f3也是唯一的
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f2,f3) INDEX idx5 NOCHECK";

        // 如果没有指定INDEX，则从已存在的唯一索引中选择一个合适的，如果没有合适的就自动创建一个
        // 因为idx4满足了，所以使用idx4
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f2) NOCHECK";

        // 不存在满足f1,f3的唯一索引，所以要自动创建一个
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 UNIQUE KEY INDEX myunique(f1,f3) NOCHECK";

        executeUpdate(sql);

        // 因为此时idx4己属于myunique2约束，所以不能删除idx4索引
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c5 UNIQUE KEY INDEX myunique2(f1,f2) INDEX idx4 NOCHECK";
        executeUpdate(sql);
        // 抛异常: Index "IDX4" belongs to a constraint; SQL statement: DROP INDEX
        // IF EXISTS idx4 [90085-171]
        tryExecuteUpdate("DROP INDEX IF EXISTS idx4");
    }

    void ALTER_TABLE_ADD_CONSTRAINT_REFERENTIAL() throws Exception {
        executeUpdate("DROP TABLE IF EXISTS mytable");
        executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int default 10, f2 int not null, f3 int, ch varchar(10))");

        executeUpdate("DROP TABLE IF EXISTS mytable2");
        executeUpdate("CREATE TABLE IF NOT EXISTS mytable2 (f1 int PRIMARY KEY, f2 int not null)");

        executeUpdate("DROP TABLE IF EXISTS mytable3");
        executeUpdate("CREATE TABLE IF NOT EXISTS mytable3 (f1 int, f2 int not null)");

        // 如果未指定引用列，则默认使用引用表中的主键列，这里会默认使用mytable2的f1字段，因为f1是PRIMARY KEY
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1) REFERENCES mytable2";
        executeUpdate(sql);

        // 如果引用表没有PRIMARY KEY，则抛异常:
        // Exception in thread "main" org.h2.jdbc.JdbcSQLException: Index
        // "PRIMARY_KEY_" not found; SQL statement:
        // ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1)
        // REFERENCES mytable3 [42112-171]
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c1 FOREIGN KEY(f1) REFERENCES mytable3";
        tryExecuteUpdate(sql);

        // 外部列与引用列的个数不一样时，也抛错: org.h2.jdbc.JdbcSQLException: Column count does
        // not match;
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1, f2) REFERENCES mytable2";

        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1) REFERENCES mytable2 CHECK";

        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1) REFERENCES mytable2 ON DELETE CASCADE CHECK";
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1) REFERENCES mytable2 ON DELETE SET DEFAULT CHECK";

        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1) REFERENCES mytable2 ON UPDATE CASCADE CHECK";
        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c3 FOREIGN KEY(f1) REFERENCES mytable2 ON UPDATE SET DEFAULT CHECK";

        // sql =
        // "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c3 FOREIGN KEY(f1) REFERENCES mytable2 ON UPDATE RESTRICT";

        // sql =
        // "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c0 FOREIGN KEY(f1) REFERENCES(f2)";

        sql = "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c4 FOREIGN KEY(f1) REFERENCES mytable2 ON UPDATE SET DEFAULT ON DELETE CASCADE";

        executeUpdate(sql);

        executeUpdate("SET REFERENTIAL_INTEGRITY 0");
        executeUpdate("insert into mytable2(f1, f2) values(1,1)");
        executeUpdate("SET REFERENTIAL_INTEGRITY 1");

        executeUpdate("ALTER TABLE mytable2 SET REFERENTIAL_INTEGRITY FALSE");
        executeUpdate("insert into mytable2(f1, f2) values(2,2)");
        executeUpdate("ALTER TABLE mytable2 SET REFERENTIAL_INTEGRITY TRUE CHECK");

        executeUpdate("insert into mytable2(f1, f2) values(3,3)");

        executeUpdate("insert into mytable(f1, f2) values(2,2)");
        executeUpdate("insert into mytable(f1, f2) values(3,3)");

        // 注意: 为引用字段建立了唯一索引，所以f2是三时，如果是自引用，就抛异常
        // executeUpdate("insert into mytable(f1, f2) values(2,3)");

        executeUpdate("insert into mytable(f1, f2) values(2,4)");

        // Referential integrity constraint violation
        tryExecuteUpdate("update mytable set f1=20 where f1=2");

        executeUpdate("insert into mytable(f1, f2) values(null,2)");

        executeUpdate("update mytable set f1=null where f1=2");
        executeUpdate("update mytable set f2=30 where f1=3");

        executeUpdate("delete from mytable where f1 = 2");

        // executeUpdate("update mytable2 set f2=10 where f1=1");

        // 如果是ON UPDATE RESTRICT，当更新引用表的记录在外部表中存在时不允许更新
        // 比如此列f1是2，在mytable中存在，所以不允许更新
        // 如果是ON UPDATE SET DEFAULT，可以为mytable的f1指定一个默认值，比如10，
        // 虽然10不出现在mytable2中，但这是允许的
        executeUpdate("update mytable2 set f1=20 where f1=2");

        executeUpdate("update mytable2 set f2=30 where f1=3");

        sql = "select * from mytable";

        executeQuery();

        // 这是允许的，因为在mytable中不存在3
        // executeUpdate("update mytable2 set f1=30 where f1=3");

        //
        // sql =
        // "ALTER TABLE mytable ADD CONSTRAINT IF NOT EXISTS c6 COMMENT IS 'haha6' FOREIGN KEY(f1) REFERENCES mytable(f2)"
        // +
        // "ON DELETE CASCADE ON UPDATE RESTRICT ON DELETE NO ACTION ON UPDATE SET NULL ON DELETE SET DEFAULT NOT DEFERRABLE";

    }
}
