import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader();
        Parent root = fxmlLoader.load(getClass().getResource("layout.fxml").openStream());
        Controller controller = fxmlLoader.getController();

        primaryStage.setTitle("Pirate Bay Offline");
        primaryStage.setMinWidth(680);
        primaryStage.setMinHeight(500);

        Scene scene = new Scene(root);
        scene.getStylesheets().add("style.css");

        primaryStage.setScene(scene);

        scene.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<>() {
            final KeyCombination keyComb = new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN);

            public void handle(KeyEvent ke) {
                if (keyComb.match(ke)) {
                    controller.searchField.requestFocus();
                    ke.consume();
                }
            }
        });

        primaryStage.show(); // goto Controller
    }

    public static void main(String[] args) {
        launch();
    }

}