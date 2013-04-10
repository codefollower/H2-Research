字符串函数43个

函数名            函数种类    返回值的类型  参数个数(-1表示可变参数)   nullIfParameterIsNull   deterministic    fast  
------------------------------------------------------------------------------------
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