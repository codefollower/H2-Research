    private void read(String expected) {
		//用`、[]、“包围起来的字符串所代表的Token会使得currentTokenQuoted=true
		//如"CREATE or `REPLACE` TABLE IF NOT EXISTS
		//expected = currentToken = REPLACE
		//currentTokenQuoted = true
        if (currentTokenQuoted || !equalsToken(expected, currentToken)) {
            addExpected(expected);
            throw getSyntaxError();
        }
        read();
    }

	//read与readIf的差别
	//read: expected与currentToken必须一样，不一样则报语法错误，如是没有语法错误预读下一个token
	//readIf: 只有token与currentToken一样时才预读下一个token，不相同不会报语法错误
    private boolean readIf(String token) {
        if (!currentTokenQuoted && equalsToken(token, currentToken)) {
            read();
            return true;
        }
        addExpected(token);
        return false;
    }

	//与readIf(String token)大部份相同，唯一差别是isToken不会预读下一个token
	private boolean isToken(String token) {
        boolean result = equalsToken(token, currentToken) && !currentTokenQuoted;
        if (result) {
            return true;
        }
        addExpected(token);
        return false;
    }

    private boolean equalsToken(String a, String b) {
        if (a == null) {
            return b == null;
        } else if (a.equals(b)) {
            return true;
        } else if (!identifiersToUpper && a.equalsIgnoreCase(b)) {
            return true;
        }
        return false;
    }

    private void read() {
        currentTokenQuoted = false;
        if (expectedList != null) {
            expectedList.clear();
        }
        int[] types = characterTypes;
        lastParseIndex = parseIndex;
        int i = parseIndex;
        int type = types[i];
        while (type == 0) { //跳过最前面type为0的元素，因为0对应的字符是空白类的，没意义
            type = types[++i];
        }
        int start = i;
        char[] chars = sqlCommandChars;
        char c = chars[i++];
        currentToken = "";
        switch (type) {
        case CHAR_NAME:
            while (true) {
                type = types[i];
                if (type != CHAR_NAME && type != CHAR_VALUE) {
                    break;
                }
                i++;
            }
            currentToken = StringUtils.fromCacheOrNew(sqlCommand.substring(start, i));
            currentTokenType = getTokenType(currentToken);
            parseIndex = i;
            return;
        case CHAR_QUOTED: {
            String result = null;
			//内部的for循环用于找出第一对双引号中包含的字符
			//如果双引号中包含的字符又有双引号，while循环继续寻找后面的字符
			//比如对于"aaa""bbb"，i先从第一个a开始，到达第二个"号时，if (chars[i] == '\"')为true，
			//因为此时result为null，所以result = sqlCommand.substring(begin, i) = aaa
			//接着退出for循环，因为chars[++i]="，所以while循环继续,此时begin从第一个b开始，
			//进入到if (chars[i] == '\"')时，因为前面result = aaa，
			//所以result += sqlCommand.substring(begin - 1, i) = aaa"bbb
			//也就是说双引号中包含的字符如果是连续的两个""那么就表示"号自身
            while (true) {
                for (int begin = i;; i++) {
                    if (chars[i] == '\"') {
                        if (result == null) {
                            result = sqlCommand.substring(begin, i);
                        } else {
                            result += sqlCommand.substring(begin - 1, i); //begin - 1表示把前面的"号也加进来
                        }
                        break;
                    }
                }
                if (chars[++i] != '\"') { //比如"aaa""bbb"的场景，最终会转换成aaa"bbb
                    break;
                }
                i++;
            }
            currentToken = StringUtils.fromCacheOrNew(result);
            parseIndex = i;
            currentTokenQuoted = true;
            currentTokenType = IDENTIFIER;
            return;
        }
        case CHAR_SPECIAL_2:
			//两个CHAR_SPECIAL_2类型的字符要合并。例如!=
            if (types[i] == CHAR_SPECIAL_2) {
                i++;
            }
            currentToken = sqlCommand.substring(start, i);
            currentTokenType = getSpecialType(currentToken);
            parseIndex = i;
            return;
        case CHAR_SPECIAL_1:
            currentToken = sqlCommand.substring(start, i);
            currentTokenType = getSpecialType(currentToken);
            parseIndex = i;
            return;
        case CHAR_VALUE:
            if (c == '0' && chars[i] == 'X') { //在initialize中已把x转换成大写X
                // hex number
                long number = 0;
                start += 2;
                i++;
                while (true) {
                    c = chars[i];
                    if ((c < '0' || c > '9') && (c < 'A' || c > 'F')) {
                        checkLiterals(false);
                        currentValue = ValueInt.get((int) number);
                        currentTokenType = VALUE;
                        currentToken = "0";
                        parseIndex = i;
                        return;
                    }
                    number = (number << 4) + c - (c >= 'A' ? ('A' - 0xa) : ('0'));
                    if (number > Integer.MAX_VALUE) {
                        readHexDecimal(start, i);
                        return;
                    }
                    i++;
                }
            }
            long number = c - '0';
            while (true) {
                c = chars[i];
                if (c < '0' || c > '9') {
                    if (c == '.') {
                        readDecimal(start, i);
                        break;
                    }
                    if (c == 'E') {
                        readDecimal(start, i);
                        break;
                    }
                    checkLiterals(false);
                    currentValue = ValueInt.get((int) number);
                    currentTokenType = VALUE;
                    currentToken = "0";
                    parseIndex = i;
                    break;
                }
                number = number * 10 + (c - '0');
                if (number > Integer.MAX_VALUE) {
                    readDecimal(start, i);
                    break;
                }
                i++;
            }
            return;
        case CHAR_DOT:
            if (types[i] != CHAR_VALUE) {
                currentTokenType = KEYWORD;
                currentToken = ".";
                parseIndex = i;
                return;
            }
            readDecimal(i - 1, i);
            return;
        case CHAR_STRING: {
            String result = null;
			//与CHAR_QUOTED类似
            while (true) {
                for (int begin = i;; i++) {
                    if (chars[i] == '\'') {
                        if (result == null) {
                            result = sqlCommand.substring(begin, i);
                        } else {
                            result += sqlCommand.substring(begin - 1, i);
                        }
                        break;
                    }
                }
                if (chars[++i] != '\'') {
                    break;
                }
                i++;
            }
            currentToken = "'";
            checkLiterals(true);
            currentValue = ValueString.get(StringUtils.fromCacheOrNew(result));
            parseIndex = i;
            currentTokenType = VALUE;
            return;
        }
        case CHAR_DOLLAR_QUOTED_STRING: {
            String result = null;
            int begin = i - 1;
            while (types[i] == CHAR_DOLLAR_QUOTED_STRING) {
                i++;
            }
            result = sqlCommand.substring(begin, i);
            currentToken = "'";
            checkLiterals(true);
            currentValue = ValueString.get(StringUtils.fromCacheOrNew(result));
            parseIndex = i;
            currentTokenType = VALUE;
            return;
        }
        case CHAR_END:
            currentToken = "";
            currentTokenType = END;
            parseIndex = i;
            return;
        default:
            throw getSyntaxError();
        }
    }
