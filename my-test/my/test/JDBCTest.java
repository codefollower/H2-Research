package my.test;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.h2.jdbc.JdbcConnection;

public class JDBCTest {
    static Properties prop = new Properties();
    static String url = "jdbc:h2:tcp://localhost:9092/test9";

    public static void main2(String[] args) throws Exception {
        Class.forName("org.h2.Driver");

        Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mydb", "sa", "");
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS my_table");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS my_table(name varchar(20))");
        stmt.executeUpdate("INSERT INTO my_table(name) VALUES('zhh')");

        ResultSet rs = stmt.executeQuery("SELECT name FROM my_table");
        rs.next();
        System.out.println(rs.getString(1));

        stmt.close();
        conn.close();
    }

    public static class MyDatabaseEventListener implements org.h2.api.DatabaseEventListener {

        @Override
        public void init(String url) {
            // TODO Auto-generated method stub

        }

        @Override
        public void opened() {
            // TODO Auto-generated method stub

        }

        @Override
        public void exceptionThrown(SQLException e, String sql) {
            // TODO Auto-generated method stub

        }

        @Override
        public void setProgress(int state, String name, int x, int max) {
            // TODO Auto-generated method stub

        }

        @Override
        public void closingDatabase() {
            // TODO Auto-generated method stub

        }

    }

    public static void main(String[] args) throws Exception {
        prop.setProperty("user", "sa");
        prop.setProperty("password", "");

        // testJdbcConnection();
        // testJdbcStatement();
        testBlob();
    }

    public static void testJdbcConnection() throws Exception {

        System.setProperty("h2.baseDir", "E:\\H2\\baseDir");

        // TRACE_LEVEL_FILE参数放System中无效，放在url中非法，只能放在prop中
        // System.setProperty("TRACE_LEVEL_FILE",
        // "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE");
        // url +="; TRACE_LEVEL_FILE=E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE";

        // prop.setProperty("TRACE_LEVEL_FILE",
        // "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE.txt"); //只能是数字

        prop.setProperty("TRACE_LEVEL_FILE", "10");

        prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");

        url = "jdbc:h2:tcp://localhost:9092,localhost:9093/test9";

        // prop.setProperty("AUTO_SERVER", "true");
        // //AUTO_SERVER为true时url中不能指定多个server

        prop.setProperty("AUTO_RECONNECT", "true");

        // prop.setProperty("DATABASE_EVENT_LISTENER",
        // "org.h2.samples.ShowProgress");
        // prop.setProperty("DATABASE_EVENT_LISTENER",
        // "my.test.JdbcConnectionTest$MyDatabaseEventListener");

        // prop.setProperty("CIPHER", "my_cipher");
        // prop.setProperty("password", "my_password1 my_password2");

        JdbcConnection conn = new JdbcConnection(url, prop);

        PreparedStatement ps = conn.prepareStatement("insert into t values(?,?)");
        ps.setInt(2, 20);
        ps.setString(1, "aaa");
        ps.executeUpdate();

        ps.close();
        // stmt.close();
        conn.close();
    }

    public static void testJdbcStatement() throws Exception {
        System.setProperty("h2.baseDir", "E:\\H2\\baseDir");

        // TRACE_LEVEL_FILE参数放System中无效，放在url中非法，只能放在prop中
        // System.setProperty("TRACE_LEVEL_FILE",
        // "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE");
        // url +="; TRACE_LEVEL_FILE=E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE";

        // prop.setProperty("TRACE_LEVEL_FILE",
        // "E:\\H2\\baseDir\\MY_TRACE_LEVEL_FILE.txt"); //只能是数字

        // prop.setProperty("TRACE_LEVEL_FILE", "10");

        // prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");

        // url = "jdbc:h2:tcp://localhost:9092,localhost:9093/test9";

        // prop.setProperty("AUTO_SERVER", "true");
        // //AUTO_SERVER为true时url中不能指定多个server

        prop.setProperty("AUTO_RECONNECT", "true");

        // prop.setProperty("DATABASE_EVENT_LISTENER",
        // "org.h2.samples.ShowProgress");
        // prop.setProperty("DATABASE_EVENT_LISTENER",
        // "my.test.JdbcConnectionTest$MyDatabaseEventListener");

        // prop.setProperty("CIPHER", "my_cipher");
        // prop.setProperty("password", "my_password1 my_password2");

        JdbcConnection conn = new JdbcConnection(url, prop);

        Statement stmt = conn.createStatement();
        // stmt.executeUpdate("update t set(name, age)=('123',10)");

        stmt.setFetchSize(5);
        ResultSet rs = stmt.executeQuery("select * from t");
        rs.getMetaData();

        while (rs.next()) {
            System.out.println(rs.getString(1));
            System.out.println(rs.getString(2));
        }

        PreparedStatement ps = conn.prepareStatement("select * from t");
        ps.getMetaData();

        ps.close();
        stmt.close();
        conn.close();
    }

    public static void testBlob() throws Exception {
        System.setProperty("h2.lobInDatabase", "false");
        System.setProperty("h2.lobClientMaxSizeMemory", "1024");
        System.setProperty("java.io.tmpdir", "E:\\H2\\tmp");

        JdbcConnection conn = new JdbcConnection(url, prop);
        Statement stmt = conn.createStatement();
        ResultSet rs;

        // stmt.execute("DROP TABLE IF EXISTS my_lob");
        stmt.execute("CREATE TABLE IF NOT EXISTS my_lob(name varchar(20), b blob, c clob)");
        // byte[] bytes={ (byte)1,(byte)2};

        byte[] bytes = new byte[5049];
        for (int i = 0; i < 5049; i++)
            bytes[i] = (byte) i;
        Blob b = conn.createBlob();
        b.setBytes(1, bytes);
        b.length();

        Clob c = conn.createClob();
        c.setString(1, new String(bytes));
        c.length();

        PreparedStatement ps = conn.prepareStatement("insert into my_lob(name, b, c) values(?,?,?)");
        ps.setString(1, "abc");
        ps.setBlob(2, b);
        ps.setClob(3, c);
        ps.executeUpdate();
        rs = stmt.executeQuery("select name, b, c from my_lob");
        while (rs.next()) {
            System.out.println(rs.getString(1));
            System.out.println(rs.getString(2));
            System.out.println(rs.getString(3));
        }
        // stmt.executeUpdate("delete from my_lob");
        rs.close();

        ps.close();
        stmt.close();
        conn.close();
    }
}
