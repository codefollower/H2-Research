///*
// * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// *
// */
//
///*
// *
// *
// *
// *
// *
// * Written by Doug Lea with assistance from members of JCP JSR-166
// * Expert Group and released to the public domain, as explained at
// * http://creativecommons.org/publicdomain/zero/1.0/
// */
//
//package my.test.mvstore;
//
//import java.lang.reflect.Field;
//import java.util.AbstractCollection;
//import java.util.AbstractMap;
//import java.util.AbstractSet;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.Comparator;
//import java.util.ConcurrentModificationException;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//import java.util.NavigableSet;
//import java.util.NoSuchElementException;
//import java.util.Random;
//import java.util.Set;
//import java.util.SortedMap;
//import java.util.concurrent.ConcurrentNavigableMap;
//
///**
// * A scalable concurrent {@link ConcurrentNavigableMap} implementation.
// * The map is sorted according to the {@linkplain Comparable natural
// * ordering} of its keys, or by a {@link Comparator} provided at map
// * creation time, depending on which constructor is used.
// *
// * <p>This class implements a concurrent variant of <a
// * href="http://en.wikipedia.org/wiki/Skip_list" target="_top">SkipLists</a>
// * providing expected average <i>log(n)</i> time cost for the
// * <tt>containsKey</tt>, <tt>get</tt>, <tt>put</tt> and
// * <tt>remove</tt> operations and their variants.  Insertion, removal,
// * update, and access operations safely execute concurrently by
// * multiple threads.  Iterators are <i>weakly consistent</i>, returning
// * elements reflecting the state of the map at some point at or since
// * the creation of the iterator.  They do <em>not</em> throw {@link
// * ConcurrentModificationException}, and may proceed concurrently with
// * other operations. Ascending key ordered views and their iterators
// * are faster than descending ones.
// *
// * <p>All <tt>Map.Entry</tt> pairs returned by methods in this class
// * and its views represent snapshots of mappings at the time they were
// * produced. They do <em>not</em> support the <tt>Entry.setValue</tt>
// * method. (Note however that it is possible to change mappings in the
// * associated map using <tt>put</tt>, <tt>putIfAbsent</tt>, or
// * <tt>replace</tt>, depending on exactly which effect you need.)
// *
// * <p>Beware that, unlike in most collections, the <tt>size</tt>
// * method is <em>not</em> a constant-time operation. Because of the
// * asynchronous nature of these maps, determining the current number
// * of elements requires a traversal of the elements, and so may report
// * inaccurate results if this collection is modified during traversal.
// * Additionally, the bulk operations <tt>putAll</tt>, <tt>equals</tt>,
// * <tt>toArray</tt>, <tt>containsValue</tt>, and <tt>clear</tt> are
// * <em>not</em> guaranteed to be performed atomically. For example, an
// * iterator operating concurrently with a <tt>putAll</tt> operation
// * might view only some of the added elements.
// *
// * <p>This class and its views and iterators implement all of the
// * <em>optional</em> methods of the {@link Map} and {@link Iterator}
// * interfaces. Like most other concurrent collections, this class does
// * <em>not</em> permit the use of <tt>null</tt> keys or values because some
// * null return values cannot be reliably distinguished from the absence of
// * elements.
// *
// * <p>This class is a member of the
// * <a href="{@docRoot}/../technotes/guides/collections/index.html">
// * Java Collections Framework</a>.
// *
// * @author Doug Lea
// * @param <K> the type of keys maintained by this map
// * @param <V> the type of mapped values
// * @since 1.6
// */
//public class ConcurrentSkipListMap2 extends AbstractMap implements ConcurrentNavigableMap, Cloneable,
//        java.io.Serializable {
//    /*
//     * This class implements a tree-like two-dimensionally linked skip
//     * list in which the index levels are represented in separate
//     * nodes from the base nodes holding data.  There are two reasons
//     * for taking this approach instead of the usual array-based
//     * structure: 1) Array based implementations seem to encounter
//     * more complexity and overhead 2) We can use cheaper algorithms
//     * for the heavily-traversed index lists than can be used for the
//     * base lists.  Here's a picture of some of the basics for a
//     * possible list with 2 levels of index:
//     *
//     * Head nodes          Index nodes
//     * +-+    right        +-+                      +-+
//     * |2|---------------->| |--------------------->| |->null
//     * +-+                 +-+                      +-+
//     *  | down              |                        |
//     *  v                   v                        v
//     * +-+            +-+  +-+       +-+            +-+       +-+
//     * |1|----------->| |->| |------>| |----------->| |------>| |->null
//     * +-+            +-+  +-+       +-+            +-+       +-+
//     *  v              |    |         |              |         |
//     * Nodes  next     v    v         v              v         v
//     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
//     * | |->|A|->|B|->|C|->|D|->|E|->|F|->|G|->|H|->|I|->|J|->|K|->null
//     * +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+  +-+
//     *
//     * The base lists use a variant of the HM linked ordered set
//     * algorithm. See Tim Harris, "A pragmatic implementation of
//     * non-blocking linked lists"
//     * http://www.cl.cam.ac.uk/~tlh20/publications.html and Maged
//     * Michael "High Performance Dynamic Lock-Free Hash Tables and
//     * List-Based Sets"
//     * http://www.research.ibm.com/people/m/michael/pubs.htm.  The
//     * basic idea in these lists is to mark the "next" pointers of
//     * deleted nodes when deleting to avoid conflicts with concurrent
//     * insertions, and when traversing to keep track of triples
//     * (predecessor, node, successor) in order to detect when and how
//     * to unlink these deleted nodes.
//     *
//     * Rather than using mark-bits to mark list deletions (which can
//     * be slow and space-intensive using AtomicMarkedReference), nodes
//     * use direct CAS'able next pointers.  On deletion, instead of
//     * marking a pointer, they splice in another node that can be
//     * thought of as standing for a marked pointer (indicating this by
//     * using otherwise impossible field values).  Using plain nodes
//     * acts roughly like "boxed" implementations of marked pointers,
//     * but uses new nodes only when nodes are deleted, not for every
//     * link.  This requires less space and supports faster
//     * traversal. Even if marked references were better supported by
//     * JVMs, traversal using this technique might still be faster
//     * because any search need only read ahead one more node than
//     * otherwise required (to check for trailing marker) rather than
//     * unmasking mark bits or whatever on each read.
//     *
//     * This approach maintains the essential property needed in the HM
//     * algorithm of changing the next-pointer of a deleted node so
//     * that any other CAS of it will fail, but implements the idea by
//     * changing the pointer to point to a different node, not by
//     * marking it.  While it would be possible to further squeeze
//     * space by defining marker nodes not to have key/value fields, it
//     * isn't worth the extra type-testing overhead.  The deletion
//     * markers are rarely encountered during traversal and are
//     * normally quickly garbage collected. (Note that this technique
//     * would not work well in systems without garbage collection.)
//     *
//     * In addition to using deletion markers, the lists also use
//     * nullness of value fields to indicate deletion, in a style
//     * similar to typical lazy-deletion schemes.  If a node's value is
//     * null, then it is considered logically deleted and ignored even
//     * though it is still reachable. This maintains proper control of
//     * concurrent replace vs delete operations -- an attempted replace
//     * must fail if a delete beat it by nulling field, and a delete
//     * must return the last non-null value held in the field. (Note:
//     * Null, rather than some special marker, is used for value fields
//     * here because it just so happens to mesh with the Map API
//     * requirement that method get returns null if there is no
//     * mapping, which allows nodes to remain concurrently readable
//     * even when deleted. Using any other marker value here would be
//     * messy at best.)
//     *
//     * Here's the sequence of events for a deletion of node n with
//     * predecessor b and successor f, initially:
//     *
//     *        +------+       +------+      +------+
//     *   ...  |   b  |------>|   n  |----->|   f  | ...
//     *        +------+       +------+      +------+
//     *
//     * 1. CAS n's value field from non-null to null.
//     *    From this point on, no public operations encountering
//     *    the node consider this mapping to exist. However, other
//     *    ongoing insertions and deletions might still modify
//     *    n's next pointer.
//     *
//     * 2. CAS n's next pointer to point to a new marker node.
//     *    From this point on, no other nodes can be appended to n.
//     *    which avoids deletion errors in CAS-based linked lists.
//     *
//     *        +------+       +------+      +------+       +------+
//     *   ...  |   b  |------>|   n  |----->|marker|------>|   f  | ...
//     *        +------+       +------+      +------+       +------+
//     *
//     * 3. CAS b's next pointer over both n and its marker.
//     *    From this point on, no new traversals will encounter n,
//     *    and it can eventually be GCed.
//     *        +------+                                    +------+
//     *   ...  |   b  |----------------------------------->|   f  | ...
//     *        +------+                                    +------+
//     *
//     * A failure at step 1 leads to simple retry due to a lost race
//     * with another operation. Steps 2-3 can fail because some other
//     * thread noticed during a traversal a node with null value and
//     * helped out by marking and/or unlinking.  This helping-out
//     * ensures that no thread can become stuck waiting for progress of
//     * the deleting thread.  The use of marker nodes slightly
//     * complicates helping-out code because traversals must track
//     * consistent reads of up to four nodes (b, n, marker, f), not
//     * just (b, n, f), although the next field of a marker is
//     * immutable, and once a next field is CAS'ed to point to a
//     * marker, it never again changes, so this requires less care.
//     *
//     * Skip lists add indexing to this scheme, so that the base-level
//     * traversals start close to the locations being found, inserted
//     * or deleted -- usually base level traversals only traverse a few
//     * nodes. This doesn't change the basic algorithm except for the
//     * need to make sure base traversals start at predecessors (here,
//     * b) that are not (structurally) deleted, otherwise retrying
//     * after processing the deletion.
//     *
//     * Index levels are maintained as lists with volatile next fields,
//     * using CAS to link and unlink.  Races are allowed in index-list
//     * operations that can (rarely) fail to link in a new index node
//     * or delete one. (We can't do this of course for data nodes.)
//     * However, even when this happens, the index lists remain sorted,
//     * so correctly serve as indices.  This can impact performance,
//     * but since skip lists are probabilistic anyway, the net result
//     * is that under contention, the effective "p" value may be lower
//     * than its nominal value. And race windows are kept small enough
//     * that in practice these failures are rare, even under a lot of
//     * contention.
//     *
//     * The fact that retries (for both base and index lists) are
//     * relatively cheap due to indexing allows some minor
//     * simplifications of retry logic. Traversal restarts are
//     * performed after most "helping-out" CASes. This isn't always
//     * strictly necessary, but the implicit backoffs tend to help
//     * reduce other downstream failed CAS's enough to outweigh restart
//     * cost.  This worsens the worst case, but seems to improve even
//     * highly contended cases.
//     *
//     * Unlike most skip-list implementations, index insertion and
//     * deletion here require a separate traversal pass occuring after
//     * the base-level action, to add or remove index nodes.  This adds
//     * to single-threaded overhead, but improves contended
//     * multithreaded performance by narrowing interference windows,
//     * and allows deletion to ensure that all index nodes will be made
//     * unreachable upon return from a public remove operation, thus
//     * avoiding unwanted garbage retention. This is more important
//     * here than in some other data structures because we cannot null
//     * out node fields referencing user keys since they might still be
//     * read by other ongoing traversals.
//     *
//     * Indexing uses skip list parameters that maintain good search
//     * performance while using sparser-than-usual indices: The
//     * hardwired parameters k=1, p=0.5 (see method randomLevel) mean
//     * that about one-quarter of the nodes have indices. Of those that
//     * do, half have one level, a quarter have two, and so on (see
//     * Pugh's Skip List Cookbook, sec 3.4).  The expected total space
//     * requirement for a map is slightly less than for the current
//     * implementation of java.util.TreeMap.
//     *
//     * Changing the level of the index (i.e, the height of the
//     * tree-like structure) also uses CAS. The head index has initial
//     * level/height of one. Creation of an index with height greater
//     * than the current level adds a level to the head index by
//     * CAS'ing on a new top-most head. To maintain good performance
//     * after a lot of removals, deletion methods heuristically try to
//     * reduce the height if the topmost levels appear to be empty.
//     * This may encounter races in which it possible (but rare) to
//     * reduce and "lose" a level just as it is about to contain an
//     * index (that will then never be encountered). This does no
//     * structural harm, and in practice appears to be a better option
//     * than allowing unrestrained growth of levels.
//     *
//     * The code for all this is more verbose than you'd like. Most
//     * operations entail locating an element (or position to insert an
//     * element). The code to do this can't be nicely factored out
//     * because subsequent uses require a snapshot of predecessor
//     * and/or successor and/or value fields which can't be returned
//     * all at once, at least not without creating yet another object
//     * to hold them -- creating such little objects is an especially
//     * bad idea for basic internal search operations because it adds
//     * to GC overhead.  (This is one of the few times I've wished Java
//     * had macros.) Instead, some traversal code is interleaved within
//     * insertion and removal operations.  The control logic to handle
//     * all the retry conditions is sometimes twisty. Most search is
//     * broken into 2 parts. findPredecessor() searches index nodes
//     * only, returning a base-level predecessor of the key. findNode()
//     * finishes out the base-level search. Even with this factoring,
//     * there is a fair amount of near-duplication of code to handle
//     * variants.
//     *
//     * For explanation of algorithms sharing at least a couple of
//     * features with this one, see Mikhail Fomitchev's thesis
//     * (http://www.cs.yorku.ca/~mikhail/), Keir Fraser's thesis
//     * (http://www.cl.cam.ac.uk/users/kaf24/), and Hakan Sundell's
//     * thesis (http://www.cs.chalmers.se/~phs/).
//     *
//     * Given the use of tree-like index nodes, you might wonder why
//     * this doesn't use some kind of search tree instead, which would
//     * support somewhat faster search operations. The reason is that
//     * there are no known efficient lock-free insertion and deletion
//     * algorithms for search trees. The immutability of the "down"
//     * links of index nodes (as opposed to mutable "left" fields in
//     * true trees) makes this tractable using only CAS operations.
//     *
//     * Notation guide for local variables
//     * Node:         b, n, f    for  predecessor, node, successor
//     * Index:        q, r, d    for index node, right, down.
//     *               t          for another index node
//     * Head:         h
//     * Levels:       j
//     * Keys:         k, key
//     * Values:       v, value
//     * Comparisons:  c
//     */
//
//    private static final long serialVersionUID = -8627078645895051609L;
//
//    /**
//     * Generates the initial random seed for the cheaper per-instance
//     * random number generators used in randomLevel.
//     */
//    private static final Random seedGenerator = new Random();
//
//    /**
//     * Special value used to identify base-level header
//     */
//    private static final Object BASE_HEADER = new Object();
//
//    /**
//     * The topmost head index of the skiplist.
//     */
//    private transient volatile HeadIndex head;
//
//    /**
//     * The comparator used to maintain order in this map, or null
//     * if using natural ordering.
//     * @serial
//     */
//    private final Comparator comparator;
//
//    /**
//     * Seed for simple random number generator.  Not volatile since it
//     * doesn't matter too much if different threads don't see updates.
//     */
//    private transient int randomSeed;
//
//    /** Lazily initialized key set */
//    private transient KeySet keySet;
//    /** Lazily initialized entry set */
//    private transient EntrySet entrySet;
//    /** Lazily initialized values collection */
//    private transient Values values;
//    /** Lazily initialized descending key set */
//    private transient ConcurrentNavigableMap descendingMap;
//
//    /**
//     * Initializes or resets state. Needed by constructors, clone,
//     * clear, readObject. and ConcurrentSkipListSet.clone.
//     * (Note that comparator must be separately initialized.)
//     */
//    final void initialize() {
//        keySet = null;
//        entrySet = null;
//        values = null;
//        descendingMap = null;
//        randomSeed = seedGenerator.nextInt() | 0x0100; // ensure nonzero
//        head = new HeadIndex(new Node(null, BASE_HEADER, null), null, null, 1);
//    }
//
//    /**
//     * compareAndSet head node
//     */
//    private boolean casHead(HeadIndex cmp, HeadIndex val) {
//        return UNSAFE.compareAndSwapObject(this, headOffset, cmp, val);
//    }
//
//    /* ---------------- Nodes -------------- */
//
//    /**
//     * Nodes hold keys and values, and are singly linked in sorted
//     * order, possibly with some intervening marker nodes. The list is
//     * headed by a dummy node accessible as head.node. The value field
//     * is declared only as Object because it takes special non-V
//     * values for marker and header nodes.
//     */
//    static final class Node {
//        final Object key;
//        volatile Object value;
//        volatile Node next;
//
//        /**
//         * Creates a new regular node.
//         */
//        Node(Object key, Object value, Node next) {
//            this.key = key;
//            this.value = value;
//            this.next = next;
//        }
//
//        /**
//         * Creates a new marker node. A marker is distinguished by
//         * having its value field point to itself.  Marker nodes also
//         * have null keys, a fact that is exploited in a few places,
//         * but this doesn't distinguish markers from the base-level
//         * header node (head.node), which also has a null key.
//         */
//        Node(Node next) {
//            this.key = null;
//            this.value = this;
//            this.next = next;
//        }
//        
//        public String toString() {
//            return "Node(key="+key+", next="+(next==null?"null":next.key)+")";
//        }
//
//        /**
//         * compareAndSet value field
//         */
//        boolean casValue(Object cmp, Object val) {
//            return UNSAFE.compareAndSwapObject(this, valueOffset, cmp, val);
//        }
//
//        /**
//         * compareAndSet next field
//         */
//        boolean casNext(Node cmp, Node val) {
//            return UNSAFE.compareAndSwapObject(this, nextOffset, cmp, val);
//        }
//
//        /**
//         * Returns true if this node is a marker. This method isn't
//         * actually called in any current code checking for markers
//         * because callers will have already read value field and need
//         * to use that read (not another done here) and so directly
//         * test if value points to node.
//         * @param n a possibly null reference to a node
//         * @return true if this node is a marker node
//         */
//        boolean isMarker() {
//            return value == this;
//        }
//
//        /**
//         * Returns true if this node is the header of base-level list.
//         * @return true if this node is header node
//         */
//        boolean isBaseHeader() {
//            return value == BASE_HEADER;
//        }
//
//        /**
//         * Tries to append a deletion marker to this node.
//         * @param f the assumed current successor of this node
//         * @return true if successful
//         */
//        boolean appendMarker(Node f) {
//            return casNext(f, new Node(f));
//        }
//
//        /**
//         * Helps out a deletion by appending marker or unlinking from
//         * predecessor. This is called during traversals when value
//         * field seen to be null.
//         * @param b predecessor
//         * @param f successor
//         */
//        void helpDelete(Node b, Node f) {
//            /*
//             * Rechecking links and then doing only one of the
//             * help-out stages per call tends to minimize CAS
//             * interference among helping threads.
//             */
//            if (f == next && this == b.next) {
//                if (f == null || f.value != f) // not already marked
//                    appendMarker(f);
//                else
//                    b.casNext(this, f.next);
//            }
//        }
//
//        /**
//         * Returns value if this node contains a valid key-value pair,
//         * else null.
//         * @return this node's value if it isn't a marker or header or
//         * is deleted, else null.
//         */
//        Object getValidValue() {
//            Object v = value;
//            if (v == this || v == BASE_HEADER)
//                return null;
//            return (Object) v;
//        }
//
//        /**
//         * Creates and returns a new SimpleImmutableEntry holding current
//         * mapping if this node holds a valid value, else null.
//         * @return new entry or null
//         */
//        AbstractMap.SimpleImmutableEntry createSnapshot() {
//            Object v = getValidValue();
//            if (v == null)
//                return null;
//            return new AbstractMap.SimpleImmutableEntry(key, v);
//        }
//
//        // UNSAFE mechanics
//
//        private static final sun.misc.Unsafe UNSAFE;
//        private static final long valueOffset;
//        private static final long nextOffset;
//
//        static {
//            try {
//                //UNSAFE = sun.misc.Unsafe.getUnsafe();
//                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
//                field.setAccessible(true);
//                UNSAFE = (sun.misc.Unsafe) field.get(null);
//                Class k = Node.class;
//                valueOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("value"));
//                nextOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("next"));
//            } catch (Exception e) {
//                throw new Error(e);
//            }
//        }
//    }
//
//    /* ---------------- Indexing -------------- */
//
//    /**
//     * Index nodes represent the levels of the skip list.  Note that
//     * even though both Nodes and Indexes have forward-pointing
//     * fields, they have different types and are handled in different
//     * ways, that can't nicely be captured by placing field in a
//     * shared abstract class.
//     */
//    static class Index {
//        final Node node;
//        final Index down;
//        volatile Index right;
//        
//        public String toString() {
//            StringBuilder s = new StringBuilder();
//            s.append(node).append("-->");
//            if(right!=null)
//                s.append(right.node);
//            else
//                s.append("null");
//            s.append("\r\n");
//            s.append("|");
//            s.append("\r\n");
//            s.append("ˇ");
//            s.append("\r\n");
//
//            if(down!=null)
//                s.append(down.node);
//            else
//                s.append("null");
//            
//            return s.toString();
//        }
//
//        /**
//         * Creates index node with given values.
//         */
//        Index(Node node, Index down, Index right) {
//            this.node = node;
//            this.down = down;
//            this.right = right;
//        }
//
//        /**
//         * compareAndSet right field
//         */
//        final boolean casRight(Index cmp, Index val) {
//            return UNSAFE.compareAndSwapObject(this, rightOffset, cmp, val);
//        }
//
//        /**
//         * Returns true if the node this indexes has been deleted.
//         * @return true if indexed node is known to be deleted
//         */
//        final boolean indexesDeletedNode() {
//            return node.value == null;
//        }
//
//        /**
//         * Tries to CAS newSucc as successor.  To minimize races with
//         * unlink that may lose this index node, if the node being
//         * indexed is known to be deleted, it doesn't try to link in.
//         * @param succ the expected current successor
//         * @param newSucc the new successor
//         * @return true if successful
//         */
//        final boolean link(Index succ, Index newSucc) {
//            Node n = node;
//            newSucc.right = succ;
//            return n.value != null && casRight(succ, newSucc);
//        }
//
//        /**
//         * Tries to CAS right field to skip over apparent successor
//         * succ.  Fails (forcing a retraversal by caller) if this node
//         * is known to be deleted.
//         * @param succ the expected current successor
//         * @return true if successful
//         */
//        final boolean unlink(Index succ) {
//            return !indexesDeletedNode() && casRight(succ, succ.right);
//        }
//
//        // Unsafe mechanics
//        private static final sun.misc.Unsafe UNSAFE;
//        private static final long rightOffset;
//        static {
//            try {
//                //UNSAFE = sun.misc.Unsafe.getUnsafe();
//                Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
//                field.setAccessible(true);
//                UNSAFE = (sun.misc.Unsafe) field.get(null);
//                Class k = Index.class;
//                rightOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("right"));
//            } catch (Exception e) {
//                throw new Error(e);
//            }
//        }
//    }
//
//    /* ---------------- Head nodes -------------- */
//
//    /**
//     * Nodes heading each level keep track of their level.
//     */
//    static final class HeadIndex extends Index {
//        final int level;
//
//        HeadIndex(Node node, Index down, Index right, int level) {
//            super(node, down, right);
//            this.level = level;
//        }
//    }
//
//    /* ---------------- Comparison utilities -------------- */
//
//    /**
//     * Represents a key with a comparator as a Comparable.
//     *
//     * Because most sorted collections seem to use natural ordering on
//     * Comparables (Strings, Integers, etc), most internal methods are
//     * geared to use them. This is generally faster than checking
//     * per-comparison whether to use comparator or comparable because
//     * it doesn't require a (Comparable) cast for each comparison.
//     * (Optimizers can only sometimes remove such redundant checks
//     * themselves.) When Comparators are used,
//     * ComparableUsingComparators are created so that they act in the
//     * same way as natural orderings. This penalizes use of
//     * Comparators vs Comparables, which seems like the right
//     * tradeoff.
//     */
//    static final class ComparableUsingComparator<Object> implements Comparable<Object> {
//        final Object actualKey;
//        final Comparator<? super Object> cmp;
//
//        ComparableUsingComparator(Object key, Comparator<? super Object> cmp) {
//            this.actualKey = key;
//            this.cmp = cmp;
//        }
//
//        @Override
//        public int compareTo(Object k2) {
//            return cmp.compare(actualKey, k2);
//        }
//    }
//
//    /**
//     * If using comparator, return a ComparableUsingComparator, else
//     * cast key as Comparable, which may cause ClassCastException,
//     * which is propagated back to caller.
//     */
//    private Comparable<? super Object> comparable(Object key) throws ClassCastException {
//        if (key == null)
//            throw new NullPointerException();
//        if (comparator != null)
//            return new ComparableUsingComparator<Object>((Object) key, comparator);
//        else
//            return (Comparable<? super Object>) key;
//    }
//
//    /**
//     * Compares using comparator or natural ordering. Used when the
//     * ComparableUsingComparator approach doesn't apply.
//     */
//    int compare(Object k1, Object k2) throws ClassCastException {
//        Comparator<? super Object> cmp = comparator;
//        if (cmp != null)
//            return cmp.compare(k1, k2);
//        else
//            return ((Comparable<? super Object>) k1).compareTo(k2);
//    }
//
//    /**
//     * Returns true if given key greater than or equal to least and
//     * strictly less than fence, bypassing either test if least or
//     * fence are null. Needed mainly in submap operations.
//     */
//    boolean inHalfOpenRange(Object key, Object least, Object fence) {
//        if (key == null)
//            throw new NullPointerException();
//        return ((least == null || compare(key, least) >= 0) && (fence == null || compare(key, fence) < 0));
//    }
//
//    /**
//     * Returns true if given key greater than or equal to least and less
//     * or equal to fence. Needed mainly in submap operations.
//     */
//    boolean inOpenRange(Object key, Object least, Object fence) {
//        if (key == null)
//            throw new NullPointerException();
//        return ((least == null || compare(key, least) >= 0) && (fence == null || compare(key, fence) <= 0));
//    }
//
//    /* ---------------- Traversal -------------- */
//
//    /**
//     * Returns a base-level node with key strictly less than given key,
//     * or the base-level header if there is no such node.  Also
//     * unlinks indexes to deleted nodes found along the way.  Callers
//     * rely on this side-effect of clearing indices to deleted nodes.
//     * @param key the key
//     * @return a predecessor of key
//     */
//    private Node findPredecessor(Comparable<? super Object> key) {
//        if (key == null)
//            throw new NullPointerException(); // don't postpone errors
//        for (;;) {
//            Index q = head;
//            Index r = q.right;
//            for (;;) {
//                if (r != null) {
//                    Node n = r.node;
//                    Object k = n.key;
//                    if (n.value == null) {
//                        if (!q.unlink(r))
//                            break; // restart
//                        r = q.right; // reread r
//                        continue;
//                    }
//                    if (key.compareTo(k) > 0) {
//                        q = r;
//                        r = r.right;
//                        continue;
//                    }
//                }
//                Index d = q.down;
//                if (d != null) {
//                    q = d;
//                    r = d.right;
//                } else
//                    return q.node;
//            }
//        }
//    }
//
//    /**
//     * Returns node holding key or null if no such, clearing out any
//     * deleted nodes seen along the way.  Repeatedly traverses at
//     * base-level looking for key starting at predecessor returned
//     * from findPredecessor, processing base-level deletions as
//     * encountered. Some callers rely on this side-effect of clearing
//     * deleted nodes.
//     *
//     * Restarts occur, at traversal step centered on node n, if:
//     *
//     *   (1) After reading n's next field, n is no longer assumed
//     *       predecessor b's current successor, which means that
//     *       we don't have a consistent 3-node snapshot and so cannot
//     *       unlink any subsequent deleted nodes encountered.
//     *
//     *   (2) n's value field is null, indicating n is deleted, in
//     *       which case we help out an ongoing structural deletion
//     *       before retrying.  Even though there are cases where such
//     *       unlinking doesn't require restart, they aren't sorted out
//     *       here because doing so would not usually outweigh cost of
//     *       restarting.
//     *
//     *   (3) n is a marker or n's predecessor's value field is null,
//     *       indicating (among other possibilities) that
//     *       findPredecessor returned a deleted node. We can't unlink
//     *       the node because we don't know its predecessor, so rely
//     *       on another call to findPredecessor to notice and return
//     *       some earlier predecessor, which it will do. This check is
//     *       only strictly needed at beginning of loop, (and the
//     *       b.value check isn't strictly needed at all) but is done
//     *       each iteration to help avoid contention with other
//     *       threads by callers that will fail to be able to change
//     *       links, and so will retry anyway.
//     *
//     * The traversal loops in doPut, doRemove, and findNear all
//     * include the same three kinds of checks. And specialized
//     * versions appear in findFirst, and findLast and their
//     * variants. They can't easily share code because each uses the
//     * reads of fields held in locals occurring in the orders they
//     * were performed.
//     *
//     * @param key the key
//     * @return node holding key, or null if no such
//     */
//    private Node findNode(Comparable<? super Object> key) {
//        for (;;) {
//            Node b = findPredecessor(key);
//            Node n = b.next;
//            for (;;) {
//                if (n == null)
//                    return null;
//                Node f = n.next;
//                if (n != b.next) // inconsistent read
//                    break;
//                Object v = n.value;
//                if (v == null) { // n is deleted
//                    n.helpDelete(b, f);
//                    break;
//                }
//                if (v == n || b.value == null) // b is deleted
//                    break;
//                int c = key.compareTo(n.key);
//                if (c == 0)
//                    return n;
//                if (c < 0)
//                    return null;
//                b = n;
//                n = f;
//            }
//        }
//    }
//
//    /**
//     * Gets value for key using findNode.
//     * @param okey the key
//     * @return the value, or null if absent
//     */
//    private Object doGet(Object okey) {
//        Comparable<? super Object> key = comparable(okey);
//        /*
//         * Loop needed here and elsewhere in case value field goes
//         * null just as it is about to be returned, in which case we
//         * lost a race with a deletion, so must retry.
//         */
//        for (;;) {
//            Node n = findNode(key);
//            if (n == null)
//                return null;
//            Object v = n.value;
//            if (v != null)
//                return (Object) v;
//        }
//    }
//
//    /* ---------------- Insertion -------------- */
//
//    /**
//     * Main insertion method.  Adds element if not present, or
//     * replaces value if present and onlyIfAbsent is false.
//     * @param kkey the key
//     * @param value  the value that must be associated with key
//     * @param onlyIfAbsent if should not insert if already present
//     * @return the old value, or null if newly inserted
//     */
//    private Object doPut(Object kkey, Object value, boolean onlyIfAbsent) {
//        Comparable<? super Object> key = comparable(kkey);
//        for (;;) {
//            Node b = findPredecessor(key); //不是node级别的前趋节点，而是Index级别的前趋节点
//            Node n = b.next;
//            for (;;) {
//                if (n != null) {
//                    Node f = n.next;
//                    if (n != b.next) // inconsistent read
//                        break;
//                    Object v = n.value;
//                    if (v == null) { // n is deleted
//                        n.helpDelete(b, f);
//                        break;
//                    }
//                    if (v == n || b.value == null) // b is deleted
//                        break;
//                    int c = key.compareTo(n.key);
//                    if (c > 0) {
//                        b = n;
//                        n = f;
//                        continue;
//                    }
//                    if (c == 0) {
//                        if (onlyIfAbsent || n.casValue(v, value))
//                            return (Object) v;
//                        else
//                            break; // restart if lost race to replace value
//                    }
//                    // else c < 0; fall through
//                }
//
//                Node z = new Node(kkey, value, n);
//                if (!b.casNext(n, z))
//                    break; // restart if lost race to append to b
//                int level = randomLevel();
//                //level=3; //我加的
//                if (level > 0)
//                    insertIndex(z, level);
//                return null;
//            }
//        }
//    }
//
//    /**
//     * Returns a random level for inserting a new node.
//     * Hardwired to k=1, p=0.5, max 31 (see above and
//     * Pugh's "Skip List Cookbook", sec 3.4).
//     *
//     * This uses the simplest of the generators described in George
//     * Marsaglia's "Xorshift RNGs" paper.  This is not a high-quality
//     * generator but is acceptable here.
//     */
//    private int randomLevel() {
//        int x = randomSeed;
//        x ^= x << 13;
//        x ^= x >>> 17;
//        randomSeed = x ^= x << 5;
//        if ((x & 0x80000001) != 0) // test highest and lowest bits
//            return 0;
//        int level = 1;
//        while (((x >>>= 1) & 1) != 0)
//            ++level;
//        return level;
//    }
//
//    /**
//     * Creates and adds index nodes for the given node.
//     * @param z the node
//     * @param level the level of the index
//     */
//    private void insertIndex(Node z, int level) {
//        HeadIndex h = head;
//        int max = h.level;
//
//        if (level <= max) {
//            Index idx = null;
//            for (int i = 1; i <= level; ++i)
//                idx = new Index(z, idx, null);
//            addIndex(idx, h, level);
//
//        } else { // Add a new level
//            /*
//             * To reduce interference by other threads checking for
//             * empty levels in tryReduceLevel, new levels are added
//             * with initialized right pointers. Which in turn requires
//             * keeping levels in an array to access them while
//             * creating new head index nodes from the opposite
//             * direction.
//             */
//            level = max + 1;
//            Index[] idxs = new Index[level + 1];
//            Index idx = null;
//            for (int i = 1; i <= level; ++i)
//                idxs[i] = idx = new Index(z, idx, null);
//
//            HeadIndex oldh;
//            int k;
//            for (;;) {
//                oldh = head;
//                int oldLevel = oldh.level;
//                if (level <= oldLevel) { // lost race to add level
//                    k = level;
//                    break;
//                }
//                HeadIndex newh = oldh;
//                Node oldbase = oldh.node;
//                for (int j = oldLevel + 1; j <= level; ++j)
//                    newh = new HeadIndex(oldbase, newh, idxs[j], j);
//                if (casHead(oldh, newh)) {
//                    k = oldLevel;
//                    break;
//                }
//            }
//            addIndex(idxs[k], oldh, k);
//        }
//    }
//
//    /**
//     * Adds given index nodes from given level down to 1.
//     * @param idx the topmost index node being inserted
//     * @param h the value of head to use to insert. This must be
//     * snapshotted by callers to provide correct insertion level
//     * @param indexLevel the level of the index
//     */
//    private void addIndex(Index idx, HeadIndex h, int indexLevel) {
//        // Track next level to insert in case of retries
//        int insertionLevel = indexLevel;
//        Comparable<? super Object> key = comparable(idx.node.key);
//        if (key == null)
//            throw new NullPointerException();
//
//        // Similar to findPredecessor, but adding index nodes along
//        // path to key.
//        for (;;) {
//            int j = h.level;
//            Index q = h;
//            Index r = q.right;
//            Index t = idx;
//            for (;;) {
//                if (r != null) {
//                    Node n = r.node;
//                    // compare before deletion check avoids needing recheck
//                    int c = key.compareTo(n.key);
//                    if (n.value == null) {
//                        if (!q.unlink(r))
//                            break;
//                        r = q.right;
//                        continue;
//                    }
//                    if (c > 0) {
//                        q = r;
//                        r = r.right;
//                        continue;
//                    }
//                }
//
//                if (j == insertionLevel) {
//                    // Don't insert index if node already deleted
//                    if (t.indexesDeletedNode()) {
//                        findNode(key); // cleans up
//                        return;
//                    }
//                    if (!q.link(r, t))
//                        break; // restart
//                    if (--insertionLevel == 0) {
//                        // need final deletion check before return
//                        if (t.indexesDeletedNode())
//                            findNode(key);
//                        return;
//                    }
//                }
//
//                if (--j >= insertionLevel && j < indexLevel)
//                    t = t.down;
//                q = q.down;
//                r = q.right;
//            }
//        }
//    }
//
//    /* ---------------- Deletion -------------- */
//
//    /**
//     * Main deletion method. Locates node, nulls value, appends a
//     * deletion marker, unlinks predecessor, removes associated index
//     * nodes, and possibly reduces head index level.
//     *
//     * Index nodes are cleared out simply by calling findPredecessor.
//     * which unlinks indexes to deleted nodes found along path to key,
//     * which will include the indexes to this node.  This is done
//     * unconditionally. We can't check beforehand whether there are
//     * index nodes because it might be the case that some or all
//     * indexes hadn't been inserted yet for this node during initial
//     * search for it, and we'd like to ensure lack of garbage
//     * retention, so must call to be sure.
//     *
//     * @param okey the key
//     * @param value if non-null, the value that must be
//     * associated with key
//     * @return the node, or null if not found
//     */
//    final Object doRemove(Object okey, Object value) {
//        Comparable<? super Object> key = comparable(okey);
//        for (;;) {
//            Node b = findPredecessor(key);
//            Node n = b.next;
//            for (;;) {
//                if (n == null)
//                    return null;
//                Node f = n.next;
//                if (n != b.next) // inconsistent read
//                    break;
//                Object v = n.value;
//                if (v == null) { // n is deleted
//                    n.helpDelete(b, f);
//                    break;
//                }
//                if (v == n || b.value == null) // b is deleted
//                    break;
//                int c = key.compareTo(n.key);
//                if (c < 0)
//                    return null;
//                if (c > 0) {
//                    b = n;
//                    n = f;
//                    continue;
//                }
//                if (value != null && !value.equals(v))
//                    return null;
//                if (!n.casValue(v, null))
//                    break;
//                if (!n.appendMarker(f) || !b.casNext(n, f))
//                    findNode(key); // Retry via findNode
//                else {
//                    findPredecessor(key); // Clean index
//                    if (head.right == null)
//                        tryReduceLevel();
//                }
//                return (Object) v;
//            }
//        }
//    }
//
//    /**
//     * Possibly reduce head level if it has no nodes.  This method can
//     * (rarely) make mistakes, in which case levels can disappear even
//     * though they are about to contain index nodes. This impacts
//     * performance, not correctness.  To minimize mistakes as well as
//     * to reduce hysteresis, the level is reduced by one only if the
//     * topmost three levels look empty. Also, if the removed level
//     * looks non-empty after CAS, we try to change it back quick
//     * before anyone notices our mistake! (This trick works pretty
//     * well because this method will practically never make mistakes
//     * unless current thread stalls immediately before first CAS, in
//     * which case it is very unlikely to stall again immediately
//     * afterwards, so will recover.)
//     *
//     * We put up with all this rather than just let levels grow
//     * because otherwise, even a small map that has undergone a large
//     * number of insertions and removals will have a lot of levels,
//     * slowing down access more than would an occasional unwanted
//     * reduction.
//     */
//    private void tryReduceLevel() {
//        HeadIndex h = head;
//        HeadIndex d;
//        HeadIndex e;
//        if (h.level > 3 && (d = (HeadIndex) h.down) != null && (e = (HeadIndex) d.down) != null && e.right == null
//                && d.right == null && h.right == null && casHead(h, d) && // try to set
//                h.right != null) // recheck
//            casHead(d, h); // try to backout
//    }
//
//    /* ---------------- Finding and removing first element -------------- */
//
//    /**
//     * Specialized variant of findNode to get first valid node.
//     * @return first node or null if empty
//     */
//    Node findFirst() {
//        for (;;) {
//            Node b = head.node;
//            Node n = b.next;
//            if (n == null)
//                return null;
//            if (n.value != null)
//                return n;
//            n.helpDelete(b, n.next);
//        }
//    }
//
//    /**
//     * Removes first entry; returns its snapshot.
//     * @return null if empty, else snapshot of first entry
//     */
//    Map.Entry doRemoveFirstEntry() {
//        for (;;) {
//            Node b = head.node;
//            Node n = b.next;
//            if (n == null)
//                return null;
//            Node f = n.next;
//            if (n != b.next)
//                continue;
//            Object v = n.value;
//            if (v == null) {
//                n.helpDelete(b, f);
//                continue;
//            }
//            if (!n.casValue(v, null))
//                continue;
//            if (!n.appendMarker(f) || !b.casNext(n, f))
//                findFirst(); // retry
//            clearIndexToFirst();
//            return new AbstractMap.SimpleImmutableEntry(n.key, (Object) v);
//        }
//    }
//
//    /**
//     * Clears out index nodes associated with deleted first entry.
//     */
//    private void clearIndexToFirst() {
//        for (;;) {
//            Index q = head;
//            for (;;) {
//                Index r = q.right;
//                if (r != null && r.indexesDeletedNode() && !q.unlink(r))
//                    break;
//                if ((q = q.down) == null) {
//                    if (head.right == null)
//                        tryReduceLevel();
//                    return;
//                }
//            }
//        }
//    }
//
//    /* ---------------- Finding and removing last element -------------- */
//
//    /**
//     * Specialized version of find to get last valid node.
//     * @return last node or null if empty
//     */
//    Node findLast() {
//        /*
//         * findPredecessor can't be used to traverse index level
//         * because this doesn't use comparisons.  So traversals of
//         * both levels are folded together.
//         */
//        Index q = head;
//        for (;;) {
//            Index d, r;
//            if ((r = q.right) != null) {
//                if (r.indexesDeletedNode()) {
//                    q.unlink(r);
//                    q = head; // restart
//                } else
//                    q = r;
//            } else if ((d = q.down) != null) {
//                q = d;
//            } else {
//                Node b = q.node;
//                Node n = b.next;
//                for (;;) {
//                    if (n == null)
//                        return b.isBaseHeader() ? null : b;
//                    Node f = n.next; // inconsistent read
//                    if (n != b.next)
//                        break;
//                    Object v = n.value;
//                    if (v == null) { // n is deleted
//                        n.helpDelete(b, f);
//                        break;
//                    }
//                    if (v == n || b.value == null) // b is deleted
//                        break;
//                    b = n;
//                    n = f;
//                }
//                q = head; // restart
//            }
//        }
//    }
//
//    /**
//     * Specialized variant of findPredecessor to get predecessor of last
//     * valid node.  Needed when removing the last entry.  It is possible
//     * that all successors of returned node will have been deleted upon
//     * return, in which case this method can be retried.
//     * @return likely predecessor of last node
//     */
//    private Node findPredecessorOfLast() {
//        for (;;) {
//            Index q = head;
//            for (;;) {
//                Index d, r;
//                if ((r = q.right) != null) {
//                    if (r.indexesDeletedNode()) {
//                        q.unlink(r);
//                        break; // must restart
//                    }
//                    // proceed as far across as possible without overshooting
//                    if (r.node.next != null) {
//                        q = r;
//                        continue;
//                    }
//                }
//                if ((d = q.down) != null)
//                    q = d;
//                else
//                    return q.node;
//            }
//        }
//    }
//
//    /**
//     * Removes last entry; returns its snapshot.
//     * Specialized variant of doRemove.
//     * @return null if empty, else snapshot of last entry
//     */
//    Map.Entry doRemoveLastEntry() {
//        for (;;) {
//            Node b = findPredecessorOfLast();
//            Node n = b.next;
//            if (n == null) {
//                if (b.isBaseHeader()) // empty
//                    return null;
//                else
//                    continue; // all b's successors are deleted; retry
//            }
//            for (;;) {
//                Node f = n.next;
//                if (n != b.next) // inconsistent read
//                    break;
//                Object v = n.value;
//                if (v == null) { // n is deleted
//                    n.helpDelete(b, f);
//                    break;
//                }
//                if (v == n || b.value == null) // b is deleted
//                    break;
//                if (f != null) {
//                    b = n;
//                    n = f;
//                    continue;
//                }
//                if (!n.casValue(v, null))
//                    break;
//                Object key = n.key;
//                Comparable<? super Object> ck = comparable(key);
//                if (!n.appendMarker(f) || !b.casNext(n, f))
//                    findNode(ck); // Retry via findNode
//                else {
//                    findPredecessor(ck); // Clean index
//                    if (head.right == null)
//                        tryReduceLevel();
//                }
//                return new AbstractMap.SimpleImmutableEntry(key, (Object) v);
//            }
//        }
//    }
//
//    /* ---------------- Relational operations -------------- */
//
//    // Control values OR'ed as arguments to findNear
//
//    private static final int EQ = 1;
//    private static final int LT = 2;
//    private static final int GT = 0; // Actually checked as !LT
//
//    /**
//     * Utility for ceiling, floor, lower, higher methods.
//     * @param kkey the key
//     * @param rel the relation -- OR'ed combination of EQ, LT, GT
//     * @return nearest node fitting relation, or null if no such
//     */
//    Node findNear(Object kkey, int rel) {
//        Comparable<? super Object> key = comparable(kkey);
//        for (;;) {
//            Node b = findPredecessor(key);
//            Node n = b.next;
//            for (;;) {
//                if (n == null)
//                    return ((rel & LT) == 0 || b.isBaseHeader()) ? null : b;
//                Node f = n.next;
//                if (n != b.next) // inconsistent read
//                    break;
//                Object v = n.value;
//                if (v == null) { // n is deleted
//                    n.helpDelete(b, f);
//                    break;
//                }
//                if (v == n || b.value == null) // b is deleted
//                    break;
//                int c = key.compareTo(n.key);
//                if ((c == 0 && (rel & EQ) != 0) || (c < 0 && (rel & LT) == 0))
//                    return n;
//                if (c <= 0 && (rel & LT) != 0)
//                    return b.isBaseHeader() ? null : b;
//                b = n;
//                n = f;
//            }
//        }
//    }
//
//    /**
//     * Returns SimpleImmutableEntry for results of findNear.
//     * @param key the key
//     * @param rel the relation -- OR'ed combination of EQ, LT, GT
//     * @return Entry fitting relation, or null if no such
//     */
//    AbstractMap.SimpleImmutableEntry getNear(Object key, int rel) {
//        for (;;) {
//            Node n = findNear(key, rel);
//            if (n == null)
//                return null;
//            AbstractMap.SimpleImmutableEntry e = n.createSnapshot();
//            if (e != null)
//                return e;
//        }
//    }
//
//    /* ---------------- Constructors -------------- */
//
//    /**
//     * Constructs a new, empty map, sorted according to the
//     * {@linkplain Comparable natural ordering} of the keys.
//     */
//    public ConcurrentSkipListMap2() {
//        this.comparator = null;
//        initialize();
//    }
//
//    /**
//     * Constructs a new, empty map, sorted according to the specified
//     * comparator.
//     *
//     * @param comparator the comparator that will be used to order this map.
//     *        If <tt>null</tt>, the {@linkplain Comparable natural
//     *        ordering} of the keys will be used.
//     */
//    public ConcurrentSkipListMap2(Comparator<? super Object> comparator) {
//        this.comparator = comparator;
//        initialize();
//    }
//
//    /**
//     * Constructs a new map containing the same mappings as the given map,
//     * sorted according to the {@linkplain Comparable natural ordering} of
//     * the keys.
//     *
//     * @param  m the map whose mappings are to be placed in this map
//     * @throws ClassCastException if the keys in <tt>m</tt> are not
//     *         {@link Comparable}, or are not mutually comparable
//     * @throws NullPointerException if the specified map or any of its keys
//     *         or values are null
//     */
//    public ConcurrentSkipListMap2(Map<? extends Object, ? extends Object> m) {
//        this.comparator = null;
//        initialize();
//        putAll(m);
//    }
//
//    /**
//     * Constructs a new map containing the same mappings and using the
//     * same ordering as the specified sorted map.
//     *
//     * @param m the sorted map whose mappings are to be placed in this
//     *        map, and whose comparator is to be used to sort this map
//     * @throws NullPointerException if the specified sorted map or any of
//     *         its keys or values are null
//     */
//    public ConcurrentSkipListMap2(SortedMap<Object, ? extends Object> m) {
//        this.comparator = m.comparator();
//        initialize();
//        buildFromSorted(m);
//    }
//
//    /**
//     * Returns a shallow copy of this <tt>ConcurrentSkipListMap</tt>
//     * instance. (The keys and values themselves are not cloned.)
//     *
//     * @return a shallow copy of this map
//     */
//    @Override
//    public ConcurrentSkipListMap2 clone() {
//        ConcurrentSkipListMap2 clone = null;
//        try {
//            clone = (ConcurrentSkipListMap2) super.clone();
//        } catch (CloneNotSupportedException e) {
//            throw new InternalError();
//        }
//
//        clone.initialize();
//        clone.buildFromSorted(this);
//        return clone;
//    }
//
//    /**
//     * Streamlined bulk insertion to initialize from elements of
//     * given sorted map.  Call only from constructor or clone
//     * method.
//     */
//    private void buildFromSorted(SortedMap<Object, ? extends Object> map) {
//        if (map == null)
//            throw new NullPointerException();
//
//        HeadIndex h = head;
//        Node basepred = h.node;
//
//        // Track the current rightmost node at each level. Uses an
//        // ArrayList to avoid committing to initial or maximum level.
//        ArrayList<Index> preds = new ArrayList<Index>();
//
//        // initialize
//        for (int i = 0; i <= h.level; ++i)
//            preds.add(null);
//        Index q = h;
//        for (int i = h.level; i > 0; --i) {
//            preds.set(i, q);
//            q = q.down;
//        }
//
//        Iterator<? extends Map.Entry<? extends Object, ? extends Object>> it = map.entrySet().iterator();
//        while (it.hasNext()) {
//            Map.Entry<? extends Object, ? extends Object> e = it.next();
//            int j = randomLevel();
//            if (j > h.level)
//                j = h.level + 1;
//            Object k = e.getKey();
//            Object v = e.getValue();
//            if (k == null || v == null)
//                throw new NullPointerException();
//            Node z = new Node(k, v, null);
//            basepred.next = z;
//            basepred = z;
//            if (j > 0) {
//                Index idx = null;
//                for (int i = 1; i <= j; ++i) {
//                    idx = new Index(z, idx, null);
//                    if (i > h.level)
//                        h = new HeadIndex(h.node, h, idx, i);
//
//                    if (i < preds.size()) {
//                        preds.get(i).right = idx;
//                        preds.set(i, idx);
//                    } else
//                        preds.add(idx);
//                }
//            }
//        }
//        head = h;
//    }
//
//    /* ---------------- Serialization -------------- */
//
//    /**
//     * Save the state of this map to a stream.
//     *
//     * @serialData The key (Object) and value (Object) for each
//     * key-value mapping represented by the map, followed by
//     * <tt>null</tt>. The key-value mappings are emitted in key-order
//     * (as determined by the Comparator, or by the keys' natural
//     * ordering if no Comparator).
//     */
//    private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
//        // Write out the Comparator and any hidden stuff
//        s.defaultWriteObject();
//
//        // Write out keys and values (alternating)
//        for (Node n = findFirst(); n != null; n = n.next) {
//            Object v = n.getValidValue();
//            if (v != null) {
//                s.writeObject(n.key);
//                s.writeObject(v);
//            }
//        }
//        s.writeObject(null);
//    }
//
//    /**
//     * Reconstitute the map from a stream.
//     */
//    private void readObject(final java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
//        // Read in the Comparator and any hidden stuff
//        s.defaultReadObject();
//        // Reset transients
//        initialize();
//
//        /*
//         * This is nearly identical to buildFromSorted, but is
//         * distinct because readObject calls can't be nicely adapted
//         * as the kind of iterator needed by buildFromSorted. (They
//         * can be, but doing so requires type cheats and/or creation
//         * of adaptor classes.) It is simpler to just adapt the code.
//         */
//
//        HeadIndex h = head;
//        Node basepred = h.node;
//        ArrayList<Index> preds = new ArrayList<Index>();
//        for (int i = 0; i <= h.level; ++i)
//            preds.add(null);
//        Index q = h;
//        for (int i = h.level; i > 0; --i) {
//            preds.set(i, q);
//            q = q.down;
//        }
//
//        for (;;) {
//            Object k = s.readObject();
//            if (k == null)
//                break;
//            Object v = s.readObject();
//            if (v == null)
//                throw new NullPointerException();
//            Object key = (Object) k;
//            Object val = (Object) v;
//            int j = randomLevel();
//            if (j > h.level)
//                j = h.level + 1;
//            Node z = new Node(key, val, null);
//            basepred.next = z;
//            basepred = z;
//            if (j > 0) {
//                Index idx = null;
//                for (int i = 1; i <= j; ++i) {
//                    idx = new Index(z, idx, null);
//                    if (i > h.level)
//                        h = new HeadIndex(h.node, h, idx, i);
//
//                    if (i < preds.size()) {
//                        preds.get(i).right = idx;
//                        preds.set(i, idx);
//                    } else
//                        preds.add(idx);
//                }
//            }
//        }
//        head = h;
//    }
//
//    /* ------ Map API methods ------ */
//
//    /**
//     * Returns <tt>true</tt> if this map contains a mapping for the specified
//     * key.
//     *
//     * @param key key whose presence in this map is to be tested
//     * @return <tt>true</tt> if this map contains a mapping for the specified key
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public boolean containsKey(Object key) {
//        return doGet(key) != null;
//    }
//
//    /**
//     * Returns the value to which the specified key is mapped,
//     * or {@code null} if this map contains no mapping for the key.
//     *
//     * <p>More formally, if this map contains a mapping from a key
//     * {@code k} to a value {@code v} such that {@code key} compares
//     * equal to {@code k} according to the map's ordering, then this
//     * method returns {@code v}; otherwise it returns {@code null}.
//     * (There can be at most one such mapping.)
//     *
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Object get(Object key) {
//        return doGet(key);
//    }
//
//    /**
//     * Associates the specified value with the specified key in this map.
//     * If the map previously contained a mapping for the key, the old
//     * value is replaced.
//     *
//     * @param key key with which the specified value is to be associated
//     * @param value value to be associated with the specified key
//     * @return the previous value associated with the specified key, or
//     *         <tt>null</tt> if there was no mapping for the key
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if the specified key or value is null
//     */
//    @Override
//    public Object put(Object key, Object value) {
//        if (value == null)
//            throw new NullPointerException();
//        return doPut(key, value, false);
//    }
//
//    /**
//     * Removes the mapping for the specified key from this map if present.
//     *
//     * @param  key key for which mapping should be removed
//     * @return the previous value associated with the specified key, or
//     *         <tt>null</tt> if there was no mapping for the key
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Object remove(Object key) {
//        return doRemove(key, null);
//    }
//
//    /**
//     * Returns <tt>true</tt> if this map maps one or more keys to the
//     * specified value.  This operation requires time linear in the
//     * map size. Additionally, it is possible for the map to change
//     * during execution of this method, in which case the returned
//     * result may be inaccurate.
//     *
//     * @param value value whose presence in this map is to be tested
//     * @return <tt>true</tt> if a mapping to <tt>value</tt> exists;
//     *         <tt>false</tt> otherwise
//     * @throws NullPointerException if the specified value is null
//     */
//    @Override
//    public boolean containsValue(Object value) {
//        if (value == null)
//            throw new NullPointerException();
//        for (Node n = findFirst(); n != null; n = n.next) {
//            Object v = n.getValidValue();
//            if (v != null && value.equals(v))
//                return true;
//        }
//        return false;
//    }
//
//    /**
//     * Returns the number of key-value mappings in this map.  If this map
//     * contains more than <tt>Integer.MAX_VALUE</tt> elements, it
//     * returns <tt>Integer.MAX_VALUE</tt>.
//     *
//     * <p>Beware that, unlike in most collections, this method is
//     * <em>NOT</em> a constant-time operation. Because of the
//     * asynchronous nature of these maps, determining the current
//     * number of elements requires traversing them all to count them.
//     * Additionally, it is possible for the size to change during
//     * execution of this method, in which case the returned result
//     * will be inaccurate. Thus, this method is typically not very
//     * useful in concurrent applications.
//     *
//     * @return the number of elements in this map
//     */
//    @Override
//    public int size() {
//        long count = 0;
//        for (Node n = findFirst(); n != null; n = n.next) {
//            if (n.getValidValue() != null)
//                ++count;
//        }
//        return (count >= Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) count;
//    }
//
//    /**
//     * Returns <tt>true</tt> if this map contains no key-value mappings.
//     * @return <tt>true</tt> if this map contains no key-value mappings
//     */
//    @Override
//    public boolean isEmpty() {
//        return findFirst() == null;
//    }
//
//    /**
//     * Removes all of the mappings from this map.
//     */
//    @Override
//    public void clear() {
//        initialize();
//    }
//
//    /* ---------------- View methods -------------- */
//
//    /*
//     * Note: Lazy initialization works for views because view classes
//     * are stateless/immutable so it doesn't matter wrt correctness if
//     * more than one is created (which will only rarely happen).  Even
//     * so, the following idiom conservatively ensures that the method
//     * returns the one it created if it does so, not one created by
//     * another racing thread.
//     */
//
//    /**
//     * Returns a {@link NavigableSet} view of the keys contained in this map.
//     * The set's iterator returns the keys in ascending order.
//     * The set is backed by the map, so changes to the map are
//     * reflected in the set, and vice-versa.  The set supports element
//     * removal, which removes the corresponding mapping from the map,
//     * via the {@code Iterator.remove}, {@code Set.remove},
//     * {@code removeAll}, {@code retainAll}, and {@code clear}
//     * operations.  It does not support the {@code add} or {@code addAll}
//     * operations.
//     *
//     * <p>The view's {@code iterator} is a "weakly consistent" iterator
//     * that will never throw {@link ConcurrentModificationException},
//     * and guarantees to traverse elements as they existed upon
//     * construction of the iterator, and may (but is not guaranteed to)
//     * reflect any modifications subsequent to construction.
//     *
//     * <p>This method is equivalent to method {@code navigableKeySet}.
//     *
//     * @return a navigable set view of the keys in this map
//     */
//    @Override
//    public NavigableSet<Object> keySet() {
//        KeySet ks = keySet;
//        return (ks != null) ? ks : (keySet = new KeySet(this));
//    }
//
//    @Override
//    public NavigableSet<Object> navigableKeySet() {
//        KeySet ks = keySet;
//        return (ks != null) ? ks : (keySet = new KeySet(this));
//    }
//
//    /**
//     * Returns a {@link Collection} view of the values contained in this map.
//     * The collection's iterator returns the values in ascending order
//     * of the corresponding keys.
//     * The collection is backed by the map, so changes to the map are
//     * reflected in the collection, and vice-versa.  The collection
//     * supports element removal, which removes the corresponding
//     * mapping from the map, via the <tt>Iterator.remove</tt>,
//     * <tt>Collection.remove</tt>, <tt>removeAll</tt>,
//     * <tt>retainAll</tt> and <tt>clear</tt> operations.  It does not
//     * 
//     * support the <tt>add</tt> or <tt>addAll</tt> operations.
//     *
//     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator
//     * that will never throw {@link ConcurrentModificationException},
//     * and guarantees to traverse elements as they existed upon
//     * construction of the iterator, and may (but is not guaranteed to)
//     * reflect any modifications subsequent to construction.
//     */
//    @Override
//    public Collection<Object> values() {
//        Values vs = values;
//        return (vs != null) ? vs : (values = new Values(this));
//    }
//
//    /**
//     * Returns a {@link Set} view of the mappings contained in this map.
//     * The set's iterator returns the entries in ascending key order.
//     * The set is backed by the map, so changes to the map are
//     * reflected in the set, and vice-versa.  The set supports element
//     * removal, which removes the corresponding mapping from the map,
//     * via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
//     * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt>
//     * operations.  It does not support the <tt>add</tt> or
//     * <tt>addAll</tt> operations.
//     *
//     * <p>The view's <tt>iterator</tt> is a "weakly consistent" iterator
//     * that will never throw {@link ConcurrentModificationException},
//     * and guarantees to traverse elements as they existed upon
//     * construction of the iterator, and may (but is not guaranteed to)
//     * reflect any modifications subsequent to construction.
//     *
//     * <p>The <tt>Map.Entry</tt> elements returned by
//     * <tt>iterator.next()</tt> do <em>not</em> support the
//     * <tt>setValue</tt> operation.
//     *
//     * @return a set view of the mappings contained in this map,
//     *         sorted in ascending key order
//     */
//    @Override
//    public Set<Map.Entry> entrySet() {
//        EntrySet es = entrySet;
//        return (es != null) ? es : (entrySet = new EntrySet(this));
//    }
//
//    @Override
//    public ConcurrentNavigableMap descendingMap() {
//        ConcurrentNavigableMap dm = descendingMap;
//        return (dm != null) ? dm : (descendingMap = new SubMap(this, null, false, null, false, true));
//    }
//
//    @Override
//    public NavigableSet<Object> descendingKeySet() {
//        return descendingMap().navigableKeySet();
//    }
//
//    /* ---------------- AbstractMap Overrides -------------- */
//
//    /**
//     * Compares the specified object with this map for equality.
//     * Returns <tt>true</tt> if the given object is also a map and the
//     * two maps represent the same mappings.  More formally, two maps
//     * <tt>m1</tt> and <tt>m2</tt> represent the same mappings if
//     * <tt>m1.entrySet().equals(m2.entrySet())</tt>.  This
//     * operation may return misleading results if either map is
//     * concurrently modified during execution of this method.
//     *
//     * @param o object to be compared for equality with this map
//     * @return <tt>true</tt> if the specified object is equal to this map
//     */
//    @Override
//    public boolean equals(Object o) {
//        if (o == this)
//            return true;
//        if (!(o instanceof Map))
//            return false;
//        Map<?, ?> m = (Map<?, ?>) o;
//        try {
//            for (Map.Entry e : this.entrySet())
//                if (!e.getValue().equals(m.get(e.getKey())))
//                    return false;
//            for (Map.Entry<?, ?> e : m.entrySet()) {
//                Object k = e.getKey();
//                Object v = e.getValue();
//                if (k == null || v == null || !v.equals(get(k)))
//                    return false;
//            }
//            return true;
//        } catch (ClassCastException unused) {
//            return false;
//        } catch (NullPointerException unused) {
//            return false;
//        }
//    }
//
//    /* ------ ConcurrentMap API methods ------ */
//
//    /**
//     * {@inheritDoc}
//     *
//     * @return the previous value associated with the specified key,
//     *         or <tt>null</tt> if there was no mapping for the key
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if the specified key or value is null
//     */
//    @Override
//    public Object putIfAbsent(Object key, Object value) {
//        if (value == null)
//            throw new NullPointerException();
//        return doPut(key, value, true);
//    }
//
//    /**
//     * {@inheritDoc}
//     *
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public boolean remove(Object key, Object value) {
//        if (key == null)
//            throw new NullPointerException();
//        if (value == null)
//            return false;
//        return doRemove(key, value) != null;
//    }
//
//    /**
//     * {@inheritDoc}
//     *
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if any of the arguments are null
//     */
//    @Override
//    public boolean replace(Object key, Object oldValue, Object newValue) {
//        if (oldValue == null || newValue == null)
//            throw new NullPointerException();
//        Comparable<? super Object> k = comparable(key);
//        for (;;) {
//            Node n = findNode(k);
//            if (n == null)
//                return false;
//            Object v = n.value;
//            if (v != null) {
//                if (!oldValue.equals(v))
//                    return false;
//                if (n.casValue(v, newValue))
//                    return true;
//            }
//        }
//    }
//
//    /**
//     * {@inheritDoc}
//     *
//     * @return the previous value associated with the specified key,
//     *         or <tt>null</tt> if there was no mapping for the key
//     * @throws ClassCastException if the specified key cannot be compared
//     *         with the keys currently in the map
//     * @throws NullPointerException if the specified key or value is null
//     */
//    @Override
//    public Object replace(Object key, Object value) {
//        if (value == null)
//            throw new NullPointerException();
//        Comparable<? super Object> k = comparable(key);
//        for (;;) {
//            Node n = findNode(k);
//            if (n == null)
//                return null;
//            Object v = n.value;
//            if (v != null && n.casValue(v, value))
//                return (Object) v;
//        }
//    }
//
//    /* ------ SortedMap API methods ------ */
//
//    @Override
//    public Comparator<? super Object> comparator() {
//        return comparator;
//    }
//
//    /**
//     * @throws NoSuchElementException {@inheritDoc}
//     */
//    @Override
//    public Object firstKey() {
//        Node n = findFirst();
//        if (n == null)
//            throw new NoSuchElementException();
//        return n.key;
//    }
//
//    /**
//     * @throws NoSuchElementException {@inheritDoc}
//     */
//    @Override
//    public Object lastKey() {
//        Node n = findLast();
//        if (n == null)
//            throw new NoSuchElementException();
//        return n.key;
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if {@code fromKey} or {@code toKey} is null
//     * @throws IllegalArgumentException {@inheritDoc}
//     */
//    @Override
//    public ConcurrentNavigableMap subMap(Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive) {
//        if (fromKey == null || toKey == null)
//            throw new NullPointerException();
//        return new SubMap(this, fromKey, fromInclusive, toKey, toInclusive, false);
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if {@code toKey} is null
//     * @throws IllegalArgumentException {@inheritDoc}
//     */
//    @Override
//    public ConcurrentNavigableMap headMap(Object toKey, boolean inclusive) {
//        if (toKey == null)
//            throw new NullPointerException();
//        return new SubMap(this, null, false, toKey, inclusive, false);
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if {@code fromKey} is null
//     * @throws IllegalArgumentException {@inheritDoc}
//     */
//    @Override
//    public ConcurrentNavigableMap tailMap(Object fromKey, boolean inclusive) {
//        if (fromKey == null)
//            throw new NullPointerException();
//        return new SubMap(this, fromKey, inclusive, null, false, false);
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if {@code fromKey} or {@code toKey} is null
//     * @throws IllegalArgumentException {@inheritDoc}
//     */
//    @Override
//    public ConcurrentNavigableMap subMap(Object fromKey, Object toKey) {
//        return subMap(fromKey, true, toKey, false);
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if {@code toKey} is null
//     * @throws IllegalArgumentException {@inheritDoc}
//     */
//    @Override
//    public ConcurrentNavigableMap headMap(Object toKey) {
//        return headMap(toKey, false);
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if {@code fromKey} is null
//     * @throws IllegalArgumentException {@inheritDoc}
//     */
//    @Override
//    public ConcurrentNavigableMap tailMap(Object fromKey) {
//        return tailMap(fromKey, true);
//    }
//
//    /* ---------------- Relational operations -------------- */
//
//    /**
//     * Returns a key-value mapping associated with the greatest key
//     * strictly less than the given key, or <tt>null</tt> if there is
//     * no such key. The returned entry does <em>not</em> support the
//     * <tt>Entry.setValue</tt> method.
//     *
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Map.Entry lowerEntry(Object key) {
//        return getNear(key, LT);
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Object lowerKey(Object key) {
//        Node n = findNear(key, LT);
//        return (n == null) ? null : n.key;
//    }
//
//    /**
//     * Returns a key-value mapping associated with the greatest key
//     * less than or equal to the given key, or <tt>null</tt> if there
//     * is no such key. The returned entry does <em>not</em> support
//     * the <tt>Entry.setValue</tt> method.
//     *
//     * @param key the key
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Map.Entry floorEntry(Object key) {
//        return getNear(key, LT | EQ);
//    }
//
//    /**
//     * @param key the key
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Object floorKey(Object key) {
//        Node n = findNear(key, LT | EQ);
//        return (n == null) ? null : n.key;
//    }
//
//    /**
//     * Returns a key-value mapping associated with the least key
//     * greater than or equal to the given key, or <tt>null</tt> if
//     * there is no such entry. The returned entry does <em>not</em>
//     * support the <tt>Entry.setValue</tt> method.
//     *
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Map.Entry ceilingEntry(Object key) {
//        return getNear(key, GT | EQ);
//    }
//
//    /**
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Object ceilingKey(Object key) {
//        Node n = findNear(key, GT | EQ);
//        return (n == null) ? null : n.key;
//    }
//
//    /**
//     * Returns a key-value mapping associated with the least key
//     * strictly greater than the given key, or <tt>null</tt> if there
//     * is no such key. The returned entry does <em>not</em> support
//     * the <tt>Entry.setValue</tt> method.
//     *
//     * @param key the key
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Map.Entry higherEntry(Object key) {
//        return getNear(key, GT);
//    }
//
//    /**
//     * @param key the key
//     * @throws ClassCastException {@inheritDoc}
//     * @throws NullPointerException if the specified key is null
//     */
//    @Override
//    public Object higherKey(Object key) {
//        Node n = findNear(key, GT);
//        return (n == null) ? null : n.key;
//    }
//
//    /**
//     * Returns a key-value mapping associated with the least
//     * key in this map, or <tt>null</tt> if the map is empty.
//     * The returned entry does <em>not</em> support
//     * the <tt>Entry.setValue</tt> method.
//     */
//    @Override
//    public Map.Entry firstEntry() {
//        for (;;) {
//            Node n = findFirst();
//            if (n == null)
//                return null;
//            AbstractMap.SimpleImmutableEntry e = n.createSnapshot();
//            if (e != null)
//                return e;
//        }
//    }
//
//    /**
//     * Returns a key-value mapping associated with the greatest
//     * key in this map, or <tt>null</tt> if the map is empty.
//     * The returned entry does <em>not</em> support
//     * the <tt>Entry.setValue</tt> method.
//     */
//    @Override
//    public Map.Entry lastEntry() {
//        for (;;) {
//            Node n = findLast();
//            if (n == null)
//                return null;
//            AbstractMap.SimpleImmutableEntry e = n.createSnapshot();
//            if (e != null)
//                return e;
//        }
//    }
//
//    /**
//     * Removes and returns a key-value mapping associated with
//     * the least key in this map, or <tt>null</tt> if the map is empty.
//     * The returned entry does <em>not</em> support
//     * the <tt>Entry.setValue</tt> method.
//     */
//    @Override
//    public Map.Entry pollFirstEntry() {
//        return doRemoveFirstEntry();
//    }
//
//    /**
//     * Removes and returns a key-value mapping associated with
//     * the greatest key in this map, or <tt>null</tt> if the map is empty.
//     * The returned entry does <em>not</em> support
//     * the <tt>Entry.setValue</tt> method.
//     */
//    @Override
//    public Map.Entry pollLastEntry() {
//        return doRemoveLastEntry();
//    }
//
//    /* ---------------- Iterators -------------- */
//
//    /**
//     * Base of iterator classes:
//     */
//    abstract class Iter<T> implements Iterator<T> {
//        /** the last node returned by next() */
//        Node lastReturned;
//        /** the next node to return from next(); */
//        Node next;
//        /** Cache of next value field to maintain weak consistency */
//        Object nextValue;
//
//        /** Initializes ascending iterator for entire range. */
//        Iter() {
//            for (;;) {
//                next = findFirst();
//                if (next == null)
//                    break;
//                Object x = next.value;
//                if (x != null && x != next) {
//                    nextValue = (Object) x;
//                    break;
//                }
//            }
//        }
//
//        @Override
//        public final boolean hasNext() {
//            return next != null;
//        }
//
//        /** Advances next to higher entry. */
//        final void advance() {
//            if (next == null)
//                throw new NoSuchElementException();
//            lastReturned = next;
//            for (;;) {
//                next = next.next;
//                if (next == null)
//                    break;
//                Object x = next.value;
//                if (x != null && x != next) {
//                    nextValue = (Object) x;
//                    break;
//                }
//            }
//        }
//
//        @Override
//        public void remove() {
//            Node l = lastReturned;
//            if (l == null)
//                throw new IllegalStateException();
//            // It would not be worth all of the overhead to directly
//            // unlink from here. Using remove is fast enough.
//            ConcurrentSkipListMap2.this.remove(l.key);
//            lastReturned = null;
//        }
//
//    }
//
//    final class ValueIterator extends Iter<Object> {
//        @Override
//        public Object next() {
//            Object v = nextValue;
//            advance();
//            return v;
//        }
//    }
//
//    final class KeyIterator extends Iter<Object> {
//        @Override
//        public Object next() {
//            Node n = next;
//            advance();
//            return n.key;
//        }
//    }
//
//    final class EntryIterator extends Iter<Map.Entry> {
//        @Override
//        public Map.Entry next() {
//            Node n = next;
//            Object v = nextValue;
//            advance();
//            return new AbstractMap.SimpleImmutableEntry(n.key, v);
//        }
//    }
//
//    // Factory methods for iterators needed by ConcurrentSkipListSet etc
//
//    Iterator<Object> keyIterator() {
//        return new KeyIterator();
//    }
//
//    Iterator<Object> valueIterator() {
//        return new ValueIterator();
//    }
//
//    Iterator<Map.Entry> entryIterator() {
//        return new EntryIterator();
//    }
//
//    /* ---------------- View Classes -------------- */
//
//    /*
//     * View classes are static, delegating to a ConcurrentNavigableMap
//     * to allow use by SubMaps, which outweighs the ugliness of
//     * needing type-tests for Iterator methods.
//     */
//
//    static final <E> List<E> toList(Collection<E> c) {
//        // Using size() here would be a pessimization.
//        List<E> list = new ArrayList<E>();
//        for (E e : c)
//            list.add(e);
//        return list;
//    }
//
//    static final class KeySet<E> extends AbstractSet<E> implements NavigableSet<E> {
//        private final ConcurrentNavigableMap<E, Object> m;
//
//        KeySet(ConcurrentNavigableMap<E, Object> map) {
//            m = map;
//        }
//
//        @Override
//        public int size() {
//            return m.size();
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return m.isEmpty();
//        }
//
//        @Override
//        public boolean contains(Object o) {
//            return m.containsKey(o);
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            return m.remove(o) != null;
//        }
//
//        @Override
//        public void clear() {
//            m.clear();
//        }
//
//        @Override
//        public E lower(E e) {
//            return m.lowerKey(e);
//        }
//
//        @Override
//        public E floor(E e) {
//            return m.floorKey(e);
//        }
//
//        @Override
//        public E ceiling(E e) {
//            return m.ceilingKey(e);
//        }
//
//        @Override
//        public E higher(E e) {
//            return m.higherKey(e);
//        }
//
//        @Override
//        public Comparator<? super E> comparator() {
//            return m.comparator();
//        }
//
//        @Override
//        public E first() {
//            return m.firstKey();
//        }
//
//        @Override
//        public E last() {
//            return m.lastKey();
//        }
//
//        @Override
//        public E pollFirst() {
//            Map.Entry<E, Object> e = m.pollFirstEntry();
//            return (e == null) ? null : e.getKey();
//        }
//
//        @Override
//        public E pollLast() {
//            Map.Entry<E, Object> e = m.pollLastEntry();
//            return (e == null) ? null : e.getKey();
//        }
//
//        @Override
//        public Iterator<E> iterator() {
//            if (m instanceof ConcurrentSkipListMap2)
//                return (Iterator<E>) ((ConcurrentSkipListMap2) m).keyIterator();
//            else
//                return (Iterator<E>) ((ConcurrentSkipListMap2.SubMap) m).keyIterator();
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (o == this)
//                return true;
//            if (!(o instanceof Set))
//                return false;
//            Collection<?> c = (Collection<?>) o;
//            try {
//                return containsAll(c) && c.containsAll(this);
//            } catch (ClassCastException unused) {
//                return false;
//            } catch (NullPointerException unused) {
//                return false;
//            }
//        }
//
//        @Override
//        public Object[] toArray() {
//            return toList(this).toArray();
//        }
//
//        @Override
//        public <T> T[] toArray(T[] a) {
//            return toList(this).toArray(a);
//        }
//
//        @Override
//        public Iterator<E> descendingIterator() {
//            return descendingSet().iterator();
//        }
//
//        @Override
//        public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
//            return new KeySet<E>(m.subMap(fromElement, fromInclusive, toElement, toInclusive));
//        }
//
//        @Override
//        public NavigableSet<E> headSet(E toElement, boolean inclusive) {
//            return new KeySet<E>(m.headMap(toElement, inclusive));
//        }
//
//        @Override
//        public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
//            return new KeySet<E>(m.tailMap(fromElement, inclusive));
//        }
//
//        @Override
//        public NavigableSet<E> subSet(E fromElement, E toElement) {
//            return subSet(fromElement, true, toElement, false);
//        }
//
//        @Override
//        public NavigableSet<E> headSet(E toElement) {
//            return headSet(toElement, false);
//        }
//
//        @Override
//        public NavigableSet<E> tailSet(E fromElement) {
//            return tailSet(fromElement, true);
//        }
//
//        @Override
//        public NavigableSet<E> descendingSet() {
//            return new KeySet(m.descendingMap());
//        }
//    }
//
//    static final class Values<E> extends AbstractCollection<E> {
//        private final ConcurrentNavigableMap<Object, E> m;
//
//        Values(ConcurrentNavigableMap<Object, E> map) {
//            m = map;
//        }
//
//        @Override
//        public Iterator<E> iterator() {
//            if (m instanceof ConcurrentSkipListMap2)
//                return (Iterator<E>) ((ConcurrentSkipListMap2) m).valueIterator();
//            else
//                return (Iterator<E>) ((SubMap) m).valueIterator();
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return m.isEmpty();
//        }
//
//        @Override
//        public int size() {
//            return m.size();
//        }
//
//        @Override
//        public boolean contains(Object o) {
//            return m.containsValue(o);
//        }
//
//        @Override
//        public void clear() {
//            m.clear();
//        }
//
//        @Override
//        public Object[] toArray() {
//            return toList(this).toArray();
//        }
//
//        @Override
//        public <T> T[] toArray(T[] a) {
//            return toList(this).toArray(a);
//        }
//    }
//
//    static final class EntrySet extends AbstractSet<Map.Entry> {
//        private final ConcurrentNavigableMap m;
//
//        EntrySet(ConcurrentNavigableMap map) {
//            m = map;
//        }
//
//        @Override
//        public Iterator<Entry> iterator() {
//            if (m instanceof ConcurrentSkipListMap2)
//                return ((ConcurrentSkipListMap2) m).entryIterator();
//            else
//                return ((SubMap) m).entryIterator();
//        }
//
//        @Override
//        public boolean contains(Object o) {
//            if (!(o instanceof Map.Entry))
//                return false;
//            Map.Entry e = (Map.Entry) o;
//            Object v = m.get(e.getKey());
//            return v != null && v.equals(e.getValue());
//        }
//
//        @Override
//        public boolean remove(Object o) {
//            if (!(o instanceof Map.Entry))
//                return false;
//            Map.Entry e = (Map.Entry) o;
//            return m.remove(e.getKey(), e.getValue());
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return m.isEmpty();
//        }
//
//        @Override
//        public int size() {
//            return m.size();
//        }
//
//        @Override
//        public void clear() {
//            m.clear();
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (o == this)
//                return true;
//            if (!(o instanceof Set))
//                return false;
//            Collection<?> c = (Collection<?>) o;
//            try {
//                return containsAll(c) && c.containsAll(this);
//            } catch (ClassCastException unused) {
//                return false;
//            } catch (NullPointerException unused) {
//                return false;
//            }
//        }
//
//        @Override
//        public Object[] toArray() {
//            return toList(this).toArray();
//        }
//
//        @Override
//        public <T> T[] toArray(T[] a) {
//            return toList(this).toArray(a);
//        }
//    }
//
//    /**
//     * Submaps returned by {@link ConcurrentSkipListMap2} submap operations
//     * represent a subrange of mappings of their underlying
//     * maps. Instances of this class support all methods of their
//     * underlying maps, differing in that mappings outside their range are
//     * ignored, and attempts to add mappings outside their ranges result
//     * in {@link IllegalArgumentException}.  Instances of this class are
//     * constructed only using the <tt>subMap</tt>, <tt>headMap</tt>, and
//     * <tt>tailMap</tt> methods of their underlying maps.
//     *
//     * @serial include
//     */
//    static final class SubMap extends AbstractMap implements ConcurrentNavigableMap, Cloneable,
//            java.io.Serializable {
//        private static final long serialVersionUID = -7647078645895051609L;
//
//        /** Underlying map */
//        private final ConcurrentSkipListMap2 m;
//        /** lower bound key, or null if from start */
//        private final Object lo;
//        /** upper bound key, or null if to end */
//        private final Object hi;
//        /** inclusion flag for lo */
//        private final boolean loInclusive;
//        /** inclusion flag for hi */
//        private final boolean hiInclusive;
//        /** direction */
//        private final boolean isDescending;
//
//        // Lazily initialized view holders
//        private transient KeySet<Object> keySetView;
//        private transient Set<Map.Entry> entrySetView;
//        private transient Collection<Object> valuesView;
//
//        /**
//         * Creates a new submap, initializing all fields
//         */
//        SubMap(ConcurrentSkipListMap2 map, Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive,
//                boolean isDescending) {
//            if (fromKey != null && toKey != null && map.compare(fromKey, toKey) > 0)
//                throw new IllegalArgumentException("inconsistent range");
//            this.m = map;
//            this.lo = fromKey;
//            this.hi = toKey;
//            this.loInclusive = fromInclusive;
//            this.hiInclusive = toInclusive;
//            this.isDescending = isDescending;
//        }
//
//        /* ----------------  Utilities -------------- */
//
//        private boolean tooLow(Object key) {
//            if (lo != null) {
//                int c = m.compare(key, lo);
//                if (c < 0 || (c == 0 && !loInclusive))
//                    return true;
//            }
//            return false;
//        }
//
//        private boolean tooHigh(Object key) {
//            if (hi != null) {
//                int c = m.compare(key, hi);
//                if (c > 0 || (c == 0 && !hiInclusive))
//                    return true;
//            }
//            return false;
//        }
//
//        private boolean inBounds(Object key) {
//            return !tooLow(key) && !tooHigh(key);
//        }
//
//        private void checkKeyBounds(Object key) throws IllegalArgumentException {
//            if (key == null)
//                throw new NullPointerException();
//            if (!inBounds(key))
//                throw new IllegalArgumentException("key out of range");
//        }
//
//        /**
//         * Returns true if node key is less than upper bound of range
//         */
//        private boolean isBeforeEnd(ConcurrentSkipListMap2.Node n) {
//            if (n == null)
//                return false;
//            if (hi == null)
//                return true;
//            Object k = n.key;
//            if (k == null) // pass by markers and headers
//                return true;
//            int c = m.compare(k, hi);
//            if (c > 0 || (c == 0 && !hiInclusive))
//                return false;
//            return true;
//        }
//
//        /**
//         * Returns lowest node. This node might not be in range, so
//         * most usages need to check bounds
//         */
//        private ConcurrentSkipListMap2.Node loNode() {
//            if (lo == null)
//                return m.findFirst();
//            else if (loInclusive)
//                return m.findNear(lo, m.GT | m.EQ);
//            else
//                return m.findNear(lo, m.GT);
//        }
//
//        /**
//         * Returns highest node. This node might not be in range, so
//         * most usages need to check bounds
//         */
//        private ConcurrentSkipListMap2.Node hiNode() {
//            if (hi == null)
//                return m.findLast();
//            else if (hiInclusive)
//                return m.findNear(hi, m.LT | m.EQ);
//            else
//                return m.findNear(hi, m.LT);
//        }
//
//        /**
//         * Returns lowest absolute key (ignoring directonality)
//         */
//        private Object lowestKey() {
//            ConcurrentSkipListMap2.Node n = loNode();
//            if (isBeforeEnd(n))
//                return n.key;
//            else
//                throw new NoSuchElementException();
//        }
//
//        /**
//         * Returns highest absolute key (ignoring directonality)
//         */
//        private Object highestKey() {
//            ConcurrentSkipListMap2.Node n = hiNode();
//            if (n != null) {
//                Object last = n.key;
//                if (inBounds(last))
//                    return last;
//            }
//            throw new NoSuchElementException();
//        }
//
//        private Map.Entry lowestEntry() {
//            for (;;) {
//                ConcurrentSkipListMap2.Node n = loNode();
//                if (!isBeforeEnd(n))
//                    return null;
//                Map.Entry e = n.createSnapshot();
//                if (e != null)
//                    return e;
//            }
//        }
//
//        private Map.Entry highestEntry() {
//            for (;;) {
//                ConcurrentSkipListMap2.Node n = hiNode();
//                if (n == null || !inBounds(n.key))
//                    return null;
//                Map.Entry e = n.createSnapshot();
//                if (e != null)
//                    return e;
//            }
//        }
//
//        private Map.Entry removeLowest() {
//            for (;;) {
//                Node n = loNode();
//                if (n == null)
//                    return null;
//                Object k = n.key;
//                if (!inBounds(k))
//                    return null;
//                Object v = m.doRemove(k, null);
//                if (v != null)
//                    return new AbstractMap.SimpleImmutableEntry(k, v);
//            }
//        }
//
//        private Map.Entry removeHighest() {
//            for (;;) {
//                Node n = hiNode();
//                if (n == null)
//                    return null;
//                Object k = n.key;
//                if (!inBounds(k))
//                    return null;
//                Object v = m.doRemove(k, null);
//                if (v != null)
//                    return new AbstractMap.SimpleImmutableEntry(k, v);
//            }
//        }
//
//        /**
//         * Submap version of ConcurrentSkipListMap.getNearEntry
//         */
//        private Map.Entry getNearEntry(Object key, int rel) {
//            if (isDescending) { // adjust relation for direction
//                if ((rel & m.LT) == 0)
//                    rel |= m.LT;
//                else
//                    rel &= ~m.LT;
//            }
//            if (tooLow(key))
//                return ((rel & m.LT) != 0) ? null : lowestEntry();
//            if (tooHigh(key))
//                return ((rel & m.LT) != 0) ? highestEntry() : null;
//            for (;;) {
//                Node n = m.findNear(key, rel);
//                if (n == null || !inBounds(n.key))
//                    return null;
//                Object k = n.key;
//                Object v = n.getValidValue();
//                if (v != null)
//                    return new AbstractMap.SimpleImmutableEntry(k, v);
//            }
//        }
//
//        // Almost the same as getNearEntry, except for keys
//        private Object getNearKey(Object key, int rel) {
//            if (isDescending) { // adjust relation for direction
//                if ((rel & m.LT) == 0)
//                    rel |= m.LT;
//                else
//                    rel &= ~m.LT;
//            }
//            if (tooLow(key)) {
//                if ((rel & m.LT) == 0) {
//                    ConcurrentSkipListMap2.Node n = loNode();
//                    if (isBeforeEnd(n))
//                        return n.key;
//                }
//                return null;
//            }
//            if (tooHigh(key)) {
//                if ((rel & m.LT) != 0) {
//                    ConcurrentSkipListMap2.Node n = hiNode();
//                    if (n != null) {
//                        Object last = n.key;
//                        if (inBounds(last))
//                            return last;
//                    }
//                }
//                return null;
//            }
//            for (;;) {
//                Node n = m.findNear(key, rel);
//                if (n == null || !inBounds(n.key))
//                    return null;
//                Object k = n.key;
//                Object v = n.getValidValue();
//                if (v != null)
//                    return k;
//            }
//        }
//
//        /* ----------------  Map API methods -------------- */
//
//        @Override
//        public boolean containsKey(Object key) {
//            if (key == null)
//                throw new NullPointerException();
//            Object k = (Object) key;
//            return inBounds(k) && m.containsKey(k);
//        }
//
//        @Override
//        public Object get(Object key) {
//            if (key == null)
//                throw new NullPointerException();
//            Object k = (Object) key;
//            return ((!inBounds(k)) ? null : m.get(k));
//        }
//
//        @Override
//        public Object put(Object key, Object value) {
//            checkKeyBounds(key);
//            return m.put(key, value);
//        }
//
//        @Override
//        public Object remove(Object key) {
//            Object k = (Object) key;
//            return (!inBounds(k)) ? null : m.remove(k);
//        }
//
//        @Override
//        public int size() {
//            long count = 0;
//            for (ConcurrentSkipListMap2.Node n = loNode(); isBeforeEnd(n); n = n.next) {
//                if (n.getValidValue() != null)
//                    ++count;
//            }
//            return count >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) count;
//        }
//
//        @Override
//        public boolean isEmpty() {
//            return !isBeforeEnd(loNode());
//        }
//
//        @Override
//        public boolean containsValue(Object value) {
//            if (value == null)
//                throw new NullPointerException();
//            for (ConcurrentSkipListMap2.Node n = loNode(); isBeforeEnd(n); n = n.next) {
//                Object v = n.getValidValue();
//                if (v != null && value.equals(v))
//                    return true;
//            }
//            return false;
//        }
//
//        @Override
//        public void clear() {
//            for (ConcurrentSkipListMap2.Node n = loNode(); isBeforeEnd(n); n = n.next) {
//                if (n.getValidValue() != null)
//                    m.remove(n.key);
//            }
//        }
//
//        /* ----------------  ConcurrentMap API methods -------------- */
//
//        @Override
//        public Object putIfAbsent(Object key, Object value) {
//            checkKeyBounds(key);
//            return m.putIfAbsent(key, value);
//        }
//
//        @Override
//        public boolean remove(Object key, Object value) {
//            Object k = (Object) key;
//            return inBounds(k) && m.remove(k, value);
//        }
//
//        @Override
//        public boolean replace(Object key, Object oldValue, Object newValue) {
//            checkKeyBounds(key);
//            return m.replace(key, oldValue, newValue);
//        }
//
//        @Override
//        public Object replace(Object key, Object value) {
//            checkKeyBounds(key);
//            return m.replace(key, value);
//        }
//
//        /* ----------------  SortedMap API methods -------------- */
//
//        @Override
//        public Comparator<? super Object> comparator() {
//            Comparator<? super Object> cmp = m.comparator();
//            if (isDescending)
//                return Collections.reverseOrder(cmp);
//            else
//                return cmp;
//        }
//
//        /**
//         * Utility to create submaps, where given bounds override
//         * unbounded(null) ones and/or are checked against bounded ones.
//         */
//        private SubMap newSubMap(Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive) {
//            if (isDescending) { // flip senses
//                Object tk = fromKey;
//                fromKey = toKey;
//                toKey = tk;
//                boolean ti = fromInclusive;
//                fromInclusive = toInclusive;
//                toInclusive = ti;
//            }
//            if (lo != null) {
//                if (fromKey == null) {
//                    fromKey = lo;
//                    fromInclusive = loInclusive;
//                } else {
//                    int c = m.compare(fromKey, lo);
//                    if (c < 0 || (c == 0 && !loInclusive && fromInclusive))
//                        throw new IllegalArgumentException("key out of range");
//                }
//            }
//            if (hi != null) {
//                if (toKey == null) {
//                    toKey = hi;
//                    toInclusive = hiInclusive;
//                } else {
//                    int c = m.compare(toKey, hi);
//                    if (c > 0 || (c == 0 && !hiInclusive && toInclusive))
//                        throw new IllegalArgumentException("key out of range");
//                }
//            }
//            return new SubMap(m, fromKey, fromInclusive, toKey, toInclusive, isDescending);
//        }
//
//        @Override
//        public SubMap subMap(Object fromKey, boolean fromInclusive, Object toKey, boolean toInclusive) {
//            if (fromKey == null || toKey == null)
//                throw new NullPointerException();
//            return newSubMap(fromKey, fromInclusive, toKey, toInclusive);
//        }
//
//        @Override
//        public SubMap headMap(Object toKey, boolean inclusive) {
//            if (toKey == null)
//                throw new NullPointerException();
//            return newSubMap(null, false, toKey, inclusive);
//        }
//
//        @Override
//        public SubMap tailMap(Object fromKey, boolean inclusive) {
//            if (fromKey == null)
//                throw new NullPointerException();
//            return newSubMap(fromKey, inclusive, null, false);
//        }
//
//        @Override
//        public SubMap subMap(Object fromKey, Object toKey) {
//            return subMap(fromKey, true, toKey, false);
//        }
//
//        @Override
//        public SubMap headMap(Object toKey) {
//            return headMap(toKey, false);
//        }
//
//        @Override
//        public SubMap tailMap(Object fromKey) {
//            return tailMap(fromKey, true);
//        }
//
//        @Override
//        public SubMap descendingMap() {
//            return new SubMap(m, lo, loInclusive, hi, hiInclusive, !isDescending);
//        }
//
//        /* ----------------  Relational methods -------------- */
//
//        @Override
//        public Map.Entry ceilingEntry(Object key) {
//            return getNearEntry(key, (m.GT | m.EQ));
//        }
//
//        @Override
//        public Object ceilingKey(Object key) {
//            return getNearKey(key, (m.GT | m.EQ));
//        }
//
//        @Override
//        public Map.Entry lowerEntry(Object key) {
//            return getNearEntry(key, (m.LT));
//        }
//
//        @Override
//        public Object lowerKey(Object key) {
//            return getNearKey(key, (m.LT));
//        }
//
//        @Override
//        public Map.Entry floorEntry(Object key) {
//            return getNearEntry(key, (m.LT | m.EQ));
//        }
//
//        @Override
//        public Object floorKey(Object key) {
//            return getNearKey(key, (m.LT | m.EQ));
//        }
//
//        @Override
//        public Map.Entry higherEntry(Object key) {
//            return getNearEntry(key, (m.GT));
//        }
//
//        @Override
//        public Object higherKey(Object key) {
//            return getNearKey(key, (m.GT));
//        }
//
//        @Override
//        public Object firstKey() {
//            return isDescending ? highestKey() : lowestKey();
//        }
//
//        @Override
//        public Object lastKey() {
//            return isDescending ? lowestKey() : highestKey();
//        }
//
//        @Override
//        public Map.Entry firstEntry() {
//            return isDescending ? highestEntry() : lowestEntry();
//        }
//
//        @Override
//        public Map.Entry lastEntry() {
//            return isDescending ? lowestEntry() : highestEntry();
//        }
//
//        @Override
//        public Map.Entry pollFirstEntry() {
//            return isDescending ? removeHighest() : removeLowest();
//        }
//
//        @Override
//        public Map.Entry pollLastEntry() {
//            return isDescending ? removeLowest() : removeHighest();
//        }
//
//        /* ---------------- Submap Views -------------- */
//
//        @Override
//        public NavigableSet<Object> keySet() {
//            KeySet<Object> ks = keySetView;
//            return (ks != null) ? ks : (keySetView = new KeySet(this));
//        }
//
//        @Override
//        public NavigableSet<Object> navigableKeySet() {
//            KeySet<Object> ks = keySetView;
//            return (ks != null) ? ks : (keySetView = new KeySet(this));
//        }
//
//        @Override
//        public Collection<Object> values() {
//            Collection<Object> vs = valuesView;
//            return (vs != null) ? vs : (valuesView = new Values(this));
//        }
//
//        @Override
//        public Set<Map.Entry> entrySet() {
//            Set<Map.Entry> es = entrySetView;
//            return (es != null) ? es : (entrySetView = new EntrySet(this));
//        }
//
//        @Override
//        public NavigableSet<Object> descendingKeySet() {
//            return descendingMap().navigableKeySet();
//        }
//
//        Iterator<Object> keyIterator() {
//            return new SubMapKeyIterator();
//        }
//
//        Iterator<Object> valueIterator() {
//            return new SubMapValueIterator();
//        }
//
//        Iterator<Map.Entry> entryIterator() {
//            return new SubMapEntryIterator();
//        }
//
//        /**
//         * Variant of main Iter class to traverse through submaps.
//         */
//        abstract class SubMapIter<T> implements Iterator<T> {
//            /** the last node returned by next() */
//            Node lastReturned;
//            /** the next node to return from next(); */
//            Node next;
//            /** Cache of next value field to maintain weak consistency */
//            Object nextValue;
//
//            SubMapIter() {
//                for (;;) {
//                    next = isDescending ? hiNode() : loNode();
//                    if (next == null)
//                        break;
//                    Object x = next.value;
//                    if (x != null && x != next) {
//                        if (!inBounds(next.key))
//                            next = null;
//                        else
//                            nextValue = (Object) x;
//                        break;
//                    }
//                }
//            }
//
//            @Override
//            public final boolean hasNext() {
//                return next != null;
//            }
//
//            final void advance() {
//                if (next == null)
//                    throw new NoSuchElementException();
//                lastReturned = next;
//                if (isDescending)
//                    descend();
//                else
//                    ascend();
//            }
//
//            private void ascend() {
//                for (;;) {
//                    next = next.next;
//                    if (next == null)
//                        break;
//                    Object x = next.value;
//                    if (x != null && x != next) {
//                        if (tooHigh(next.key))
//                            next = null;
//                        else
//                            nextValue = (Object) x;
//                        break;
//                    }
//                }
//            }
//
//            private void descend() {
//                for (;;) {
//                    next = m.findNear(lastReturned.key, LT);
//                    if (next == null)
//                        break;
//                    Object x = next.value;
//                    if (x != null && x != next) {
//                        if (tooLow(next.key))
//                            next = null;
//                        else
//                            nextValue = (Object) x;
//                        break;
//                    }
//                }
//            }
//
//            @Override
//            public void remove() {
//                Node l = lastReturned;
//                if (l == null)
//                    throw new IllegalStateException();
//                m.remove(l.key);
//                lastReturned = null;
//            }
//
//        }
//
//        final class SubMapValueIterator extends SubMapIter<Object> {
//            @Override
//            public Object next() {
//                Object v = nextValue;
//                advance();
//                return v;
//            }
//        }
//
//        final class SubMapKeyIterator extends SubMapIter<Object> {
//            @Override
//            public Object next() {
//                Node n = next;
//                advance();
//                return n.key;
//            }
//        }
//
//        final class SubMapEntryIterator extends SubMapIter<Map.Entry> {
//            @Override
//            public Map.Entry next() {
//                Node n = next;
//                Object v = nextValue;
//                advance();
//                return new AbstractMap.SimpleImmutableEntry(n.key, v);
//            }
//        }
//    }
//
//    // Unsafe mechanics
//    private static final sun.misc.Unsafe UNSAFE;
//    private static final long headOffset;
//    //    static {
//    //        try {
//    //            UNSAFE = sun.misc.Unsafe.getUnsafe();
//    //            Class k = ConcurrentSkipListMap.class;
//    //            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
//    //        } catch (Exception e) {
//    //            throw new Error(e);
//    //        }
//    //    }
//    static {
//        try {
//            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
//            field.setAccessible(true);
//            UNSAFE = (sun.misc.Unsafe) field.get(null);
//            Class k = ConcurrentSkipListMap2.class;
//            headOffset = UNSAFE.objectFieldOffset(k.getDeclaredField("head"));
//        } catch (Exception e) {
//            throw new Error(e);
//        }
//    }
//    
//    public String toString() {
//        StringBuilder s = new StringBuilder();
//        s.append(head.level).append("\r\n");
//
//        Index q = head;
//        s.append(q.node.key).append(" -> "); //总是HeadIndex，key是null
//        Index r = q.right;
//        for (;;) {
//            if (r != null) {
//                Node n = r.node;
//                Object k = n.key;
//                s.append(k).append(" -> ");
//                r = r.right;
//                continue;
//            }
//            Index d = q.down;
//            if (d != null) {
//                s.append("\r\n");
//                s.append(d.node.key).append(" -> "); //总是HeadIndex，key是null
//                q = d;
//                r = d.right;
//            } else
//                break;
//        }
//        return s.toString();
//    }
// }
