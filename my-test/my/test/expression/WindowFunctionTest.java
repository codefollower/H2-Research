package my.test.expression;

import my.test.TestBase;

public class WindowFunctionTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new WindowFunctionTest().start();
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void startInternal() throws Exception {
        // createTable();
        String sql = "SELECT *, ROW_NUMBER() OVER W1, ROW_NUMBER() OVER W2 FROM TEST  group by id "
                + "WINDOW W1 AS (W2 ORDER BY ID), W2 AS (PARTITION BY CATEGORY ORDER BY ID DESC)";
        executeQuery(sql);
    }

    void createTable() throws Exception {
        stmt.executeUpdate("drop table IF EXISTS TEST");
        stmt.executeUpdate("CREATE TABLE TEST(ID INT, R INT, CATEGORY INT)");
        stmt.executeUpdate("INSERT INTO TEST VALUES (1, 4, 1), (2, 3, 1), (3, 2, 2), (4, 1, 2)");
    }
}
