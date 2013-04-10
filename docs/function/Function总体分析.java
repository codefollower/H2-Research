有以下几种不同类型:

只有FunctionAlias也就是JavaFunction才有schema

1. Aggregate Function

对应org.h2.expression.Aggregate，只能用在select语句中

		addAggregate("COUNT", COUNT);
        addAggregate("SUM", SUM);
        addAggregate("MIN", MIN);
        addAggregate("MAX", MAX);
        addAggregate("AVG", AVG);
        addAggregate("GROUP_CONCAT", GROUP_CONCAT);
        addAggregate("STDDEV_SAMP", STDDEV_SAMP);
        addAggregate("STDDEV", STDDEV_SAMP);
        addAggregate("STDDEV_POP", STDDEV_POP);
        addAggregate("STDDEVP", STDDEV_POP);
        addAggregate("VAR_POP", VAR_POP);
        addAggregate("VARP", VAR_POP);
        addAggregate("VAR_SAMP", VAR_SAMP);
        addAggregate("VAR", VAR_SAMP);
        addAggregate("VARIANCE", VAR_SAMP);
        addAggregate("BOOL_OR", BOOL_OR);
        // HSQLDB compatibility, but conflicts with x > EVERY(...)
        addAggregate("SOME", BOOL_OR);
        addAggregate("BOOL_AND", BOOL_AND);
        // HSQLDB compatibility, but conflicts with x > SOME(...)
        addAggregate("EVERY", BOOL_AND);
        addAggregate("SELECTIVITY", SELECTIVITY);
        addAggregate("HISTOGRAM", HISTOGRAM);
此类函数名在使用时不带有schema名称



2. 用户定义的Aggregate Function

使用CREATE AGGREGATE语句创建，
如CREATE AGGREGATE MEDIAN FOR "com.acme.db.Median" ，
"com.acme.db.Median" 类必须实现org.h2.api.AggregateFunction接口
此类函数名在使用时不带有schema名称


3. FunctionAlias
使用CREATE ALIAS语句创建，

此类函数是直接对Java中的static方法的直接调用
如:
		stmt.executeUpdate("CREATE SCHEMA IF NOT EXISTS myschema AUTHORIZATION sa");
		stmt.executeUpdate("create table IF NOT EXISTS mytable(id int primary key, name varchar(500))");
		stmt.executeUpdate("CREATE ALIAS IF NOT EXISTS myschema.MY_SQRT FOR \"java.lang.Math.sqrt\"");
		CallableStatement cs = conn.prepareCall("?= CALL myschema.MY_SQRT(2.2)");
		cs.registerOutParameter(1, 0);
		cs.execute();
		
		System.out.println(cs.getDouble(1));
此类函数名在使用时可以带有schema名称(也可以不带，此时使用当前默认schema)




4. built-in function
对应org.h2.expression.Function

H2提供的内置函数，不带schema名称

H2内置了4种类别的函数:
----------------------------
Numeric Functions: 数值函数
String Functions: 字符串函数
Time and Date Functions: 时间与日期函数
System Functions: 系统函数


用FunctionInfo类来描述一个函数的相关信息，此类共7个字段:

class FunctionInfo { //7个字段
    String name;   //函数名
    int type;      //函数种类(就是函数的数字名称, 比如用0表示ABS函数)
    int dataType;  //函数返回值的类型
    int parameterCount; //函数参数个数
    boolean nullIfParameterIsNull; //只要传给函数的参数有一个是null，那么函数返回值就是null
	
	//对于相同的参数(0个或多个)，是否每次都返回相同的值，每次调用可能返回不同值，比如:CURRENT_TIME、RAND之类的函数
    boolean deterministic; 

    /**
     * Whether the function is fast, meaning the result shouldn't be cached.
     */
    boolean fast; //只有CSVREAD这个系统函数是true

}

总共150个函数
数值函数40个
字符串函数43个
时间与日期函数26个
系统函数41个

TIMESTAMPADD与DATEADD的函数种类一样，但是返回值不一样，其他当函数种类相同的两个函数其他字段都一样
fast字段为true的只有CSVREAD

函数名            函数种类    返回值的类型  参数个数(-1表示可变参数)   nullIfParameterIsNull   deterministic    fast  
------------------------------------------------------------------------------------
ABS                 0         NULL           1     true    true    false   
ACOS                1         DOUBLE         1     true    true    false   
ASIN                2         DOUBLE         1     true    true    false   
ATAN                3         DOUBLE         1     true    true    false   
ATAN2               4         DOUBLE         2     true    true    false   
BITAND              5         BIGINT         2     true    true    false   
BITOR               6         BIGINT         2     true    true    false   
BITXOR              7         BIGINT         2     true    true    false   
CEILING             8         DOUBLE         1     true    true    false   CEIL                8         DOUBLE         1     true    true    false    与前面相同  
COS                 9         DOUBLE         1     true    true    false   
COSH                36        DOUBLE         1     true    true    false   
COT                 10        DOUBLE         1     true    true    false   
DEGREES             11        DOUBLE         1     true    true    false   
EXP                 12        DOUBLE         1     true    true    false   
FLOOR               13        DOUBLE         1     true    true    false   
LOG                 14        DOUBLE         1     true    true    false   
LN                  39        DOUBLE         1     true    true    false   
LOG10               15        DOUBLE         1     true    true    false   
MOD                 16        BIGINT         2     true    true    false   
PI                  17        DOUBLE         0     true    true    false   
POWER               18        DOUBLE         2     true    true    false   
RADIANS             19        DOUBLE         1     true    true    false   
RAND                20        DOUBLE         -1    true    false   false   RANDOM              20        DOUBLE         -1    true    false   false    与前面相同  
ROUND               21        DOUBLE         -1    true    true    false   
ROUNDMAGIC          22        DOUBLE         1     true    true    false   
SIGN                23        INTEGER        1     true    true    false   
SIN                 24        DOUBLE         1     true    true    false   
SINH                37        DOUBLE         1     true    true    false   
SQRT                25        DOUBLE         1     true    true    false   
TAN                 26        DOUBLE         1     true    true    false   
TANH                38        DOUBLE         1     true    true    false   
TRUNCATE            27        DOUBLE         2     true    true    false   TRUNC               27        DOUBLE         2     true    true    false    与前面相同  
HASH                29        VARBINARY      3     true    true    false   
ENCRYPT             30        VARBINARY      3     true    true    false   
DECRYPT             31        VARBINARY      3     true    true    false   
SECURE_RAND         28        VARBINARY      1     true    false   false   
COMPRESS            32        VARBINARY      -1    true    true    false   
EXPAND              33        VARBINARY      1     true    true    false   
ZERO                34        INTEGER        0     true    true    false   
RANDOM_UUID         35        UUID           0     true    false   false   SYS_GUID            35        UUID           0     true    false   false    与前面相同  

ASCII               50        INTEGER        1     true    true    false   
BIT_LENGTH          51        BIGINT         1     true    true    false   
CHAR                52        VARCHAR        1     true    true    false   CHR                 52        VARCHAR        1     true    true    false    与前面相同  
CHAR_LENGTH         53        INTEGER        1     true    true    false   CHARACTER_LENGTH    53        INTEGER        1     true    true    false    与前面相同  
CONCAT              54        VARCHAR        -1    false   true    false   
CONCAT_WS           92        VARCHAR        -1    false   true    false   
DIFFERENCE          55        INTEGER        2     true    true    false   
HEXTORAW            56        VARCHAR        1     true    true    false   
INSERT              57        VARCHAR        4     false   true    false   
LCASE               59        VARCHAR        1     true    true    false   
LEFT                60        VARCHAR        2     true    true    false   
LENGTH              61        BIGINT         1     true    true    false   
LOCATE              62        INTEGER        -1    true    true    false   
POSITION            77        INTEGER        2     true    true    false   
INSTR               58        INTEGER        -1    true    true    false   
LTRIM               63        VARCHAR        -1    true    true    false   
OCTET_LENGTH        64        BIGINT         1     true    true    false   
RAWTOHEX            65        VARCHAR        1     true    true    false   
REPEAT              66        VARCHAR        2     true    true    false   
REPLACE             67        VARCHAR        -1    true    true    false   
RIGHT               68        VARCHAR        2     true    true    false   
RTRIM               69        VARCHAR        -1    true    true    false   
SOUNDEX             70        VARCHAR        1     true    true    false   
SPACE               71        VARCHAR        1     true    true    false   
SUBSTR              72        VARCHAR        -1    true    true    false   
SUBSTRING           73        VARCHAR        -1    true    true    false   
UCASE               74        VARCHAR        1     true    true    false   
LOWER               75        VARCHAR        1     true    true    false   
UPPER               76        VARCHAR        1     true    true    false   
TRIM                78        VARCHAR        -1    true    true    false   
STRINGENCODE        79        VARCHAR        1     true    true    false   
STRINGDECODE        80        VARCHAR        1     true    true    false   
STRINGTOUTF8        81        VARBINARY      1     true    true    false   
UTF8TOSTRING        82        VARCHAR        1     true    true    false   
XMLATTR             83        VARCHAR        2     true    true    false   
XMLNODE             84        VARCHAR        -1    false   true    false   
XMLCOMMENT          85        VARCHAR        1     true    true    false   
XMLCDATA            86        VARCHAR        1     true    true    false   
XMLSTARTDOC         87        VARCHAR        0     true    true    false   
XMLTEXT             88        VARCHAR        -1    true    true    false   
REGEXP_REPLACE      89        VARCHAR        3     true    true    false   
RPAD                90        VARCHAR        -1    true    true    false   
LPAD                91        VARCHAR        -1    true    true    false   

CURRENT_DATE        117       DATE           0     true    false   false   
CURDATE             100       DATE           0     true    false   false   
CURRENT_TIME        118       TIME           0     true    false   false   
CURTIME             101       TIME           0     true    false   false   
CURRENT_TIMESTAMP   119       TIMESTAMP      -1    true    false   false   
NOW                 112       TIMESTAMP      -1    true    false   false   
DATEADD             102       TIMESTAMP      3     true    true    false   TIMESTAMPADD        102       BIGINT         3     true    true    false    与前面不同  
DATEDIFF            103       BIGINT         3     true    true    false   TIMESTAMPDIFF       103       BIGINT         3     true    true    false    与前面相同  
DAYNAME             104       VARCHAR        1     true    true    false   
DAY                 105       INTEGER        1     true    true    false   DAY_OF_MONTH        105       INTEGER        1     true    true    false    与前面相同  DAYOFMONTH          105       INTEGER        1     true    true    false    与前面相同  
DAY_OF_WEEK         106       INTEGER        1     true    true    false   DAYOFWEEK           106       INTEGER        1     true    true    false    与前面相同  
DAY_OF_YEAR         107       INTEGER        1     true    true    false   DAYOFYEAR           107       INTEGER        1     true    true    false    与前面相同  
HOUR                108       INTEGER        1     true    true    false   
MINUTE              109       INTEGER        1     true    true    false   
MONTH               110       INTEGER        1     true    true    false   
MONTHNAME           111       VARCHAR        1     true    true    false   
QUARTER             113       INTEGER        1     true    true    false   
SECOND              114       INTEGER        1     true    true    false   
WEEK                115       INTEGER        1     true    true    false   
YEAR                116       INTEGER        1     true    true    false   
EXTRACT             120       INTEGER        2     true    true    false   
FORMATDATETIME      121       VARCHAR        -1    false   true    false   
PARSEDATETIME       122       TIMESTAMP      -1    false   true    false   
ISO_YEAR            123       INTEGER        1     true    true    false   
ISO_WEEK            124       INTEGER        1     true    true    false   
ISO_DAY_OF_WEEK     125       INTEGER        1     true    true    false   

DATABASE            150       VARCHAR        0     true    false   false   
USER                151       VARCHAR        0     true    false   false   
CURRENT_USER        152       VARCHAR        0     true    false   false   
IDENTITY            153       BIGINT         0     true    false   false   IDENTITY_VAL_LOCAL  153       BIGINT         0     true    false   false    与前面相同  LAST_INSERT_ID      153       BIGINT         0     true    false   false    与前面相同  LASTVAL             153       BIGINT         0     true    false   false    与前面相同  
SCOPE_IDENTITY      154       BIGINT         0     true    false   false   
AUTOCOMMIT          155       BOOLEAN        0     true    false   false   
READONLY            156       BOOLEAN        0     true    false   false   
DATABASE_PATH       157       VARCHAR        0     true    true    false   
LOCK_TIMEOUT        158       INTEGER        0     true    false   false   
IFNULL              200       NULL           2     false   true    false   ISNULL              200       NULL           2     false   true    false    与前面相同  
CASEWHEN            201       NULL           3     false   true    false   
CONVERT             202       NULL           1     false   true    false   
CAST                203       NULL           1     false   true    false   
TRUNCATE_VALUE      227       NULL           3     false   true    false   
COALESCE            204       NULL           -1    false   true    false   NVL                 204       NULL           -1    false   true    false    与前面相同  
NVL2                228       NULL           3     false   true    false   
NULLIF              205       NULL           2     false   true    false   
CASE                206       NULL           -1    false   true    false   
NEXTVAL             207       BIGINT         -1    true    false   false   
CURRVAL             208       BIGINT         -1    true    false   false   
ARRAY_GET           209       VARCHAR        2     true    true    false   
ARRAY_CONTAINS      230       BOOLEAN        2     false   true    false   
CSVREAD             210       RESULT_SET     -1    false   false   true    
CSVWRITE            211       INTEGER        -1    false   false   false   
MEMORY_FREE         212       INTEGER        0     true    false   false   
MEMORY_USED         213       INTEGER        0     true    false   false   
LOCK_MODE           214       INTEGER        0     true    false   false   
SCHEMA              215       VARCHAR        0     true    false   false   
SESSION_ID          216       INTEGER        0     true    false   false   
ARRAY_LENGTH        217       INTEGER        1     true    true    false   
LINK_SCHEMA         218       RESULT_SET     6     true    false   false   
LEAST               220       NULL           -1    false   true    false   
GREATEST            219       NULL           -1    false   true    false   
CANCEL_SESSION      221       BOOLEAN        1     true    false   false   
SET                 222       NULL           2     false   false   false   
FILE_READ           225       NULL           -1    false   false   false   
TRANSACTION_ID      226       VARCHAR        0     true    false   false   
DECODE              229       NULL           -1    false   true    false   
TABLE               223       RESULT_SET     -1    false   true    false   
TABLE_DISTINCT      224       RESULT_SET     -1    false   true    false   
ROW_NUMBER          300       BIGINT         0     false   true    false   


4.1 Numeric Functions: 数值函数39个(LOG和LN等价，所以算一个)
ABS
ACOS
ASIN
ATAN
COS
COSH
COT
SIN
SINH
TAN
TANH
ATAN2
BITAND
BITOR
BITXOR
MOD
CEILING
DEGREES
EXP
FLOOR
LOG
LOG10
RADIANS
SQRT
PI
POWER
RAND
RANDOM_UUID
ROUND
ROUNDMAGIC
SECURE_RAND
SIGN
ENCRYPT
DECRYPT
HASH
TRUNCATE
COMPRESS
EXPAND
ZERO