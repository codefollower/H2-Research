package my.test.command.dml;

import my.test.TestBase;

public class ShowTest extends TestBase {

    public static void main(String[] args) throws Exception {
        new ShowTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        sql = "SHOW SCHEMAS";
        executeQuery();

        sql = "SHOW TABLES";
        executeQuery();
    }

}
