import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import com.mongodb.DBCursor;

import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;

/**
 * @author      Hans Lo <hansshulo@gmail.com>
 * @version     0.1
 * @since       2012-12-3
 */
public class MongoFileSystem {

    private FileSystem fs;
    private HashMap<FSElement, ObjectId> memoryToMongo;
    private ArrayList<Link> pendingLinks;

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
    public MongoFileSystem(String address, String db, String fsName) throws UnknownHostException {
        this.fs = FileSystem.getInstance();
        this.mongoClient = new MongoClient(address);
        this.db = mongoClient.getDB(db);
        this.mongoFS = this.db.getCollection(fsName);
        this.metadata = this.db.getCollection(fsName + "_metadata");
    }

    /**
     * Load an existing filesystem from memory and back it with MongoDB.
     *
     * @param address Address of database.
     * @param db Name of the database.
     * @param fsName Name of filesystem collection in the database.
     * @param fs Filesystem to back.
     */

    public MongoFileSystem(String address, String db, String fsName, FileSystem fs) throws UnknownHostException {
        this.fs = fs;
        this.mongoClient = new MongoClient(address);
        this.db = mongoClient.getDB(db);
        this.mongoFS = this.db.getCollection(fsName);
        this.metadata = this.db.getCollection(fsName + "_metadata");
    }

    /**
     * Save the current filesystem in memory to the mongo database.
     */
    public void saveDB() {
        // warning if database alreadt exists, let user confirm that current data will be lost
        //drop db
        this.mongoFS.drop();
        this.metadata.drop();
        this.memoryToMongo = new HashMap<FSElement, ObjectId>();
        this.pendingLinks = new ArrayList<Link>();
        createMongoDirectory(fs.getRoot(), null);
        for (Link link:this.pendingLinks) {

            ObjectId linkId = this.memoryToMongo.get(link);
            ObjectId targetId = this.memoryToMongo.get(link.getTarget());
            BasicDBObject query = new BasicDBObject("_id", linkId);
            BasicDBObject update = new BasicDBObject("$set",
                                                     new BasicDBObject("target", targetId));
            this.mongoFS.update(query, update);
        }
    }

    public void retrieveDB() {
        //find root from db metadata
        BasicDBObject rootQuery = new BasicDBObject("root", new BasicDBObject("$exists", true));
        DBCursor cursor = this.metadata.find(rootQuery);
        DBObject root;
        try {
            while(cursor.hasNext()) {
                root = cursor.next();
                loadFileSystem((ObjectId)root.get("root"));
            }
        } finally {
            cursor.close();
        }
    }

    private void loadFileSystem(ObjectId rootId) {
        BasicDBObject rootDir;
        Directory rootDirectory;
        DBCursor cursor = this.mongoFS.find(new BasicDBObject("_id", rootId));
        try {
            while(cursor.hasNext()) {
                rootDir = (BasicDBObject)cursor.next();
                String elementType = rootDir.getString("type");
                String name = rootDir.getString("name");
                String owner = rootDir.getString("owner");
                Date created = rootDir.getDate("created");
                int size = rootDir.getInt("size");
                rootDirectory = createFSDirectory(rootDir, name, owner, created);
                this.fs.setRoot(rootDirectory);
            }
        } finally {
            cursor.close();
        }
    }

    private Directory createFSDirectory(BasicDBObject directory,
                                        String name,
                                        String owner,
                                        Date created) {
        Directory newDir = new Directory(name, owner, created, null, this.fs);
        ObjectId id = (ObjectId)directory.get("_id");
        BasicDBObject childrenQuery = new BasicDBObject("parent", id);
        DBObject current;
        DBCursor cursor;
        FSElement newElement;
        cursor = this.mongoFS.find(childrenQuery);
        try {
            while(cursor.hasNext()) {
                current = cursor.next();
                newElement = FSElementFromDocument((BasicDBObject)current);
                newElement.setParent(newDir);
                newDir.appendChild(newElement);
            }
        } finally {
            cursor.close();
        }
        return newDir;
    }

    private FSElement FSElementFromDocument(BasicDBObject doc) {
        FSElement newElement;
        String elementType = doc.getString("type");
        String name = doc.getString("name");
        String owner = doc.getString("owner");
        Date created = doc.getDate("created");
        //ObjectId parent = (ObjectId)doc.get("parent");
        int size = doc.getInt("size");

        if (elementType.equals("file")) {
            newElement = new File(name, owner, created, null, size);
        } else if (elementType.equals("link")) {
            /* At this point, the target may not have been generated yet,
               so we set it to null and deal with it later */
            newElement = new Link(name, owner, created, null, null, size);
        } else if (elementType.equals("directory")) {
            newElement = createFSDirectory(doc, name, owner, created);
        } else {
            System.out.println("Warning: Unknown type in the database");
            newElement = null;
        }
        return newElement;
    }

    private void createMongoDirectory(Directory directory, BasicDBObject parent) {
        // do a preorder walk through the filesystem tree
        // preorder operation: create the directory in mongodb
        BasicDBObject newFile = createFSElement(directory);
        if (parent != null) {
            newFile.append("parent", parent.get("_id"));
        }
        else {
            newFile.append("parent", null);
        }
        this.mongoFS.insert(newFile);
        ObjectId dirId = (ObjectId)newFile.get( "_id" );
        this.memoryToMongo.put(directory, dirId);
        if (parent == null) {
            this.metadata.insert(new BasicDBObject("root", dirId));
        }
        for (FSElement e: directory.getChildren()) {
            if (e instanceof Directory) {
                createMongoDirectory((Directory)e, newFile);
            } else {
                BasicDBObject nonDirectoryChild = createFSElement(e);
                nonDirectoryChild.append("parent", newFile.get("_id"));
                this.mongoFS.insert(nonDirectoryChild);
                ObjectId childId = (ObjectId)nonDirectoryChild.get( "_id" );
                this.memoryToMongo.put(e, childId);
                if (e instanceof Link) {
                    this.pendingLinks.add((Link)e);
                }
            }
        }
    }

    public BasicDBObject createFSElement(FSElement element) {
        BasicDBObject newFile = new BasicDBObject("name", element.getName()).
                                           append("owner", element.getOwner()).
                                           append("created", element.getCreated()).
                                           append("last_modified", element.getLastModified()).
                                           append("size", element.getSize());
        if (element instanceof Directory)
            newFile.append("type", "directory");
        else if (element instanceof File)
            newFile.append("type", "file");
        else if (element instanceof Link)
            newFile.append("type", "link");
        return newFile;
    }

    /**
     * Load recreate the filesystem in memory from the database.
     *
     * Note: this will erase the current filesystem that is in memory.
     */
    public void retrieveFromDB() {
        // check if root exists
        // if it doesn't then error

    }

    /**
     * Set the root directory on both the in-memory filesystem and the database.
     *
     * Note: this will erase the current filesystem that is in memory.
     */
    public void setRoot(Directory root) {
        //fs.setRoot();
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
