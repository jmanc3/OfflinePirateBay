import components.ButtonColorLerper;
import components.MaskerPane;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import uriSchemeHandler.URISchemeHandler;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.CharBuffer;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public TextField searchField;
    public CheckBox caseSensitiveCheckBox;
    public CheckBox regexCheckBox;
    public StackPane stackPanel;
    public TableView tableView;
    public Button openTorrentButton;
    public Button copyMagnetButton;
    public Button copyInfoHashButton;
    public Button googleTorrentButton;
    public Text statusText;
    public Text tooltipsText;
    public Button searchOffline;
    public Button updateButton;

    private MaskerPane maskerPane = new MaskerPane();
    private UpdateManager updateManager = new UpdateManager();
    private DumpManager dumpManager = new DumpManager();
    private boolean stillBootingUp = true;
    private boolean shouldDoASearchAfterBoot = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        regexCheckBox.setOnMouseEntered(event -> tooltipsText.setText("If checked, search will use Regex instead of the default behaviour (search by keywords) to find matches"));
        caseSensitiveCheckBox.setOnMouseEntered(event -> tooltipsText.setText("If checked, search will take capitalization into account"));

        ButtonColorLerper.lerp(openTorrentButton, tooltipsText, "Opens selected torrent in default magnet handler");
        ButtonColorLerper.lerp(copyMagnetButton, tooltipsText, "Copies magnet link to clipboard");
        ButtonColorLerper.lerp(copyInfoHashButton, tooltipsText, "Copies the info hash to clipboard");
        ButtonColorLerper.lerp(googleTorrentButton, tooltipsText, "Googles the specified torrent");
        ButtonColorLerper.lerp(searchOffline, tooltipsText, "Search for torrent in dump");
        ButtonColorLerper.lerp(updateButton, tooltipsText, "Update dump");

        stackPanel.getChildren().add(maskerPane);

        maskerPane.setText("");

        tableView.setPlaceholder(new Label(""));

        TableColumn<DumpManager.LineIndexesMeta, CharBuffer> dateCol = new TableColumn<>("Date");

        TableColumn<DumpManager.LineIndexesMeta, CharBuffer> nameCol = new TableColumn<>("Name");

        TableColumn<DumpManager.LineIndexesMeta, String> sizeCol = new TableColumn<>("Size");

        setTableViewColumns(dateCol, nameCol, sizeCol);

        Utils.executorService.submit(new Task<Void>() {
            @Override
            protected Void call() {
                afterStageShown();
                return null;
            }
        });
    }

    public void afterStageShown() {
        updateManager.setupFiles();

        Platform.runLater(() -> {
            maskerPane.setText("Checking status");
        });

        out:
        if (!updateManager.workingDirectory.exists()) {
            updateManager.workingDirectory.mkdirs();

            boolean success = updateManager.fullDownloadAndUnzip(maskerPane);

            if (success) {
                success = dumpManager.loadDump(updateManager.dumpFile, statusText, maskerPane);

                dumpManager.loadRequired = !success;
            } else {
                updateManager.fullUpdateRequired = true;
            }
        } else if (updateManager.dumpFile.exists()) {
            boolean success = dumpManager.loadDump(updateManager.dumpFile, statusText, maskerPane);

            dumpManager.loadRequired = !success;
        } else if (updateManager.dumpZipFile.exists()) {
            boolean success = updateManager.unzipDump(maskerPane);
            if (success) {
                success = dumpManager.loadDump(updateManager.dumpFile, statusText, maskerPane);

                dumpManager.loadRequired = !success;
            } else {
                updateManager.fullUpdateRequired = true;
            }
        } else if (updateManager.appInfoFile.exists()) {
            updateManager.loadAppInfo();

            boolean success = updateManager.downloadDump(maskerPane);
            if (!success) {
                updateManager.fullUpdateRequired = true;
                break out;
            }
            success = updateManager.unzipDump(maskerPane);

            if (success) {
                success = dumpManager.loadDump(updateManager.dumpFile, statusText, maskerPane);

                dumpManager.loadRequired = !success;
            } else {
                updateManager.fullUpdateRequired = true;
            }
        } else {
            boolean success = updateManager.fullDownloadAndUnzip(maskerPane);

            if (success) {
                success = dumpManager.loadDump(updateManager.dumpFile, statusText, maskerPane);

                dumpManager.loadRequired = !success;
            } else {
                updateManager.fullUpdateRequired = true;
            }
        }

        stillBootingUp = false;

        Platform.runLater(() -> {
            maskerPane.setVisible(false);
            maskerPane.setProgress(-1.0);
        });

        if (shouldDoASearchAfterBoot) {
            Platform.runLater(this::search);
        }
    }

    public void openTorrentSettings() {

    }

    @FXML
    private void search() {
        if (stillBootingUp) {
            shouldDoASearchAfterBoot = true;
            statusText.setText("Status: Will perform your search after files are loaded");
        } else if (updateManager.fullUpdateRequired) {
            boolean wantsTo = updateManager.promptUpdateAvailable();

            if (wantsTo) {
                boolean success = updateManager.fullDownloadAndUnzip(maskerPane);

                if (success) {
                    success = dumpManager.loadDump(updateManager.dumpFile, statusText, maskerPane);

                    dumpManager.loadRequired = !success;
                } else {
                    updateManager.fullUpdateRequired = true;
                }
            }
        } else if (dumpManager.loadRequired) {
            Platform.runLater(() -> {
                maskerPane.setVisible(true);
                maskerPane.setProgress(-1.0);
            });

            boolean success = dumpManager.loadDump(updateManager.dumpFile, statusText, maskerPane);

            if (!success) {
                statusText.setText("Status: Something seems to be wrong when loading the dump. You might want to try a full dump update.");
            }

            Platform.runLater(() -> {
                maskerPane.setVisible(false);
            });
        } else {
            statusText.setText("Status: Searching...");
            dumpManager.regex = regexCheckBox.isSelected();
            dumpManager.caseSensitive = caseSensitiveCheckBox.isSelected();
            List<DumpManager.LineIndexesMeta> results = dumpManager.search(dumpManager, searchField.getText());

            tableView.getColumns().clear();

            ObservableList<DumpManager.LineIndexesMeta> lineIndexes = FXCollections.observableArrayList();
            lineIndexes.addAll(results);

            tableView.setItems(lineIndexes);

            TableColumn<DumpManager.LineIndexesMeta, CharBuffer> dateCol = new TableColumn<>("Date");
            dateCol.setCellValueFactory(p -> new SimpleObjectProperty<>(
                    dumpManager.data.subSequence(p.getValue().startIndex, p.getValue().startIndex + 20)
            ));

            TableColumn<DumpManager.LineIndexesMeta, CharBuffer> nameCol = new TableColumn<>("Name");
            nameCol.setCellValueFactory(p -> new SimpleObjectProperty<>(
                    dumpManager.data.subSequence(p.getValue().firstQuote + 1, p.getValue().secondQuote)
            ));

            TableColumn<DumpManager.LineIndexesMeta, String> sizeCol = new TableColumn<>("Size");
            sizeCol.setCellValueFactory(p -> new SimpleObjectProperty<>(
                    Utils.formatBytes(Float.valueOf(String.valueOf(
                            dumpManager.data.subSequence(p.getValue().secondQuote + 2,
                                    p.getValue().startIndex + p.getValue().length))), 1)
            ));

            setTableViewColumns(dateCol, nameCol, sizeCol);

            Platform.runLater(() -> tableView.refresh());

            statusText.setText("Found: " + results.size());
        }
    }

    private void setTableViewColumns(TableColumn<DumpManager.LineIndexesMeta, CharBuffer> dateCol, TableColumn<DumpManager.LineIndexesMeta, CharBuffer> nameCol, TableColumn<DumpManager.LineIndexesMeta, String> sizeCol) {
        dateCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.2));
        sizeCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.15));
        nameCol.prefWidthProperty().bind(tableView.widthProperty().multiply(0.65));

        dateCol.setResizable(false);
        nameCol.setResizable(false);
        sizeCol.setResizable(false);

        tableView.getColumns().addAll(nameCol, sizeCol, dateCol);
    }

    public void openTorrent() {
        DumpManager.LineIndexesMeta selectedItem = (DumpManager.LineIndexesMeta) tableView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            String info = Utils.base64toHex(dumpManager.data.subSequence(selectedItem.startIndex + 21, selectedItem.startIndex + 49));

            Utils.executorService.submit(() -> {
                try {
                    String magnetLink = "magnet:?xt=urn:btih:" + info;

                    URI magnetLinkUri = new URI(magnetLink);
                    URISchemeHandler uriSchemeHandler = new URISchemeHandler();
                    uriSchemeHandler.open(magnetLinkUri);
                    Platform.runLater(() -> statusText.setText("Should've opened..."));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            statusText.setText("No torrent selected");
        }
    }

    public void copyMagnet() {
        DumpManager.LineIndexesMeta selectedItem = (DumpManager.LineIndexesMeta) tableView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            String info = Utils.base64toHex(dumpManager.data.subSequence(selectedItem.startIndex + 21, selectedItem.startIndex + 49));

            String magnetLink = "magnet:?xt=urn:btih:" + info;

            StringSelection selection = new StringSelection(magnetLink);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);

            statusText.setText("Magnet copied: " + magnetLink);
        } else {
            statusText.setText("No torrent selected");
        }
    }

    public void copyInfoHash() {
        DumpManager.LineIndexesMeta selectedItem = (DumpManager.LineIndexesMeta) tableView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            String info = Utils.base64toHex(dumpManager.data.subSequence(selectedItem.startIndex + 21, selectedItem.startIndex + 49));

            StringSelection selection = new StringSelection(info);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, selection);


            statusText.setText("Hash copied: " + info);
        } else {
            statusText.setText("No torrent selected");
        }
    }

    public void googleTorrent() {
        DumpManager.LineIndexesMeta selectedItem = (DumpManager.LineIndexesMeta) tableView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            String info = Utils.base64toHex(dumpManager.data.subSequence(selectedItem.startIndex + 21, selectedItem.startIndex + 49));

            statusText.setText("Searched: " + info);

            Utils.executorService.submit(new Task<Void>() {
                @Override
                protected Void call() {
                    try {
                        Utils.openWebpage(new URL("https://www.google.com/search?q=" + info));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            });
        } else {
            statusText.setText("No torrent selected");
        }
    }

    public void updateDump() {
        updateManager.requestUpdate();
    }
}
