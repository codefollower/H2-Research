	//可以用Create建立13种数据库对象
	//USER ROLE SCHEMA
	//AGGREGATE ALIAS CONSTANT
	//SEQUENCE TRIGGER DOMAIN(或TYPE或DATATYPE)
	//TABLE LINKED_TABLE VIEW INDEX
    private Prepared parseCreate() {
        boolean orReplace = false; //只对VIEW真正起作用
        if (readIf("OR")) {
            read("REPLACE");
            orReplace = true;
        }
        boolean force = readIf("FORCE"); //只对VIEW、AGGREGATE、ALIAS、TRIGGER、LINKED_TABLE真正起作用
        if (readIf("VIEW")) {
            return parseCreateView(force, orReplace);
        } else if (readIf("ALIAS")) {
            return parseCreateFunctionAlias(force);
        } else if (readIf("SEQUENCE")) {
            return parseCreateSequence();
        } else if (readIf("USER")) {
            return parseCreateUser();
        } else if (readIf("TRIGGER")) {
            return parseCreateTrigger(force);
        } else if (readIf("ROLE")) {
            return parseCreateRole();
        } else if (readIf("SCHEMA")) {
            return parseCreateSchema();
        } else if (readIf("CONSTANT")) {
            return parseCreateConstant();
        } else if (readIf("DOMAIN")) {
            return parseCreateUserDataType();
        } else if (readIf("TYPE")) {
            return parseCreateUserDataType();
        } else if (readIf("DATATYPE")) {
            return parseCreateUserDataType();
        } else if (readIf("AGGREGATE")) {
            return parseCreateAggregate(force);
        } else if (readIf("LINKED")) {
            return parseCreateLinkedTable(false, false, force);
        }
        // tables or linked tables or 索引(最后一个else部份的代码)
        boolean memory = false, cached = false;
        if (readIf("MEMORY")) {
            memory = true;
        } else if (readIf("CACHED")) {
            cached = true;
        }
        if (readIf("LOCAL")) {
            read("TEMPORARY");
            if (readIf("LINKED")) {
                return parseCreateLinkedTable(true, false, force);
            }
            read("TABLE");
            return parseCreateTable(true, false, cached);
        } else if (readIf("GLOBAL")) {
            read("TEMPORARY");
            if (readIf("LINKED")) {
                return parseCreateLinkedTable(true, true, force);
            }
            read("TABLE");
            return parseCreateTable(true, true, cached);
        } else if (readIf("TEMP") || readIf("TEMPORARY")) {
            if (readIf("LINKED")) {
                return parseCreateLinkedTable(true, true, force);
            }
            read("TABLE");
            return parseCreateTable(true, true, cached);
        } else if (readIf("TABLE")) {
            if (!cached && !memory) {
                cached = database.getDefaultTableType() == Table.TYPE_CACHED;
            }
            return parseCreateTable(false, false, cached);
        } else {
            boolean hash = false, primaryKey = false, unique = false;
            String indexName = null;
            Schema oldSchema = null;
            boolean ifNotExists = false;
            if (readIf("PRIMARY")) {
                read("KEY");
                if (readIf("HASH")) {
                    hash = true;
                }
                primaryKey = true;
                if (!isToken("ON")) {
                    ifNotExists = readIfNoExists();
                    indexName = readIdentifierWithSchema(null);
                    oldSchema = getSchema();
                }
            } else {
                if (readIf("UNIQUE")) {
                    unique = true;
                }
                if (readIf("HASH")) {
                    hash = true;
                }
                if (readIf("INDEX")) {
                    if (!isToken("ON")) {
                        ifNotExists = readIfNoExists();
                        indexName = readIdentifierWithSchema(null);
                        oldSchema = getSchema();
                    }
                } else {
                    throw getSyntaxError();
                }
            }
            read("ON");
            String tableName = readIdentifierWithSchema();
            checkSchema(oldSchema);
            CreateIndex command = new CreateIndex(session, getSchema());
            command.setIfNotExists(ifNotExists);
            command.setHash(hash);
            command.setPrimaryKey(primaryKey);
            command.setTableName(tableName);
            command.setUnique(unique);
            command.setIndexName(indexName);
            command.setComment(readCommentIf());
            read("(");
            command.setIndexColumns(parseIndexColumnList());
            return command;
        }
    }
