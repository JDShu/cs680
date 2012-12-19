import java.util.Date;

public abstract class FSElement {
    private String name;
    private String owner;
    private Date created;
    private Date lastModified;
    private int size;
    private Directory parent;

    FSElement(Directory parent, Date created) {
        this.parent = parent;
        this.created = created;
    }

    public Directory getParent() {
        return parent;
    }

    public void setParent(Directory parent) {
        this.parent = parent;
    }

    abstract public boolean isLeaf();
    abstract public String getInfo();
    abstract public int getDiskUtil();
    abstract public void accept(FSVisitor visitor);

    public int getSize() {
        return this.size;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setLastModified(Date date) {
        this.lastModified = date;
    }

    public Date getLastModified() {
        return this.lastModified;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }
    public String getOwner() {
        return this.owner;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
