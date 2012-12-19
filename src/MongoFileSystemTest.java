import java.util.Date;

import java.net.UnknownHostException;

public class MongoFileSystemTest {
    public static void main(String[] args) {
        FileSystem fs = FileSystem.getInstance();
        CommandLine cmd = new CommandLine(fs);
        cmd.main();
    }
}
