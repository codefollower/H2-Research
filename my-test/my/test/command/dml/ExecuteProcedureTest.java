package my.test.command.dml;

import my.test.TestBase;

//可以测试下面4个类:
//org.h2.engine.Procedure
//org.h2.command.ddl.PrepareProcedure
//org.h2.command.dml.ExecuteProcedure
//org.h2.command.ddl.DeallocateProcedure
public class ExecuteProcedureTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new ExecuteProcedureTest().start();
    }

    @Override
    public void init() throws Exception {
        // prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "2");
    }

    @Override
    public void startInternal() throws Exception {
        executeUpdate("drop table IF EXISTS ExecuteProcedureTest");
        executeUpdate("create table IF NOT EXISTS ExecuteProcedureTest(id int, name varchar(500), b boolean)");
        executeUpdate("CREATE INDEX IF NOT EXISTS ExecuteProcedureTestIndex ON ExecuteProcedureTest(name)");

        executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(1, 'a1', true)");
        executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(1, 'b1', true)");
        executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(2, 'a2', false)");
        executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(2, 'b2', true)");
        executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(3, 'a3', false)");
        executeUpdate("insert into ExecuteProcedureTest(id, name, b) values(3, 'b3', true)");

        sql = "PREPARE mytest (int, varchar2, boolean) AS insert into ExecuteProcedureTest(id, name, b) values(?, ?, ?)";
        executeUpdate(sql);

        sql = "EXECUTE mytest(4, 'b4', true)";
        executeUpdate(sql);

        sql = "select * from ExecuteProcedureTest";
        executeQuery();

        executeUpdate("DEALLOCATE PLAN mytest");
    }
}
