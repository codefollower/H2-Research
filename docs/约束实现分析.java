
代码关系:

org.h2.command.Parser.parseCreateTable(boolean, boolean, boolean)

org.h2.command.Parser.parseAlterTableAddConstraintIf(String, Schema)
   (org.h2.command.Parser.parseAlterTable()或parseCreateTable会触发parseAlterTableAddConstraintIf)


在parseCreateTable或parseAlterTableAddConstraintIf触发AlterTableAddConstraint的构造函数
=> org.h2.command.ddl.AlterTableAddConstraint.AlterTableAddConstraint(Session, Schema, boolean)
=> org.h2.command.ddl.AlterTableAddConstraint.tryUpdate()
	=> org.h2.constraint.ConstraintCheck 构造函数
	=> org.h2.constraint.ConstraintUnique 构造函数
	=> org.h2.constraint.ConstraintReferential 构造函数


只有下面两种SQL能定义约束
ALTER TABLE ADD CONSTRAINT

CREATE TABLE


