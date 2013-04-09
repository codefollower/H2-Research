package my.test.command.ddl;

import my.test.TestBase;

public class CreateLinkedTableTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new CreateLinkedTableTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("DROP TABLE IF EXISTS CreateLinkedTableTest");
        stmt.executeUpdate("CREATE LINKED TABLE IF NOT EXISTS CreateLinkedTableTest " +
        		"('com.mysql.jdbc.Driver','jdbc:mysql://localhost:3306/test'," +
        		"'root', 'zhh', 'BenchWrite') EMIT UPDATES");

        sql = "select * from CreateLinkedTableTest";
        executeQuery();

    }
}
