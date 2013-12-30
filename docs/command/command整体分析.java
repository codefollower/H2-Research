主要相关的类在org.h2.command包中

SQL语句由org.h2.command.Parser类解析

SQL语句分两种:
DML (data manipulation language)
	=> 对应org.h2.command.dml包

DDL (data definition language) 
	=> 对应org.h2.command.ddl包


比如CREATE TABLE语句对应org.h2.command.ddl.CreateTable类
再比如INSERT语句对应org.h2.command.dml.Insert类

SHOW语句在Parser类中当成SELECT语句，对应org.h2.command.dml.Select类

org.h2.command.Prepared是所有SQL语句对应的类的超类

Prepared类里的api，一般是先调用prepare，再调用update或query，update对应更新操作，query对应查询操作。


org.h2.command.CommandInterface接口用来表示执行的SQL命令
	<= org.h2.command.Command (用于服务端)
		<= org.h2.command.CommandContainer
		<= org.h2.command.CommandList
	<= org.h2.command.CommandRemote (用于客户端)

	Command与CommandRemote通常是对应的，CommandRemote类用在JDBC的实现中(org.h2.jdbc包中的类)，
	用CommandRemote来发送命令、处理结果集，Command与之对应。



1. DDL分类

DDL分三类:
alter
drop
create

2. Schema对象

有7种Schema对象
---------------------------------
    private final HashMap<String, Table> tablesAndViews;
    private final HashMap<String, Index> indexes;
    private final HashMap<String, Sequence> sequences;
    private final HashMap<String, TriggerObject> triggers;
    private final HashMap<String, Constraint> constraints;
    private final HashMap<String, Constant> constants;
    private final HashMap<String, FunctionAlias> functions;


