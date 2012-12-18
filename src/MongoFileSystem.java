import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;

import java.util.Stack;

/**
 * @author      Hans Lo <hansshulo@gmail.com>
 * @version     0.1
 * @since       2012-12-3
 */
public class MongoFileSystem {

    private FileSystem fs;
    MongoClient mongoClient;
    DBCollection mongoFS;
    DBCollection metadata;
    DB db;

    /**
     * Create a new filesystem that is backed by MongoDB. If it
     * already exists, then load it into memory.
     *
     * @param address Address of database.
     * @param db Name of the database.
     * @param fsName Name of filesystem collection in the database.
     */
    public MongoFileSystem(String address, String db, String fsName) {
        this.fs = FileSystem.getInstance();
        this.mongoClient = new MongoClient(address);
        this.db = mongoClient.getDB(db);
        this.mongoFS = db.getCollection(fsName);
        this.metadata = db.getCollection(fsName + "_metadata");
        // TODO: implement loading
    }

    /**
     * Load an existing filesystem from memory and back it with MongoDB.
     *
     * @param address Address of database.
     * @param db Name of the database.
     * @param fsName Name of filesystem collection in the database.
     * @param fs Filesystem to back.
     */
    public MongoFileSystem(String address, String db, String fsName, Filesystem fs) {
        this.fs = fs;
    }

    /**
     * Save the current filesystem in memory to the mongo database.
     */
    public void saveDB() {
        // warning if database alreadt exists, let user confirm
        createMongoDirectory(fs.getRoot());
    }

    private void createMongoDirectory(Directory directory) {
        // Use visitor pattern? no because it requires some messy context

        // do a preorder walk through the filesystem tree
        // preorder operation: create the directory in mongodb
        BasicDBObject newFile = createFSElement(directory);

        for (FSElement e: directory.getChildren()) {
            if (e instanceof Directory) {
                createMongoDirectory((Directory)e);
            } else {

            }
        }

    }

    public BasicDBObject createFSElement(FSElement element) {
        BasicDBObject newFile = new BasicDBObject("name", element.getName()).
                                           append("owner", element.getName()).
                                           append("created", element.getCreated()).
                                           append("last_modified", element.getLastModified()).
                                           append("size", element.getSize()).
                                           append("parent", mongoCurrent);

        return newFile;
    }

    /**
     * Load recreate the filesystem in memory from the database.
     *
     * Note: this will erase the current filesystem that is in memory.
     */
    public void retrieveFromDB() {
    }

    /**
     * Set the root directory on both the in-memory filesystem and the database.
     *
     * Note: this will erase the current filesystem that is in memory.
     */
    public void setRoot(Directory root) {
        fs.setRoot();
        // TODO: Change the mongodb root metadata.
    }

    public Directory getRoot() {
        return fs.getRoot();
    }

    public void setCurrent(Directory current) {
        fs.setCurrent(current);
    }

    public Directory getCurrent() {
        return fs.getCurrent();
    }

    /**
     * Add the file system element to both the filesystem in memory and the database.
     *
     * Note: this will erase the current filesystem that is in memory.
     */
    public void addChild(Directory parent, FSElement child, int index) {
        fs.addChild(parent, child, index);
        // TODO: update mongodb so that the entry is added and dir tree is updated
    }

    public FSElement getChild(Directory parent, int index) {
        return fs.getChild(parent, index);
    }

    public String getFSElementInfo(FSElement element) {
        return fs.getFSElementInfo(element);
    }

    public ArrayList<String> getCurrentChildrenInfo() {
        return fs.getCurrentChildrenInfo();
    }

    public void showElements() {
        fs.showAllElements();
    }

}
