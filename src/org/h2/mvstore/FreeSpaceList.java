/*
 * Copyright 2004-2013 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.mvstore;

import java.util.ArrayList;
import java.util.List;

/**
 * A list that maintains ranges of free space (in pages) in a file.
 */
public class FreeSpaceList {

    /**
     * The first 2 pages are occupied by the file header.
     */
    private static final int FIRST_FREE_PAGE = 2; //前两页固定给FileHeader了，相当于0号和1号page不能分配，只能从2号page开始

    /**
     * The maximum number of pages. Smaller than than MAX_VALUE to avoid
     * overflow errors during arithmetic operations.
     */
    private static final int MAX_PAGE_COUNT = Integer.MAX_VALUE / 2;

    private List<PageRange> freeSpaceList = new ArrayList<PageRange>();

    public FreeSpaceList() {
        clear();
    }

    /**
     * Reset the list.
     */
    public synchronized void clear() {
        freeSpaceList.clear();
        freeSpaceList.add(new PageRange(FIRST_FREE_PAGE, MAX_PAGE_COUNT));
    }

    /**
     * Allocate a number of pages.
     *
     * @param length the number of bytes to allocate
     * @return the position in pages
     */
    public synchronized int allocatePages(long length) {
    	//需要多少个page, 为什么要加1，因为每个chunk后都跟一个FileHeader，FileHeader占用一个块
        int required = (int) (length / MVStore.BLOCK_SIZE) + 1;
        for (PageRange pr : freeSpaceList) {
            if (pr.length >= required) {
                return pr.start;
            }
        }
        throw DataUtils.newIllegalStateException(
                "Could not find a free page to allocate");
    }

    /**
     * Mark a chunk as used.
     *
     * @param c the chunk
     */
    public synchronized void markUsed(Chunk c) {
        int chunkStart = (int) (c.start / MVStore.BLOCK_SIZE);
        //实际上就是: (c.length / MVStore.BLOCK_SIZE) + 2，
        //那为什么在markFree中又是加1? 之所以加1，是为了方便判断
        //int required = (int) ((c.start + c.length) / MVStore.BLOCK_SIZE) + 2 - chunkStart;
        int required = (c.length / MVStore.BLOCK_SIZE) + 1;
        PageRange found = null;
        int i = 0;
        for (PageRange pr : freeSpaceList) {
        	//寻找chunkStart在[start, start+length)这个半闭半开区间内的PageRange
        	//这里没有考虑chunkStart + required > found.start + found.length的情况
            if (chunkStart >= pr.start && chunkStart < (pr.start + pr.length)) {
                found = pr;
                break;
            }
            i++;
        }
        if (found == null) {
            throw DataUtils.newIllegalStateException(
                    "Cannot find spot to mark chunk as used in free list: {0}", c);
        }
        if (chunkStart + required > found.start + found.length) {
            throw DataUtils.newIllegalStateException(
                    "Chunk runs over edge of free space: {0}", c);
        }
        //第1种情况: 找到的PageRange的start跟chunkStart一样
        //例如found=[10, 11, 12, 13, 14, 15]
        //而chunk只要found开头的4个块，相当于chunk是[10, 11, 12, 13]，而found变成[14, 15]
        if (found.start == chunkStart) {
            // if the used-chunk is at the beginning of a free-space-range
            found.start += required;
            found.length -= required;
            //chunk要化了所有块，chunk是[10, 11, 12, 13, 14, 15]，而found没有了，可直接删除
            if (found.length == 0) { //第1.1种情况: 找到的PageRange的start跟chunkStart一样，length跟required也一样
                // if the free-space-range is now empty, remove it
                freeSpaceList.remove(i);
            }
        //第2种情况: end位置一样
        //例如found=[10, 11, 12, 13, 14, 15]
        //而chunk只要[13, 14, 15]，而found变成[10, 11, 12]
        } else if (found.start + found.length == chunkStart + required) {
            // if the used-chunk is at the end of a free-space-range
            found.length -= required;
            //这种情况不会出现
            //因为如果假设(found.start + found.length == chunkStart + required)为true，
            //如果found.length==required那么found.start == chunkStart，所以不可能进入当前的else if
//            if (found.length == 0) {
//                // if the free-space-range is now empty, remove it
//                freeSpaceList.remove(i);
//            }
        } else {
        	//第3种情况: 找中间位置
        	//例如found=[10, 11, 12, 13, 14, 15]
            //而chunk只要[12, 13]，而found变成[10, 11]，同时新增一个[14, 15]
        	
        	//例如found是[start=5, length=100]，此时chunk的start是10，而required是5，
        	//那么found就变成[start=5, length=5]相当于占用[5, 6, 7, 8, 9]这5个位置的块，
        	//同时多出来一个新的PageRange[start=15, length=90(100-5-5)]
            // it's in the middle, so split the existing entry
            int length1 = chunkStart - found.start;
            int start2 = chunkStart + required;
            int length2 = found.start + found.length - chunkStart - required;

            found.length = length1; //found的start不变，但是length要变
            PageRange newRange = new PageRange(start2, length2);
            freeSpaceList.add(i + 1, newRange);
        }
    }

    /**
     * Mark the chunk as free.
     *
     * @param c the chunk
     */
    public synchronized void markFree(Chunk c) {
        int chunkStart = (int) (c.start / MVStore.BLOCK_SIZE);
        int required = (c.length / MVStore.BLOCK_SIZE) + 1; //markUsed中是加2，这里是加1，所以在下面的if中要多加1
        PageRange found = null;
        int i = 0;
        for (PageRange pr : freeSpaceList) {
            if (pr.start > chunkStart) {
                found = pr;
                break;
			} else if (pr.start <= chunkStart && pr.start + pr.length >= chunkStart + required) {
				return;
			}
            i++;
        }
        if (found == null) {
            throw DataUtils.newIllegalStateException(
                    "Cannot find spot to mark chunk as unused in free list: {0}", c);
        }
        //第1种情况: 假设有A、C，这里相当于Chunk和C紧邻，所以Chunk和C要合并
        //例如:
        //A=[10, 11]
        //C=[14, 15] = found
        //Chunk = [12, 13] (chunkStart=12, required=1)
        //那么chunkStart + required + 1 == found.start = 12 + 1 + 1 = 14
        //if (chunkStart + required + 1 == found.start) {
        if (chunkStart + required == found.start) {
            // if the used-chunk is adjacent to the beginning of a
            // free-space-range
            found.start = chunkStart;
            found.length += required; //这行是不对的，会少一个块
            //found.length += required + 1;
            // compact: merge the previous entry into this one if
            // they are now adjacent
            if (i > 0) {
            	//假设有A、C，这里相当于Chunk和C紧邻，所以Chunk和C要合并得到C1，同时A又和Chunk紧邻，
            	//所以A和C1要合并，并且删除原来C所在的位置
                PageRange previous = freeSpaceList.get(i - 1);
                //if (previous.start + previous.length + 1 == found.start) { //不需要加1
                if (previous.start + previous.length == found.start) {
                    previous.length += found.length;
                    freeSpaceList.remove(i);
                }
            }
            return;
        }
        //第2种情况: 假设有A、C，这里相当于found=C，而Chunk和A紧邻，所以Chunk和A要合并
        if (i > 0) {
            // if the used-chunk is adjacent to the end of a free-space-range
            PageRange previous = freeSpaceList.get(i - 1);
            //if (previous.start + previous.length + 1 == chunkStart) {
            if (previous.start + previous.length == chunkStart) {
                previous.length += required;
                // compact: merge the next entry into this one if
                // they are now adjacent
                //假设有A、C，这里相当于found=C，而Chunk和A紧邻，所以Chunk和A要合并得到A1，同时A1又和C紧邻，
            	//所以A1和C要合并，并且删除原来C所在的位置
                //TODO 会出现这种情况吗？如果真满足这个if条件，
                //那么在前面那个if (chunkStart + required + 1 == found.start)也会满足？
//                if (previous.start + previous.length + 1 == found.start) {
//                    previous.length += found.length;
//                    freeSpaceList.remove(i);
//                }
                return;
            }
        }

        //第3种情况: 假设有A、C，Chunk和A、C都不相邻，所以单独生成一个PageRange
        // it is between 2 entries, so add a new one
        PageRange newRange = new PageRange(chunkStart, required);
        freeSpaceList.add(i, newRange);
    }

    public String toString() {
        StringBuilder buff = new StringBuilder();
        boolean first = true;
        for (PageRange r : freeSpaceList) {
            if (first) {
                first = false;
            } else {
                buff.append(", ");
            }
            //包含两头，假设start=2, length=3, 那么就是[2, 2+3-1] = [2, 4]
            //[2, 4]包含2、3、4，刚好是length=3，也就是3个块
            buff.append(r.start + "-" + (r.start + r.length - 1));
        }
        return buff.toString();
    }

    /**
     * A range of free pages.
     */
    private static final class PageRange {

        /**
         * The starting point, in pages.
         */
        public int start; //按page算，从第几个page开始

        /**
         * The length, in pages.
         */
        public int length; //按page算，有多少个page

        public PageRange(int start, int length) {
            this.start = start;
            this.length = length;
        }

        @Override
        public String toString() {
            return "start:" + start + " length:" + length;
        }
    }

}
