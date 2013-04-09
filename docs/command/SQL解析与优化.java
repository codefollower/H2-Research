1. SQL解析

词法分析、语法分析、语义分析混合


使用两遍扫描

1.1 第一遍扫描: 初始化阶段

a. 确定每个字符的类型
   SQL对应一个字符类型数组，此数组的每个元素表示字符的词法类型
    // used during the tokenizer phase
    private static final int CHAR_END = 1, CHAR_VALUE = 2, CHAR_QUOTED = 3;
    private static final int CHAR_NAME = 4, CHAR_SPECIAL_1 = 5, CHAR_SPECIAL_2 = 6;
    private static final int CHAR_STRING = 7, CHAR_DOT = 8, CHAR_DOLLAR_QUOTED_STRING = 9;

	还有一个0值，除上面9种以外的类型(用来表示注释、空白等)

b. sql预处理

做了如下事情:

1)
把以"//"或"--"开头的单行注释全换成空格
把"/* ... */"块注释全换成空格

注释在字符类型数组中对应的类型都是0,包括"//"、"--"、"/*"、"*/"

2)
$$...$$(用于自定义函数或存储过程)
如:
CREATE ALIAS IP_ADDRESS AS $$
import java.net.*;
@CODE
String ipAddress(String host) throws Exception {
    return InetAddress.getByName(host).getHostAddress();
}
$$;
那么会预先把$$替换成空格，类型是0,　而$$中包含的字符不变，类型是CHAR_DOLLAR_QUOTED_STRING。
$$中如果包含空格、回车换行类的字符也不做特殊处理。

3) 字符串
在sql里字符串是用单引号括起来，不像java是用双引号，
字符串保持不变，但是对于字符类型数组只对第一个单引号用CHAR_STRING表示，其他还是0，
在调用read读取token时，只要判断第一个字符类型是CHAR_STRING，那么就一直找到出现下一个单引号为止


4) 别名
a. SQL Server支持[..]格式，需要设置MODE = MSSQLServer
把[和]替换成双引号，对应[的类型是CHAR_QUOTED，其他的类型是0，
在调用read读取token时，只要判断第一个字符类型是CHAR_QUOTED，那么就一直找到出现下一个双引号为止

b. MySQL支持"`"号格式
把"`"号替换成双引号，对应第一个"`"号的类型是CHAR_QUOTED，其他的类型是0，
"`"号括起来的字符全转成大写,
在调用read读取token时，只要判断第一个字符类型是CHAR_QUOTED，那么就一直找到出现下一个双引号为止

c. 双引号格式
对应第一个双引号的类型是CHAR_QUOTED，其他的类型是0，
在调用read读取token时，只要判断第一个字符类型是CHAR_QUOTED，那么就一直找到出现下一个双引号为止




5)
下划线、a-z、A-Z当成CHAR_NAME，如果DATABASE_TO_UPPER参数为true，那么a-z要转成大写
0-9被当成CHAR_VALUE

上面以外的字符调用java.lang.Character.isJavaIdentifierPart(char)，
如果为true那么当成CHAR_NAME,如果DATABASE_TO_UPPER参数为true那么转化成大写


1.2 read 方法，读下一个token

Token类型:
    // this are token types
    private static final int KEYWORD = 1, IDENTIFIER = 2, PARAMETER = 3, END = 4, VALUE = 5;
    private static final int EQUAL = 6, BIGGER_EQUAL = 7, BIGGER = 8;
    private static final int SMALLER = 9, SMALLER_EQUAL = 10, NOT_EQUAL = 11, AT = 12;
    private static final int MINUS = 13, PLUS = 14, STRING_CONCAT = 15;
    private static final int OPEN = 16, CLOSE = 17, NULL = 18, TRUE = 19, FALSE = 20;
    private static final int CURRENT_TIMESTAMP = 21, CURRENT_DATE = 22, CURRENT_TIME = 23, ROWNUM = 24;



总共38个特殊token，不能充当标识符用

7个特殊的日期时间类型:
---------------------------------------------------
CURRENT_TIMESTAMP、CURRENT_TIME、CURRENT_DATE是三个特殊的token，他们即不是关键字，也不是标识符，
但是不能用它们充当标识符去，比如不能用CURRENT_DATE当表名:
CREATE TABLE current_date 这样的语法是错误的。

SYSTIMESTAMP、SYSTIME、SYSDATE与CURRENT_TIMESTAMP、CURRENT_TIME、CURRENT_DATE等价
TODAY也与CURRENT_DATE等价

3个常量token类型:
---------------------------------------------------
FALSE
TRUE
NULL

28个KEYWORD(关键字)
---------------------------------------------------
CROSS
DISTINCT
EXCEPT
EXISTS

FROM
FOR
FULL
FETCH 如果数据库支持supportOffsetFetch(DB2、Derby、PostgreSQL都支持)

GROUP
HAVING
INNER
INTERSECT
IS
JOIN

LIMIT
LIKE
MINUS
NOT
NATURAL

ON
OFFSET  如果数据库支持supportOffsetFetch(DB2、Derby、PostgreSQL都支持)
ORDER
ORDER
PRIMARY

ROWNUM
SELECT

UNIQUE
UNION
WHERE





2. SQL优化

2.1 表达式优化

2.1 索引选择(执行计划)