package my.test.command.ddl;

import my.test.TestBase;

public class CreateTableTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new CreateTableTest().start();
    }

    @Override
    public void startInternal() throws Exception {
        stmt.executeUpdate("DROP TABLE IF EXISTS mytable3");
        stmt.executeUpdate("DROP TABLE IF EXISTS mytable2");
        stmt.executeUpdate("DROP TABLE IF EXISTS mytable1");

        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mytable1 (f1 int,PRIMARY KEY(f1), f2 int not null)");
        //		stmt.executeUpdate("CREATE INDEX IF NOT EXISTS myindex ON mytable1(f2)");
        //
        //		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mytable2 (f1 int PRIMARY KEY, f2 int REFERENCES(f1))");
        //		
        //		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mytable3 (f1 int REFERENCES mytable1(f2) INDEX myindex ON DELETE CASCADE)");

        //parseAlterTableAddConstraintIf();
        parseColumnForTable();

        sql = "CREATE TABLE IF NOT EXISTS mytable3 (" + "f1 int CONSTRAINT c1 PRIMARY KEY HASH AUTO_INCREMENT(1000, 10), " + //此时CONSTRAINT名无用
                "f2 int CONSTRAINT c2 UNIQUE, " + //
                "f3 int CONSTRAINT c3 NOT NULL, " + //
                "f4 int CONSTRAINT c4 NULL , " + //
                "f5 int CONSTRAINT c5 NOT NULL CHECK f5>0, " + //
                "f6 int CONSTRAINT c6 NOT NULL REFERENCES(f1) , " + //自引用
                "f7 int CONSTRAINT c7 NOT NULL REFERENCES mytable1(f1) , " + //
                "f8 int CONSTRAINT c8 NOT NULL REFERENCES mytable1(f1) ON DELETE CASCADE " + //
                ")";
        // stmt.executeUpdate(sql);

    }

    void parseAlterTableAddConstraintIf() throws Exception {
        sql = "CREATE TABLE IF NOT EXISTS mytable3 (f1 int, CONSTRAINT c1 PRIMARY KEY(f1), f2 int, CONSTRAINT c2 PRIMARY KEY(f2))";

        sql = "CREATE TABLE IF NOT EXISTS mytable3 (f1 int, CONSTRAINT c1 PRIMARY KEY(f1), f2 int)";

        sql = "CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint0 COMMENT IS 'haha0' PRIMARY KEY HASH(f1,f2) INDEX myindex)," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint1 COMMENT IS 'haha1' INDEX myindex(f1,f2)," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint2 COMMENT IS 'haha2' INDEX(f1,f2)," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint3 COMMENT IS 'haha3' CHECK f1>0 and f2<10 CHECK," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint4 COMMENT IS 'haha4' UNIQUE KEY INDEX myunique(f1,f2) NOCHECK," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint5 COMMENT IS 'haha5' FOREIGN KEY(f1) REFERENCES(f2)," //
                + "CONSTRAINT IF NOT EXISTS myschema.my_constraint6 COMMENT IS 'haha6' FOREIGN KEY(f1) REFERENCES myschema.mytable(f2) " //
                + "ON DELETE CASCADE ON UPDATE RESTRICT ON DELETE NO ACTION ON UPDATE SET NULL ON DELETE SET DEFAULT NOT DEFERRABLE)";

        stmt.executeUpdate(sql);
    }

    void parseColumnForTable() throws Exception {
        //sql = "CREATE SCHEMA IF NOT EXISTS myschema AUTHORIZATION SA";
        //sql = "DROP TABLE IF EXISTS myschema.mytable";
        stmt.executeUpdate("DROP TABLE IF EXISTS mytable");

        parseColumnWithType();
        //parseColumnForTable0();
    }

    void parseColumnForTable0() throws Exception {

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int)";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 IDENTITY)";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 IDENTITY(1,10))";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 SERIAL(1,10)))";

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 IDENTITY(1,10),PRIMARY KEY(f1))";
        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int,PRIMARY KEY(f1))";

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int, f2 int AS 10 )";

        stmt.executeUpdate("DROP SEQUENCE IF EXISTS myseq");
        stmt.executeUpdate("CREATE SEQUENCE IF NOT EXISTS myseq START WITH 1000 INCREMENT BY 1 CACHE 20");

        sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int, f2 int NULL_TO_DEFAULT SEQUENCE myseq) ";
        //sql = "CREATE TABLE IF NOT EXISTS mytable (f1 int, f2 int NOT NULL NULL_TO_DEFAULT SEQUENCE myseq) ";

        stmt.executeUpdate(sql);

        stmt.executeUpdate("insert into mytable(f1) values(1)");
        stmt.executeUpdate("insert into mytable(f1) values(2)");
        sql = "select * from mytable";
        executeQuery();
    }

    void parseColumnWithType() throws Exception {
        stmt.executeUpdate("DROP DOMAIN IF EXISTS EMAIL");
        //VALUE是CREATE DOMAIN语句的默认临时列名
        stmt.executeUpdate("CREATE DOMAIN IF NOT EXISTS EMAIL AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");

        sql = "CREATE TABLE IF NOT EXISTS mytable (" + //
                "f1 LONG RAW, " + //
                "f2 DOUBLE PRECISION, " + //
                "f3 CHARACTER VARYING, " + //
                "f4 EMAIL , " + //
                //                "f5 int CONSTRAINT c5 NOT NULL CHECK f5>0, " + //
                //                "f6 int CONSTRAINT c6 NOT NULL REFERENCES(f1) , " + //自引用
                //                "f7 int CONSTRAINT c7 NOT NULL REFERENCES mytable1(f1) , " + //
                //                "f8 int CONSTRAINT c8 NOT NULL REFERENCES mytable1(f1) ON DELETE CASCADE " + //
                "f9 int" + //
                ")";

        stmt.executeUpdate(sql);

        //        stmt.executeUpdate("insert into mytable(f1) values(1)");
        //        stmt.executeUpdate("insert into mytable(f1) values(2)");
        //        sql = "select * from mytable";
        //        executeQuery();
    }

    //@Override
    public void startInternal0() throws Exception {

        //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' INDEX int)");

        //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,f2 int,CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' INDEX my_int(f1,f2))");
        //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS TEST9.public.mytable (f1 int,f2 int,"
        //		+ "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' CHECK f1>0)");
        //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,f2 int,"
        //		+ "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' UNIQUE KEY INDEX my_constraint2(f1,f2) INDEX myi)");
        //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS mytable (f1 int,f2 int,"
        //		+ "CONSTRAINT IF NOT EXISTS my_constraint COMMENT IS 'haha' FOREIGN KEY(f1,f2))  INDEX my-i REFERENCES(f1)");

        //stmt.executeUpdate("CREATE CACHED GLOBAL TEMPORARY TABLE IF NOT EXISTS mytable (f1 int)");
        //stmt.executeUpdate("CREATE CACHED GLOBAL TEMPORARY TABLE IF NOT EXISTS TEST9.SESSION.mytable (f1 int)");

        //表名schema和约束schema必须一样
        //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int,"
        //		+ "CONSTRAINT IF NOT EXISTS public.my_constraint COMMENT IS 'haha' PRIMARY KEY HASH(f1,f2))");

        //stmt.executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int,"
        //		+ "CONSTRAINT IF NOT EXISTS myschema.my_constraint COMMENT IS 'haha' PRIMARY KEY HASH(f1,f2) INDEX myindex)");

        //		stmt.executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (f1 int,f2 int,"
        //				+ "CONSTRAINT IF NOT EXISTS myschema.my_constraint1 COMMENT IS 'haha1' INDEX myindex(f1,f2),"
        //				+ "CONSTRAINT IF NOT EXISTS myschema.my_constraint2 COMMENT IS 'haha2' INDEX(f1,f2),"
        //				+ "CONSTRAINT IF NOT EXISTS myschema.my_constraint3 COMMENT IS 'haha3' CHECK f1>0 and f2<10 CHECK,"
        //				+ "CONSTRAINT IF NOT EXISTS myschema.my_constraint4 COMMENT IS 'haha4' UNIQUE KEY INDEX myunique(f1,f2) NOCHECK,"
        //				+ "CONSTRAINT IF NOT EXISTS myschema.my_constraint5 COMMENT IS 'haha5' FOREIGN KEY(f1) REFERENCES(f2),"
        //				+ "CONSTRAINT IF NOT EXISTS myschema.my_constraint6 COMMENT IS 'haha6' FOREIGN KEY(f1) REFERENCES myschema.mytable(f2) "
        //				+ "ON DELETE CASCADE ON UPDATE RESTRICT ON DELETE NO ACTION ON UPDATE SET NULL ON DELETE SET DEFAULT NOT DEFERRABLE)");

        stmt.executeUpdate("set IGNORECASE true");
        stmt.executeUpdate("CREATE DATATYPE IF NOT EXISTS EMAIL AS VARCHAR(255) CHECK (POSITION('@', VALUE) > 1)");
        stmt.executeUpdate("CREATE TABLE IF NOT EXISTS myschema.mytable (" //
                + "f0 varchar(MAX),f00 varchar(100K CHAR),f01 DECIMAL(100K CHAR, 90)," //
                + "f1 LONG RAW, f2 DOUBLE PRECISION, f3 CHARACTER VARYING, f4 EMAIL, f5 varchar, f6 varchar FOR BIT DATA)");
    }
}
