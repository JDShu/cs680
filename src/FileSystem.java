import java.util.Date;
import java.util.ArrayList;

public class FileSystem {
    private FileSystem(){};
    private Directory root;
    private Directory current;
    private static FileSystem instance = null;

    public static FileSystem getInstance() {
        if (instance == null)
            instance = new FileSystem();
        return instance;
    }

    public void setRoot(Directory root) {
        this.root = root;
    }

    public Directory getRoot() {
        return this.root;
    }

    public void setCurrent(Directory current) {
        this.current = current;
    }

    public Directory getCurrent() {
        return this.current;
    }

    public void addChild(Directory parent, FSElement child, int index) {
        parent.addChild(child, index);
    }

    public FSElement getChild(Directory parent, int index) {
        return parent.getChildren().get(index);
    }

    public String getFSElementInfo(FSElement element) {
        return element.getInfo();
    }

    public ArrayList<String> getCurrentChildrenInfo() {
        ArrayList<String> info = new ArrayList<String>();
        for (FSElement e:current.getChildren()) {
            info.add(e.getInfo());
        }
        return info;
    }

    public void showAllElements() {
        showElements(root,1);
    }

    private void showElements(Directory current, int indent) {
        System.out.println("[" + current.getName() + "]");
        for(FSElement e:current.getChildren()) {
            for (int i = 0; i < indent; i++) {
                System.out.print("    ");
            }

            if (e instanceof Directory) {
                showElements((Directory)e, indent+1);
            } else {
                System.out.println(e.getName());
            }
        }
    }
}
