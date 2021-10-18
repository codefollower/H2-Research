package my.test;

import java.sql.SQLException;

import org.h2.tools.Shell;

public class ShellTest {

    public static void main(String[] args) throws SQLException {
        String url = "jdbc:h2:tcp://localhost:9092/mydb";
        String[] args2 = { "-url", url, "-user", "sa" };
        Shell.main(args2);
    }
}