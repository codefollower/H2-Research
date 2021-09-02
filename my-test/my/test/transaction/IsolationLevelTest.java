package my.test.transaction;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import my.test.TestBase;

public class IsolationLevelTest  extends TestBase {
    public static void main(String[] args) throws Exception {
        new IsolationLevelTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        Statement stmt = conn.createStatement();
        stmt.executeUpdate("DROP TABLE IF EXISTS my_table");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS my_table(name varchar(20), age int)");
        stmt.executeUpdate("INSERT INTO my_table(name, age) VALUES('zhh', 18)");
        stmt.close();
        
        Connection c1 = getConnection() ; 
        c1.setAutoCommit(false);
        Statement stmt1 = c1.createStatement();
        stmt1.executeUpdate("SET SESSION CHARACTERISTICS AS TRANSACTION ISOLATION LEVEL REPEATABLE READ"); 
        ResultSet rs = stmt1.executeQuery("SELECT age FROM my_table");
        rs.next();
        System.out.println(rs.getString(1));
        rs.close();

        Connection c2 = getConnection() ; 
        Statement stmt2 = c2.createStatement();  
        stmt2.executeUpdate("UPDATE my_table SET age = 20 WHERE name = 'zhh'"); 
        
        rs = stmt1.executeQuery("SELECT age FROM my_table");
        rs.next();
        System.out.println(rs.getString(1));
        
        c1.commit();
        c1.close();
        c2.close();
    }
}
