package my.test.mvstore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestMapIsClosedException {
	public static void main(String[] args) throws SQLException {

		Connection conn = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/mydb", "sa", "");
		Statement stmt = conn.createStatement();

		stmt.executeUpdate("DROP TABLE IF EXISTS mvstore_test CASCADE");
		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mvstore_test(id int, name varchar(500), b boolean) "
				+ "ENGINE \"org.h2.mvstore.db.MVTableEngine\"");
		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS mvstore_test_index ON mvstore_test(name)");

		stmt.executeUpdate("insert into mvstore_test(id, name, b) values(10, 'a1', true)");
		stmt.executeUpdate("insert into mvstore_test(id, name, b) values(20, 'b1', true)");
		stmt.executeUpdate("insert into mvstore_test(id, name, b) values(30, 'a2', false)");
		stmt.executeUpdate("insert into mvstore_test(id, name, b) values(40, 'b2', true)");
		stmt.executeUpdate("insert into mvstore_test(id, name, b) values(50, 'a3', false)");
		stmt.executeUpdate("insert into mvstore_test(id, name, b) values(60, 'b3', true)");
		stmt.executeUpdate("insert into mvstore_test(id, name, b) values(70, 'b3', true)");

		ResultSet rs = stmt.executeQuery("select * from mvstore_test");
		int n = rs.getMetaData().getColumnCount();
		while (rs.next()) {
			for (int i = 1; i <= n; i++) {
				System.out.print(rs.getString(i) + " ");
			}
			System.out.println();
		}
		rs.close();
		stmt.close();
		conn.close();
	}
}
