/*
 * Copyright 2004-2021 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mode;

/**
 * This class contains information about a built-in function.
 */
public final class FunctionInfo { //8个字段

    /**
     * The name of the function.
     */
    public final String name;

    /**
     * The function type.
     */
    public final int type;

    /**
     * The number of parameters.
     */
    final int parameterCount;

    /**
     * The data type of the return value.
     */
    public final int returnDataType;

    /**
     * If the result of the function is NULL if any of the parameters is NULL.
     */
    //注释多了个if，这个变量表示: 只要传给函数的参数有一个是null，那么函数返回值就是null
    public final boolean nullIfParameterIsNull;

    /**
     * If this function always returns the same value for the same parameters.
     */
    //对于相同的参数，每次调用可能返回不同值，比如:CURRENT_TIME、RAND之类的函数
    public final boolean deterministic;

    /**
     * Creates new instance of built-in function information.
     *
     * @param name
     *            the name of the function
     * @param type
     *            the function type
     * @param parameterCount
     *            the number of parameters
     * @param returnDataType
     *            the data type of the return value
     * @param nullIfParameterIsNull
     *            if the result of the function is NULL if any of the parameters
     *            is NULL
     * @param deterministic
     *            if this function always returns the same value for the same
     *            parameters
     */
    public FunctionInfo(String name, int type, int parameterCount, int returnDataType, boolean nullIfParameterIsNull,
            boolean deterministic) {
        this.name = name;
        this.type = type;
        this.parameterCount = parameterCount;
        this.returnDataType = returnDataType;
        this.nullIfParameterIsNull = nullIfParameterIsNull;
        this.deterministic = deterministic;
    }

    /**
     * Creates a copy of built-in function information with a different name. A
     * copy will require parentheses.
     *
     * @param source
     *            the source information
     * @param name
     *            the new name
     */
    public FunctionInfo(FunctionInfo source, String name) {
        this.name = name;
        type = source.type;
        returnDataType = source.returnDataType;
        parameterCount = source.parameterCount;
        nullIfParameterIsNull = source.nullIfParameterIsNull;
        deterministic = source.deterministic;
    }

//	public String toString() { //我加上的
////		return name + " type=" + type + " dataType="
////				+ DataType.getDataType(dataType).name + " parameterCount="
////				+ parameterCount + " nullIfParameterIsNull="
////				+ nullIfParameterIsNull + " deterministic=" + deterministic
////				+ " bufferResultSetToLocalTemp=" + bufferResultSetToLocalTemp;
//		
//		return pad(name, 20) + pad(type, 10) + pad(DataType.getDataType(returnDataType).name,15) + pad(parameterCount, 6)
//				+ pad(nullIfParameterIsNull, 8) + pad(deterministic, 8);
//	}

	public int hashCode() { //我加上的
		return type;
	}

	public static String pad(Object string, int n) {
		return org.h2.util.StringUtils.pad(string.toString(), n, " ", true);
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof FunctionInfo))
			return false;
		FunctionInfo fi = (FunctionInfo) obj;
		return !name.equals(fi.name)
				&& 
				type == fi.type //
				&& returnDataType == fi.returnDataType
				&& parameterCount == fi.parameterCount //
				&& nullIfParameterIsNull == fi.nullIfParameterIsNull
				&& deterministic == fi.deterministic;
	}
}
