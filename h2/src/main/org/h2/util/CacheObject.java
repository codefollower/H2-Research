/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.util;

import org.h2.message.DbException;

/**
 * The base object for all cached objects.
 */
public abstract class CacheObject implements Comparable<CacheObject> {

    /**
     * The previous element in the LRU linked list. If the previous element is
     * the head, then this element is the most recently used object.
     */
    public CacheObject cachePrevious; //如果cachePrevious是head，那么说么当前CacheObject是最近最常使用的对象

    /**
     * The next element in the LRU linked list. If the next element is the head,
     * then this element is the least recently used object.
     */
    public CacheObject cacheNext; //如果cacheNext是head，那么说么当前CacheObject是最近最不常使用的对象

    /**
     * The next element in the hash chain.
     */
    public CacheObject cacheChained; //LRU链表中的下一个CacheObject

    private int pos; //当子类是org.h2.store.Page的子类时，实际上就是pageId，如PageBtreeLeaf的pageId
    private boolean changed;

    /**
     * Check if the object can be removed from the cache.
     * For example pinned objects can not be removed.
     *
     * @return true if it can be removed
     */
    public abstract boolean canRemove();

    /**
     * Get the estimated used memory.
     *
     * @return number of words (one word is 4 bytes)
     */
    public abstract int getMemory();

    public void setPos(int pos) { //没有子类覆盖
        if (cachePrevious != null || cacheNext != null || cacheChained != null) {
            DbException.throwInternalError("setPos too late");
        }
        this.pos = pos;
    }

    public int getPos() {  //没有子类覆盖
        return pos;
    }

    /**
     * Check if this cache object has been changed and thus needs to be written
     * back to the storage.
     *
     * @return if it has been changed
     */
    public boolean isChanged() { //没有子类覆盖
        return changed;
    }

    public void setChanged(boolean b) { //没有子类覆盖
        changed = b;
    }
    
    //比较pageId，相等为0，当前CacheObject小于other时返回-1，大于时返回1
    @Override
//<<<<<<< HEAD
//    public int compareTo(CacheObject other) { //没有子类覆盖
//        return MathUtils.compareInt(getPos(), other.getPos());
//=======
    public int compareTo(CacheObject other) {
        return Integer.compare(getPos(), other.getPos());
    }
    
    //子类org.h2.index.PageDataLeaf和org.h2.index.PageDataOverflow覆盖了此方法，
    //说明是用于溢出页的
    public boolean isStream() {
        return false;
    }

}
