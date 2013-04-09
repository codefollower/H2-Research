    private Expression readTerm() {
        Expression r;
        switch (currentTokenType) {
        case AT:
            read();
            r = new Variable(session, readAliasIdentifier());
            if (readIf(":=")) {
                Expression value = readExpression();
                Function function = Function.getFunction(database, "SET");
                function.setParameter(0, r);
                function.setParameter(1, value);
                r = function;
            }
            break;
        case PARAMETER:
            // there must be no space between ? and the number
            boolean indexed = Character.isDigit(sqlCommandChars[parseIndex]);
            read();
            Parameter p;
            if (indexed && currentTokenType == VALUE && currentValue.getType() == Value.INT) {
                if (indexedParameterList == null) {
                    if (parameters == null) {
                        // this can occur when parsing expressions only (for example check constraints)
                        throw getSyntaxError();
                    } else if (parameters.size() > 0) {
                        throw DbException.get(ErrorCode.CANNOT_MIX_INDEXED_AND_UNINDEXED_PARAMS);
                    }
                    indexedParameterList = New.arrayList();
                }
                int index = currentValue.getInt() - 1;
                if (index < 0 || index >= Constants.MAX_PARAMETER_INDEX) {
                    throw DbException.getInvalidValueException("parameter index", index);
                }
                if (indexedParameterList.size() <= index) {
                    indexedParameterList.ensureCapacity(index + 1);
                    while (indexedParameterList.size() <= index) {
                        indexedParameterList.add(null);
                    }
                }
                p = indexedParameterList.get(index);
                if (p == null) {
                    p = new Parameter(index);
                    indexedParameterList.set(index, p);
                }
                read();
            } else {
                if (indexedParameterList != null) {
                    throw DbException.get(ErrorCode.CANNOT_MIX_INDEXED_AND_UNINDEXED_PARAMS);
                }
                p = new Parameter(parameters.size());
            }
            parameters.add(p);
            r = p;
            break;
        case KEYWORD:
            if (isToken("SELECT") || isToken("FROM")) {
                Query query = parseSelect();
                r = new Subquery(query);
            } else {
                throw getSyntaxError();
            }
            break;
        case IDENTIFIER:
            String name = currentToken;
            if (currentTokenQuoted) {
                read();
                if (readIf("(")) {
                    r = readFunction(null, name);
                } else if (readIf(".")) {
                    r = readTermObjectDot(name);
                } else {
                    r = new ExpressionColumn(database, null, null, name);
                }
            } else {
                read();
                if (readIf(".")) {
                    r = readTermObjectDot(name);
                } else if (equalsToken("CASE", name)) {
                    // CASE must be processed before (,
                    // otherwise CASE(3) would be a function call, which it is
                    // not
                    if (isToken("WHEN")) {
                        r = readWhen(null);
                    } else {
                        Expression left = readExpression();
                        r = readWhen(left);
                    }
                } else if (readIf("(")) {
                    r = readFunction(null, name);
                } else if (equalsToken("CURRENT_USER", name)) {
                    r = readFunctionWithoutParameters("USER");
                } else if (equalsToken("CURRENT", name)) {
                    if (readIf("TIMESTAMP")) {
                        r = readFunctionWithoutParameters("CURRENT_TIMESTAMP");
                    } else if (readIf("TIME")) {
                        r = readFunctionWithoutParameters("CURRENT_TIME");
                    } else if (readIf("DATE")) {
                        r = readFunctionWithoutParameters("CURRENT_DATE");
                    } else {
                        r = new ExpressionColumn(database, null, null, name);
                    }
                } else if (equalsToken("NEXT", name) && readIf("VALUE")) {
                    read("FOR");
                    Sequence sequence = readSequence();
                    r = new SequenceValue(sequence);
                } else if (currentTokenType == VALUE && currentValue.getType() == Value.STRING) {
                    if (equalsToken("DATE", name)) {
                        String date = currentValue.getString();
                        read();
                        r = ValueExpression.get(ValueDate.parse(date));
                    } else if (equalsToken("TIME", name)) {
                        String time = currentValue.getString();
                        read();
                        r = ValueExpression.get(ValueTime.parse(time));
                    } else if (equalsToken("TIMESTAMP", name)) {
                        String timestamp = currentValue.getString();
                        read();
                        r = ValueExpression.get(ValueTimestamp.parse(timestamp));
                    } else if (equalsToken("X", name)) {
                        read();
                        byte[] buffer = StringUtils.convertHexToBytes(currentValue.getString());
                        r = ValueExpression.get(ValueBytes.getNoCopy(buffer));
                    } else if (equalsToken("E", name)) {
                        String text = currentValue.getString();
                        // the PostgreSQL ODBC driver uses
                        // LIKE E'PROJECT\\_DATA' instead of LIKE 'PROJECT\_DATA'
                        // N: SQL-92 "National Language" strings
                        text = StringUtils.replaceAll(text, "\\\\", "\\");
                        read();
                        r = ValueExpression.get(ValueString.get(text));
                    } else if (equalsToken("N", name)) {
                        // SQL-92 "National Language" strings
                        String text = currentValue.getString();
                        read();
                        r = ValueExpression.get(ValueString.get(text));
                    } else {
                        r = new ExpressionColumn(database, null, null, name);
                    }
                } else {
                    r = new ExpressionColumn(database, null, null, name);
                }
            }
            break;
        case MINUS:
            read();
            if (currentTokenType == VALUE) {
                r = ValueExpression.get(currentValue.negate());
                if (r.getType() == Value.LONG && r.getValue(session).getLong() == Integer.MIN_VALUE) {
                    // convert Integer.MIN_VALUE to type 'int'
                    // (Integer.MAX_VALUE+1 is of type 'long')
                    r = ValueExpression.get(ValueInt.get(Integer.MIN_VALUE));
                } else if (r.getType() == Value.DECIMAL && r.getValue(session).getBigDecimal().compareTo(ValueLong.MIN_BD) == 0) {
                    // convert Long.MIN_VALUE to type 'long'
                    // (Long.MAX_VALUE+1 is of type 'decimal')
                    r = ValueExpression.get(ValueLong.get(Long.MIN_VALUE));
                }
                read();
            } else {
                r = new Operation(Operation.NEGATE, readTerm(), null);
            }
            break;
        case PLUS:
            read();
            r = readTerm();
            break;
        case OPEN:
            read();
            if (readIf(")")) {
                r = new ExpressionList(new Expression[0]);
            } else {
                r = readExpression();
                if (readIf(",")) {
                    ArrayList<Expression> list = New.arrayList();
                    list.add(r);
                    while (!readIf(")")) {
                        r = readExpression();
                        list.add(r);
                        if (!readIf(",")) {
                            read(")");
                            break;
                        }
                    }
                    Expression[] array = new Expression[list.size()];
                    list.toArray(array);
                    r = new ExpressionList(array);
                } else {
                    read(")");
                }
            }
            break;
        case TRUE:
            read();
            r = ValueExpression.get(ValueBoolean.get(true));
            break;
        case FALSE:
            read();
            r = ValueExpression.get(ValueBoolean.get(false));
            break;
        case CURRENT_TIME:
            read();
            r = readFunctionWithoutParameters("CURRENT_TIME");
            break;
        case CURRENT_DATE:
            read();
            r = readFunctionWithoutParameters("CURRENT_DATE");
            break;
        case CURRENT_TIMESTAMP: {
            Function function = Function.getFunction(database, "CURRENT_TIMESTAMP");
            read();
            if (readIf("(")) {
                if (!readIf(")")) {
                    function.setParameter(0, readExpression());
                    read(")");
                }
            }
            function.doneWithParameters();
            r = function;
            break;
        }
        case ROWNUM:
            read();
            if (readIf("(")) {
                read(")");
            }
            r = new Rownum(currentSelect == null ? currentPrepared : currentSelect);
            break;
        case NULL:
            read();
            r = ValueExpression.getNull();
            break;
        case VALUE:
            r = ValueExpression.get(currentValue);
            read();
            break;
        default:
            throw getSyntaxError();
        }
        if (readIf("[")) {
            Function function = Function.getFunction(database, "ARRAY_GET");
            function.setParameter(0, r);
            r = readExpression();
            r = new Operation(Operation.PLUS, r, ValueExpression.get(ValueInt.get(1)));
            function.setParameter(1, r);
            r = function;
            read("]");
        }
        if (readIf("::")) {
            // PostgreSQL compatibility
            if (readIf("REGCLASS")) {
                FunctionAlias f = findFunctionAlias(Constants.SCHEMA_MAIN, "PG_GET_OID");
                if (f == null) {
                    throw getSyntaxError();
                }
                Expression[] args = { r };
                JavaFunction func = new JavaFunction(f, args);
                r = func;
            } else {
                Column col = parseColumnWithType(null);
                Function function = Function.getFunction(database, "CAST");
                function.setDataType(col);
                function.setParameter(0, r);
                r = function;
            }
        }
        return r;
    }
