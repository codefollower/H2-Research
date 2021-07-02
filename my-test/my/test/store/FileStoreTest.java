package my.test.store;

import org.h2.message.DbException;
import org.h2.store.DataHandler;
import org.h2.store.FileStore;
import org.h2.store.LobStorageInterface;
import org.h2.util.MathUtils;
import org.h2.util.SmallLRUCache;
import org.h2.util.TempFileDeleter;
import org.h2.value.CompareMode;

public class FileStoreTest {

    /**
     * @param args
     */
    public static void main(String[] args) {

        String name = null;
        FileStore store;
        MyDataHandler dh = new MyDataHandler();
        String mode = null;
        mode = "rw";
        name = "E:\\H2\\tmp\\FileStoreTest\\my.txt";
        // name = "file:E:\\H2\\tmp\\FileStoreTest\\my.txt";
        // name = "cache:E:\\H2\\tmp\\FileStoreTest\\my.txt";

        name = "memFS:E:\\H2\\tmp\\FileStoreTest\\my.txt";

        System.setProperty("user.home", "E:/H2/tmp");
        name = "~";
        name = "file:~/FileStoreTest/my.txt";

        store = getFileStore(dh, name, mode);
        // store = getSecureFileStore(dh, name, mode);

        store.setCheckedWriting(false);
        try {
            store.init();
        } catch (DbException e) {
            store.closeSilently();
            throw e;
        }
    }

    static FileStore getFileStore(MyDataHandler dh, String name, String mode) {
        return FileStore.open(dh, name, mode);
    }

    static FileStore getSecureFileStore(MyDataHandler dh, String name, String mode) {
        String cipher = "AES";
        byte[] fileEncryptionKey = MathUtils.secureRandomBytes(1264);
        return FileStore.open(dh, name, mode, cipher, fileEncryptionKey, 0);
    }

    static class MyDataHandler implements DataHandler {

        @Override
        public String getDatabasePath() {

            return null;
        }

        @Override
        public FileStore openFile(String name, String mode, boolean mustExist) {

            return null;
        }

        @Override
        public void checkPowerOff() throws DbException {

        }

        @Override
        public void checkWritingAllowed() throws DbException {

        }

        @Override
        public int getMaxLengthInplaceLob() {

            return 0;
        }

        @Override
        public TempFileDeleter getTempFileDeleter() {
            return TempFileDeleter.getInstance();
        }

        @Override
        public Object getLobSyncObject() {

            return null;
        }

        @Override
        public SmallLRUCache<String, String[]> getLobFileListCache() {

            return null;
        }

        @Override
        public LobStorageInterface getLobStorage() {
            return null;
        }

        @Override
        public int readLob(long lobId, byte[] hmac, long offset, byte[] buff, int off, int length) {

            return 0;
        }

        @Override
        public CompareMode getCompareMode() {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
