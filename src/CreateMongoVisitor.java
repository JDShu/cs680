import com.mongodb.DBCollection;
import com.mongodb.BasicDBObject;
import java.util.Stack;

public class CreateMongoVisitor implements FSVisitor {
    DBCollection collection;
    Stack<BasicDBObject> mongoDirectoryStack;
    Directory current;

    public CreateMongoVisitor(DBCollection coll, Directory root) {
        this.collection = coll;
        this.current = root;
        this.mongoDirectoryStack = new Stack<BasicDBObject>();
    }

    public void visit(Link link) {
        BasicDBObject newFile = createFSElement(file);
        collection.insert(newFile);
    }

    public void visit(Directory directory) {
        BasicDBObject newFile = createFSElement(file);
        collection.insert(newFile);
        //set mongocurrent
    }

    public void visit(File file) {
        BasicDBObject newFile = createFSElement(file);
        collection.insert(newFile);
    }

    public int getTotalSize() {
        return totalSize;
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
}
