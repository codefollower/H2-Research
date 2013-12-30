package my.test.command.ddl;

import my.test.TestBase;

public class CreateSchemaTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new CreateSchemaTest().start();
    }

    public void init() throws Exception {
        //        prop.setProperty("user", "sa1");
        //        prop.setProperty("password", "abc");
        //        dbCloseDelay = false;
    }

    @Override
    public void startInternal() throws Exception {
        //executeUpdate("DROP USER IF EXISTS sa1 CASCADE");
        //executeUpdate("CREATE USER IF NOT EXISTS sa1 PASSWORD 'abc'");

        executeUpdate("DROP SCHEMA IF EXISTS schema0");
        executeUpdate("DROP SCHEMA IF EXISTS schema1");
        sql = "CREATE SCHEMA IF NOT EXISTS schema0 AUTHORIZATION notExists";
        tryExecuteUpdate();

        executeUpdate("CREATE SCHEMA IF NOT EXISTS schema0 AUTHORIZATION sa");
        executeUpdate("CREATE ALIAS IF NOT EXISTS schema0.my_sqrt1 DETERMINISTIC FOR \"java.lang.Math.sqrt\"");
        executeUpdate("CREATE ALIAS IF NOT EXISTS schema0.my_sqrt2 DETERMINISTIC FOR \"java.lang.Math.sqrt\"");

        executeUpdate("ALTER SCHEMA mydb.public.schema0 RENAME TO schema1");

        tryExecuteUpdate("DROP SCHEMA IF EXISTS public");
    }
}
