package my.test;

public class TwoPC extends TestBase {
    public static void main(String[] args) throws Exception {
        new TwoPC().start();
    }

    @Override
    public void startInternal() throws Exception {
        conn.setAutoCommit(false);

        //stmt.executeUpdate("create table testdata (id int not null auto_increment primary key,foo int)");
        //stmt.executeUpdate("insert into testdata values(null, 1)");

        sql = "select * from testdata";
        executeQuery();
    }
}
