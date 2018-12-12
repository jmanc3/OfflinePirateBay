import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class AppInfo {
    public int version;
    public String url;
    public File file;

    public static AppInfo fromFile(File file) {
        AppInfo appInfo = null;
        try {
            appInfo = new AppInfo();
            Scanner scanner = new Scanner(file);
            appInfo.version = Integer.parseInt(scanner.nextLine());
            appInfo.url = scanner.nextLine();
            appInfo.file = file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return appInfo;
    }
}
