/*
 * Copyright 2004-2018 H2 Group. Multiple-Licensed under the MPL 2.0,
 * and the EPL 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.index;

import java.util.Arrays;
import org.h2.api.ErrorCode;
import org.h2.engine.Constants;
import org.h2.engine.Session;
import org.h2.engine.SysProperties;
import org.h2.message.DbException;
import org.h2.result.SearchRow;
import org.h2.store.Data;
import org.h2.store.Page;
import org.h2.store.PageStore;

/**
 * A b-tree leaf page that contains index data. Format:
 * <ul>
 * <li>page type: byte</li>
 * <li>checksum: short</li>
 * <li>parent page id (0 for root): int</li>
 * <li>index id: varInt</li>
 * <li>entry count: short</li>
 * <li>list of offsets: short</li>
 * <li>data (key: varLong, value,...)</li>
 * </ul>
 */
public class PageBtreeLeaf extends PageBtree {

    private static final int OFFSET_LENGTH = 2;

    private final boolean optimizeUpdate;
    private boolean writtenData;

    private PageBtreeLeaf(PageBtreeIndex index, int pageId, Data data) {
        super(index, pageId, data);
        this.optimizeUpdate = index.getDatabase().getSettings().optimizeUpdate; //默认为true
    }

    /**
     * Read a b-tree leaf page.
     *
     * @param index the index
     * @param data the data
     * @param pageId the page id
     * @return the page
     */
    public static Page read(PageBtreeIndex index, Data data, int pageId) {
        PageBtreeLeaf p = new PageBtreeLeaf(index, pageId, data);
        p.read();
        return p;
    }

    /**
     * Create a new page.
     *
     * @param index the index
     * @param pageId the page id
     * @param parentPageId the parent
     * @return the page
     */
    static PageBtreeLeaf create(PageBtreeIndex index, int pageId,
            int parentPageId) {
        PageBtreeLeaf p = new PageBtreeLeaf(index, pageId, index.getPageStore()
                .createData());
        index.getPageStore().logUndo(p, null);
        p.rows = SearchRow.EMPTY_ARRAY;
        p.parentPageId = parentPageId;
        p.writeHead();
        p.start = p.data.length(); //PageBtreeLeaf页头部结束的位置，即写完entryCount项后的位置
        return p;
    }

    private void read() {
        data.reset();
        int type = data.readByte();
        data.readShortInt();
        this.parentPageId = data.readInt();
        onlyPosition = (type & Page.FLAG_LAST) == 0;
        int indexId = data.readVarInt();
        if (indexId != index.getId()) {
            throw DbException.get(ErrorCode.FILE_CORRUPTED_1,
                    "page:" + getPos() + " expected index:" + index.getId() +
                    "got:" + indexId);
        }
        entryCount = data.readShortInt();
        offsets = new int[entryCount];
        rows = new SearchRow[entryCount];
        for (int i = 0; i < entryCount; i++) {
            offsets[i] = data.readShortInt();
        }
        start = data.length();
        written = true;
        writtenData = true;
    }

    @Override
    int addRowTry(SearchRow row) {
        int x = addRow(row, true);
        memoryChange();
        return x;
    }

    //假设一个块128字节，写索引记录的顺序是从块尾开始，offset从块尾开始
    private int addRow(SearchRow row, boolean tryOnly) {
        int rowLength = index.getRowSize(data, row, onlyPosition);
        int pageSize = index.getPageStore().getPageSize();
        int last = entryCount == 0 ? pageSize : offsets[entryCount - 1];
        //确保page剩余空间能够保存offset
        if (last - rowLength < start + OFFSET_LENGTH) {
            if (tryOnly && entryCount > 1) {
                int x = find(row, false, true, true);
                if (entryCount < 5) {
                    // required, otherwise the index doesn't work correctly
                    return entryCount / 2;
                }
                // split near the insertion point to better fill pages
                // split in half would be:
                // return entryCount / 2;
                int third = entryCount / 3;
                return x < third ? third : x >= 2 * third ? 2 * third : x;
            }
            //当索引字段值的大小超过pageSize时只存位置不存字段值
            readAllRows();
            writtenData = false;
            onlyPosition = true;
            // change the offsets (now storing only positions)
            int o = pageSize;
            for (int i = 0; i < entryCount; i++) {
                o -= index.getRowSize(data, getRow(i), true);
                offsets[i] = o;
            }
            last = entryCount == 0 ? pageSize : offsets[entryCount - 1];
            rowLength = index.getRowSize(data, row, true);
            if (SysProperties.CHECK && last - rowLength < start + OFFSET_LENGTH) {
                throw DbException.throwInternalError();
            }
        }
        index.getPageStore().logUndo(this, data);
        if (!optimizeUpdate) {
            readAllRows();
        }
        changeCount = index.getPageStore().getChangeCount();
        written = false;
        int x;
        if (entryCount == 0) {
            x = 0;
        } else {
            x = find(row, false, true, true);
        }
        start += OFFSET_LENGTH;
        //x所在元素的值，是它前面的元素值-rowLength
        int offset = (x == 0 ? pageSize : offsets[x - 1]) - rowLength;
        //索引或内存中已有记录的情况(第一次运行完JDBC客户端程序后不删表和索引，第2次运行JDBC客户端程序添加记录就能测试)此种情况
        if (optimizeUpdate && writtenData) {
            if (entryCount > 0) {
                byte[] d = data.getBytes();
//<<<<<<< HEAD
//                int dataStart = offsets[entryCount - 1]; //offsets总是降序的，所以offsets[entryCount - 1]是data的最小下标
//                int dataEnd = offset;
//                //将data数组中dataStart下标开始的dataEnd - dataStart + rowLength个元素往左移到dataStart - rowLength开始处
//                //(即: 把数据移到dataStart位置的左边，因为dataStart前的位置还没写数据，所以整体往前挪rowLength, 右边挪出的空位置
//                //用来放新的row，这个row的位置就是offset)
//                System.arraycopy(d, dataStart, d, dataStart - rowLength, dataEnd - dataStart + rowLength);
//=======
                int dataStart = offsets[entryCount - 1];
                System.arraycopy(d, dataStart, d, dataStart - rowLength,
                        offset - dataStart + rowLength);
            }
            index.writeRow(data, offset, row, onlyPosition);
        }
        //offsets这个数组内的元素是由大到小的，所以在rows中的记录最先的反而写在文件的后面
        offsets = insert(offsets, entryCount, x, offset);
        //为什么要entryCount + 1呢，因前经过前面一行代码后，offsets的实际长度已经增加1了，但是entryCount在后面才加1
        add(offsets, x + 1, entryCount + 1, -rowLength); //x是中间位置时，从x之后的offset要减去rowLength
        rows = insert(rows, entryCount, x, row);
        entryCount++;
        index.getPageStore().update(this);
        return -1;
    }

    private void removeRow(int at) {
        if (!optimizeUpdate) {
            readAllRows();
        }
        index.getPageStore().logUndo(this, data);
        entryCount--;
        written = false;
        changeCount = index.getPageStore().getChangeCount();
        if (entryCount <= 0) {
            DbException.throwInternalError(Integer.toString(entryCount));
        }
        int startNext = at > 0 ? offsets[at - 1] : index.getPageStore().getPageSize();
        int rowLength = startNext - offsets[at];
        start -= OFFSET_LENGTH;

        if (optimizeUpdate) {
            if (writtenData) {
                byte[] d = data.getBytes();
                int dataStart = offsets[entryCount];
                System.arraycopy(d, dataStart, d,
                        dataStart + rowLength, offsets[at] - dataStart);
                Arrays.fill(d, dataStart, dataStart + rowLength, (byte) 0);
            }
        }

        offsets = remove(offsets, entryCount + 1, at);
        add(offsets, at, entryCount, rowLength);
        rows = remove(rows, entryCount + 1, at);
    }

    int getEntryCount() {
        return entryCount;
    }

    @Override
    PageBtree split(int splitPoint) {
        int newPageId = index.getPageStore().allocatePage();
        PageBtreeLeaf p2 = PageBtreeLeaf.create(index, newPageId, parentPageId);
        while (splitPoint < entryCount) {
            p2.addRow(getRow(splitPoint), false);
            removeRow(splitPoint);
        }
        memoryChange();
        p2.memoryChange();
        return p2;
    }

    @Override
    PageBtreeLeaf getFirstLeaf() {
        return this;
    }

    @Override
    PageBtreeLeaf getLastLeaf() {
        return this;
    }

    @Override
    SearchRow remove(SearchRow row) {
        int at = find(row, false, false, true);
        SearchRow delete = getRow(at);
        if (index.compareRows(row, delete) != 0 || delete.getKey() != row.getKey()) {
            throw DbException.get(ErrorCode.ROW_NOT_FOUND_WHEN_DELETING_1,
                    index.getSQL() + ": " + row);
        }
        index.getPageStore().logUndo(this, data);
        if (entryCount == 1) {
            // the page is now empty
            return row;
        }
        removeRow(at);
        memoryChange();
        index.getPageStore().update(this);
        if (at == entryCount) {
            // the last row changed
            return getRow(at - 1);
        }
        // the last row didn't change
        return null;
    }

    @Override
    void freeRecursive() {
        index.getPageStore().logUndo(this, data);
        index.getPageStore().free(getPos());
    }

    @Override
    int getRowCount() {
        return entryCount;
    }

    @Override
    void setRowCountStored(int rowCount) {
        // ignore
    }

    @Override
    public void write() {
        writeData();
        index.getPageStore().writePage(getPos(), data);
    }

    private void writeHead() {
        data.reset();
        data.writeByte((byte) (Page.TYPE_BTREE_LEAF |
                (onlyPosition ? 0 : Page.FLAG_LAST)));
        data.writeShortInt(0);
        data.writeInt(parentPageId);
        data.writeVarInt(index.getId());
        data.writeShortInt(entryCount); //最开始为0
    }

    private void writeData() {
        if (written) {
            return;
        }
        if (!optimizeUpdate) {
            readAllRows();
        }
        writeHead();
        for (int i = 0; i < entryCount; i++) {
            data.writeShortInt(offsets[i]);
        }
        if (!writtenData || !optimizeUpdate) {
            for (int i = 0; i < entryCount; i++) {
                index.writeRow(data, offsets[i], rows[i], onlyPosition);
            }
            writtenData = true;
        }
        written = true;
        memoryChange();
    }

    @Override
    void find(PageBtreeCursor cursor, SearchRow first, boolean bigger) {
        int i = find(first, bigger, false, false); //这里返回的i怎么会>entryCount
        if (i > entryCount) { //什么情况下才会使得i > entryCount? 
            if (parentPageId == PageBtree.ROOT) {
                return;
            }
            PageBtreeNode next = (PageBtreeNode) index.getPage(parentPageId);
            next.find(cursor, first, bigger);
            return;
        }
        cursor.setCurrent(this, i);
    }

    @Override
    void last(PageBtreeCursor cursor) {
        cursor.setCurrent(this, entryCount - 1);
    }

    @Override
    void remapChildren() {
        // nothing to do
    }

    /**
     * Set the cursor to the first row of the next page.
     *
     * @param cursor the cursor
     */
    void nextPage(PageBtreeCursor cursor) {
        if (parentPageId == PageBtree.ROOT) {
            cursor.setCurrent(null, 0);
            return;
        }
        PageBtreeNode next = (PageBtreeNode) index.getPage(parentPageId);
        next.nextPage(cursor, getPos());
    }

    /**
     * Set the cursor to the last row of the previous page.
     *
     * @param cursor the cursor
     */
    void previousPage(PageBtreeCursor cursor) {
        if (parentPageId == PageBtree.ROOT) {
            cursor.setCurrent(null, 0);
            return;
        }
        PageBtreeNode next = (PageBtreeNode) index.getPage(parentPageId);
        next.previousPage(cursor, getPos());
    }

    @Override
    public String toString() {
    	return tree();
        //return "page[" + getPos() + "] b-tree leaf table:" + index.getId() + " entries:" + entryCount;
    }

    @Override
    public void moveTo(Session session, int newPos) {
        PageStore store = index.getPageStore();
        readAllRows();
        PageBtreeLeaf p2 = PageBtreeLeaf.create(index, newPos, parentPageId);
        store.logUndo(this, data);
        store.logUndo(p2, null);
        p2.rows = rows;
        p2.entryCount = entryCount;
        p2.offsets = offsets;
        p2.onlyPosition = onlyPosition;
        p2.parentPageId = parentPageId;
        p2.start = start;
        store.update(p2);
        if (parentPageId == ROOT) { //最顶层的leaf，重新把rootPageId指向newPos
            index.setRootPageId(session, newPos);
        } else {
        	//此leaf有父PageBtreeNode
            PageBtreeNode p = (PageBtreeNode) store.getPage(parentPageId);
            p.moveChild(getPos(), newPos);
        }
        store.free(getPos());
    }

    @Override
    protected void memoryChange() {
        if (!PageBtreeIndex.isMemoryChangeRequired()) {
            return;
        }
        int memory = Constants.MEMORY_PAGE_BTREE + index.getPageStore().getPageSize();
        if (rows != null) {
            memory += getEntryCount() * (4 + Constants.MEMORY_POINTER);
            for (int i = 0; i < entryCount; i++) {
                SearchRow r = rows[i];
                if (r != null) {
                    memory += r.getMemory();
                }
            }
        }
        index.memoryChange(memory >> 2);
    }

	// 我加上的
	@Override
	public String tree(String p) {
		StringBuilder s = new StringBuilder(200);

		s.append(p + "PageBtreeLeaf {\r\n");
		s.append(p + "\t" + "indexId = " + index.getId() + "\r\n");
		s.append(p + "\t" + "parentPageId = " + parentPageId + "\r\n");
		s.append(p + "\t" + "pageId = " + getPos() + "\r\n");
		s.append(p + "\t" + "start = " + start + "\r\n");
		s.append(p + "\t" + "offsets = " + PageBtreeNode.stringArray2(offsets, entryCount) + "\r\n");
		s.append(p + "\t" + "entryCount = " + entryCount + "\r\n");
		s.append(p + "\t" + "rows = " + PageBtreeNode.stringArray(rows, p, entryCount));

		s.append(p + "}\r\n");
		return s.toString();
	}
}
