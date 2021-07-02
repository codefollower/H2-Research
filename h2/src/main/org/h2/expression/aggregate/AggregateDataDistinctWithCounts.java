/*
 * Copyright 2004-2021 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.expression.aggregate;

import java.util.TreeMap;
import org.h2.engine.SessionLocal;
import org.h2.value.Value;
import org.h2.value.ValueNull;

/**
 * Data stored while calculating an aggregate that needs distinct values with
 * their counts.
 */
final class AggregateDataDistinctWithCounts extends AggregateData {

    private final boolean ignoreNulls;

    private final int maxDistinctCount;

    private TreeMap<Value, LongDataCounter> values;

    /**
     * Creates new instance of data for aggregate that needs distinct values
     * with their counts.
     *
     * @param ignoreNulls
     *            whether NULL values should be ignored
     * @param maxDistinctCount
     *            maximum count of distinct values to collect
     */
    AggregateDataDistinctWithCounts(boolean ignoreNulls, int maxDistinctCount) {
        this.ignoreNulls = ignoreNulls;
        this.maxDistinctCount = maxDistinctCount;
    }

    // 是基于某个表达式(多数是单个字段)算不重复的记录数所占总记录数的百分比
    // org.h2.engine.Constants.SELECTIVITY_DISTINCT_COUNT默认是1万，这个值不能改，
    // 对统计值影响很大。通常这个值越大，统计越精确，但是会使用更多内存。
    // SELECTIVITY越大，说明重复的记录越少，在选择索引时更有利。
    @Override
    void add(SessionLocal session, Value v) {
        if (ignoreNulls && v == ValueNull.INSTANCE) {
            return;
        }
        if (values == null) {
            values = new TreeMap<>(session.getDatabase().getCompareMode());
        }
        LongDataCounter a = values.get(v);
        if (a == null) {
            if (values.size() >= maxDistinctCount) {
                return;
            }
            a = new LongDataCounter();
            values.put(v, a);
        }
        a.count++;
    }

    @Override
    Value getValue(SessionLocal session) {
        return null;
    }

    /**
     * Returns map with values and their counts.
     *
     * @return map with values and their counts
     */
    TreeMap<Value, LongDataCounter> getValues() {
        return values;
    }

}
