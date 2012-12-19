import java.util.Date;

import java.net.UnknownHostException;

public class MongoFileSystemTest2 {
    public static void main(String[] args) {
        try {
            //MongoFileSystem mfs = new MongoFileSystem("localhost", "mfs", "test", fs);
            MongoFileSystem mfs2 = new MongoFileSystem("localhost", "mfs", "test");
            //mfs.showElements();
            //System.out.println();
            //mfs.saveDB();
            mfs2.retrieveDB();
            mfs2.showElements();
            mfs2.saveDB();
        } catch (UnknownHostException ex) {
            System.out.println("Unknown Host: " + ex);
        }
    }
}
