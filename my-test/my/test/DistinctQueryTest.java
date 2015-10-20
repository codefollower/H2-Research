package my.test;

public class DistinctQueryTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new DistinctQueryTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        // stmt.executeUpdate("drop table IF EXISTS DistinctQueryTest");
        // stmt.executeUpdate("create table IF NOT EXISTS DistinctQueryTest(f1 int SELECTIVITY 10, f2 int, f3 int)");
        //
        // stmt.executeUpdate("create index IF NOT EXISTS DistinctQueryTest_i1 on DistinctQueryTest(f1)");
        //
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(1,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(5,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(3,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(8,2,3)");

        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(3,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(8,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(3,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(8,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(3,2,3)");
        // stmt.executeUpdate("insert into DistinctQueryTest(f1, f2, f3) values(8,2,3)");

        sql = "select distinct * from DistinctQueryTest where f1 > 3";
        sql = "select distinct f1 from DistinctQueryTest";
        printResultSet();
    }

}
