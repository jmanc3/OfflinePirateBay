import components.MaskerPane;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class UpdateManager {

    public boolean fullUpdateRequired = false;
    public boolean updateAvailable = false;

    public File workingDirectory = null;
    public File appInfoFile = null;
    public File dumpZipFile = null;
    public File dumpFile = null;

    private AppInfo currentAppInfo = null;

    public UpdateManager() {
    }

    public void setupFiles() {
        workingDirectory = new File(Utils.getOS().getUserDataDir("OfflinePirateBay", "1.0", "jmanc3"));
        dumpFile = new File(workingDirectory.getPath() + File.separator + "dump.csv");
        dumpZipFile = new File(workingDirectory.getPath() + File.separator + "dump.csv.gz");
        appInfoFile = new File(workingDirectory.getPath() + File.separator + "appinfo.txt");
    }

    public boolean promptUpdateAvailable() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update Required");
        alert.setHeaderText("You need to update before continuing");
        alert.setContentText("Do you want to update your dump?");

        alert.getButtonTypes().clear();

        alert.getButtonTypes().add(new ButtonType("Yes", ButtonBar.ButtonData.YES));
        alert.getButtonTypes().add(new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE));

        Optional<ButtonType> btOptional = alert.showAndWait();

        return btOptional.map(buttonType -> buttonType.getText().equals("Yes")).orElse(false);
    }

    public void requestUpdate() {
        if (updateAvailable || !appInfoFile.exists() || !dumpFile.exists()) {
//            downloadAndUnzipDump(maskerPane);
        } else {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Update Availability");
            alert.setHeaderText("You already have the latest dump version");
            alert.setContentText("Do you want to re-download everything anyways?");

            alert.getButtonTypes().clear();

            alert.getButtonTypes().add(new ButtonType("Yes", ButtonBar.ButtonData.YES));
            alert.getButtonTypes().add(new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE));

            Optional<ButtonType> btOptional = alert.showAndWait();

            if (btOptional.isPresent()) {
                ButtonType buttonType = btOptional.get();
                if (buttonType.getText().equals("Yes")) {
//                    downloadAndUnzipDump(maskerPane);
                }
            }
        }
    }

    public void loadAppInfo() {
        currentAppInfo = AppInfo.fromFile(appInfoFile);
    }

    public boolean downloadAppInfo(MaskerPane maskerPane) {
        Platform.runLater(()-> {
            maskerPane.setText("Downloading latest info");
            maskerPane.setProgress(-1.0);
        });
        File tempAppInfo = new File(appInfoFile.getPath() + ".temp");
        boolean success = Utils.fromUrlToFile("https://www.dropbox.com/s/h8rhpudln9q4tm1/meta.txt?dl=1", tempAppInfo);

        if (success) {
            boolean renameSuccess = tempAppInfo.renameTo(appInfoFile);

            loadAppInfo();
        }

        return success;
    }

    public boolean downloadDump(MaskerPane maskerPane) {
        Task<Boolean> downloadTask = new Task<>() {
            @Override
            protected Boolean call() {
                try {
                    URLConnection openConnection = new URL(currentAppInfo.url).openConnection();

                    openConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
                    int contentLength = openConnection.getContentLength();

                    InputStream initialStream = openConnection.getInputStream();
                    File tempDumpZipFile = new File(dumpZipFile.getPath() + ".temp");

                    OutputStream outStream = new FileOutputStream(tempDumpZipFile);

                    Platform.runLater(() -> {
                        maskerPane.setText("Downloading dump from: " + currentAppInfo.url + " (" + Utils.formatBytes(contentLength, 3) + ")");
                    });

                    byte[] buffer = new byte[8 * 1024];
                    int bytesRead;
                    int totalBytesRead = 0;

                    while ((bytesRead = initialStream.read(buffer)) != -1) {
                        totalBytesRead += bytesRead;
                        int finalTotalBytesRead = totalBytesRead;
                        updateProgress(finalTotalBytesRead, contentLength);

                        outStream.write(buffer, 0, bytesRead);
                    }
                    initialStream.close();
                    outStream.close();

                    boolean renameSuccess = tempDumpZipFile.renameTo(dumpZipFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
        };

        downloadTask.progressProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> maskerPane.setProgress((Double) newValue));
        });

        downloadTask.run();

        return dumpZipFile.exists();
    }

    public boolean unzipDump(MaskerPane maskerPane) {
        Platform.runLater(() -> {
            maskerPane.setText("Extracting " + dumpZipFile.getPath());
            maskerPane.setProgress(-1.0);
        });
        try {
            File outFile = new File(dumpZipFile.getParent(), dumpZipFile.getName().replaceAll("\\.gz$", "") + ".temp");
            try (GZIPInputStream gin = new GZIPInputStream(new FileInputStream(dumpZipFile)); FileOutputStream fos = new FileOutputStream(outFile)) {
                byte[] buf = new byte[100000];
                int len;
                while ((len = gin.read(buf)) > 0) {
                    fos.write(buf, 0, len);
                }
            }
            boolean renameSuccess = outFile.renameTo(dumpFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean fullDownloadAndUnzip(MaskerPane maskerPane) {
        boolean success = downloadAppInfo(maskerPane);
        if (!success) {
            return false;
        }
        success = downloadDump(maskerPane);
        if (!success) {
            return false;
        }
        success = unzipDump(maskerPane);

        return success;
    }
}
