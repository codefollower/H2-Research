package my.test.command.dml;

import java.sql.Connection;
import java.sql.SQLException;

import my.test.TestBase;

//找断点条件
//table.getName().equalsIgnoreCase("InsertTest");
public class InsertTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new InsertTest().start();
    }

    public static class MyInsertTrigger implements org.h2.api.Trigger {

        @Override
        public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before,
                int type) throws SQLException {
            System.out.println("schemaName=" + schemaName + " tableName=" + tableName);

        }

        @Override
        public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
            System.out.println("oldRow=" + oldRow + " newRow=" + newRow);
        }

        @Override
        public void close() throws SQLException {
            System.out.println("my.test.sql.InsertTest.MyInsertTrigger.close()");
        }

        @Override
        public void remove() throws SQLException {
            System.out.println("my.test.sql.InsertTest.MyInsertTrigger.remove()");
        }

    }

    // 测试org.h2.command.Parser.parseInsert()和org.h2.command.dml.Insert
    @Override
    public void startInternal() throws Exception {
        conn.setAutoCommit(false);

        stmt.executeUpdate("DROP TABLE IF EXISTS InsertTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS InsertTest(id int not null, name varchar(500) not null)");
        // stmt.executeUpdate("CREATE TABLE IF NOT EXISTS InsertTest(id int, name varchar(500) as '123')");

        stmt.executeUpdate("CREATE TRIGGER IF NOT EXISTS TriggerInsertTest BEFORE INSERT ON InsertTest "
        // + "FOR EACH ROW CALL \"my.test.sql.InsertTest$MyInsertTrigger\"");
                + "CALL \"" + MyInsertTrigger.class.getName() + "\"");

        stmt.executeUpdate("DROP TABLE IF EXISTS tmpSelectTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS tmpSelectTest(id int, name varchar(500))");
        stmt.executeUpdate("INSERT INTO tmpSelectTest VALUES(DEFAULT, DEFAULT),(10, 'a'),(20, 'b')");

        // 从另一表查数据，然后插入此表
        sql = "INSERT INTO InsertTest(SELECT * FROM tmpSelectTest)";
        // sql = "INSERT INTO InsertTest(FROM tmpSelectTest SELECT *)"; //FROM开头先也是支持的

        // DEFAULT VALUES这种语法不适合用于not null字段
        // sql = "INSERT INTO InsertTest DIRECT SORTED DEFAULT VALUES";
        // DEFAULT VALUES这种语法不能在表名之后又指定字段列表
        // sql = "INSERT INTO InsertTest(name) DIRECT SORTED DEFAULT VALUES");

        // 这种语法可查入多条记录
        // null null
        // 10 a
        // 20 b
        // sql = "INSERT INTO InsertTest VALUES(DEFAULT, DEFAULT),(10, 'a'),(20, 'b')";

        // SET语法不能在表名之后又指定字段列表
        // sql = "INSERT INTO InsertTest(name) SET name='xyz')";
        // 虽然在语法上可以重复相同的字段，本意是想插入多条记录，但是实际上只有一条，就是最后一个id和name
        // sql = "INSERT INTO InsertTest SET id=DEFAULT, name=DEFAULT, id=10, name='a', id=20, name='b'");

        // 列必须一样多，否则:
        // Exception in thread "main" org.h2.jdbc.JdbcSQLException: Column count does not match; SQL statement:
        // INSERT INTO InsertTest(name) (SELECT * FROM tmpSelectTest) [21002-169]
        // sql = "INSERT INTO InsertTest(name) (SELECT * FROM tmpSelectTest)";
        // sql = "INSERT INTO InsertTest(name) (FROM tmpSelectTest SELECT *)"; //FROM开头先也是支持的
        //
        // sql = "INSERT INTO InsertTest(name) (SELECT name FROM tmpSelectTest)";
        // sql = "INSERT INTO InsertTest(name) (FROM tmpSelectTest SELECT name)"; //FROM开头先也是支持的

        // SELECT语句不带括号也是允许的
        // sql = "INSERT INTO InsertTest(name) SELECT name FROM tmpSelectTest";
        // sql = "INSERT INTO InsertTest(name) FROM tmpSelectTest SELECT name"; //FROM开头先也是支持的
        //
        // sql = "INSERT INTO InsertTest(name) DIRECT FROM tmpSelectTest SELECT name"; //FROM开头先也是支持的

        sql = "INSERT INTO InsertTest(id, name) SORTED VALUES(100,'abc')"; // FROM开头先也是支持的
        sql = "INSERT INTO InsertTest(id, name) SORTED VALUES(100,DEFAULT)"; // FROM开头先也是支持的

        stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS myschema AUTHORIZATION sa");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS myschema.InsertTest2(id int, name varchar(500) as '123')");
        stmt.executeUpdate("SET SCHEMA_SEARCH_PATH INFORMATION_SCHEMA, PUBLIC, myschema");

        sql = "INSERT INTO InsertTest2(id, name) SORTED VALUES(100,DEFAULT)"; // FROM开头先也是支持的
        stmt.executeUpdate(sql);

        // ps = conn.prepareStatement("INSERT INTO InsertTest(id, name) VALUES(?, ?)");
        // ps.setInt(1, 30);
        // ps.setString(2, "c");
        // ps.executeUpdate();

        stmt.executeQuery("EXPLAIN INSERT INTO InsertTest(name) DIRECT FROM tmpSelectTest SELECT name");

        sql = "select id,name from InsertTest";
        sql = "select * from SYS";
        rs = stmt.executeQuery(sql);
        while (rs.next()) {
            System.out.println(rs.getString(1) + " " + rs.getString(2));
        }

        conn.commit();
        // conn.rollback();

        sql = "INSERT INTO InsertTest(id, name) VALUES(?,?) {1:600, 2:'abc'}";
        executeUpdate(sql);

        sql = "delete from InsertTest";
        executeUpdate(sql);

        sql = "INSERT INTO InsertTest(id, name) VALUES(1, 'a')";
        executeUpdate(sql);
        sql = "INSERT INTO InsertTest(id, name) VALUES(1, 'b')";
        executeUpdate(sql);
        sql = "INSERT INTO InsertTest(id, name) VALUES(1, 'c')";
        executeUpdate(sql);

        sql = "INSERT INTO InsertTest(id, name) VALUES(2, 'a')";
        executeUpdate(sql);
        sql = "INSERT INTO InsertTest(id, name) VALUES(2, 'b')";
        executeUpdate(sql);
        sql = "INSERT INTO InsertTest(id, name) VALUES(2, 'c')";
        executeUpdate(sql);

        sql = "select top 1 id,name from InsertTest group by id";
        printResultSet();
    }
}