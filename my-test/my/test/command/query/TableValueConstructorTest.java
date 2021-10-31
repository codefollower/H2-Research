package my.test.command.query;

import my.test.TestBase;

public class TableValueConstructorTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new TableValueConstructorTest().start();
    }

    @Override
    public void startInternal() throws Exception { 
        sql = "SELECT * FROM VALUES(1, 'Hello'), (2, 'World')";
        sql = "VALUES(1, 'Hello'), (2, 'World')";
        executeQuery();
    }
}
