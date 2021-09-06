package my.test.jdbc;

import java.sql.Statement;

import my.test.TestBase;

public class GeneratedKeysTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new GeneratedKeysTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS GeneratedKeysTest");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS GeneratedKeysTest" //
                + "(id int primary key, name varchar(20), age int, f1 int auto_increment)");

        stmt.executeUpdate("INSERT INTO GeneratedKeysTest(id, name, age) VALUES(1, 'zhh2', 10)",
                Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        while (rs.next())
            System.out.println(rs.getString(1));

        int columnIndexes[] = { 4 }; // 从1开始
        stmt.executeUpdate("INSERT INTO GeneratedKeysTest(id, name, age) VALUES(2, 'zhh2', 20)", columnIndexes);
        rs = stmt.getGeneratedKeys();
        while (rs.next())
            System.out.println(rs.getString(1));

        String columnNames[] = { "f1" };
        stmt.executeUpdate("INSERT INTO GeneratedKeysTest(id, name, age) VALUES(3, 'zhh2', 30)", columnNames);
        rs = stmt.getGeneratedKeys();
        while (rs.next())
            System.out.println(rs.getString(1));

        rs = stmt.executeQuery("SELECT age FROM GeneratedKeysTest");
        while (rs.next())
            System.out.println(rs.getString(1));
        rs.close();
    }
}
