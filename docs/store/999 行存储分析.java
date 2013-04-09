(H2与HBase)面向行or面向列的存储模型?

                目录　(H2与HBase)面向行or面向列的存储模型? http://t.cn/zOLdrmE 今天状态不佳，感冒、没精神，不过还是尽力完成了。早上还走了弯路，一心想把H2和HBase的存储引擎都讲全，发现太难，时间有限，H2研究透了精力充沛时再写。
	0. 示例

	1. H2怎么存储pet表的记录?
		1. 1 DATA_LEAF页格式
		1. 2 DATA_NODE页格式

	2. HBase怎么存储pet表的记录?
		2. 1 Data Block页格式
		2. 2 Data Block如何存下面这些记录?
		2. 3 leaf索引块的格式:
		2. 4 root索引块的格式:
		2. 5 IntermediateLevel索引块

0. 示例

假设有如下一张pet表(改编自MySQL参考手册 http://dev.mysql.com/doc/refman/5.1/zh/tutorial.html#database-use)

CREATE TABLE pet (
    id INT PRIMARY KEY,
	name VARCHAR(20),
	owner VARCHAR(20),
	species VARCHAR(20),
	sex CHAR(1)
);

有如下记录:
id    name    owner   species sex
===============================
1001  Fluffy  Harold  cat	 f
1002  Claws   Gwen    cat	 m
1003  Buffy   Harold  dog	 f


1. H2怎么存储pet表的记录?

H2是一个Java SQL Database Engine，使用面向行(row-oriented)的存储模型(如果觉得拗口，就叫基于行(row-based)的存储模型吧)。
表元数据与表的记录分开，可以通过与INFORMATION_SCHEMA相关的系统表来查找元数据，
还可以通过JDBC的java.sql.DatabaseMetaData提供的相关API来查找。


H2内部的存储引擎使用页(Page)来组织数据，页的大小默认是2K，可通过参数PAGE_SIZE调整，
有8种不同的页：
DATA_LEAF
DATA_NODE
DATA_OVERFLOW
BTREE_LEAF
BTREE_NODE
FREE_LIST
STREAM_TRUNK
STREAM_DATA

本文只是说明H2怎么按行的方式来组织数据，所以只重点讲DATA_LEAF、DATA_NODE这两种页。

1. 1 DATA_LEAF页格式

项             占用字节数
======================================================
type           1
checksum       2 (程序代码中一般是先用0值占位，等写完一页后对此页的数据计算校验和再回填)
parentPageId   4 (就是父DATA_NODE节点的ID)
tableId        可变int (该页所属的表的ID)
columnCount    可变int (该页所属的表有多少列)
entryCount     2 (该页有多少条记录)

entryCount个
{
	key        可变long (如果表存在主键，并且是BYTE、SHORT、INT、LONG类型，这个key就是主键的值，否则为每行生成一个唯一的数值)
	offset     2 (该行在此页中的相对位置)
}

entryCount个
{
	columnValues 每一行记录从对应的offset位置开始存放，不同类型的字段会使用1个Byte来标识自己的格式
}
======================================================

DATA_LEAF页并不存放表名、列名、列类型这些元数据
对于pet表中的三行记录放到一个DATA_LEAF页中会是这样(为了方便阅读未细化每一个字节，实际更复杂一些):

id    name    owner   species sex
===============================
1001  Fluffy  Harold  cat	 f
1002  Claws   Gwen    cat	 m
1003  Buffy   Harold  dog	 f


项             值
======================================================
type           17 (Page.TYPE_DATA_LEAF | Page.FLAG_LAST(当前只有一个页))
checksum       0x75CE (校验和计算比较复杂，也受当前pageId、pageTyep、pageSize影响)
parentPageId   0 (就是父DATA_NODE节点的ID，0说明是root节点)
tableId        14 (该页所属的表的ID)
columnCount    5 (该页所属的表有多少列)
entryCount     3 (该页有多少条记录)

{
	1001, 2024, 1002, 2003, 1003, 1980 (其中1001、1002、1003是key，2024、2003、1980是offset)
}
{
	( /* key:1001 */ 1001, 'Fluffy', 'Harold', 'cat', 'f')
	( /* key:1002 */ 1002, 'Claws', 'Gwen', 'cat', 'm')
	( /* key:1003 */ 1003, 'Buffy', 'Harold', 'dog', 'f')
}
======================================================


1. 2 DATA_NODE页格式

当某个DATA_LEAF页(page0)的大小超过pageSize时，会把它切掉一部分，得到一个新的DATA_LEAF页(page1)，
page0从切割点开始往右的keys和记录被转到page1中，切割点左边的继续留在page0，
同时生成一个新的DATA_NODE页作为page0、page1的父节点。

DATA_NODE页格式

项             占用字节数
======================================================
type           1
checksum       2 (程序代码中一般是先用0值占位，等写完一页后对些页的数据计算校验和再回填)
parentPageId   4 (就是父DATA_NODE节点的ID)
tableId        可变int (该页所属的表的ID)
rowCountStored 4
entryCount     2

entryCount个
{
	childPageId  4
	key          可变long
}

======================================================



2. HBase怎么存储pet表的记录?

HBase使用面向列(column-oriented)的存储模型，不需要定义表的结构(schema-free),
可以随时动态添加新的列，理论上对于列的个数没有限制，
如果列很多，可以把相关的一组列归属到一个列族中(Column Family)，充分发挥数据的聚合特性。

通过RowKey能把同一个或多个列族中的列关联起来，
HBase的RowKey和列族的组合有点类似于传统关系数据库中有外键引用关系的两个关联表，
但是只是型像神不想，HBase中两个不同列族是平等的，没有主从关系，只是通过RowKey把两者关联起来。

HBase的Data Block对应H2的DATA_LEAF，
HBase的Leaf Index Block对应H2的DATA_NODE，


2. 1 Data Block页格式

Data Block的格式有点复杂，如果不打算看代码可以不用关心的

每个block有一个33字节的头
===========================
前8个字节是表示block类型的MAGIC，对应org.apache.hadoop.hbase.io.hfile.BlockType的那些枚举常量名，
接着4个字节表示onDiskBytesWithHeader.length - HEADER_SIZE
接着4个字节表示uncompressedSizeWithoutHeader
接着8个字节表示prevOffset (前一个块的offset，比如，对于第一个块，那么它看到的prevOffset是-1，对于第二个块，是0)

接着1个字节表示checksumType code(默认是1: org.apache.hadoop.hbase.util.ChecksumType.CRC32)
接着4个字节表示bytesPerChecksum(默认16k，不能小于block头长度(头长度是33个字节))
最后4个字节表示onDiskDataSizeWithHeader



当不使用压缩时onDiskBytesWithHeader不包含checksums,
此时checksums放在onDiskChecksum中，
当使用压缩时checksums放在onDiskBytesWithHeader

checksums就是把onDiskBytesWithHeader中的所有字节以bytesPerChecksum个字节为单位求校验和，这个校验和用int(4字节)表示。
注意，在求校验和时，onDiskBytesWithHeader中还没有checksums


默认每个Data Block的大小是64K(头(33字节)不包含在内)(可以通过HColumnDescriptor.setBlocksize设置)，
64K只是一个阀值，实际的块大小要比它大(取决于最后存入的KeyValue的大小)，
比如上次存入的KeyValue导致块大小变成63K了，但是还没到64K，那么接着存入下一个KeyValue，如果此KeyValue有5K，
那么这个块的大小就变成了63+5=68K了。

每次append到writer中的是一个KeyValue，
包括4字节的key长度和4字节的value长度，
接着再写入key的字节流和value的字节码。

同时把每个block的第一个key保存到firstKeyInBlock字段中
=================================================================================


从这开始是重点:

Data Block的核心是一串KeyValue，
KeyValue的格式如下:

名称　　　          字节数                  说明
--------------------------------------------------------------------
keyLength　　         4                     表示Key所占的总字节数
valueLength           4                     表示Value所占的总字节数

rowKeyLength          2                     表示rowKey所占的字节数
rowKey                rowKeyLength          rowKey
columnFamilyLength    1                     表示列族名称所占的字节数
columnFamily          columnFamilyLength    列族名称
columnName            columnNameLength      列名
timestamp             8                     时间戳
type                  1                     Key类型，比如是新增(Put)，还是删除(Delete)

value                 valueLength           列值
--------------------------------------------------------------------
                      表2.1

关于KeyValue更完整更详细的内容请看这里: http://zhh2009.iteye.com/blog/1412315


2. 2 Data Block如何存下面这些记录?

id    name    owner   species sex
===============================
1001  Fluffy  Harold  cat	 f
1002  Claws   Gwen    cat	 m
1003  Buffy   Harold  dog	 f

把id作为rowkey，其他列不动，另外HBase至少需要一个列族，假设列族名是"mycf"，
这三行记录会产生4*3=12个KeyValue， 
存到Data Block会是这样，先按rowkey升序，rowkey相同的按列名升序排，
最后的布局类似这样(为了简化去掉了一些细节):

<rowKey 列族名称 列名 时间戳, 列值>
====================================================
<1001  mycf  name     timestamp, Fluffy>
<1001  mycf  owner    timestamp, Harold>
<1001  mycf  sex      timestamp, f>
<1001  mycf  species  timestamp, cat>

<1002  mycf  name     timestamp, Claws>
<1002  mycf  owner    timestamp, Gwen>
<1002  mycf  sex      timestamp, m>
<1002  mycf  species  timestamp, cat>

<1003  mycf  name     timestamp, Buffy>
<1003  mycf  owner    timestamp, Harold>
<1003  mycf  sex      timestamp, f>
<1003  mycf  species  timestamp, dog>
====================================================

与H2的DATA_LEAF相比，
====================================================
( /* key:1001 */ 1001, 'Fluffy', 'Harold', 'cat', 'f')
( /* key:1002 */ 1002, 'Claws', 'Gwen', 'cat', 'm')
( /* key:1003 */ 1003, 'Buffy', 'Harold', 'dog', 'f')
======================================================

HBase的格式存在大量的冗余(比如rowKey、列族名称、列名、时间戳)
之所以要这样做是为了水平扩展、文件合并切分更容易，
因为HBase把列名、列族名称这些元数据和列值合在一起了，所以在分区时只须简单按rowkey切分，
就能把所有数据都转移到另一台机器上，不需要像H2那样要考虑INFORMATION_SCHEMA中的元数据与表记录是否同步的问题。

HBase基于LSM-Tree来存放KeyValue，H2基于类B+Tree的结构，
LSM-Tree只允许一次性添加，不需要考虑结点的删除修改，
数据会先写到内存(MemStore)，然后内存满了就flush到硬盘变成一棵小LSM-Tree，
多棵LSM-Tree会定期合并成一棵更大的LSM-Tree，大到一定程度再切分自动扩散到其他机器。

B+Tree对于行、列的添加、删除需要对结点进行调整，数据更新会出现overflow。


另外，观察上面两组数据，H2的方案只是把元数据抽出来放到别处了，然后通过表id把DATA_LEAF和元数据关联，
HBase 0.94可以使用前缀压缩的办法，把重复的东西提取出来，
如果把上面的两组数据分别串成一行，其实差别不大，只是HBase多了很多冗余信息而已，
把冗余信息清除一部份我看不出row-oriented和column-oriented有什么本质区别，至少HBase与H2是有点相似的。

反而差异最大的是:
1) LSM-Tree与B+Tree
2) 是否是schema-free的


(以下内容不重要)

2. 3 leaf索引块的格式:

   数据块总个数N(int类型,4字节)
   N个"数据块在此索引块中的相对位置"(从0开始，根据每个Entry的大小累加，每个相对位置是int类型,4字节)
   N个Entry的总字节数(int类型,4字节)
   N个Entry {
     数据块在文件中的相对位置(long类型,8字节)
     数据块的总长度(包括头)  (int类型,4字节)
	 数据块第一个KeyValue中的Key字节数组
   }


2. 4 root索引块的格式:

   N个leaf索引块Entry {
     leaf索引块在文件中的相对位置(long类型,8字节)
     leaf索引块的总长度(包括头)  (int类型,4字节)
	 leaf索引块第一个Entry的Key字节数组
   }


2. 5 IntermediateLevel索引块

   与leaf索引块类似，只不过它的Entry在第一层IntermediateLevel是leaf索引块Entry，第二层以后是IntermediateLevel块的entry。

查找key的顺序
root索引块 ==> IntermediateLevel索引块 ==> leaf索引块 ==> 数据块


