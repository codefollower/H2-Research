package my.test.expression;

import my.test.TestBase;

public class AggregateTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new AggregateTest().start();
    }

    // 测试org.h2.command.Parser.readAggregate(int)
    // org.h2.expression.Aggregate
    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("drop table IF EXISTS AggregateTest");
        stmt.executeUpdate("create table IF NOT EXISTS AggregateTest(id int, name varchar(500))");
        // stmt.executeUpdate("ALTER TABLE AggregateTest ALTER COLUMN id SELECTIVITY 10");
        // stmt.executeUpdate("ALTER TABLE AggregateTest ALTER COLUMN name SELECTIVITY 10");
        // stmt.executeUpdate("ALTER TABLE AggregateTest ADD CONSTRAINT NAME_UNIQUE UNIQUE(name,id)");

        // stmt.executeUpdate("insert into AggregateTest(id, name) values(1, 'a1')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(1, 'b1')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(2, 'a2')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(2, 'b2')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(3, 'a3')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(3, 'b3')");

        // stmt.executeUpdate("insert into AggregateTest(id, name) values(1, 'a1')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(2, 'b1')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(3, 'a2')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(1, 'b2')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(2, 'a3')");
        // stmt.executeUpdate("insert into AggregateTest(id, name) values(3, 'b3')");
        //
        stmt.executeUpdate("insert into AggregateTest(id, name) values(1, 'a1')");
        stmt.executeUpdate("insert into AggregateTest(id, name) values(3, 'b1')");
        stmt.executeUpdate("insert into AggregateTest(id, name) values(5, 'a2')");
        stmt.executeUpdate("insert into AggregateTest(id, name) values(7, 'b2')");
        stmt.executeUpdate("insert into AggregateTest(id, name) values(9, 'a3')");
        stmt.executeUpdate("insert into AggregateTest(id, name) values(11, 'b3')");

        // for(int i=1;i<=30;i++)
        // stmt.executeUpdate("insert into AggregateTest(id, name) values("+i+", 'a2')");

        sql = "select distinct name from AggregateTest";
        sql = "select count(*) from AggregateTest";
        // 不允许这样
        sql = "select count(DISTINCT *) from AggregateTest";
        // 只能这样
        // 但是h2似乎有bug(注:应该不是bug，AggregateTest.*是所有字段，加DISTINCT它并不知道要应用于哪个字段
        sql = "select count(DISTINCT AggregateTest.*) from AggregateTest";

        sql = "select count(AggregateTest.*) from AggregateTest"; // 这样就没问题

        sql = "select count(DISTINCT AggregateTest.name) from AggregateTest"; // 这样也没问题
        sql = "select GROUP_CONCAT(DISTINCT name ORDER BY id SEPARATOR ',') from AggregateTest"; // 这样也没问题

        // sql = "select sum(DISTINCT id) from AggregateTest"; //这样也没问题

        // sql = "select HISTOGRAM(id) from AggregateTest";

        sql = "select SELECTIVITY(id) from AggregateTest";
        executeQuery();

        sql = "select ABS(id) from AggregateTest";
        sql = "select ABS(SELECTIVITY(id)) from AggregateTest";
        // sql = "select ABS(SELECTIVITY(id)) from AggregateTest where max(id)>9"; //聚合函数不能用于Where中

        sql = "select id,count(id) from AggregateTest group by id having max(id)>9"; // 但是可以用于having中
        sql = "select STDDEV_POP(id),STDDEV_SAMP(id),VAR_POP(id),VAR_SAMP(id) from AggregateTest where id=11";
        sql = "select STDDEV_POP(id),STDDEV_SAMP(id),VAR_POP(id),VAR_SAMP(id) from AggregateTest where id>=7";
        executeQuery();
        // std_var()
    }

    void std_var() throws Exception {
        sql = "select STDDEV_POP(id) from AggregateTest where id>1";
        sql = "select STDDEV_POP(id) from AggregateTest where id<=5";

        sql = "select STDDEV_POP(id) from AggregateTest where id between 1 and 10";

        sql = "select STDDEV_POP(id) from AggregateTest"; // 3.415650255319866
        sql = "select STDDEV_SAMP(id) from AggregateTest"; // 3.7416573867739413
        sql = "select VAR_POP(id) from AggregateTest"; // 11.666666666666666
        sql = "select VAR_SAMP(id) from AggregateTest"; // 14.0

        sql = "select count(id),sum(id),sum(id*id) from AggregateTest where id between 1 and 5";
        rs = stmt.executeQuery(sql);
        int count = 0;
        double sum = 0;
        double sum2 = 0;
        while (rs.next()) {
            count += rs.getInt(1);
            sum += rs.getInt(2);
            sum2 += rs.getInt(3);
        }

        sql = "select count(id),sum(id),sum(id*id) from AggregateTest where id between 7 and 11";
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
            count += rs.getInt(1);
            sum += rs.getInt(2);
            sum2 += rs.getInt(3);
        }

        // STDDEV_POP 3.415650255319866
        System.out.println(Math.sqrt(sum2 / count - (sum / count) * (sum / count)));
        // STDDEV_SAMP 3.7416573867739413
        // 见:http://en.wikipedia.org/wiki/Algorithms_for_calculating_variance
        System.out.println(Math.sqrt((sum2 - (sum * sum / count)) / (count - 1)));

        // VAR_POP 11.666666666666664
        System.out.println(sum2 / count - (sum / count) * (sum / count));
        // VAR_SAMP 14.0
        System.out.println((sum2 - (sum * sum / count)) / (count - 1));

        // sql = "select STDDEV_POP(id) from AggregateTest where id between 11 and 20";
        // executeQuery();
        //
        // sql = "select STDDEV_POP(id) from AggregateTest where id between 21 and 30";
        // executeQuery();
    }

}
