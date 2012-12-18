import java.util.Date;

public class FileSystemTest {
    public static void main(String[] args) {
        FileSystem fs = FileSystem.getInstance();
        String owner = "John";
        Directory root = new Directory("root", owner, new Date(2012,10,12), null, fs);
        fs.setRoot(root);
        Directory windows = new Directory("Windows", owner, new Date(2012,10,13), root, fs);
        root.appendChild(windows);
        Directory myDocument = new Directory("MyDocument", owner, new Date(2012,10,14), root, fs);
        root.appendChild(myDocument);
        Directory myPictures = new Directory("MyPictures", owner, new Date(2012,10,15), myDocument, fs);
        myDocument.appendChild(myPictures);

        File a = new File("a", owner, new Date(2012,10,14), windows, 12);
        File b = new File("b", owner, new Date(2012,10,13), windows, 5);
        File c = new File("c", owner, new Date(2012,10,15), windows, 27);
        windows.appendChild(a);
        windows.appendChild(b);
        windows.appendChild(c);
        File d = new File("d", owner, new Date(2012,10,15), myDocument, 15);
        myDocument.appendChild(d);
        File e = new File("e", owner, new Date(2012,10,15), myPictures, 8);
        File f = new File("f", owner, new Date(2012,10,15), myPictures, 4);
        myPictures.appendChild(e);
        myPictures.appendChild(f);
        Link windowsLink = new Link("WindowsLink", owner, new Date(2012,10,15),
                                    myPictures, windows, 10);
        myPictures.appendChild(windowsLink);

        fs.setRoot(root);
        fs.setCurrent(root);

        SizeCountingVisitor visitor = new SizeCountingVisitor();
        root.accept(visitor);
   }
}
