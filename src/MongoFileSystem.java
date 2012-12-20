import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;
import org.bson.types.ObjectId;
import com.mongodb.DBCursor;

import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;

/**
 * @author      Hans Lo <hansshulo@gmail.com>
 * @version     0.1
 * @since       2012-12-3
 */
public class MongoFileSystem {

    private FileSystem fs;
    /* Associate the FileSystem elements with their corresponding
       database instances */
    private HashMap<FSElement, ObjectId> memoryToMongo;
    private HashMap<ObjectId, FSElement> mongoToMemory;
    private HashMap<Link, ObjectId> linkToTargetID;
    private ArrayList<Link> pendingLinks;

    MongoClient mongoClient;
    DBCollection mongoFS;
    DBCollection metadata;
    DB db;

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
     * This will delete the existing content.
     */
    public void saveDB() {
        // Delete the filesystem
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

    /**
     * Load the filesystem in the database into memory.
     * This will delete the existing content in memory.
     */
    public void retrieveDB() {
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

    /**
     * Make an element on the database.
     */
    public void makeElement(FSElement newElement) {
        ObjectId currentId = memoryToMongo.get(this.fs.getCurrent());
        BasicDBObject newMongoElement = createFSElement(newElement);
        newMongoElement.append("parent",currentId);
        this.mongoFS.insert(newMongoElement);
        this.memoryToMongo.put(newElement, (ObjectId)newMongoElement.get("_id"));
    }

    public void deleteElement(FSElement element) {
        ObjectId elementId = memoryToMongo.get(element);
        BasicDBObject removeQuery = new BasicDBObject("_id", elementId);
        this.mongoFS.remove(removeQuery);
    }


    /**
     * Convenience function to show the whole filesystem tree.
     */
    public void showElements() {
        fs.showAllElements();
    }

    // We perform a depth first traversal on the database filesystem
    // and create the corresponding in-memory object.
    // Along the way we fill the mapping table
    private void loadFileSystem(ObjectId rootId) {
        BasicDBObject rootDir;
        Directory rootDirectory;
        DBCursor cursor = this.mongoFS.find(new BasicDBObject("_id", rootId));
        try {
            while(cursor.hasNext()) {
                this.linkToTargetID = new HashMap<Link, ObjectId>();
                this.mongoToMemory = new HashMap<ObjectId, FSElement>();
                this.memoryToMongo = new HashMap<FSElement, ObjectId>();
                rootDir = (BasicDBObject)cursor.next();
                String elementType = rootDir.getString("type");
                String name = rootDir.getString("name");
                String owner = rootDir.getString("owner");
                Date created = rootDir.getDate("created");
                int size = rootDir.getInt("size");
                rootDirectory = createFSDirectory(rootDir, name, owner, created);
                this.fs.setRoot(rootDirectory);
                this.memoryToMongo.put(rootDirectory, (ObjectId)rootDir.get("_id"));
                Iterator it = this.linkToTargetID.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry entry = (Map.Entry) it.next();
                    Link link = (Link)entry.getKey();
                    ObjectId targetId = (ObjectId)entry.getValue();
                    FSElement target = this.mongoToMemory.get(targetId);
                    link.setTarget(target);
                }
                this.fs.setRoot(rootDirectory);
                this.fs.setCurrent(rootDirectory);
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
        int size = doc.getInt("size");

        if (elementType.equals("file")) {
            newElement = new File(name, owner, created, null, size);
        } else if (elementType.equals("link")) {
            /* At this point, the target may not have been generated yet,
               so we set it to null and deal with it later */
            Link newLink = new Link(name, owner, created, null, null, size);
            this.linkToTargetID.put(newLink, (ObjectId)doc.get("target"));
            newElement = newLink;
        } else if (elementType.equals("directory")) {
            newElement = createFSDirectory(doc, name, owner, created);
        } else {
            System.out.println("Warning: Unknown type in the database");
            newElement = null;
        }
        this.mongoToMemory.put((ObjectId)doc.get("_id"), newElement);
        this.memoryToMongo.put(newElement, (ObjectId)doc.get("_id"));
        return newElement;
    }

    private void createMongoDirectory(Directory directory, BasicDBObject parent) {
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
}
