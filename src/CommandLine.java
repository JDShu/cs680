import java.util.Date;
import java.util.Scanner;
import java.util.LinkedList;
import java.net.UnknownHostException;

public class CommandLine {
    FileSystem fs;
    private MongoFileSystem mongoFs;
    private String address;
    private String db;
    private String fsName;

    public CommandLine(FileSystem fs) {
        this.fs = fs;
    }

    public void main() {
        Scanner userInput = new Scanner(System.in).useDelimiter("\n");
        String input;
        String[] command;
        System.out.println("Username: ");
        String user = userInput.next();
        System.out.println("Welcome " + user + "!");
        Directory root = new Directory("root", user, new Date(), null, fs);
        this.fs.setRoot(root);
        this.fs.setCurrent(root);
        do {
            System.out.print(">>> ");
            input = userInput.next();
            command = input.split(" ");

            if (command[0].equals("pwd")) {
                LinkedList<String> path = new LinkedList<String>();
                FSElement current = fs.getCurrent();
                do {
                    path.addFirst(current.getName());
                    current = current.getParent();
                } while (current != null);

                for (String dir:path)
                    System.out.print("/" + dir);
                System.out.println();
            }
            else if (command[0].equals("cd"))
                if (command.length == 1)
                    fs.setCurrent(fs.getRoot());
                else {
                    String[] path = command[1].split("/");
                    Directory current = fs.getCurrent();
                    boolean success = true;
                    for (int i = 0; i < path.length-1; i++) {
                        current = getDirectoryFromName(path[i], current);
                        if (current == null) {
                            System.out.println("No such file or directory: " + command[1]);
                            success = false;
                            break;
                        }
                    }
                    if (success) {
                        Directory d = getDirectoryFromName(path[path.length-1], current);
                        if (d != null)
                            fs.setCurrent(d);
                        else
                            System.out.println("No such directory: " + command[1]);
                    }
                }
            else if (command[0].equals("ls")) {
                for (FSElement e:fs.getCurrent().getChildren()) {
                    System.out.println(e.getName());
                }
            }
            else if (command[0].equals("dir"))
                if (command.length == 1) {
                    for (String info:fs.getCurrentChildrenInfo()) {
                        System.out.println(info);
                        System.out.println("---");
                    }
                }
                else {
                    String[] path = command[1].split("/");
                    Directory current = fs.getCurrent();
                    boolean success = true;
                    for (int i = 0; i < path.length-1; i++) {
                        current = getDirectoryFromName(path[i], current);
                        if (current == null) {
                            System.out.println("No such file or directory: " + command[1]);
                            success = false;
                            break;
                        }
                    }
                    if (success) {
                        FSElement element = getElementFromName(path[path.length-1], current);
                        if (element != null)
                            System.out.println(fs.getFSElementInfo(element));
                        else
                            System.out.println("No such file or directory: " + command[1]);
                    }
                }
            else if (command[0].equals("mkdir"))
                if (command.length == 1)
                    System.out.println("Invalid command: Must specify a name for new directory");
                else {
                    Directory newDir = new Directory(command[1],
                                                     user,
                                                     new Date(),
                                                     fs.getCurrent(),
                                                     fs);
                    fs.addChild(fs.getCurrent(),
                                newDir,
                                fs.getCurrent().getChildren().size());
                    if (this.mongoFs != null) {
                        this.mongoFs.makeElement(newDir);
                    }
                }
            else if (command[0].equals("mkfl")) {
                if (command.length < 3)
                    System.out.println("Syntax: mkfl <file name> <file size>");
                else {
                    int size = Integer.parseInt(command[2]);
                    File newFile = new File(command[1],
                                            user,
                                            new Date(),
                                            fs.getCurrent(),
                                            size);
                    fs.addChild(fs.getCurrent(),
                                newFile,
                                fs.getCurrent().getChildren().size());
                    if (this.mongoFs != null) {
                        this.mongoFs.makeElement(newFile);
                    }
                }
            }
            else if (command[0].equals("rm")) {
                if (command.length < 2) {
                    System.out.println("Syntax: rm <file>");
                } else {
                    FSElement element = getElementFromName(command[1],
                                                           fs.getCurrent());
                    if (element != null) {
                        if (this.mongoFs != null) {
                            this.mongoFs.deleteElement(element);
                        }
                        element.getParent().removeChild(element);
                    }
                    else
                        System.out.println("No such file!");
                }

            }
            else if (command[0].equals("mongo")) {
                handleMongoCommand(command);
            }
        } while (!(command[0].equals("exit")));

    }

    private void handleMongoCommand(String[] command) {
        if (command.length == 1) {
            System.out.println("MongoFS Options: save, load, use, unuse" );
        } else if (command[1].equals("save")) {
            if (this.mongoFs != null)
                this.mongoFs.saveDB();

            else
                System.out.println("No database configured.");
        } else if (command[1].equals("load")){
            if (this.mongoFs != null)
                this.mongoFs.retrieveDB();
            else
                System.out.println("No database configured.");
        } else if (command[1].equals("use")) {
            if (command.length != 5)
                System.out.println("Syntax: mongo use <db address> <db name> <filesystem name>");
            else {
                this.address = command[2];
                this.db = command[3];
                this.fsName = command[4];
                 try {
                     this.mongoFs = new MongoFileSystem(this.address, this.db, this.fsName, this.fs);
                     System.out.println(this.address);
                     System.out.println(this.db);
                     System.out.println(this.fsName);
                 } catch (UnknownHostException e) {
                     System.out.println("Could not access filesystem: " + e);
                 }
            }
        } else if (command[1].equals("unuse")) {
            this.mongoFs = null;
        }
    }

    private Directory getDirectoryFromName(String name, Directory current) {
        if (name.equals("..")) {
            return current.getParent();
        }
        for(FSElement c:current.getChildren()) {
            if (c.getName().equals(name)) {
                FSElement target = c;
                while(target instanceof Link) {
                    target = ((Link)target).getTarget();
                }
                if (target instanceof Directory) {
                    return (Directory)target;
                }
            }
        }
        return null;
    }

    private FSElement getElementFromName(String name, Directory current) {
        if (name.equals("..")) {
            return current.getParent();
        }
        for(FSElement c:current.getChildren()) {
            if (c.getName().equals(name))
                return c;
        }
        return null;
    }
}
