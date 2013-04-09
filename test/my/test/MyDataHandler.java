package my.test;

import java.sql.Connection;

import org.h2.constant.ErrorCode;
import org.h2.message.DbException;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.LobStorage;
import org.h2.store.fs.FileUtils;
import org.h2.util.SmallLRUCache;
import org.h2.util.TempFileDeleter;

public class MyDataHandler implements DataHandler {

	@Override
	public String getDatabasePath() {
		return "";
	}

	@Override
	public FileStore openFile(String name, String mode, boolean mustExist) {
		if (mustExist && !FileUtils.exists(name)) {
			throw DbException.get(ErrorCode.FILE_NOT_FOUND_1, name);
		}
		FileStore store = FileStore.open(this, name, mode);
		store.setCheckedWriting(false);
		try {
			store.init();
		} catch (DbException e) {
			store.closeSilently();
			throw e;
		}
		return store;
	}

	@Override
	public void checkPowerOff() throws DbException {
		// TODO Auto-generated method stub

	}

	@Override
	public void checkWritingAllowed() throws DbException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getMaxLengthInplaceLob() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLobCompressionAlgorithm(int type) {
		return "LZF";
	}

	@Override
	public TempFileDeleter getTempFileDeleter() {
		return TempFileDeleter.getInstance();
	}

	@Override
	public Object getLobSyncObject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SmallLRUCache<String, String[]> getLobFileListCache() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public LobStorage getLobStorage() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Connection getLobConnection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int readLob(long lobId, byte[] hmac, long offset, byte[] buff,
			int off, int length) {
		// TODO Auto-generated method stub
		return 0;
	}
}
