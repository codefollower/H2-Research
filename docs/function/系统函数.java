系统函数41个

在下面两个方法中
org.h2.expression.Function.getValueWithArgs(Session, Expression[])
org.h2.expression.Function.getSimpleValue(Session, Value, Expression[], Value[])

		少了3个函数: ROW_NUMBER、TABLE、TABLE_DISTINCT
        //ROW_NUMBER函数虽然定义了，但ROW_NUMBER()函数无效，不支持这样的语法
		//sql = "SELECT ROW_NUMBER()"; 
		//ROWNUM函数虽然没有定义，但ROWNUM()是有效，Parser在解析时把他当成ROWNUM伪字段处理
		//当成了org.h2.expression.Rownum，见org.h2.command.Parser.readTerm()
		//sql = "SELECT ROWNUM()";

		//这样就没问题了,在这个方法中org.h2.command.Parser.readFunction(Schema, String)
		//把ROW_NUMBER转成org.h2.expression.Rownum了
		//sql = "SELECT ROW_NUMBER()OVER()"; 
		
在这个方法中
org.h2.command.Parser.readFunction(Schema, String)
把TABLE、TABLE_DISTINCT转成TableFunction了。


函数名            函数种类    返回值的类型  参数个数(-1表示可变参数)   nullIfParameterIsNull   deterministic    fast  
------------------------------------------------------------------------------------
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
