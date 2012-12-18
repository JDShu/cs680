public class SizeCountingVisitor implements FSVisitor {
    private int totalSize = 0;

    public void visit(Link link) {
        totalSize += link.getDiskUtil();
    }

    public void visit(Directory directory) {
        totalSize += directory.getDiskUtil();
    }

    public void visit(File file) {
        totalSize += file.getDiskUtil();
    }

    public int getTotalSize() {
        return totalSize;
    }
}
