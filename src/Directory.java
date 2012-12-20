import java.util.ArrayList;
import java.util.Date;

class Directory extends FSElement {
    private ArrayList<FSElement> children;
    private FileSystem fs;

    Directory(String name, String owner, Date created, Directory parent, FileSystem fs) {
        super(parent, created);
        setName(name);
        setOwner(owner);
        setLastModified(created);
        setSize(0);
        this.children = new ArrayList<FSElement>();
        this.fs = fs;
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public String getInfo() {
        return "Name: " + getName() + "\nType: Directory\nSize: " +
            getSize() + "\nOwner: " + getOwner();
    }

    public int getSize() {
        int total = 0;
        for (FSElement e:children) {
            total += e.getSize();
        }
        return total;
    }

    public ArrayList<FSElement> getChildren() {
        return this.children;
    }

    public void appendChild(FSElement child) {
        this.children.add(child);
    }

    public void addChild(FSElement child, int index) {
        this.children.add(index, child);
    }

    public void removeChild(FSElement element) {
        this.children.remove(element);
    }

    public int getDiskUtil() {
        return 0;
    }

    public void accept(FSVisitor v) {
        v.visit(this);
        for (FSElement e:this.children)
            e.accept(v);
    }
}
