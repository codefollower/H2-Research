	//此方法涉及以下实例字段:
	//originalSQL
	//sqlCommand
	//sqlCommandChars
	//characterTypes
	//parseIndex(从0开始)

	//types初始化时每个元素都是0
	//此方法将注释、$$用空格替换, 把"`"、"["换成双引号，
	//同时对SQL中的每个字符表明其类型，以便下一步在read方法中识别sql中的各种结构。
    private void initialize(String sql) {
        if (sql == null) {
            sql = "";
        }
        originalSQL = sql; //不会变，最原始的SQL
        sqlCommand = sql; //会变
        int len = sql.length() + 1;

		//command和types的长度要比sql的长度多1，command的最后一个字符command[len]是空格，
		//types的最后一个元素types[len]是CHAR_END(是1)

		//最终的command和types会分别存到sqlCommandChars和characterTypes字段
		//command如果有变动，则sqlCommand字段的值重新从command生成
        char[] command = new char[len];
        int[] types = new int[len];
        len--;
        sql.getChars(0, len, command, 0);
        boolean changed = false;
        command[len] = ' ';
        int startLoop = 0;
        int lastType = 0;
        for (int i = 0; i < len; i++) {
            char c = command[i];
            int type = 0;
            switch (c) {
			//"单个"/"表示CHAR_SPECIAL_1字符，"/*"是块注释的开始标志，"//"是单行注释的开始标志
            case '/':
                if (command[i + 1] == '*') {
                    // block comment
                    changed = true;
                    command[i] = ' ';
                    command[i + 1] = ' ';
					
					//startLoop是"/"号开始的位置，当出现语法错误时才有用，会在它的位置之前放置[*]
					//例子: Syntax error in SQL statement "DROP [*]/*TABLE TEST";
                    startLoop = i;
                    i += 2;
                    checkRunOver(i, len, startLoop);
                    while (command[i] != '*' || command[i + 1] != '/') {
                        command[i++] = ' ';
                        checkRunOver(i, len, startLoop);
                    }
                    command[i] = ' ';
                    command[i + 1] = ' ';
					//这里对i增加后还是指向'/'的位置，
					//因为type此时是0，所以后面的types[i] = type;　lastType = type;都是0
                    i++;
                } else if (command[i + 1] == '/') {
                    // single line comment
                    changed = true;
                    startLoop = i;
                    while (true) {
                        c = command[i];
                        if (c == '\n' || c == '\r' || i >= len - 1) { //i >= len - 1是对应最后一行没有回车换行符

							//当c == '\n' || c == '\r' || i >= len - 1时这里就退出了，command[i]的值没变
							//例如"DROP TABLE //single line comment, drop table t"，没有回车换行符, 此时i >= len - 1
							//sql语句变成"DROP TABLE                                   t "
							//但是对应"t"的type是0，所以碰巧避免了问题: 
							//Syntax error in SQL statement "DROP TABLE                                   t "; 
							//expected "identifier"; SQL statement:DROP TABLE //single line comment, drop table t [42001-168]
                            break;
                        }
                        command[i++] = ' ';
                        checkRunOver(i, len, startLoop);
                    }
                } else {
                    type = CHAR_SPECIAL_1;
                }
                break;
            case '-': //与"//"相同，都是表示单行注释
                if (command[i + 1] == '-') {
                    // single line comment
                    changed = true;
                    startLoop = i;
                    while (true) {
                        c = command[i];
                        if (c == '\n' || c == '\r' || i >= len - 1) {
                            break;
                        }
                        command[i++] = ' ';
                        checkRunOver(i, len, startLoop);
                    }
                } else {
                    type = CHAR_SPECIAL_1;
                }
                break;
            case '$':
				//$$...$$用来表示java源代码，见h2文档: Features => User-Defined Functions and Stored Procedures
				//(i == 0 || command[i - 1] <= ' ')表示如果sql以$$开始，或者$$前面第一个字符是空格或控制字符，
				//说明这里用来表示java源代码
				//ASCII码表中0到31是控制字符, 32是空格
                if (command[i + 1] == '$' && (i == 0 || command[i - 1] <= ' ')) {
                    // dollar quoted string
                    changed = true;
                    command[i] = ' ';
                    command[i + 1] = ' ';
                    startLoop = i;
                    i += 2;
                    checkRunOver(i, len, startLoop);
                    while (command[i] != '$' || command[i + 1] != '$') {
                        types[i++] = CHAR_DOLLAR_QUOTED_STRING;
                        checkRunOver(i, len, startLoop);
                    }
                    command[i] = ' ';
                    command[i + 1] = ' ';
                    i++;
                } else {
					//$作为标识符的一部份
                    if (lastType == CHAR_NAME || lastType == CHAR_VALUE) {
                        // $ inside an identifier is supported
                        type = CHAR_NAME;
                    } else {
                        // but not at the start, to support PostgreSQL $1
                        type = CHAR_SPECIAL_1;
                    }
                }
                break;
            case '(':
            case ')':
            case '{':
            case '}':
            case '*':
            case ',':
            case ';':
            case '+':
            case '%':
            case '?':
            case '@':
            case ']':
                type = CHAR_SPECIAL_1;
                break;
            case '!':
            case '<':
            case '>':
            case '|':
            case '=':
            case ':':
            case '~':
                type = CHAR_SPECIAL_2;
                break;
            case '.':
                type = CHAR_DOT;
                break;
            case '\'':
                type = types[i] = CHAR_STRING;
                startLoop = i;
                while (command[++i] != '\'') {
                    checkRunOver(i, len, startLoop);
                }
                break;
            case '[': //SQL Server alias语法
                if (database.getMode().squareBracketQuotedNames) {
                    // SQL Server alias for "
                    command[i] = '"';
                    changed = true;
                    type = types[i] = CHAR_QUOTED;
                    startLoop = i;
                    while (command[++i] != ']') {
                        checkRunOver(i, len, startLoop);
                    }
                    command[i] = '"';
                } else {
                    type = CHAR_SPECIAL_1;
                }
                break;
            case '`': //MySQL alias语法，不过不区分大小写，默认都是大写
                // MySQL alias for ", but not case sensitive
                command[i] = '"';
                changed = true;
                type = types[i] = CHAR_QUOTED;
                startLoop = i;
                while (command[++i] != '`') {
                    checkRunOver(i, len, startLoop);
                    c = command[i];
                    command[i] = Character.toUpperCase(c);
                }
                command[i] = '"';
                break;
            case '\"':
                type = types[i] = CHAR_QUOTED;
                startLoop = i;
                while (command[++i] != '\"') {
                    checkRunOver(i, len, startLoop);
                }
                break;
            case '_':
                type = CHAR_NAME;
                break;
            default:
                if (c >= 'a' && c <= 'z') {
                    if (identifiersToUpper) {
                        command[i] = (char) (c - ('a' - 'A'));
                        changed = true;
                    }
                    type = CHAR_NAME;
                } else if (c >= 'A' && c <= 'Z') {
                    type = CHAR_NAME;
                } else if (c >= '0' && c <= '9') {
                    type = CHAR_VALUE;
                } else {
                    if (c <= ' ' || Character.isSpaceChar(c)) {
                        // whitespace
                    } else if (Character.isJavaIdentifierPart(c)) {
                        type = CHAR_NAME;
                        if (identifiersToUpper) {
                            char u = Character.toUpperCase(c);
                            if (u != c) {
                                command[i] = u;
                                changed = true;
                            }
                        }
                    } else {
                        type = CHAR_SPECIAL_1;
                    }
                }
            }
            types[i] = type;
            lastType = type;
        }
        sqlCommandChars = command;
        types[len] = CHAR_END;
        characterTypes = types;
        if (changed) {
            sqlCommand = new String(command);
        }
        parseIndex = 0;
    }
