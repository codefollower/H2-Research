package my.test;

import java.util.HashMap;
import java.util.Map.Entry;

import org.h2.constant.DbSettings;

public class ConfigParamTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("SetTypes.size="+org.h2.command.dml.SetTypes.getTypes().size());
		for(String p: org.h2.command.dml.SetTypes.getTypes())
			System.out.println((p!=null)? p.toLowerCase(): "null");
		
		System.out.println();
		
		String[] connectionTime = { "ACCESS_MODE_DATA", "AUTOCOMMIT", "CIPHER",
                "CREATE", "CACHE_TYPE", "FILE_LOCK", "IGNORE_UNKNOWN_SETTINGS",
                "IFEXISTS", "INIT", "PASSWORD", "RECOVER", "RECOVER_TEST",
                "USER", "AUTO_SERVER", "AUTO_SERVER_PORT", "NO_UPGRADE",
                "AUTO_RECONNECT", "OPEN_NEW", "PAGE_SIZE", "PASSWORD_HASH", "JMX" };
		
		System.out.println("connectionTime.size="+connectionTime.length);
		for(String p: connectionTime)
			System.out.println((p!=null)? p.toLowerCase(): "null");
		
		System.out.println();
		
		DbSettings dbSettings = DbSettings.getInstance(null);
		HashMap<String, String> dbSettingsMap = dbSettings.getSettings();
		System.out.println("dbSettingsMap.size="+dbSettingsMap.size());
		
		for(Entry<String, String> e : dbSettingsMap.entrySet())
			System.out.println(((e.getKey()!=null)? e.getKey().toLowerCase(): "null")+"\t\t\t"+e.getValue());
		

	}

}
