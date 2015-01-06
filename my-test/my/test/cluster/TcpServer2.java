package my.test.cluster;

import java.util.ArrayList;

public class TcpServer2 {
    public static void main(String[] args)  throws Exception {
        // System.setProperty("DATABASE_TO_UPPER", "false");
        System.setProperty("h2.lobInDatabase", "false");
        System.setProperty("h2.lobClientMaxSizeMemory", "1024");
        System.setProperty("java.io.tmpdir", "./target/mytest/cluster-node2/tmp");
        System.setProperty("h2.baseDir", "./target/mytest/cluster-node2");
        //System.setProperty("h2.check2", "true");
        ArrayList<String> list = new ArrayList<String>();

        list.add("-tcp");
        list.add("-tcpPort");
        list.add("9093");
        org.h2.tools.Server.main(list.toArray(new String[list.size()]));
    }

}
