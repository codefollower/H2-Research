package my.test.command.dml;

import my.test.TestBase;

public class CallTest extends TestBase {

    public static void main(String[] args) throws Exception {
        new CallTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("drop table IF EXISTS CallTest");
        stmt.executeUpdate("create table IF NOT EXISTS CallTest(id int, name varchar(500), b boolean)");

        stmt.executeUpdate("insert into CallTest(id, name, b) values(1, 'a1', true)");
        stmt.executeUpdate("insert into CallTest(id, name, b) values(2, 'a2', false)");
        stmt.executeUpdate("insert into CallTest(id, name, b) values(3, 'a3', false)");

        sql = "CALL select * from CallTest where id=1";
        sql = "CALL TABLE(ID INT=(1, 2), NAME VARCHAR=('Hello', 'World'))";
        executeQuery();

        sql = "CALL CURTIME()";
        executeQuery();

        sql = "CALL ABS(-100)";
        System.out.println(executeUpdate());
    }

}
