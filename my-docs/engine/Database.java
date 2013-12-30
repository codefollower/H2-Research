一开始会在org.h2.server.TcpServer.initManagementDb()触发一个内存数据库的初始化
url是: "jdbc:h2:mem:management_db_" + port (例如: jdbc:h2:mem:management_db_9092)


默认建了6个数据库对象
User: DBA (有Admin权限)
Schema: PUBLIC
Schema: INFORMATION_SCHEMA
Role: PUBLIC

RegularTable: SYS
Index: SYS_ID  是org.h2.index.TreeIndex



DBA这个User是PUBLIC、INFORMATION_SCHEMA这两个Schema的owner

不明确指定Schema时，默认就是PUBLIC Schema
PUBLIC这个Role默认没有授予权限

SYS表存放所有的ddl语句，此表有4个字段：ID、HEAD、TYPE、SQL，HEAD字段保留没用，都是0
SYS_ID是SYS表在ID字段上的索引

SYS表不能通过JDBC访问，因为它没被加到PUBLIC Schema中


Schema掌管7个能带Schema前缀的模式数据库对象
    private final HashMap<String, Table> tablesAndViews;
    private final HashMap<String, Index> indexes;
    private final HashMap<String, Constraint> constraints;
    private final HashMap<String, Sequence> sequences;
    private final HashMap<String, TriggerObject> triggers;
    private final HashMap<String, Constant> constants;
    private final HashMap<String, FunctionAlias> functions;

而Database类自身掌管了其他8个数据库对象，这些对象因为不是模式数据库对象，所以不能带Schema前缀

    private final HashMap<String, User> users = New.hashMap();
    private final HashMap<String, Role> roles = New.hashMap();
    private final HashMap<String, Right> rights = New.hashMap();
    private final HashMap<String, Schema> schemas = New.hashMap();
    private final HashMap<String, UserDataType> userDataTypes = New.hashMap(); //自定义的字段类型
    private final HashMap<String, UserAggregate> aggregates = New.hashMap(); //自定义聚合函数
	private final HashMap<String, Comment> comments = New.hashMap(); //注释
    private final HashMap<String, Setting> settings = New.hashMap(); //数据库、Session级别的动态参数


1. Database对象

	User
	=======

	有create、drop、alter语句


	Role
	=======

	有create、drop语句


	Right
	=======
	无create、drop、alter语句
	一个Right实例对应一条GRANT ROLE或GRANT RIGHT语句


	Schema
	=======

	有create、drop、alter语句


	UserDataType
	=======

	有create、drop语句
	CREATE DOMAIN、CREATE TYPE、CREATE DATATYPE都是一样的


	UserAggregate
	=======
	有create、drop语句


	Comment
	=======
	无create、drop、alter语句
	只有COMMENT ON，
	对于表、视图、索引、列、用户、约束的注释直接更新到他们的对象自身，
	其他的新建Comment实例
	如: COMMENT ON ROLE myrole IS 'role comment'


	Setting
	=======
	无create、drop、alter语句
	通过SET命令生成，SET命令不是DDL，也不是纯DML


2. Schema对象


	Table
	=======

	有create、drop、alter语句

	
	Index
	=======

	有create、drop、alter语句


	
	Constraint
	=======

	有create、drop、alter语句



	Sequence
	=======

	有create、drop语句

	

	TriggerObject
	=======

	有create、drop语句

	
	Constant
	=======

	有create、drop语句


	FunctionAlias
	=======

	有create、drop语句





