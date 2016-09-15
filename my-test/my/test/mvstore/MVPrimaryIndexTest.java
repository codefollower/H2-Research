package my.test.mvstore;

import my.test.TestBase;

public class MVPrimaryIndexTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new MVPrimaryIndexTest().start();
    }

    @Override
    public void init() throws Exception {
        prop.setProperty("MULTI_THREADED", "true");
    }

    @Override
    public void startInternal() throws Exception {
        executeUpdate("DROP TABLE IF EXISTS MVPrimaryIndexTest CASCADE");

        // 从1.4版本开始默认使用MVStore存储引擎
        executeUpdate("CREATE TABLE IF NOT EXISTS MVPrimaryIndexTest(id int not null, name varchar(500), b boolean) "
                + "ENGINE \"org.h2.mvstore.db.MVTableEngine\"");

        executeUpdate("CREATE TABLE IF NOT EXISTS MVPrimaryIndexTest(id int not null, name varchar(500), b boolean)");

        // executeUpdate("CREATE INDEX IF NOT EXISTS MVPrimaryIndexTestIndex ON MVPrimaryIndexTest(name)");

        executeUpdate("CREATE PRIMARY KEY IF NOT EXISTS idx_id ON MVPrimaryIndexTest(id)");

        // executeUpdate("CREATE UNIQUE HASH INDEX idx_name ON MVPrimaryIndexTest(name)");

        // conn.setAutoCommit(false);
        executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(10, 'a1', true)");
        executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(20, 'b1', true)");
        executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(30, 'a2', false)");
        executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(40, 'b2', true)");
        executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(50, 'a3', false)");
        executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(60, 'b3', true)");
        executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(70, 'b3', true)");

        executeUpdate("CREATE INDEX idx_name2 ON MVPrimaryIndexTest(name)");

        // executeUpdate("insert into MVPrimaryIndexTest(id, name, b) values(70, 'b3', true)");

        // 当更新记录时，会删除原来的记录，再插入新记录，但是新记录的rowKey是0，不会沿用原来的rowKey
        executeUpdate("update MVPrimaryIndexTest set b=false where id=70");

        // conn.commit();
        sql = "select * from MVPrimaryIndexTest";

        // 见org.h2.index.Index.findNext(Session, SearchRow, SearchRow)中的注释
        sql = "select distinct name from MVPrimaryIndexTest";
        executeQuery();

        sql = "select min(id) from MVPrimaryIndexTest";
        executeQuery();

        sql = "select max(id) from MVPrimaryIndexTest";
        executeQuery();

        executeUpdate("truncate table MVPrimaryIndexTest");
    }
}
