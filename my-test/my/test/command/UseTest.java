package my.test.command;

import my.test.TestBase;

public class UseTest extends TestBase {

    public static void main(String[] args) throws Exception {
        new UseTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        sql = "use test2";
        executeUpdate(sql);
    }

}
