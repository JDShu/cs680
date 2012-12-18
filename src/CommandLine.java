import java.util.Date;
import java.util.Scanner;
import java.util.LinkedList;

public class CommandLine {
    FileSystem fs;

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
                    fs.addChild(fs.getCurrent(),
                                new Directory(command[1],
                                              user,
                                              new Date(),
                                              fs.getCurrent(),
                                              fs),
                                fs.getCurrent().getChildren().size());
                }

        } while (!(command[0].equals("exit")));

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
