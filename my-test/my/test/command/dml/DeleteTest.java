package my.test.command.dml;

import my.test.TestBase;

public class DeleteTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new DeleteTest().start();
    }

    @Override
    public void init() throws Exception {
        // 见org.h2.message.TraceSystem
        // 0: OFF， 1: ERROR，2: INFO，3: DEBUG，4: ADAPTER
        // 值越大，那么能跟踪的信息就越详细
        // prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "2");
    }

    void create() throws Exception {

        stmt.executeUpdate("drop table IF EXISTS DeleteTest");
        stmt.executeUpdate("create table IF NOT EXISTS DeleteTest(id int, name varchar(500), b boolean)");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS DeleteTestIndexId ON DeleteTest(id)");

        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS DeleteTestIndexName ON DeleteTest(name)");

        // for(int i=0;i<5000; i++) {
        for (int i = 500; i > 0; i--) {
            stmt.executeUpdate("insert into DeleteTest(id, name, b) values(" + i + ", 'a1', true)");
            stmt.executeUpdate("insert into DeleteTest(id, name, b) values(" + i + ", 'b1', true)");
            stmt.executeUpdate("insert into DeleteTest(id, name, b) values(" + i + ", 'a2', false)");
            stmt.executeUpdate("insert into DeleteTest(id, name, b) values(" + i + ", 'b2', true)");
            stmt.executeUpdate("insert into DeleteTest(id, name, b) values(" + i + ", 'a3', false)");
            stmt.executeUpdate("insert into DeleteTest(id, name, b) values(" + i + ", 'b3', true)");
            // stmt.executeUpdate("insert into DeleteTest(id, name, b) values(1, 'a1', true)");
            // stmt.executeUpdate("insert into DeleteTest(id, name, b) values(1, 'b1', true)");
            // stmt.executeUpdate("insert into DeleteTest(id, name, b) values(2, 'a2', false)");
            // stmt.executeUpdate("insert into DeleteTest(id, name, b) values(2, 'b2', true)");
            // stmt.executeUpdate("insert into DeleteTest(id, name, b) values(3, 'a3', false)");
            // stmt.executeUpdate("insert into DeleteTest(id, name, b) values(3, 'b3', true)");
        }

    }

    // 测试org.h2.command.Parser.parseDelete()
    // org.h2.command.dml.Delete
    @Override
    public void startInternal() throws Exception {
        create();

        // stmt.executeUpdate("drop table IF EXISTS test");
        // stmt.executeUpdate("create table test(a int, b int, primary key(a, b))");
        // stmt.executeUpdate("create unique index c on test(b, a)");
        // stmt.executeUpdate("insert into test values(1, 10), (2, 20)");
        // sql = "select * from (select * from test) where a=1 and b in(10, 20)";
        // executeQuery();

        stmt.executeUpdate("SET OPTIMIZE_REUSE_RESULTS 0");

        sql = "delete top 3 from DeleteTest";
        sql = "delete top 3 from DeleteTest where name='a1'";
        sql = "delete top 3 from DeleteTest where 'a1'>name";
        sql = "delete top 3 from DeleteTest where name = null";
        sql = "delete top 3 from DeleteTest where name != null";
        sql = "delete top 3 from DeleteTest where name > null";

        sql = "delete from DeleteTest where name > 'b1'";
        sql = "delete from DeleteTest where id>2";
        sql = "delete from DeleteTest where 3<2";
        // sql = "delete from DeleteTest where b";
        // sql = "delete from DeleteTest where 3>2";
        // sql = "delete from DeleteTest limit 0"; //limit 0不删除任何行
        // sql = "delete from DeleteTest where id>2";
        //
        // sql = "delete from DeleteTest where id>2 and name='a1'";
        sql = "delete from DeleteTest where id=200";
        stmt.executeUpdate(sql);

        // sql = "delete top 3 from DeleteTest where name > ?";
        // ps = conn.prepareStatement(sql);
        // ps.setString(1, "b1");
        // ps.executeUpdate();

        // query();
    }

    void query() throws Exception {

        sql = "select * from DeleteTest";

        sql = "select * from DeleteTest where name = 'a1'order by name, id";

        sql = "select b, id, name from DeleteTest where name = 'a1'order by name, id";
        sql = "select id, name, b from DeleteTest where name = 'a1'order by name, id";

        stmt.setFetchSize(10000);

        long t1 = 0, t2 = 0;
        int count = 10;
        for (int i = 0; i < count; i++) {
            t1 += executeQuery1();
            t2 += executeQuery2();
        }

        t1 /= count;
        t2 /= count;

        System.out.println("t1 = " + t1);
        System.out.println("t2 = " + t2);
    }

    long executeQuery1() throws Exception {
        sql = "select id, name, b from DeleteTest where name > 'a1' and id>1 order by id";
        sql = "select name, id, b from DeleteTest where name > 'a1' and id>1 order by id";

        sql = "select name, id, b from DeleteTest where id>1 and name > 'a1' order by id";

        // sql = "select b, name, id from DeleteTest where id>1 and name > 'a1' order by id";
        // sql = "select id, name, b from DeleteTest where name > 'a1' and id>1 order by id";
        long t1 = System.currentTimeMillis();
        stmt.executeQuery(sql);
        // executeQuery();
        long t2 = System.currentTimeMillis();

        sql = "EXPLAIN ANALYZE " + sql;
        // executeQuery();

        // System.out.println();

        return (t2 - t1);
    }

    long executeQuery2() throws Exception {
        sql = "select b, name, id from DeleteTest where name > 'a1' and id>1 order by id";
        // sql = "select b, id, name from DeleteTest where name > 'a1' and id>1 order by id";
        long t1 = System.currentTimeMillis();
        stmt.executeQuery(sql);
        // executeQuery();
        long t2 = System.currentTimeMillis();

        sql = "EXPLAIN ANALYZE " + sql;
        // executeQuery();

        // System.out.println();

        return (t2 - t1);
    }
}
