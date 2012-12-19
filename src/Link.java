import java.util.Date;

class Link extends FSElement {
    private FSElement target;

    Link(String name, String owner, Date created, Directory parent, FSElement target, int size) {
        super(parent, created);
        setName(name);
        setOwner(owner);
        setLastModified(created);
        setSize(size);
        //if (target == null)
        //    throw new IllegalArgumentException("target cannot be null");
        this.target = target;
    }

    public boolean isLeaf() {
        return true;
    }

    public String getInfo() {
        return "<Link: " + getName() + ">\n" + this.target.getInfo();
    }

    public FSElement getTarget() {
        return this.target;
    }

    public void setTarget(FSElement target) {
        this.target = target;
    }

    public int getDiskUtil() {
        return 0;
    }

    public void accept(FSVisitor v) {
        v.visit(this);
    }
}
