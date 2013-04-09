相关代码:

1. org.h2.command.Parser.parseCreateTrigger(boolean)
负责解析触发器的SQL语法


2. org.h2.command.ddl.CreateTrigger
解析触发器的SQL语法时得到的数据放在CreateTrigger对象中，
调用org.h2.command.ddl.CreateTrigger.update()会生成内部的org.h2.schema.TriggerObject实例，
并把此TriggerObject实例的一些信息放到Database的Meta表、Schema对象、Table对象。

3. org.h2.schema.TriggerObject
内含org.h2.api.Trigger接口的实现类的实例，以及CreateTrigger对象传进来的一些参数，
当进行CRUD操作时会触发:
	org.h2.schema.TriggerObject.fire(Session, int, boolean)
		此方法对不加FOR EACH ROW的触发器有效
	org.h2.schema.TriggerObject.fireRow(Session, Row, Row, boolean, boolean)
		此方法只对加FOR EACH ROW的触发器有效


4. org.h2.table.Table类中的一些fire方法

org.h2.table.Table.fire(Session, int, boolean)
	此方法对不加FOR EACH ROW的触发器有效
	因为一条insert sql可以增加多行记录，此方法相当于应用于整个insert sql的，
	而下面三个方法应用于insert sql中的每行记录，
	具体见org.h2.command.dml.Insert.insertRows()
	在org.h2.command.dml.Insert.insertRows()的前后调用fire，
	而在org.h2.command.dml.Insert.insertRows()内部的for循环中调用下面三个方法中的前两个
	org.h2.table.Table.fireRow()只用于delete和update
	
org.h2.table.Table.fireBeforeRow(Session, Row, Row)
org.h2.table.Table.fireAfterRow(Session, Row, Row, boolean)
org.h2.table.Table.fireRow()
	这三个方法只对加FOR EACH ROW的触发器有效

