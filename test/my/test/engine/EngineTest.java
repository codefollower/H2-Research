package my.test.engine;

import my.test.TestBase;

public class EngineTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new EngineTest().start();
    }

    @Override
    public void init() throws Exception {
        prop.setProperty("FILE_LOCK", "FS");
        prop.setProperty("FILE_LOCK", "SERIALIZED");

        prop.setProperty("OPEN_NEW", "true");
        prop.setProperty("JMX", "true");

        url = "jdbc:h2:file:hbasedb";
    }

    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("SET DB_CLOSE_DELAY -1"); //不马上关闭数据库
        sql = "select sql from SYS";
        executeQuery();
    }
}
