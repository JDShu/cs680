import java.util.Date;

class File extends FSElement {
    File(String name, String owner, Date created, Directory parent, int size) {
        super(parent, created);
        setName(name);
        setOwner(owner);
        setLastModified(created);
        setSize(size);
    }

    public boolean isLeaf() {
        return true;
    }

    public String getInfo() {
        return "Name: " + getName() + "\nType: File\nSize: " +
            getSize() + "\nOwner: " + getOwner();
    }

    public int getDiskUtil() {
        return getSize();
    }

    public void accept(FSVisitor v) {
        v.visit(this);
    }
}
