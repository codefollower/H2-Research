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

public class CreateViewTest extends TestBase {
    public static void main(String[] args) throws Exception {

        new CreateViewTest().start();
        System.out.println(Integer.bitCount(3));
        System.out.println(Integer.bitCount(1));
        System.out.println(Integer.bitCount(2));
        System.out.println(Integer.bitCount(7));
    }

    // 测试org.h2.command.Parser.parseCreateView(boolean, boolean)
    // org.h2.command.ddl.CreateView
    @Override
    public void startInternal() throws Exception {
        executeUpdate("drop table IF EXISTS CreateViewTest CASCADE");
        executeUpdate("create table IF NOT EXISTS CreateViewTest(id int, name varchar(500), b boolean)");
        executeUpdate("CREATE INDEX IF NOT EXISTS CreateViewTestIndex ON CreateViewTest(name)");

        executeUpdate("insert into CreateViewTest(id, name, b) values(1, 'a1', true)");
        executeUpdate("insert into CreateViewTest(id, name, b) values(1, 'b1', true)");
        executeUpdate("insert into CreateViewTest(id, name, b) values(2, 'a2', false)");
        executeUpdate("insert into CreateViewTest(id, name, b) values(2, 'b2', true)");
        executeUpdate("insert into CreateViewTest(id, name, b) values(3, 'a3', false)");
        executeUpdate("insert into CreateViewTest(id, name, b) values(3, 'b3', true)");

        // executeUpdate("DROP VIEW IF EXISTS my_view");
        sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view COMMENT IS 'my view'(f1,f2) " //
                + "AS SELECT id,name FROM CreateViewTest";

        executeUpdate();

        sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1,f2) " //
                + "AS SELECT id,name FROM CreateViewTest";

        // select字段个数比view字段多的情况，多出来的按select字段原来的算
        // 这里实际是f1、name
        sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1) " //
                + "AS SELECT id,name FROM CreateViewTest";

        // select字段个数比view字段少的情况，view中少的字段被忽略
        // 这里实际是f1，而f2被忽略了，也不提示错误
        sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
                + "AS SELECT id FROM CreateViewTest";

        // 不管加不加FORCE，跟上面也一样
        sql = "CREATE OR REPLACE VIEW my_view COMMENT IS 'my view'(f1, f2) " //
                + "AS SELECT id FROM CreateViewTest";

        sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1,f2) " //
                + "AS SELECT id,name FROM CreateViewTest";

        executeUpdate("CREATE OR REPLACE FORCE VIEW view1 AS SELECT f1 FROM my_view");
        executeUpdate("CREATE OR REPLACE FORCE VIEW view2 AS SELECT f2 FROM my_view");

        // sql = "CREATE OR REPLACE FORCE VIEW my_view COMMENT IS 'my view'(f1,f2) " //
        // + "AS SELECT top 2 id,name FROM CreateViewTest order by id";
        //
        // 如果是这种情况，接下来要查视图时也要按CreateViewTest的字段名来查
        // sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view " //
        // + "AS SELECT id,name FROM CreateViewTest";

        // 目前不支持参数:
        // org.h2.jdbc.JdbcSQLException: Feature not supported: "parameters in views"; SQL statement:
        // sql =
        // "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view (f1,f2) AS SELECT id,name FROM CreateViewTest where id=?";
        // ps = conn.prepareStatement(sql);
        // ps.setInt(1, 2);
        // ps.executeUpdate();
        executeUpdate(sql);

        sql = "select * from my_view where f1 > 2";
        sql = "select * from my_view where f2 > 'b1'";
        sql = "select * from my_view where f2 between 'b1' and 'b2'";

        // executeUpdate("SET BATCH_JOINS 1");

        sql = "select * from my_view where f1=2 and f2 between 'b1' and 'b2'";
        executeQuery();

        // sql = "select name from (select id,name from CreateViewTest where id=? and name=?) where name='b2'";
        // ps = conn.prepareStatement(sql);
        // ps.setInt(1, 2);
        // ps.setString(2, "b2");
        // ps.executeQuery();

        // sql = "select * from CreateViewTest";

        // 测试org.h2.command.Parser.parserWith()
        // executeUpdate("CREATE LOCAL TEMPORARY TABLE IF NOT EXISTS my_tmp_table(f1 int)");
        // executeUpdate("DROP VIEW IF EXISTS my_tmp_table");
        // executeUpdate("CREATE OR REPLACE FORCE VIEW my_tmp_table AS SELECT f2 FROM my_view");
        // sql =
        // "WITH RECURSIVE my_tmp_table(f1,f2) AS(select id,name from CreateViewTest) select f1, f2 from my_tmp_table";
        // sql = "WITH my_tmp_table(f1,f2) AS(select id,name from CreateViewTest) select f1, f2 from my_tmp_table";

        // AS里面必须是UNION ALL
        sql = "WITH RECURSIVE my_tmp_table(f1,f2) " //
                + "AS(select id,name from CreateViewTest UNION ALL select 1, 2)" //
                + "select f1, f2 from my_tmp_table";

        executeQuery();

        sql = "WITH RECURSIVE my_tmp_table(f1,f2) " //
                + "AS(select id,name from CreateViewTest UNION ALL select id,name from CreateViewTest)" //
                + "select f1, f2 from my_tmp_table";

        executeQuery();

        executeUpdate("drop table IF EXISTS tb");
        executeUpdate("create table IF NOT EXISTS tb(id varchar(3) , pid varchar(3) , name varchar(10))");
        executeUpdate("insert into tb values('002' , 0 , '浙江省')");
        executeUpdate("insert into tb values('001' , 0 , '广东省')");
        executeUpdate("insert into tb values('006' , '002' , '嘉兴市')");
        executeUpdate("insert into tb values('004' , '002' , '杭州市')");
        executeUpdate("insert into tb values('014' , '004' , '上城区')");
        executeUpdate("insert into tb values('015' , '004' , '下城区')");
        executeUpdate("insert into tb values('019' , '001' , '广州市')");
        executeUpdate("insert into tb values('020' , '001' , '深圳市')");

        sql = "WITH RECURSIVE cte (id,name)" //
                + " AS(select a.id,a.name,a.pid from tb a where id='002'" //
                + "    union all select k.id,k.name,k.pid from tb k inner join cte c on c.id = k.pid)" //
                + " select id,name from cte";

        executeQuery();

        sql = "WITH RECURSIVE cte (id,name) " //
                + " AS(select a.id,cast(a.name as varchar(100)) from tb a where id='002'" //
                + "    union all "//
                + "      select k.id,cast(c.name||'>'||k.name as varchar(100)) as name "//
                + "        from tb k inner join cte c on c.id = k.pid)" //
                + " select id,name from cte";

        executeQuery();

        // 必须在from后面加括号，此时from后面的被认为是一个临时视图
        sql = "select f1, f2 from (select id,name from CreateViewTest)"; // f1,f2找不到
        sql = "select id,name from (select id,name from CreateViewTest)";
        executeQuery();

        // 这条不会使得parameters.size>0
        sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view2(f1,f2) " //
                + "AS select id,name from (select id,name from CreateViewTest) where id=? and name=?";

        ps = conn.prepareStatement(sql);
        ps.setInt(1, 2);
        ps.setString(2, "b2");
        try {
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        // 这条可以
        sql = "CREATE OR REPLACE FORCE VIEW IF NOT EXISTS my_view2(f1,f2) " //
                + "AS select id,name from (select id,name from CreateViewTest where id=? and name=?)";

        ps = conn.prepareStatement(sql);
        ps.setInt(1, 2);
        ps.setString(2, "b2");
        try {
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        sql = "select id,name from (select id,name from CreateViewTest where id=? and name=?)";
        ps = conn.prepareStatement(sql);
        ps.setInt(1, 2);
        ps.setString(2, "b2");
        rs = ps.executeQuery();
        printResultSet(rs);

        sql = "select * from my_view";
        sql = "select * from my_view where f1=2 and f2 between 'b1' and 'b2'";

        stmt.setFetchSize(2);
        executeQuery();

        executeUpdate("alter view my_view RECOMPILE");

        executeUpdate("CREATE OR REPLACE FORCE VIEW view5 AS SELECT f1 FROM my_view");
        tryExecuteUpdate("drop view IF EXISTS my_view RESTRICT");
        executeUpdate("drop view IF EXISTS my_view CASCADE");
    }
}