package my.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public abstract class TestBase {

    public static void p(Object o) {
        System.out.println(o);
    }

    public static void p() {
        System.out.println();
    }

    protected Properties prop = new Properties();
    protected Connection conn;
    protected Statement stmt;
    protected PreparedStatement ps;
    protected ResultSet rs;
    protected String sql;
    protected String url;
    protected boolean dbCloseDelay = true;

    private void initDefaults() throws Exception {
        prop.setProperty("user", "sa");
        prop.setProperty("password", "");
        // prop.setProperty("TRACE_LEVEL_FILE", "10");
        // prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
        // prop.setProperty("PAGE_SIZE", "1024");
        // prop.setProperty("FILE_LOCK", "FS");

        // prop.setProperty("PAGE_SIZE", "128");
        // prop.setProperty("MVCC", "true");

        // prop.setProperty("MODE", "DB2"); //支持SYSDUMMY1
        prop.setProperty("MULTI_THREADED", "true");
    }

    public Connection getConnection() throws Exception {
        return DriverManager.getConnection(url, prop);
    }

    public void init() throws Exception {
    }

    public void start() throws Exception {
        initDefaults();
        init();
        if (url == null)
            url = "jdbc:h2:tcp://localhost:9092/mydb";

        conn = DriverManager.getConnection(url, prop);
        // conn.setAutoCommit(false);
        stmt = conn.createStatement();

        if (dbCloseDelay)
            stmt.executeUpdate("SET DB_CLOSE_DELAY -1"); // 不马上关闭数据库

        startInternal();

        stop();
    }

    public void stop() throws Exception {
        if (rs != null)
            rs.close();
        if (ps != null)
            ps.close();
        if (stmt != null)
            stmt.close();
        if (conn != null)
            conn.close();
    }

    public void startInternal() throws Exception {

    }

    public void executeQuery() throws Exception {
        executeQuery(sql);
    }

    public void executeQuery(String sql) throws Exception {
        rs = stmt.executeQuery(sql);
        int n = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= n; i++) {
                System.out.print(rs.getString(i) + " ");
            }
            System.out.println();
            // System.out.println(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " +
            // rs.getString(4));
        }
        System.out.println();
    }

    public void printResultSet(ResultSet rs) throws Exception {
        int n = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= n; i++) {
                System.out.print(rs.getString(i) + " ");
            }
            System.out.println();
            // System.out.println(rs.getString(1) + " " + rs.getString(2) + " " + rs.getString(3) + " " +
            // rs.getString(4));
        }
    }

    public void printResultSet() throws Exception {
        rs = stmt.executeQuery(sql);

        int n = rs.getMetaData().getColumnCount();
        while (rs.next()) {
            for (int i = 1; i <= n; i++) {
                System.out.print(rs.getString(i) + " ");
            }
            System.out.println();
        }
        rs.close();
        rs = null;
        System.out.println();
    }

    public int executeUpdate() throws Exception {
        return stmt.executeUpdate(sql);
    }

    public int executeUpdate(String sql) throws Exception {
        return stmt.executeUpdate(sql);
    }

    public void tryExecuteQuery() {
        try {
            executeQuery();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void tryExecuteUpdate() {
        tryExecuteUpdate(sql);
    }

    public void tryExecuteUpdate(String sql) {
        try {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void closeResultSet() throws Exception {
        rs.close();
        rs = null;
    }

    private void check() throws Exception {
        if (rs == null) {
            rs = stmt.executeQuery(sql);
            rs.next();
        }
    }

    public int getIntValue(int i) throws Exception {
        check();
        return rs.getInt(i);
    }

    public int getIntValue(int i, boolean closeResultSet) throws Exception {
        check();
        try {
            return rs.getInt(i);
        } finally {
            if (closeResultSet)
                closeResultSet();
        }
    }

    public long getLongValue(int i) throws Exception {
        check();
        return rs.getLong(i);
    }

    public long getLongValue(int i, boolean closeResultSet) throws Exception {
        check();
        try {
            return rs.getLong(i);
        } finally {
            if (closeResultSet)
                closeResultSet();
        }
    }

    public double getDoubleValue(int i) throws Exception {
        check();
        return rs.getDouble(i);
    }

    public double getDoubleValue(int i, boolean closeResultSet) throws Exception {
        check();
        try {
            return rs.getDouble(i);
        } finally {
            if (closeResultSet)
                closeResultSet();
        }
    }

    public String getStringValue(int i) throws Exception {
        check();
        return rs.getString(i);
    }

    public String getStringValue(int i, boolean closeResultSet) throws Exception {
        check();
        try {
            return rs.getString(i);
        } finally {
            if (closeResultSet)
                closeResultSet();
        }
    }

    public boolean getBooleanValue(int i) throws Exception {
        check();
        return rs.getBoolean(i);
    }

    public boolean getBooleanValue(int i, boolean closeResultSet) throws Exception {
        check();
        try {
            return rs.getBoolean(i);
        } finally {
            if (closeResultSet)
                closeResultSet();
        }
    }

}
