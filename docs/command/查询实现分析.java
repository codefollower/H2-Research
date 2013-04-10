1. 概述

select的语法解析总入口是:
org.h2.command.Parser.parseSelect()

可以通过很多地方触发它，
最常见的是从org.h2.command.Parser.parsePrepared()触发它

触发调用parseSelect()的sql语法有三种情况:
分别是以"("、"select"、"from"开头的sql语法(不分大小写的)


2. 代码流程分析

如果sql是select name1 from mytable1 union select name2 from mytable2

parseSelect()
	=>parseSelectUnion()
		=>parseSelectSub()  
			=>parseSelectSimple()
		=>parseSelectUnionExtension(Query, int, boolean)

对于简单的sql，如select name1 from mytable1 order by name1
parseSelectSub()负责解析select name1 from mytable1
parseSelectUnionExtension负责解析 order by name1

如果union sql是select name1 from mytable1 union select name2 from mytable2 order by name1
parseSelectSub()负责解析select name1 from mytable1
parseSelectUnionExtension负责解析 union select name2 from mytable2 order by name1


3. join分析

有6种join类型
RIGHT OUTER JOIN
LEFT OUTER JOIN
FULL //暂时不支持
INNER JOIN
JOIN
CROSS JOIN
NATURAL JOIN

4.类图
org.h2.command.Prepared
	=>org.h2.command.dml.Query
		=>org.h2.command.dml.Select
		=>org.h2.command.dml.SelectUnion

调用顺序
	=>org.h2.command.dml.Query.init()(org.h2.command.dml.Select.init()或org.h2.command.dml.SelectUnion.init())
	=>org.h2.command.dml.Select.prepare()
	=>org.h2.command.dml.Query.query(int)

init在调用完org.h2.command.Parser.parseSelect()里调用(在得到一个Query后)
或者在解析完一个子查询时org.h2.command.Parser.readTableFilter(boolean)
此时也得到一个Query，
还有一种特殊情况是org.h2.command.Parser.parseValues()
即类似VALUES(1, 'Hello'), (2, 'World')这样的语法

org.h2.command.dml.Select.init()代码分析


DROP TABLE IF EXISTS JoinTest1 CASCADE;
CREATE TABLE IF NOT EXISTS JoinTest1(id int, name varchar(500), b boolean);
		
DROP TABLE IF EXISTS JoinTest2 CASCADE;
CREATE TABLE IF NOT EXISTS JoinTest2(id2 int, name2 varchar(500));

insert into JoinTest1(id, name, b) values(10, 'a1', true);
insert into JoinTest1(id, name, b) values(20, 'b1', true);
insert into JoinTest1(id, name, b) values(30, 'a2', false);
insert into JoinTest1(id, name, b) values(40, 'b2', true);
		
insert into JoinTest2(id2, name2) values(90, 'a11');
insert into JoinTest2(id2, name2) values(90, 'a11');
insert into JoinTest2(id2, name2) values(90, 'a11');
insert into JoinTest2(id2, name2) values(90, 'a11');

select rownum, * from JoinTest1 LEFT OUTER JOIN JoinTest2;
select rownum, * from JoinTest1 RIGHT OUTER JOIN JoinTest2;
select rownum, * from JoinTest1 INNER JOIN JoinTest2;
select rownum, * from JoinTest1 JOIN JoinTest2;
select * from JoinTest1 CROSS JOIN JoinTest2;
select from JoinTest1 NATURAL JOIN JoinTest2;

select * from JoinTest1 JOIN JoinTest2;


