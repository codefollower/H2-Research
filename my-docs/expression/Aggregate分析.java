有16个聚合方法，有几个方法有不同别名:

COUNT         COUNT_ALL(count(*)) 和 COUNT(字段)，COUNT_ALL和COUNT合并，不存在COUNT_ALL这样的名字，都用COUNT
SUM
MIN
MAX
AVG

GROUP_CONCAT

STDDEV_SAMP   也叫: STDDEV
STDDEV_POP    也叫: STDDEVP
VAR_SAMP      也叫: VAR、VARIANCE
VAR_POP       也叫: VARP

BOOL_OR       也叫: SOME
BOOL_AND      也叫: EVERY

BIT_OR
BIT_AND

SELECTIVITY
HISTOGRAM     这个聚合函数在H2的文档中没有介绍


聚合函数只能出现在select语句的"select (1)... from (2) ...."中的(1)

static {
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
    }

方法调用顺序:

org.h2.expression.Aggregate.mapColumns(ColumnResolver, int)
org.h2.expression.Aggregate.optimize(Session)
org.h2.expression.Aggregate.updateAggregate(Session) //针对每行调用聚合方法
	=>org.h2.expression.AggregateData.add(Database, boolean, Value)
org.h2.expression.Aggregate.getValue(Session) //获得总结果
	=>org.h2.expression.AggregateData.getValue(Database, boolean)


特殊聚合函数说明:

1. SELECTIVITY

是基于某个表达式(多数是单个字段)算不重复的记录数所占总记录数的百分比
org.h2.engine.Constants.SELECTIVITY_DISTINCT_COUNT默认是1万，这个值不能改，
对统计值影响很大。通常这个值越大，统计越精确，但是会使用更多内存。
SELECTIVITY越大，说明重复的记录越少，在选择索引时更有利。

并行算法实现:
将select SELECTIVITY(字段) 改成select count(字段), count(DISTINCT 字段)
然后再算: {总count(字段)/总count(DISTINCT 字段)} * 100





标准差在多节点上的并行算法是: 在每节点上先算: 记录数、字段和、字段平方和，然后归并所有节点上返回的这三个参数，最后标准差 = {字段平方总和/总记录数- (字段总和/总记录数)的平方} 的平方根。HBase也是这么做的。

所以结论是: 对于标准差的分布式并行计算算法的优化是将"select std(字段)"先改写成"select count(字段)，sum(字段)，sum(字段*字段)"，归并后按此公式算: 标准差 = {字段平方总和/总记录数- (字段总和/总记录数)的平方} 的平方根

聚合函数从单结点扩展到多结点后复杂性增加了好多，就拿求平均数来说，当client接收到"select avg(字段) from 表"这样的sql后，需要改写sql，把avg换成count和sum，再加上有统计区间的where条件，变成"select count(字段), sum(字段) from 表 where 字段 between x and y"，直接每个结点求avg是不对的

1 2 3 4 5

平均值: 3

(1-3)的平方 + (2-3)的平方 + (3-3)的平方 + (4-3)的平方 + (5-3)的平方
= 4 + 1 + 0 + 1 + 4
=10

标准差 = (10/5)的平方跟 =1.4
================================
1 2 3

平均值: 2

(1-2)的平方 + (2-2)的平方 + (3-2)的平方
= 1 + 0 + 1
= 2

标准差 = (2/3)的平方跟 = 0.8

================================
4 5 

平均值: 4.5

(4-4.5)的平方 + (5-4.5)的平方
= 0.25 + 0.25
= 0.5

标准差 = (0.5/2)的平方跟 = 0.5

1
mean = 1;
m2 = 0;

2

mean = 1.5;
m2 = 0;


1 3 5

总和   1 + 3 + 5


平方和 1*1 + 3*3 + 5*5

行数 3

7 9 11

总和   7 + 9 + 11


平方和 7*7 + 9*9 + 11*11

行数 3
============================

两者汇总:
总和   (1 + 3 + 5)  +  (7 + 9 + 11)
平方和 (1*1 + 3*3 + 5*5)  +  (7*7 + 9*9 + 11*11)
行数   3 + 3  = 6

{(1 + 3 + 5)  +  (7 + 9 + 11)} / 6
{(1*1 + 3*3 + 5*5)  +  (7*7 + 9*9 + 11*11)} / 6

标准差:
{ {(1*1 + 3*3 + 5*5)  +  (7*7 + 9*9 + 11*11)} / 6 } - {{(1 + 3 + 5) + (7 + 9 + 11)} / 6} * {{(1 + 3 + 5) + (7 + 9 + 11)} / 6}

= (1+9+25+49+81+121)/6 - (36/6) * (36/6)
= 286/6 - (36/6) * (36/6)
= 11.666666666666666666666666666667
= 3.4156502553198661277403462268404

(1-6) (3-6) (5-6) (7-6) (9-6) (11-6)
25 + 9 + 1 + 1 + 9 + 25
= 70
= 3.4156502553198661277403462268404


(x-2)*(x-2) = x*x-2x-2x+4

(x- {{(1 + 3 + 5) + (7 + 9 + 11)} / 6})

x1 * x1 - 2x * {{(1 + 3 + 5) + (7 + 9 + 11)} / 6} + {{(1 + 3 + 5) + (7 + 9 + 11)} / 6}

(1 - (1 + 3 + 5)/3)的平方 + (3 - (1 + 3 + 5)/3)的平方  + (5 - (1 + 3 + 5)/3)的平方


1*1 - 2*1*(1 + 3 + 5)/3 + (1 + 3 + 5)/3*(1 + 3 + 5)/3
+ 3*3 - 2*3*(1 + 3 + 5)/3 + (1 + 3 + 5)/3*(1 + 3 + 5)/3
+ 5*5 - 2*5*(1 + 3 + 5)/3 + (1 + 3 + 5)/3*(1 + 3 + 5)/3


1*1 - (2*1*(1 + 3 + 5)/3 - (1 + 3 + 5)/3*(1 + 3 + 5)/3)
+ 3*3 - (2*3*(1 + 3 + 5)/3 - (1 + 3 + 5)/3*(1 + 3 + 5)/3)
+ 5*5 - (2*5*(1 + 3 + 5)/3 - (1 + 3 + 5)/3*(1 + 3 + 5)/3)

