时间与日期函数26个

函数名            函数种类    返回值的类型  参数个数(-1表示可变参数)   nullIfParameterIsNull   deterministic    fast  
------------------------------------------------------------------------------------
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

