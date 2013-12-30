    /**
     * Compare a value to the specified type.
     *
     * @param targetType the type of the returned value
     * @return the converted value
     */
    public Value convertTo(int targetType) {
        // converting NULL is done in ValueNull
        // converting BLOB to CLOB and vice versa is done in ValueLob
        if (getType() == targetType) {
            return this;
        }
        try {
            // decimal conversion
            switch (targetType) {
            case BOOLEAN: {
                switch (getType()) {
                case BYTE:
                case SHORT:
                case INT:
                case LONG:
                case DECIMAL:
                case DOUBLE:
                case FLOAT:
                    return ValueBoolean.get(getSignum() != 0);
                case TIME:
                case DATE:
                case TIMESTAMP:
                case BYTES:
                case JAVA_OBJECT:
                case UUID:
                    throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, getString());
                }
                break;
            }
            case BYTE: {
                switch (getType()) {
                case BOOLEAN:
                    return ValueByte.get(getBoolean().booleanValue() ? (byte) 1 : (byte) 0);
                case SHORT:
                    return ValueByte.get(convertToByte(getShort()));
                case INT:
                    return ValueByte.get(convertToByte(getInt()));
                case LONG:
                    return ValueByte.get(convertToByte(getLong()));
                case DECIMAL:
                    return ValueByte.get(convertToByte(convertToLong(getBigDecimal())));
                case DOUBLE:
                    return ValueByte.get(convertToByte(convertToLong(getDouble())));
                case FLOAT:
                    return ValueByte.get(convertToByte(convertToLong(getFloat())));
                case BYTES:
                    return ValueByte.get((byte) Integer.parseInt(getString(), 16));
                }
                break;
            }
            case SHORT: {
                switch (getType()) {
                case BOOLEAN:
                    return ValueShort.get(getBoolean().booleanValue() ? (short) 1 : (short) 0);
                case BYTE:
                    return ValueShort.get(getByte());
                case INT:
                    return ValueShort.get(convertToShort(getInt()));
                case LONG:
                    return ValueShort.get(convertToShort(getLong()));
                case DECIMAL:
                    return ValueShort.get(convertToShort(convertToLong(getBigDecimal())));
                case DOUBLE:
                    return ValueShort.get(convertToShort(convertToLong(getDouble())));
                case FLOAT:
                    return ValueShort.get(convertToShort(convertToLong(getFloat())));
                case BYTES:
                    return ValueShort.get((short) Integer.parseInt(getString(), 16));
                }
                break;
            }
            case INT: {
                switch (getType()) {
                case BOOLEAN:
                    return ValueInt.get(getBoolean().booleanValue() ? 1 : 0);
                case BYTE:
                    return ValueInt.get(getByte());
                case SHORT:
                    return ValueInt.get(getShort());
                case LONG:
                    return ValueInt.get(convertToInt(getLong()));
                case DECIMAL:
                    return ValueInt.get(convertToInt(convertToLong(getBigDecimal())));
                case DOUBLE:
                    return ValueInt.get(convertToInt(convertToLong(getDouble())));
                case FLOAT:
                    return ValueInt.get(convertToInt(convertToLong(getFloat())));
                case BYTES:
                    return ValueInt.get((int) Long.parseLong(getString(), 16));
                }
                break;
            }
            case LONG: {
                switch (getType()) {
                case BOOLEAN:
                    return ValueLong.get(getBoolean().booleanValue() ? 1 : 0);
                case BYTE:
                    return ValueLong.get(getByte());
                case SHORT:
                    return ValueLong.get(getShort());
                case INT:
                    return ValueLong.get(getInt());
                case DECIMAL:
                    return ValueLong.get(convertToLong(getBigDecimal()));
                case DOUBLE:
                    return ValueLong.get(convertToLong(getDouble()));
                case FLOAT:
                    return ValueLong.get(convertToLong(getFloat()));
                case BYTES: {
                    // parseLong doesn't work for ffffffffffffffff
                    byte[] d = getBytes();
                    if (d.length == 8) {
                        return ValueLong.get(Utils.readLong(d, 0));
                    }
                    return ValueLong.get(Long.parseLong(getString(), 16));
                }
                }
                break;
            }
            case DECIMAL: {
                // convert to string is required for JDK 1.4
                switch (getType()) {
                case BOOLEAN:
                    return ValueDecimal.get(BigDecimal.valueOf(getBoolean().booleanValue() ? 1 : 0));
                case BYTE:
                    return ValueDecimal.get(BigDecimal.valueOf(getByte()));
                case SHORT:
                    return ValueDecimal.get(BigDecimal.valueOf(getShort()));
                case INT:
                    return ValueDecimal.get(BigDecimal.valueOf(getInt()));
                case LONG:
                    return ValueDecimal.get(BigDecimal.valueOf(getLong()));
                case DOUBLE: {
                    double d = getDouble();
                    if (Double.isInfinite(d) || Double.isNaN(d)) {
                        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, "" + d);
                    }
                    return ValueDecimal.get(BigDecimal.valueOf(d));
                }
                case FLOAT: {
                    float f = getFloat();
                    if (Float.isInfinite(f) || Float.isNaN(f)) {
                        throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, "" + f);
                    }
                    return ValueDecimal.get(new BigDecimal(Float.toString(f)));
                }
                }
                break;
            }
            case DOUBLE: {
                switch (getType()) {
                case BOOLEAN:
                    return ValueDouble.get(getBoolean().booleanValue() ? 1 : 0);
                case BYTE:
                    return ValueDouble.get(getByte());
                case SHORT:
                    return ValueDouble.get(getShort());
                case INT:
                    return ValueDouble.get(getInt());
                case LONG:
                    return ValueDouble.get(getLong());
                case DECIMAL:
                    return ValueDouble.get(getBigDecimal().doubleValue());
                case FLOAT:
                    return ValueDouble.get(getFloat());
                }
                break;
            }
            case FLOAT: {
                switch (getType()) {
                case BOOLEAN:
                    return ValueFloat.get(getBoolean().booleanValue() ? 1 : 0);
                case BYTE:
                    return ValueFloat.get(getByte());
                case SHORT:
                    return ValueFloat.get(getShort());
                case INT:
                    return ValueFloat.get(getInt());
                case LONG:
                    return ValueFloat.get(getLong());
                case DECIMAL:
                    return ValueFloat.get(getBigDecimal().floatValue());
                case DOUBLE:
                    return ValueFloat.get((float) getDouble());
                }
                break;
            }
            case DATE: {
                switch (getType()) {
                case TIME:
                    // because the time has set the date to 1970-01-01,
                    // this will be the result
                    return ValueDate.fromDateValue(DateTimeUtils.dateValue(1970, 1, 1));
                case TIMESTAMP:
                    return ValueDate.fromDateValue(((ValueTimestamp) this).getDateValue());
                }
                break;
            }
            case TIME: {
                switch (getType()) {
                case DATE:
                    // need to normalize the year, month and day
                    // because a date has the time set to 0, the result will be 0
                    return ValueTime.fromNanos(0);
                case TIMESTAMP:
                    return ValueTime.fromNanos(((ValueTimestamp) this).getNanos());
                }
                break;
            }
            case TIMESTAMP: {
                switch (getType()) {
                case TIME:
                    return DateTimeUtils.normalizeTimestamp(0, ((ValueTime) this).getNanos());
                case DATE:
                    return ValueTimestamp.fromDateValueAndNanos(((ValueDate) this).getDateValue(), 0);
                }
                break;
            }
            case BYTES: {
                switch(getType()) {
                case JAVA_OBJECT:
                case BLOB:
                    return ValueBytes.getNoCopy(getBytesNoCopy());
                case UUID:
                    return ValueBytes.getNoCopy(getBytes());
                case BYTE:
                    return ValueBytes.getNoCopy(new byte[]{getByte()});
                case SHORT: {
                    int x = getShort();
                    return ValueBytes.getNoCopy(new byte[]{
                            (byte) (x >> 8),
                            (byte) x
                    });
                }
                case INT: {
                    int x = getInt();
                    return ValueBytes.getNoCopy(new byte[]{
                            (byte) (x >> 24),
                            (byte) (x >> 16),
                            (byte) (x >> 8),
                            (byte) x
                    });
                }
                case LONG: {
                    long x = getLong();
                    return ValueBytes.getNoCopy(new byte[]{
                            (byte) (x >> 56),
                            (byte) (x >> 48),
                            (byte) (x >> 40),
                            (byte) (x >> 32),
                            (byte) (x >> 24),
                            (byte) (x >> 16),
                            (byte) (x >> 8),
                            (byte) x
                    });
                }
                }
                break;
            }
            case JAVA_OBJECT: {
                switch(getType()) {
                case BYTES:
                case BLOB:
                    return ValueJavaObject.getNoCopy(null, getBytesNoCopy());
                }
                break;
            }
            case BLOB: {
                switch(getType()) {
                case BYTES:

                    return LobStorage.createSmallLob(Value.BLOB, getBytesNoCopy());
                }
                break;
            }
            case UUID: {
                switch(getType()) {
                case BYTES:
                    return ValueUuid.get(getBytesNoCopy());
                }
            }
            }
            // conversion by parsing the string value
            String s = getString();
            switch (targetType) {
            case NULL:
                return ValueNull.INSTANCE;
            case BOOLEAN: {
                if (s.equalsIgnoreCase("true") ||
                        s.equalsIgnoreCase("t") ||
                        s.equalsIgnoreCase("yes") ||
                        s.equalsIgnoreCase("y")) {
                    return ValueBoolean.get(true);
                } else if (s.equalsIgnoreCase("false") ||
                        s.equalsIgnoreCase("f") ||
                        s.equalsIgnoreCase("no") ||
                        s.equalsIgnoreCase("n")) {
                    return ValueBoolean.get(false);
                } else {
                    // convert to a number, and if it is not 0 then it is true
                    return ValueBoolean.get(new BigDecimal(s).signum() != 0);
                }
            }
            case BYTE:
                return ValueByte.get(Byte.parseByte(s.trim()));
            case SHORT:
                return ValueShort.get(Short.parseShort(s.trim()));
            case INT:
                return ValueInt.get(Integer.parseInt(s.trim()));
            case LONG:
                return ValueLong.get(Long.parseLong(s.trim()));
            case DECIMAL:
                return ValueDecimal.get(new BigDecimal(s.trim()));
            case TIME:
                return ValueTime.parse(s.trim());
            case DATE:
                return ValueDate.parse(s.trim());
            case TIMESTAMP:
                return ValueTimestamp.parse(s.trim());
            case BYTES:
                return ValueBytes.getNoCopy(StringUtils.convertHexToBytes(s.trim()));
            case JAVA_OBJECT:
                return ValueJavaObject.getNoCopy(null, StringUtils.convertHexToBytes(s.trim()));
            case STRING:
                return ValueString.get(s);
            case STRING_IGNORECASE:
                return ValueStringIgnoreCase.get(s);
            case STRING_FIXED:
                return ValueStringFixed.get(s);
            case DOUBLE:
                return ValueDouble.get(Double.parseDouble(s.trim()));
            case FLOAT:
                return ValueFloat.get(Float.parseFloat(s.trim()));
            case CLOB:
                return LobStorage.createSmallLob(CLOB, StringUtils.utf8Encode(s));
            case BLOB:
                return LobStorage.createSmallLob(BLOB, StringUtils.convertHexToBytes(s.trim()));
            case ARRAY:
                return ValueArray.get(new Value[]{ValueString.get(s)});
            case RESULT_SET: {
                SimpleResultSet rs = new SimpleResultSet();
                rs.addColumn("X", Types.VARCHAR, s.length(), 0);
                rs.addRow(s);
                return ValueResultSet.get(rs);
            }
            case UUID:
                return ValueUuid.get(s);
            default:
                throw DbException.throwInternalError("type=" + targetType);
            }
        } catch (NumberFormatException e) {
            throw DbException.get(ErrorCode.DATA_CONVERSION_ERROR_1, e, getString());
        }
    }

    