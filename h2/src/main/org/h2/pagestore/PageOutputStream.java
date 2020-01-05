/*
 * Copyright 2004-2020 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (https://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.pagestore;

import java.util.BitSet;

import org.h2.message.DbException;
import org.h2.message.Trace;
import org.h2.util.IntArray;

/**
 * An output stream that writes into a page store.
 */
public class PageOutputStream {

    private PageStore store;
    private final Trace trace;
    private final BitSet exclude;
    private final boolean atEnd;
    private final int minPageId;

    private int trunkPageId;
    private int trunkNext;
    private IntArray reservedPages = new IntArray();
    private PageStreamTrunk trunk;
    private int trunkIndex;
    private PageStreamData data;
    private int reserved;
    private boolean needFlush;
    private boolean writing;
    private int pageCount;
    private int logKey;

    /**
     * Create a new page output stream.
     *
     * @param store the page store
     * @param trunkPage the first trunk page (already allocated)
     * @param exclude the pages not to use
     * @param logKey the log key of the first trunk page
     * @param atEnd whether only pages at the end of the file should be used
     */
    public PageOutputStream(PageStore store, int trunkPage, BitSet exclude,
            int logKey, boolean atEnd) {
        this.trace = store.getTrace();
        this.store = store;
        this.trunkPageId = trunkPage;
        this.exclude = exclude;
        // minus one, because we increment before creating a trunk page
        this.logKey = logKey - 1;
        this.atEnd = atEnd;
        minPageId = atEnd ? trunkPage : 0;
    }

    /**
     * Allocate the required pages so that no pages need to be allocated while
     * writing.
     *
     * @param minBuffer the number of bytes to allocate
     */
    void reserve(int minBuffer) {
        if (reserved < minBuffer) {
            int pageSize = store.getPageSize();
            //一个PageStreamData的容量: pageSize-11(因为一个PageStreamData的头就占了11字节)
            int capacityPerPage = PageStreamData.getCapacity(pageSize);
            //一个PageStreamTrunk能放多少个PageStreamData的pageId(用4字节表示)
            //(pageSize-17)/4 (一个PageStreamTrunk头就占了17字节)
            int pages = PageStreamTrunk.getPagesAddressed(pageSize);
            int pagesToAllocate = 0, totalCapacity = 0;
            do {
                // allocate x data pages plus one trunk page
                pagesToAllocate += pages + 1; //要分配多少个页，加1是包含PageStreamTrunk自身
                totalCapacity += pages * capacityPerPage;
            } while (totalCapacity < minBuffer);
            //从org.h2.store.PageStore.openNew()调用过来时，atEnd为false，
            //因为在org.h2.store.PageStore.openNew()中已为logFirstTrunkPage(就是trunkPageId)分配一个pageId了
            //firstPageToUse用在org.h2.store.PageFreeList.allocate(BitField, int)中，从来表示从哪个bit开始查找
            int firstPageToUse = atEnd ? trunkPageId : 0;
            
            //分配pagesToAllocate个pageId到reservedPages中，exclude包括排除的bit
            store.allocatePages(reservedPages, pagesToAllocate, exclude, firstPageToUse);
            reserved += totalCapacity; //总容量
            if (data == null) {
                initNextData();
            }
        }
    }

    private void initNextData() {
    	//找下一个PageStreamData的pageId
        int nextData = trunk == null ? -1 : trunk.getPageData(trunkIndex++);
        if (nextData == -1) {
            int parent = trunkPageId;
            if (trunkNext != 0) {
                trunkPageId = trunkNext;
            }
            int len = PageStreamTrunk.getPagesAddressed(store.getPageSize());
            int[] pageIds = new int[len];
            for (int i = 0; i < len; i++) {
                pageIds[i] = reservedPages.get(i);
            }
            //在上面的org.h2.store.PageOutputStream.reserve(int)中已多分配了一个pageId
            trunkNext = reservedPages.get(len); //下一个PageStreamTrunk的pageId
            logKey++;
            //第一个PageStreamTrunk的parent和trunkPageId一样
            trunk = PageStreamTrunk.create(store, parent, trunkPageId, trunkNext, logKey, pageIds);
            trunkIndex = 0; //重新置0，因为是新的PageStreamTrunk了
            pageCount++; //这里pageCount加1是对应新的PageStreamTrunk
            trunk.write(); //完整的写PageStreamTrunk了，写到store中
            reservedPages.removeRange(0, len + 1); //删除PageStreamTrunk对应的id和它的所有pageIds
            nextData = trunk.getPageData(trunkIndex++);
        }
        data = PageStreamData.create(store, nextData, trunk.getPos(), logKey);
        pageCount++; //这里pageCount加1是对应新的PageStreamData
        data.initWrite(); //只写头数据
    }

    /**
     * Write the data.
     *
     * @param b the buffer
     * @param off the offset
     * @param len the length
     */
    public void write(byte[] b, int off, int len) {
        if (len <= 0) {
            return;
        }
        if (writing) {
            DbException.throwInternalError("writing while still writing");
        }
        try {
            reserve(len);
            writing = true;
            while (len > 0) {
                int l = data.write(b, off, len);
                if (l < len) {
                    storePage();
                    initNextData();
                }
                reserved -= l;
                off += l;
                len -= l;
            }
            needFlush = true;
        } finally {
            writing = false;
        }
    }

    private void storePage() {
        if (trace.isDebugEnabled()) {
            trace.debug("pageOut.storePage " + data);
        }
        data.write();
    }

    /**
     * Write all data.
     */
    public void flush() {
        if (needFlush) {
            storePage();
            needFlush = false;
        }
    }

    /**
     * Close the stream.
     */
    public void close() {
        store = null;
    }

    int getCurrentDataPageId() {
        return data.getPos();
    }

    /**
     * Fill the data page with zeros and write it.
     * This is required for a checkpoint.
     */
    void fillPage() {
        if (trace.isDebugEnabled()) {
            trace.debug("pageOut.storePage fill " + data.getPos());
        }
        reserve(data.getRemaining() + 1);
        reserved -= data.getRemaining();
        data.write();
        initNextData();
    }

    long getSize() {
        return pageCount * store.getPageSize();
    }

    /**
     * Remove a trunk page from the stream.
     *
     * @param t the trunk page
     */
    void free(PageStreamTrunk t) {
        pageCount -= t.free(0);
    }

    /**
     * Free up all reserved pages.
     */
    void freeReserved() {
        if (reservedPages.size() > 0) {
            int[] array = new int[reservedPages.size()];
            reservedPages.toArray(array);
            reservedPages = new IntArray();
            reserved = 0;
            for (int p : array) {
                store.free(p, false);
            }
        }
    }

    /**
     * Get the smallest possible page id used. This is the trunk page if only
     * appending at the end of the file, or 0.
     *
     * @return the smallest possible page.
     */
    int getMinPageId() {
        return minPageId;
    }

}
