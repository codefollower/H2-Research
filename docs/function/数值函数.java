数值函数40个

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
