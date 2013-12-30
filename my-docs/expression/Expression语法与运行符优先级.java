优先级: 最低到最高
or
and
|| (字符串连接)

+ -
* / %
Term


以下子类的optimize方法可以忽视

org.h2.expression.Parameter
org.h2.expression.Rownum
org.h2.expression.SequenceValue
org.h2.expression.ValueExpression
org.h2.expression.Variable
org.h2.expression.Wildcard

Expression类的主要方法调用顺序：

mapColumns => optimize => updateAggregate => getValue

只有updateAggregate是可选的
用于聚合、分组中
在org.h2.command.dml.Select.queryGroup(int, LocalResult)
和org.h2.command.dml.Select.queryGroupSorted(int, ResultTarget)有用到
在遍历记录的过程中执行，在getValue之前调用



mapColumns方法主要是针对org.h2.expression.ExpressionColumn的，
主要目的是把表的列关联到它的Column column字段，并记住这个ExpressionColumn的ColumnResolver，
从ColumnResolver可获得列值，见org.h2.table.TableFilter.getValue(Column)
TableFilter类实现了ColumnResolver接口

optimize方法主要是做一些优化，比如看看是否是常量等等很多小的细节优化点，具体见每个Expression子类的不同实现

getValue得到表达式的值










