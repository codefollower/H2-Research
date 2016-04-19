//package my.test.mvstore;
//
//public class ConcurrentSkipListMapTest {
//    static class MyConcurrentSkipListMap extends ConcurrentSkipListMap2{//<String, String> {
//        
//    }
//    public static void p(Object o) {
//        System.out.println(o);
//    }
//
//    public static void p() {
//        System.out.println();
//    }
//    
//    public static void main(String[] args) {
//        java.util.concurrent.ConcurrentSkipListMap<String, String> m = 
//                new java.util.concurrent.ConcurrentSkipListMap<String, String>();
//        m.size();
//        m.values();
//        MyConcurrentSkipListMap map = new MyConcurrentSkipListMap();
//        
//        map.put("10", "abc");
//        
//        map.put("20", "abc");
//        
//        map.put("30", "abc");
//        
//        map.put("40", "abc");
//        
//        p(map.ceilingKey("21"));
//        p(map.floorKey("21"));
//        
//        p(map.higherKey("20"));
//        p(map.lowerKey("20"));
//        
//        
//
////        for (int i = 10; i < 100; i++)
////            if(i!=20)map.put(i + "", "abc");
////        // map.put((i * 10) + "", "abc");
////
////        //map.put("20", "abc");
////        
////        //p(map.get("20"));
////        
////        p(map);
////        
////        map.put("20", "abc");
////        p(map);
////        
////        p(map.get("20"));
////        
////       for(Object key : map.keySet())
////           p(key);
////       
////       for(Object key : map.descendingKeySet())
////           p(key);
//    }
// }
