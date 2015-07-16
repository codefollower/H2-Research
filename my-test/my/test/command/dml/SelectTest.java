package my.test.command.dml;

import my.test.TestBase;

public class SelectTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new SelectTest().start();
    }

    @Override
    public void init() throws Exception {
        prop.setProperty("MODE", "DB2"); // 支持SYSDUMMY1、supportOffsetFetch
        // prop.setProperty("PAGE_SIZE", "128");
        // prop.setProperty("DEFAULT_TABLE_ENGINE",
        // "org.h2.mvstore.db.MVTableEngine");
        // prop.setProperty("TRACE_LEVEL_FILE", "10");
        // prop.setProperty("TRACE_LEVEL_SYSTEM_OUT", "20");
        // prop.setProperty("PAGE_SIZE", "1024");
        // prop.setProperty("FILE_LOCK", "FS");
    }

    @Override
    public void startInternal() throws Exception {
        // createTable();

        // parseSelectSimpleSelectPart();
        // readTableFilter();
        // readJoin();
        // parseEndOfQuery();
        // parseSelectSimpleFromPart();
        //

        // queryFlat();
        select_init();
        // queryGroup();
        // queryGroupSorted();
        //
        // queryQuick();
        //
        // queryDistinct();

        // prepare();
        // queryWithoutCache();

        // preparePlan();

    }

    void createTable() throws Exception {
        stmt.executeUpdate("drop table IF EXISTS mytable,natural_join_test_table1,natural_join_test_table2,mytable1,mytable2");
        // stmt.executeUpdate("create table IF NOT EXISTS mytable(id int primary key, name varchar(500))");
        stmt.executeUpdate("create table IF NOT EXISTS mytable(id int, name varchar(500), age int)");
        stmt.executeUpdate("ALTER TABLE mytable ALTER COLUMN id SELECTIVITY 10");
        stmt.executeUpdate("ALTER TABLE mytable ALTER COLUMN name SELECTIVITY 10");
        // stmt.executeUpdate("ALTER TABLE mytable ADD CONSTRAINT NAME_UNIQUE UNIQUE(name,id)");
        stmt.executeUpdate("create index IF NOT EXISTS mytable_id_index on mytable(id)");
        stmt.executeUpdate("create index IF NOT EXISTS mytable_age_index on mytable(age)");
        stmt.executeUpdate("create index IF NOT EXISTS mytable_name_age_index on mytable(name,age)");

        stmt.executeUpdate("create table IF NOT EXISTS mytable1(id1 int primary key, name1 varchar(500))");
        stmt.executeUpdate("create table IF NOT EXISTS mytable2(id2 int primary key, name2 varchar(500))");
        stmt.executeUpdate("create table IF NOT EXISTS mytable3(id3 int primary key, name3 varchar(500))");
        stmt.executeUpdate("create table IF NOT EXISTS mytable4(id4 int primary key, name4 varchar(500))");
        stmt.executeUpdate("create table IF NOT EXISTS mytable5(id5 int primary key, name5 varchar(500))");
        stmt.executeUpdate("create table IF NOT EXISTS mytable6(id6 int primary key, name6 varchar(500))");
        stmt.executeUpdate("create table IF NOT EXISTS mytable7(id7 int primary key, name7 varchar(500))");

        stmt.executeUpdate("create table IF NOT EXISTS natural_join_test_table1(id int primary key, name varchar(500), age1 int)");
        stmt.executeUpdate("create table IF NOT EXISTS natural_join_test_table2(id int primary key, name varchar(500), age2 int)");

        for (int i = 1; i <= 0; i++) {
            if (i % 2 == 0)
                stmt.executeUpdate("insert into mytable(id, name) values(" + i + ", '" + i + "abcdef1234')");
            else
                stmt.executeUpdate("insert into mytable(id, name) values(" + i + ", null)");
            stmt.executeUpdate("insert into natural_join_test_table1(id, name) values(" + i + ", 'abcdef1234')");
            stmt.executeUpdate("insert into natural_join_test_table2(id, name) values(" + i + ", 'abcdef1234')");
            stmt.executeUpdate("insert into mytable1(id1, name1) values(" + i + ", 'abcdef1234')");
            stmt.executeUpdate("insert into mytable2(id2, name2) values(" + i * 10 + ", 'abcdef1234')");
        }
        stmt.executeUpdate("insert into mytable(id, name, age) values(" + 1 + ", '" + 1 + "abcdef1234', 10)");
        stmt.executeUpdate("insert into mytable(id, name, age) values(" + 1 + ", '" + 1 + "abcdef12343', 10)");
        stmt.executeUpdate("insert into mytable(id, name, age) values(" + 1 + ", '" + 2 + "abcdef1234', 20)");
        stmt.executeUpdate("insert into mytable(id, name, age) values(" + 2 + ", '" + 3 + "abcdef1234', 30)");
        stmt.executeUpdate("insert into mytable(id, name, age) values(" + 2 + ", '" + 4 + "abcdef1234', 40)");
        stmt.executeUpdate("insert into mytable(id, name, age) values(" + 3 + ", '" + 5 + "abcdef1234', 50)");
        stmt.executeUpdate("insert into mytable(id, name, age) values(" + 3 + ", '" + 6 + "abcdef1234', 60)");

        // 测试org.h2.command.dml.Select.queryGroup(int, LocalResult)
        sql = "SELECT DISTINCT count(*),max(id),min(id),sum(id) FROM mytable ";

        // 测试org.h2.command.dml.Select.queryQuick(int, ResultTarget)
        // sql = "SELECT DISTINCT count(*) FROM mytable ";

        // sql = "SELECT DISTINCT LENGTH(NAME) FROM mytable ";

        // 测试org.h2.command.dml.Select.queryDistinct(ResultTarget, long)
        sql = "select distinct name from mytable";

        sql = "select name from mytable where id=3";

        sql = "select max(name) from mytable";

    }

    // 测试试org.h2.command.Parser.parseSelectSimpleSelectPart(Select)
    void parseSelectSimpleSelectPart() {

        sql = "select 2";
        sql = "select TOP 10 DISTINCT * from mytable";
        sql = "select LIMIT 2 5 *, id, name n, name as n2 from mytable";
        sql = "select LIMIT 2 5 t.* from mytable t";

        sql = "select * from mytable";

        sql = "select 1 union select 1";

        sql = "select (select 1)";

        sql = "select 1";

        sql = "select id, name from mytable where id>0";

        sql = "select public.t.* from mytable as t where id>199";

        sql = "select public.t.id, *, name from mytable as t where id>199 group by id having id>99 order by t.id desc";
        sql = "select id,name from mytable as t where id>199 group by id having id>99 order by t.id desc";
        sql = "select name from mytable as t where id>199 group by id having id>99 order by t.id desc";

        // sql =
        // "select id, name from mytable as t where id>199  order by t.id desc";
        // sql = "select name from mytable as t where id>199  order by id desc";
        // sql =
        // "select distinct name from mytable as t where id>199  order by id desc";
        // sql = "select distinct name from mytable order by id desc";
        // sql = "select name from mytable order by id desc";
        // sql = "SELECT 1 AS A FROM DUAL ORDER BY -A"; //有错 Column "A" not
        // found; SQL statement:
        // sql = "SELECT 1 AS A FROM DUAL ORDER BY A";

        sql = "select * from mytable1 t1 LEFT OUTER JOIN mytable2 t2 on t1.id1=t2.id2 where t1.id1>100 group by t1.id1 having t1.id1>150 order by t1.id1 desc";
        // sql =
        // "select * from mytable1 t1 RIGHT OUTER JOIN mytable2 t2 on t1.id1=t2.id2 where t1.id1>100 group by t1.id1 having t1.id1>150 order by t1.id1 desc";

        // sql = "select id, name from mytable where id>198";

        sql = "select id1,name2 from mytable1 t1 LEFT OUTER JOIN mytable2 t2 on t1.id1=t2.id2 where t1.id1>100 group by id1,name1 having t1.id1>150 order by t1.id1 desc";
        sql = "select id1,name2 from mytable1 t1 LEFT OUTER JOIN mytable2 t2 on t1.id1=t2.id2 where t1.id1>100 group by id1,name1 having id2>150 order by t1.id1 desc, name2";
        sql = "select count(id) from mytable";
        sql = "select distinct name from mytable";
    }

    // 测试org.h2.command.Parser.readTableFilter(boolean)
    void readTableFilter() throws Exception {
        sql = "FROM (select 1, 2) SELECT * ";
        executeQuery();
        sql = "FROM (select 1, 2) as t SELECT * ";
        executeQuery();
        sql = "FROM ((select 1, 2)) SELECT * ";
        sql = "FROM (((select 1, 2))) SELECT * ";

        sql = "FROM (mytable) SELECT * ";
        executeQuery();
        sql = "FROM (mytable1 RIGHT OUTER JOIN mytable2 ON mytable1.id1=mytable2.id2) AS t SELECT * ";

        sql = "FROM VALUES(1, 'Hello'), (2, 'World') AS t SELECT * ";
        executeQuery();

        sql = "FROM mytable(index mytable_age_index) SELECT * "; // 目前index mytable_age_index并没有实现
        executeQuery();

        sql = "FROM SYSTEM_RANGE(1,100) SELECT * ";
        executeQuery();

        sql = "FROM GENERATE_SERIES(1,100,10) SELECT * ";
        executeQuery();

        sql = "FROM TABLE(ID INT=(1, 2), NAME VARCHAR=('Hello', 'World')) SELECT * ";
        executeQuery();
        sql = "FROM USER() SELECT * "; // 函数返回值类型必须是RESULT_SET
        tryExecuteQuery();

        sql = "FROM DUAL SELECT * ";
        executeQuery();

        sql = "FROM SYSDUMMY1 SELECT * "; // 要加prop.setProperty("MODE", "DB2")
        executeQuery();

        // 下面两者是一样的
        sql = "FROM mytable as t SELECT * ";
        executeQuery();

        sql = "FROM mytable t SELECT * ";
        executeQuery();
    }

    void readJoin() throws Exception {
    }

    // 以下部分测试org.h2.command.Parser.parseEndOfQuery(Query)
    void parseEndOfQuery() throws Exception {
        sql = "select id,name,age from mytable order by 1, =? DESC";
        ps = conn.prepareStatement(sql);
        ps.setInt(1, 2);
        rs = ps.executeQuery();
        printResultSet(rs);

        sql = "select id,name,age from mytable order by ? DESC";
        ps = conn.prepareStatement(sql);
        ps.setInt(1, 2);
        rs = ps.executeQuery();
        printResultSet(rs);

        sql = "select id,name from mytable order by =1 DESC";
        executeQuery();

        sql = "select id,name from mytable order by -1 DESC";
        executeQuery();

        sql = "select name from mytable order by id";

        // sql = "(select name from mytable order by id)";
        // sql =
        // "(select name from (select name from mytable where id=? and name=?) where id=?)";
        // sql =
        // "(select name from mytable where id=? and id in(select name from mytable where id=? and name=?))";
        // ps = conn.prepareStatement(sql);
        // ps.setInt(1, 1);
        // ps.setInt(2, 1);
        // ps.setString(3, "abc");
        // rs = ps.executeQuery();

        // union时LIMIT、ordey by、FOR UPDATE不能放在子句中，要放在最后
        // 比如这条是错误的:
        // sql =
        // "select name1 from mytable1 order by name1 union select name2 from mytable2";
        // 要改成这样:
        sql = "select name1 from mytable1 union select name2 from mytable2 order by name1";

        sql = "select id,name from mytable order by id DESC";
        sql = "select id,name from mytable order by -1";

        // sql = "select id,name from mytable order by ?";
        // ps = conn.prepareStatement(sql);
        // ps.setInt(1, -1);
        // rs = ps.executeQuery();
        sql = "select id,name from mytable order by =-1 DESC";
        executeQuery();

        sql = "select id,name from mytable order by -1 DESC"; // 负数表示降序，再加DESC就表示降序的降序，实际就是升序
        sql = "select id,name from mytable order by -2 NULLS FIRST";
        sql = "select id,name from mytable order by -2 NULLS LAST";

        // OFFSET 要跟ROW或ROWS，不能少
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROW";
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROWS";

        // 下面两行等价 FETCH FIRST ROW ONLY和FETCH NEXT ROW ONLY一样
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROWS FETCH FIRST ROW ONLY";
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROWS FETCH NEXT ROW ONLY";

        // 下面两行等价
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROWS FETCH NEXT 1 ROW ONLY";
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROWS FETCH NEXT 4 ROWS ONLY";

        // 下面两行等价
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROWS FETCH FIRST 1 ROW ONLY";
        sql = "select id,name from mytable order by -2 NULLS LAST OFFSET 2 ROWS FETCH FIRST 4 ROWS ONLY";

        sql = "select id,name from mytable order by id LIMIT 2 OFFSET 3"; // OFFSET按0算，OFFSET
                                                                          // 3就是从第4条记录开始

        sql = "select id,name from mytable order by id LIMIT 3, 2"; // 跟上面效果一样"3, 2"表示OFFSET
                                                                    // 3 LIMIT 2
        sql = "select id,name from mytable order by id LIMIT 3, 5 SAMPLE_SIZE 2"; // SAMPLE_SIZE
                                                                                  // 2时不输出记录
        sql = "select id,name from mytable order by id LIMIT 3, 5 SAMPLE_SIZE 6"; // SAMPLE_SIZE
                                                                                  // 6时输出记录了
        sql = "select id,name from mytable FOR UPDATE";
        sql = "select id,name from mytable FOR READ ONLY WITH RS"; // "FOR READ ONLY WITH RS"可有可无

        executeQuery();
    }

    // 测试org.h2.command.Parser.parseSelectSimpleFromPart(Select)
    // org.h2.command.Parser.parseJoinTableFilter(TableFilter, Select)
    // org.h2.command.Parser.readJoin(TableFilter, Select, boolean, boolean)
    void parseSelectSimpleFromPart() {
        sql = "SELECT * FROM mytable1 RIGHT OUTER JOIN mytable2 ON mytable1.id1 = mytable2.id2";
        sql = "SELECT * FROM mytable1 LEFT OUTER JOIN mytable2 ON mytable1.id1 = mytable2.id2";
        // 不支持FULL JOIN
        // sql =
        // "SELECT * FROM mytable1 FULL JOIN mytable2 ON mytable1.id1 = mytable2.id2";

        // INNER JOIN和JOIN一样
        sql = "SELECT * FROM mytable1 INNER JOIN mytable2 ON mytable1.id1 = mytable2.id2";
        sql = "SELECT * FROM mytable1 JOIN mytable2 ON mytable1.id1 = mytable2.id2";

        sql = "SELECT * FROM mytable1 JOIN mytable2";

        // CROSS JOIN不能跟ON
        // sql =
        // "SELECT * FROM mytable1 CROSS JOIN mytable2 ON mytable1.id1 = mytable2.id2";
        // sql = "SELECT * FROM mytable1 CROSS JOIN mytable2";

        // NATURAL JOIN不能跟ON
        // sql =
        // "SELECT * FROM mytable1 NATURAL JOIN mytable2 ON mytable1.id1 = mytable2.id2";
        // sql =
        // "SELECT * FROM natural_join_test_table1 NATURAL JOIN natural_join_test_table2";

        // sql =
        // "SELECT * FROM mytable1 INNER JOIN mytable2 ON mytable1.id1 = mytable2.id2 LEFT OUTER JOIN mytable3";

        // sql =
        // "FROM ((select 1) union (select 1)) RIGHT OUTER JOIN ((select 2) union (select 2)) SELECT * ";
        //
        // sql =
        // "SELECT * FROM mytable1 RIGHT OUTER JOIN mytable2 LEFT OUTER JOIN mytable3 INNER JOIN mytable4 "
        // + "JOIN mytable5 CROSS JOIN mytable6 NATURAL JOIN mytable7 "
        // +
        // " ON mytable7.id7 = mytable6.id6 ON mytable6.id6 = mytable5.id5 ON mytable5.id5 = mytable4.id4"
        // +
        // " ON mytable4.id4 = mytable3.id3 ON mytable3.id3 = mytable2.id2 ON mytable2.id2 = mytable1.id1";
        //
        // //ON在最外面是不对的
        // sql =
        // "SELECT * FROM mytable1 RIGHT OUTER JOIN mytable2 LEFT OUTER JOIN mytable3 INNER JOIN mytable4 "
        // + "JOIN mytable5 CROSS JOIN mytable6" //
        // + " ON mytable5.id5 = mytable4.id4" //
        // +
        // " ON mytable4.id4 = mytable3.id3 ON mytable3.id3 = mytable2.id2 ON mytable2.id2 = mytable1.id1";
        //
        // sql =
        // "SELECT * FROM mytable1 RIGHT OUTER JOIN mytable2 ON mytable2.id2 = mytable1.id1";
        //
        // sql =
        // "SELECT * FROM  mytable3 INNER JOIN mytable4 ON mytable4.id4 = mytable3.id3";
        //
        // sql =
        // "SELECT * FROM mytable1 RIGHT OUTER JOIN mytable2 LEFT OUTER JOIN mytable3 INNER JOIN mytable4 "
        // + "JOIN mytable5 CROSS JOIN mytable6 NATURAL JOIN mytable7 " +
        // " ON mytable5.id5 = mytable4.id4"
        // +
        // " ON mytable4.id4 = mytable3.id3 ON mytable3.id3 = mytable2.id2 ON mytable2.id2 = mytable1.id1";
        //
        // //ON在最里面就没问题
        // sql = "SELECT * FROM mytable1"//
        // + " RIGHT OUTER JOIN mytable2 ON mytable2.id2 = mytable1.id1"//
        // + " LEFT OUTER JOIN mytable3 ON mytable3.id3 = mytable2.id2"//
        // + " INNER JOIN mytable4 ON mytable4.id4 = mytable3.id3"//
        // + " JOIN mytable5 ON mytable5.id5 = mytable4.id4"//
        // + " CROSS JOIN mytable6"//
        // + " NATURAL JOIN mytable7";

    }

    void select_init() throws Exception {
        // expandColumnList();
        // Query_initOrder();

        // sql = "select age, count(id) from mytable group by id,name";
        // executeQuery();
        //
        // sql = "select age, count(id), name from mytable group by id,name";
        // executeQuery();
        //
        // sql = "select age, count(id), name from mytable group by name, 2";
        // executeQuery();
        //
        // 测试havingIndex
        // sql = "select id,count(id) from mytable group by id having id=3";
        // executeQuery();
        // sql = "SELECT X/3 AS A, COUNT(*) FROM SYSTEM_RANGE(1, 1) GROUP BY A HAVING A>=0";
        // executeQuery();
        // sql = "SELECT X/3 AS A, COUNT(*) FROM SYSTEM_RANGE(1, 10) GROUP BY A HAVING A>=1";
        // executeQuery();
        //
        // sql = "select id,age, count(*),name from mytable group by id,age HAVING id>0";
        // executeQuery();
        //
        sql = "SELECT id AS A FROM mytable where A>=0";
        // executeQuery();
        sql = "SELECT id/3 AS A, COUNT(*) FROM mytable GROUP BY A HAVING A>=0";
        executeQuery();
    }

    void expandColumnList() throws Exception {
        // 不允许这样
        sql = "select mytable.* from mytable t";
        // 只能这样
        sql = "select t.* from mytable t";
        executeQuery();

        stmt.executeUpdate("drop table IF EXISTS emptytable");
        stmt.executeUpdate("create table IF NOT EXISTS emptytable()");

        sql = "select 2, *, 1 from emptytable";
        executeQuery();

        sql = "select *,name from emptytable, mytable";
        executeQuery();

        sql = "select * from mytable";
        executeQuery();
        sql = "select public.mytable.*, name from mytable";
        sql = "select public.mytable.*, name from mytable as t";
        // 对于myschema.t.*这样的语法，即使myschema不存在，在parser中也没有报错的
        // 而是在org.h2.command.dml.Select.expandColumnList()中报错
        // 因为mytable没有指定schema作为前缀，所以默认是public，这跟myschema不同，所以认为Table "T" not
        // found;
        sql = "select DISTINCT myschema.t.*, name from mytable as t";

        sql = "select * from mytable1, mytable2";
        executeQuery();

        sql = "select id, name from natural_join_test_table1, natural_join_test_table2";

        sql = "select * from natural_join_test_table1  natural join natural_join_test_table2";
        executeQuery();
    }

    void Query_initOrder() throws Exception {
        sql = "select name as n, id from mytable order by 1*1";
        executeQuery();

        // 列名不存在的检查是放在org.h2.expression.ExpressionColumn.optimize(Session)里做
        // Column "ID3" not found;
        sql = "select name,id3 from mytable order by 1*1";
        tryExecuteQuery();

        sql = "select distinct name from mytable order by id desc";
        tryExecuteQuery();

        sql = "select name,id from mytable t order by 1*1, t.id";
        executeQuery();

        sql = "select name,id from mytable order by 1 desc";
        executeQuery();

        sql = "select name,id as i from mytable order by i";
        executeQuery();

        sql = "select name,id as i from mytable t order by t.i";
        tryExecuteQuery();

        sql = "select name,id as i from mytable t order by mytable.i";
        tryExecuteQuery();

        sql = "select name,id as i from mytable t order by t.id";
        executeQuery();

        stmt.executeUpdate("CREATE CONSTANT IF NOT EXISTS ONE VALUE 1");

        sql = "select name,ONE from mytable order by name";

        sql = "select name,ONE from mytable where name = 'abc' || '123'";
    }

    void queryGroup() throws Exception {
        sql = "select id from mytable group by id";

        sql = "select id from mytable group by id having id>2";

        sql = "select id, count(id) from mytable group by id having id>2";
        executeQuery();

        // sql = "select name, count(name) from mytable group by name having name>2";
        // executeQuery();

        sql = "select id, count(id) from mytable group by id";
        sql = "select count(id) from mytable group by id";
        // sql = "select id,name,count(id) from mytable where id>0";
        sql = "select id,count(id) from mytable where id>0";
        // sql = "select id,count(id) from mytable where id>0  group by id";

        sql = "select max(id), count(id) from mytable where id>1";

        executeQuery();

        stmt.executeUpdate("delete from mytable");

        stmt.executeUpdate("insert into mytable(id, name) values(" + 1 + ", '" + 1 + "abcdef1234')");
        stmt.executeUpdate("insert into mytable(id, name) values(" + 1 + ", '" + 1 + "abcdef1234')");
        stmt.executeUpdate("insert into mytable(id, name) values(" + 2 + ", '" + 3 + "abcdef1234')");
        stmt.executeUpdate("insert into mytable(id, name) values(" + 2 + ", '" + 4 + "abcdef1234')");
        stmt.executeUpdate("insert into mytable(id, name) values(" + 3 + ", '" + 5 + "abcdef1234')");
        stmt.executeUpdate("insert into mytable(id, name) values(" + 3 + ", '" + 6 + "abcdef1234')");

        sql = "select id,name,count(id),sum(id) from mytable where id>0  group by id,name  having id<3";

        // sql =
        // "select id,count(id) from mytable where id>2  group by id having id=3";

        sql = "select id,count(id) from mytable t where id>2  group by id having t.id=3";

        // stmt.executeUpdate("set @X=6");
        sql = "SELECT 6/3 AS A, COUNT(*) FROM SYSTEM_RANGE(1, 10) GROUP BY A HAVING A>2";
        executeQuery();
    }

    void queryGroupSorted() throws Exception {
        stmt.executeUpdate("drop table IF EXISTS queryGroupSorted");
        stmt.executeUpdate("create table IF NOT EXISTS queryGroupSorted(id int, name varchar(500), age int)");
        stmt.executeUpdate("create index IF NOT EXISTS queryGroupSorted_index_id on queryGroupSorted(id)");
        stmt.executeUpdate("create index IF NOT EXISTS queryGroupSorted_index_name on queryGroupSorted(name)");
        stmt.executeUpdate("create index IF NOT EXISTS queryGroupSorted_index_id_name_age on queryGroupSorted(id,name,age)");

        stmt.executeUpdate("insert into queryGroupSorted(id, name, age) values(1, 'a', 10)");
        stmt.executeUpdate("insert into queryGroupSorted(id, name, age) values(2, 'b', 10)");
        stmt.executeUpdate("insert into queryGroupSorted(id, name, age) values(1, 'a', 20)");
        stmt.executeUpdate("insert into queryGroupSorted(id, name, age) values(2, 'b', 10)");
        stmt.executeUpdate("insert into queryGroupSorted(id, name, age) values(3, 'a', 20)");
        stmt.executeUpdate("insert into queryGroupSorted(id, name, age) values(3, 'b', 10)");

        // 下面三个都会使用GroupSortedIndex
        // 也就是走queryGroupSorted_index_id_name_age索引
        // group by后面字段的顺序是不重要的，谁先谁后都可以，所以下面两条sql会得到一样的结果
        sql = "select id,name,count(id) from queryGroupSorted group by id,name";
        executeQuery();
        sql = "select id,name,count(id) from queryGroupSorted group by name,id";
        executeQuery();
        sql = "select id,name,age,count(id) from queryGroupSorted group by id,name,age having id<3";
        executeQuery();

        // 不会使用GroupSortedIndex，因为queryGroupSorted_index_id_name_age是按id, name, age的顺序，而不是id,age
        sql = "select id,age,count(id) from queryGroupSorted group by id,age";
        executeQuery();

        // 不会使用GroupSortedIndex，
        // group by字段必须是索引字段列表的前缀，这里是后缀了，少了id
        sql = "select age,name,count(name) from queryGroupSorted group by age,name";
        executeQuery();
        // 同上
        sql = "select name,age,count(name) from queryGroupSorted group by name,age";
        executeQuery();
    }

    void queryQuick() throws Exception {
        // 这样不行，因为id字段可以为null
        // sql = "select count(id),min(id),max(id) from mytable";
        // executeQuery();

        // 如果先执行上面，那么由于缓存关系会直接使用前面的结果，所以并不执行Select.queryQuick()
        stmt.executeUpdate("ALTER TABLE mytable ALTER COLUMN id SET NOT NULL");
        sql = "select count(*),count(id),min(id),max(id) from mytable";
        executeQuery();
    }

    void queryDistinct() throws Exception {
        System.out.println();
        stmt.executeUpdate("drop table IF EXISTS queryDistinct");
        stmt.executeUpdate("create table IF NOT EXISTS queryDistinct(id int, name varchar(500))");
        stmt.executeUpdate("ALTER TABLE queryDistinct ALTER COLUMN id SELECTIVITY 10");
        stmt.executeUpdate("ALTER TABLE queryDistinct ALTER COLUMN name SELECTIVITY 10");
        // stmt.executeUpdate("ALTER TABLE queryDistinct ADD CONSTRAINT NAME_UNIQUE UNIQUE(name,id)");
        stmt.executeUpdate("create index IF NOT EXISTS queryDistinct_index on queryDistinct(name)");
        // 不会选单列的唯一索引
        // stmt.executeUpdate("create UNIQUE index IF NOT EXISTS queryDistinct_index on queryDistinct(name)");
        // stmt.executeUpdate("insert into queryDistinct(id, name) values(" + 1
        // + ", null)");
        stmt.executeUpdate("insert into queryDistinct(id, name) values(" + 1 + ", 'a')");
        stmt.executeUpdate("insert into queryDistinct(id, name) values(" + 2 + ", 'b')");
        stmt.executeUpdate("insert into queryDistinct(id, name) values(" + 3 + ", 'a')");
        stmt.executeUpdate("insert into queryDistinct(id, name) values(" + 4 + ", 'b')");
        stmt.executeUpdate("insert into queryDistinct(id, name) values(" + 5 + ", 'c')");

        sql = "select LIMIT 2 2 distinct name from queryDistinct";
        sql = "select distinct name from queryDistinct";
        executeQuery();
    }

    void getSortIndex() throws Exception {
        System.out.println();
        stmt.executeUpdate("drop table IF EXISTS getSortIndex");
        stmt.executeUpdate("create table IF NOT EXISTS getSortIndex(id int, name varchar(500))");
        stmt.executeUpdate("ALTER TABLE getSortIndex ALTER COLUMN id SELECTIVITY 10");
        stmt.executeUpdate("ALTER TABLE getSortIndex ALTER COLUMN name SELECTIVITY 10");
        // stmt.executeUpdate("ALTER TABLE getSortIndex ADD CONSTRAINT NAME_UNIQUE UNIQUE(name,id)");

        stmt.executeUpdate("create index IF NOT EXISTS getSortIndex_id on getSortIndex(id)");
        stmt.executeUpdate("create  index IF NOT EXISTS getSortIndex_name_id on getSortIndex(name, id)");
        stmt.executeUpdate("create UNIQUE index IF NOT EXISTS getSortIndex_name on getSortIndex(name DESC)");
        // stmt.executeUpdate("insert into getSortIndex(id, name) values(" + 1 +
        // ", null)");
        stmt.executeUpdate("insert into getSortIndex(id, name) values(" + 1 + ", '" + 1 + "abcdef1234')");
        stmt.executeUpdate("insert into getSortIndex(id, name) values(" + 2 + ", '" + 2 + "abcdef1234')");
        stmt.executeUpdate("insert into getSortIndex(id, name) values(" + 3 + ", '" + 3 + "abcdef1234')");
        stmt.executeUpdate("insert into getSortIndex(id, name) values(" + 4 + ", '" + 4 + "abcdef1234')");
        stmt.executeUpdate("insert into getSortIndex(id, name) values(" + 5 + ", '" + 5 + "abcdef1234')");

        sql = "select LIMIT 2 2 distinct name from getSortIndex";

        // 测if (sortColumns[i].column != currentColumns[i].column)
        sql = "select name from getSortIndex where id>1 order by name";
        executeQuery();

        // 测if (sortColumns[i].sortType != currentColumns[i].sortType)
        // 如果把getSortIndex_name中的DESC去掉，swapIndex为false
        // UNIQUE索引在org.h2.index.BaseIndex.getCostRangeIndex中算出的cost最小
        sql = "select name from getSortIndex where name='1' order by name, id";
        executeQuery();

        sql = "select 3, name from getSortIndex order by name, 1";
        executeQuery();

        sql = "select 3, name from getSortIndex order by id";
        executeQuery();

        sql = "select id, name from getSortIndex order by _rowid_";
        executeQuery();
    }

    void prepareOrder() throws Exception {
        sql = "select id,name from mytable order by -1 DESC"; // 负数表示降序，再加DESC就表示降序的降序，实际就是升序
        executeQuery();
        sql = "select id,name from mytable order by -2 NULLS FIRST";
        sql = "select id,name from mytable order by -2 NULLS LAST";
    }

    void prepare() throws Exception {
        prepareOrder();
        getSortIndex();
    }

    void queryWithoutCache() throws Exception {
        // LIMIT 2 0 (实际上表示的是：OFFSET(2)、LIMIT(0))
        sql = "select LIMIT 2 0 id,name from mytable";
        executeQuery();
    }

    void queryFlat() throws Exception {
        System.out.println();
        // OFFSET(1)、LIMIT(2))
        sql = "select LIMIT 1 2 id,age from mytable for update";
        executeQuery();
    }

    void preparePlan() throws Exception {
        sql = "explain select id,name from mytable where id>2 or age<50";
        sql = "select id,name from mytable where id>2 and age<50";
        executeQuery();
    }
}
