package my.test.engine;

import java.util.Properties;

import my.test.TestBase;

import org.h2.engine.ConnectionInfo;

public class ConnectionInfoTest extends TestBase {
    public static void main(String[] args) throws Exception {
        new ConnectionInfoTest().start();
    }

    @Override
    public void init() throws Exception {
        prop.setProperty("user", "sa");
        
        //prop.setProperty("CIPHER", "AES"); //AES、XTEA、FOG
        //如果配置了CIPHER，则password包含两部份，用一个空格分开这两部份，第一部份是filePassword，第二部份是userPassword。
        //如果PASSWORD_HASH参数是true那么不再进行SHA256，此时必须使用16进制字符，字符个数是偶数。
        //如果PASSWORD_HASH参数是false，不必是16进制字符，会按SHA256算法进行hash
        //prop.setProperty("password", "abcd 1234");

        prop.setProperty("password", "1234");
    }

    @Override
    public void startInternal() throws Exception {
        //convertPasswords();
    }

    void test() {
        Properties prop = new Properties();
        prop.setProperty("user", "sa");

        prop.setProperty("not_exists", "haha"); //没有异常

        //System.setProperty("h2.urlMap", "E:/H2/my-h2/my-h2-src/my/test/h2.urlMap.properties");

        prop.setProperty("max_compact_time", "300");

        String url = "my.url";
        url = "jdbc:h2:tcp://localhost:9092/test9;optimize_distinct=true;early_filter=true;nested_joins=false";
        url = "jdbc:h2:test";

        //有异常: org.h2.message.DbException: Unsupported connection setting "NOT_EXISTS" [90113-169]
        //url = "jdbc:h2:tcp://localhost:9092/test9;optimize_distinct=true;early_filter=true;nested_joins=false;not_exists=haha";

        //有异常: org.h2.message.DbException: Duplicate property "USER" [90066-169]
        //如果值相同就不会报错
        //url = "jdbc:h2:tcp://localhost:9092/test9;optimize_distinct=true;early_filter=true;nested_joins=false;user=sa2";
        ConnectionInfo info = new ConnectionInfo(url, prop, null, null);

        System.out.println(info.getName());

    }

    void convertPasswords() {
        Properties prop = new Properties();

        //prop.setProperty("password", "");
        //prop.put("password", new char[] {});

        //prop.setProperty("PASSWORD_HASH", "true");
        prop.setProperty("CIPHER", "AES"); //AES、XTEA、FOG

        //如果配置了CIPHER，则秘密包含两部份，用空格分开，第一部份是filePassword，第二部份是真实密码
        //并且使用16进制字符，字符个数是偶数，如果PASSWORD_HASH参数是true那么不再进行SHA256

        //abc是基数
        //org.h2.message.DbException: Hexadecimal string with odd number of characters: "abc" [90003-169]
        //prop.setProperty("password", "abc 123");

        //字符"g"不是16进制字符
        //org.h2.message.DbException: Hexadecimal string contains non-hex character: "abcg" [90004-169]
        //prop.setProperty("password", "abcg 123");

        //正确的
        prop.setProperty("password", "abcd 1234");

        String url = "jdbc:h2:test";

        ConnectionInfo info = new ConnectionInfo(url, prop, "", "");

        System.out.println(info.getName());
    }

}
