/*
 * Copyright 2004-2019 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.pagestore;

import java.lang.reflect.Array;
import org.h2.engine.Session;
import org.h2.util.CacheObject;

/**
 * A page. Format:
 * <ul><li>0-3: parent page id (0 for root)
 * </li><li>4-4: page type
 * </li><li>page-type specific data
 * </li></ul>
 */
public abstract class Page extends CacheObject {

    /**
     * This is the last page of a chain.
     */
    public static final int FLAG_LAST = 16;

    /**
     * An empty page.
     */
    public static final int TYPE_EMPTY = 0;

    /**
     * A data leaf page (without overflow: + FLAG_LAST).
     */
    public static final int TYPE_DATA_LEAF = 1;

    /**
     * A data node page (never has overflow pages).
     */
    public static final int TYPE_DATA_NODE = 2;

    /**
     * A data overflow page (the last page: + FLAG_LAST).
     */
    public static final int TYPE_DATA_OVERFLOW = 3;

    /**
     * A b-tree leaf page (without overflow: + FLAG_LAST).
     */
    public static final int TYPE_BTREE_LEAF = 4;

    /**
     * A b-tree node page (never has overflow pages).
     */
    public static final int TYPE_BTREE_NODE = 5;

    /**
     * A page containing a list of free pages (the last page: + FLAG_LAST).
     */
    public static final int TYPE_FREE_LIST = 6;

    /**
     * A stream trunk page.
     */
    public static final int TYPE_STREAM_TRUNK = 7;

    /**
     * A stream data page.
     */
    public static final int TYPE_STREAM_DATA = 8;

    private static final int COPY_THRESHOLD = 4;

    /**
     * When this page was changed the last time.
     */
    protected long changeCount;

    /**
     * Copy the data to a new location, change the parent to point to the new
     * location, and free up the current page.
     *
     * @param session the session
     * @param newPos the new position
     */
    public abstract void moveTo(Session session, int newPos);

    /**
     * Write the page.
     */
    public abstract void write();

    /**
     * Insert a value in an array. A new array is created if required.
     *
     * @param old the old array
     * @param oldSize the old size
     * @param pos the position
     * @param x the value to insert
     * @return the (new) array
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] insert(T[] old, int oldSize, int pos, T x) {
    	//以下old不像其他两个insert的代码中那样判断是否为null，主要是因为此方法的old是SearchRow[] rows和Row[] rows;
    	//这两个字段在create方法中都赋值了，所以问题不大
        T[] result;
        if (old.length > oldSize) {
        	//old会多申请COPY_THRESHOLD=4个元素，oldSize是真实的有效元素个数，所以old.length > oldSize时，
        	//说明还有足够的空间
            result = old;
        } else {
            // according to a test, this is as fast as "new Row[..]"
            result = (T[]) Array.newInstance(old.getClass().getComponentType(), oldSize + 1 + COPY_THRESHOLD);
            //当pos等于0时就不复制了，是因为下面System.arraycopy(old, pos, result, pos + 1, oldSize - pos);
            //会为result预留一个位置(pos+1)说明当pos是0是，result数姐从下标1的地方复制，
            //紧接着result[pos] = x;就把x放位下标为o的位置处了。
            if (pos > 0) {
                System.arraycopy(old, 0, result, 0, pos); //复制pos位置前的元素
            }
        }
        if (oldSize - pos > 0) {  //把pos位置开始的元素向后移
            System.arraycopy(old, pos, result, pos + 1, oldSize - pos);
        }
        result[pos] = x;
        return result;
    }

    /**
     * Delete a value in an array. A new array is created if required.
     *
     * @param old the old array
     * @param oldSize the old size
     * @param pos the position
     * @return the (new) array
     */
    @SuppressWarnings("unchecked")
    public
    static <T> T[] remove(T[] old, int oldSize, int pos) {
        T[] result;
        if (old.length - oldSize < COPY_THRESHOLD) {
            result = old;
        } else {
            // according to a test, this is as fast as "new Row[..]"
            result = (T[]) Array.newInstance(
                    old.getClass().getComponentType(), oldSize - 1);
            System.arraycopy(old, 0, result, 0, Math.min(oldSize - 1, pos));
        }
        if (pos < oldSize) {
            System.arraycopy(old, pos + 1, result, pos, oldSize - pos - 1);
        }
        return result;
    }

    /**
     * Insert a value in an array. A new array is created if required.
     *
     * @param old the old array
     * @param oldSize the old size
     * @param pos the position
     * @param x the value to insert
     * @return the (new) array
     */
    protected static long[] insert(long[] old, int oldSize, int pos, long x) {
        long[] result;
        if (old != null && old.length > oldSize) {
            result = old;
        } else {
            result = new long[oldSize + 1 + COPY_THRESHOLD];
            //未像insert(int[], int, int, int)那样判断old是否为null
            if (pos > 0) {
                System.arraycopy(old, 0, result, 0, pos);
            }
        }
        if (old != null && oldSize - pos > 0) {
            System.arraycopy(old, pos, result, pos + 1, oldSize - pos);
        }
        result[pos] = x;
        return result;
    }

    /**
     * Delete a value in an array. A new array is created if required.
     *
     * @param old the old array
     * @param oldSize the old size
     * @param pos the position
     * @return the (new) array
     */
    protected static long[] remove(long[] old, int oldSize, int pos) {
        long[] result;
        if (old.length - oldSize < COPY_THRESHOLD) {
            result = old;
        } else {
            result = new long[oldSize - 1];
            System.arraycopy(old, 0, result, 0, pos);
        }
        System.arraycopy(old, pos + 1, result, pos, oldSize - pos - 1);
        return result;
    }

    /**
     * Insert a value in an array. A new array is created if required.
     *
     * @param old the old array
     * @param oldSize the old size
     * @param pos the position
     * @param x the value to insert
     * @return the (new) array
     */
    protected static int[] insert(int[] old, int oldSize, int pos, int x) {
        int[] result;
        if (old != null && old.length > oldSize) {
            result = old;
        } else {
            result = new int[oldSize + 1 + COPY_THRESHOLD];
            if (pos > 0 && old != null) {
                System.arraycopy(old, 0, result, 0, pos);
            }
        }
        if (old != null && oldSize - pos > 0) { //把pos位置开始的元素向后移
            System.arraycopy(old, pos, result, pos + 1, oldSize - pos);
        }
        result[pos] = x;
        return result;
    }

    /**
     * Delete a value in an array. A new array is created if required.
     *
     * @param old the old array
     * @param oldSize the old size
     * @param pos the position
     * @return the (new) array
     */
    protected static int[] remove(int[] old, int oldSize, int pos) {
        int[] result;
        if (old.length - oldSize < COPY_THRESHOLD) {
            result = old;
        } else {
            result = new int[oldSize - 1];
            System.arraycopy(old, 0, result, 0, Math.min(oldSize - 1, pos));
        }
        if (pos < oldSize) {
            System.arraycopy(old, pos + 1, result, pos, oldSize - pos - 1);
        }
        return result;
    }

    /**
     * Add a value to a subset of the array.
     *
     * @param array the array
     * @param from the index of the first element (including)
     * @param to the index of the last element (excluding)
     * @param x the value to add
     */
    protected static void add(int[] array, int from, int to, int x) {
        for (int i = from; i < to; i++) {
            array[i] += x;
        }
    }

    /**
     * If this page can be moved. Transaction log and free-list pages can not.
     *
     * @return true if moving is allowed
     */
    //以下三个子类覆盖了些方法，说明这三种页面不能移动
    //6. TYPE_FREE_LIST      org.h2.store.PageFreeList
    //7. TYPE_STREAM_TRUNK   org.h2.store.PageStreamTrunk
    //8. TYPE_STREAM_DATA    org.h2.store.PageStreamData
    public boolean canMove() {
        return true;
    }

}
