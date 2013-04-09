
    private Insert parseInsert() {
        Insert command = new Insert(session);
        currentPrepared = command;
        read("INTO");
        Table table = readTableOrView();
        command.setTable(table);
        Column[] columns = null;
        if (readIf("(")) {
            if (isSelect()) {
                command.setQuery(parseSelect());
                read(")");
                return command;
            }
            columns = parseColumnList(table);
            command.setColumns(columns);
        }
        if (readIf("DIRECT")) {
            command.setInsertFromSelect(true);
        }
        if (readIf("SORTED")) {
            command.setSortedInsertMode(true);
        }
        if (readIf("DEFAULT")) { //比如: INSERT INTO tableName DEFAULT VALUES，插入一行默认值
            read("VALUES");
            Expression[] expr = { };
            command.addRow(expr);
        } else if (readIf("VALUES")) {
            read("(");
            do {
                ArrayList<Expression> values = New.arrayList();
				//比如: "INSERT INTO t VALUES(DEFAULT),('abc'),('123')"，可以同时insert多行
                if (!readIf(")")) {
                    do {
                        if (readIf("DEFAULT")) {
                            values.add(null);
                        } else {
                            values.add(readExpression());
                        }
                    } while (readIfMore());
                }
                command.addRow(values.toArray(new Expression[values.size()]));
                // the following condition will allow (..),; and (..);
            } while (readIf(",") && readIf("("));
        } else if (readIf("SET")) {
            if (columns != null) { //比如INSERT INTO t(name) set name='xyz'，不能同时指定字段列表
                throw getSyntaxError();
            }
            ArrayList<Column> columnList = New.arrayList();
            ArrayList<Expression> values = New.arrayList();
            do {
				//支持INSERT INTO t set name='xyz', f2=DEFAULT，...这样的语法，
                columnList.add(parseColumn(table));
                read("=");
                Expression expression;
                if (readIf("DEFAULT")) {
                    expression = ValueExpression.getDefault();
                } else {
                    expression = readExpression();
                }
                values.add(expression);
            } while (readIf(","));
            command.setColumns(columnList.toArray(new Column[columnList.size()]));
            command.addRow(values.toArray(new Expression[values.size()]));
        } else {
            command.setQuery(parseSelect());
        }
        return command;
    }
