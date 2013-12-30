选择最佳的执行计划，也可以称为选择代价最小的索引。

只对select、update、delete这三种SQL有效



select、update、delete都可以指定where子句，

1. 首先，根据where子句建立索引条件(index condition)，

以下8种类型的表达式(org.h2.expression.Expression)子类能建立索引条件

Comparison
CompareLike

对于下面三个，都要求件left表达式是ExpressionColumn
ConditionIn
ConditionInSelect
ConditionInConstantSet

ConditionAndOr 只支持AND的情形
ExpressionColumn 只支持列的类型是BOOLEAN的情形，如 delete from DeleteTest where b (b列在建表时是boolean类型)
ValueExpression 只支持值的类型是BOOLEAN，且是false的情形，如delete from DeleteTest where 3<2


索引条件有可能有多个。

2. 如果没有索引条件，那么使用ScanIndex

3. 否则按索引条件构建masks

	代价比较:
	EQUALITY < RANGE < END < START


4. 删除无用的索引条件(但是不删除AlwaysFalse的索引条件)