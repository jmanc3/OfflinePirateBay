import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.impl.*;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Utils {

    public static ExecutorService executorService = Executors.newCachedThreadPool(r -> {
        Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    public static AppDirs getOS() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.startsWith("mac os x")) {
            return new MacOSXAppDirs();
        } else if (os.startsWith("windows")) {
            WindowsFolderResolver folderResolver = new ShellFolderResolver();
            return new WindowsAppDirs(folderResolver);
        } else {
            return new UnixAppDirs();
        }
    }

    public static boolean fromUrlToFile(String url, File file) {
        URLConnection openConnection = null;
        try {
            openConnection = new URL(url).openConnection();
            openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
            try (InputStream in = openConnection.getInputStream()) {
                Files.copy(in, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            System.out.println("No Internet Detected");
            return false;
        }
    }

    public static String formatBytes(float bytes, int digits) {
        String[] dictionary = {"bytes", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
        int index;
        for (index = 0; index < dictionary.length; index++) {
            if (bytes < 1024) {
                break;
            }
            bytes = bytes / 1024;
        }
        return String.format("%." + digits + "f", bytes) + " " + dictionary[index];
    }

    public static String base64toHex(CharBuffer charBuffer) {
        byte[] decode = Base64.getDecoder().decode(charBuffer.toString());
        return toHex(decode);
    }

    private static final char[] DIGITS
            = {'0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final String toHex(byte[] data) {
        final StringBuffer sb = new StringBuffer(data.length * 2);
        for (int i = 0; i < data.length; i++) {
            sb.append(DIGITS[(data[i] >>> 4) & 0x0F]);
            sb.append(DIGITS[data[i] & 0x0F]);
        }
        return sb.toString();
    }

    private static boolean openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean openWebpage(URL url) {
        try {
            return openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

}
