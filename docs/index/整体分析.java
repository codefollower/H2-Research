
1.综述

所有的索引都实现了org.h2.index.Index接口

Index接口有add、remove、find方法，但是没有update方法

实现Index接口的两个直接子类是:
org.h2.index.MultiVersionIndex
org.h2.index.BaseIndex

BaseIndex是一个抽象类，如果要实现新的索引类型，可以从此类扩展。

H2目前实现了13种索引类型(请看"Index类图"文件)


2. 各类索引类型说明

org.h2.index.MetaIndex

MetaIndex是只读的，不支持add、remove方法，
用于查找数据库元数据，比如通过java.sql.DatabaseMetaData提供的方法查找


org.h2.index.FunctionIndex

FunctionIndex是只读的，不支持add、remove方法，
用于"SELECT * FROM VALUES(1, 'Hello'), (2, 'World')" ，遍历VALUES


3. 查询记录时，如何通过索引中的key与PageDataIndex中的数据关联起来

在org.h2.command.dml.Select.queryFlat(int, ResultTarget, long)中调用
				for (int i = 0; i < columnCount; i++) {
                    Expression expr = expressions.get(i);
                    //触发:
                    //org.h2.expression.ExpressionColumn.getValue(Session)
                    //org.h2.table.TableFilter.getValue(Column)
                    row[i] = expr.getValue(session);
                }
会取得当前行的所有字段，expr.getValue(session)会触发org.h2.table.TableFilter.getValue(Column)
    public Value getValue(Column column) {
        if (currentSearchRow == null) {
            return null;
        }
        int columnId = column.getColumnId();
        if (columnId == -1) {
            return ValueLong.get(currentSearchRow.getKey());
        }
        if (current == null) {
            Value v = currentSearchRow.getValue(columnId);
            if (v != null) {
                return v;
            }
            current = cursor.get();
            if (current == null) {
                return ValueNull.INSTANCE;
            }
        }
        return current.getValue(columnId);
    }
currentSearchRow是索引字段组成的行，如果要取的列包含在currentSearchRow中，那么直接返回currentSearchRow中的列，
否则调用org.h2.index.IndexCursor.get() => org.h2.index.PageBtreeCursor.get()
	public Row get() {
        if (currentRow == null && currentSearchRow != null) {
			//currentSearchRow.getKey()得到的就是PageDataIndex中的key
            currentRow = index.getRow(session, currentSearchRow.getKey());
        }
        return currentRow;
    }

=>org.h2.index.PageBtreeIndex.getRow(Session, long)
    public Row getRow(Session session, long key) {
        return tableData.getRow(session, key);
    }

=>org.h2.table.RegularTable.getRow(Session, long)
    public Row getRow(Session session, long key) {
        return scanIndex.getRow(session, key);
    }

=>org.h2.index.PageDataIndex.getRow(Session, long)
    public Row getRow(Session session, long key) {
        return getRowWithKey(key);
    }