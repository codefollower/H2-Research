package my.test.command.dml;

import my.test.TestBase;

public class UpdateTest2 extends TestBase {
    public static void main(String[] args) throws Exception {
        new UpdateTest2().start();
    }

    @Override
    public void init() throws Exception {
        prop.setProperty("LARGE_TRANSACTIONS", "false");
    }

    void createTable() throws Exception {
        stmt.executeUpdate("drop table IF EXISTS UpdateTest");
        stmt.executeUpdate("create table IF NOT EXISTS UpdateTest(id int, name varchar(4))");
        stmt.executeUpdate("CREATE INDEX IF NOT EXISTS UpdateTestIndex ON UpdateTest(name)");

        stmt.executeUpdate("insert into UpdateTest(id, name) values(1, 'a1')");
        stmt.executeUpdate("insert into UpdateTest(id, name) values(1, 'b1')");
        stmt.executeUpdate("insert into UpdateTest(id, name) values(2, 'a22')");
        stmt.executeUpdate("insert into UpdateTest(id, name) values(2, 'b2')");
        stmt.executeUpdate("insert into UpdateTest(id, name) values(3, 'a3')");
        stmt.executeUpdate("insert into UpdateTest(id, name) values(3, 'b3')");
    }

    // 测试org.h2.command.Parser.parseUpdate()
    // org.h2.command.dml.Update
    @Override
    public void startInternal() throws Exception {
        // createTable();

        sql = "update UpdateTest set(id) = ('123',10)"; // 错误
        sql = "update UpdateTest set(id) = (10,'123')"; // 错误
        sql = "update UpdateTest set(id) = (10)";
        sql = "update UpdateTest set(name, id) = ('123',10)";

        sql = "update UpdateTest set name = DEFAULT, id=10 where id>2 limit 3";
        // 不允许指定相同的列
        // sql = "update UpdateTest set(name, id, id) = ('123',10,10)"; //Duplicate column name "ID";

        // sql = "update UpdateTest set name = name || 'aa'";
        // sql = "update UpdateTest set name = name || 'a'";
        // stmt.executeUpdate("SET MAX_OPERATION_MEMORY 100"); //默认是10万
        // stmt.executeUpdate("SET UNDO_LOG 0"); //默认是1，也就是true，开启撤消日志
        // stmt.executeUpdate("SET MAX_MEMORY_UNDO 3"); //默认是5万

        stmt.executeUpdate("insert into UpdateTest(id, name) values(98, 'b98')");
        sql = "update UpdateTest set name = 'b988' where id=98";
        stmt.executeUpdate(sql);

        // sql = "update UpdateTest set(name, id) = (?,10)";
        // //测试org.h2.command.dml.Update.setAssignment(Column, Expression)中的if (expression instanceof Parameter)
        // sql = "update UpdateTest set(name) = (?)";
        // ps = conn.prepareStatement(sql);
        // ps.setString(1, "b1");
        // ps.executeUpdate();

        sql = "select * from UpdateTest";
        executeQuery();

        sql = "select count(*) from UpdateTest limit 1";
        executeQuery();

        sql = "select * from UpdateTest limit 1";
        executeQuery();

        sql = "select ord from ( SELECT rownum() as ord, * FROM UpdateTest where id=1 order by name) where name='b1'";
        executeQuery();
    }
}
