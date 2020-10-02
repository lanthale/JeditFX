package at.itarchitects.jeditfx;

import java.awt.Desktop;
import java.io.File;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.stage.WindowEvent;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    Parent root;

    private static final String WINDOW_POSITION_X = "Window_Position_X";
    private static final String WINDOW_POSITION_Y = "Window_Position_Y";
    private static final String WINDOW_WIDTH = "Window_Width";
    private static final String WINDOW_HEIGHT = "Window_Height";
    private static final String WINDOW_MAXIMIZED = "Window_Maximized";
    private static final double DEFAULT_X = 100;
    private static final double DEFAULT_Y = 200;
    private static final double DEFAULT_WIDTH = 700;
    private static final double DEFAULT_HEIGHT = 500;
    private static final boolean DEFAULT_MAXIMIZED = false;
    private static final String NODE_NAME = "JeditFX";
    private static final String BUNDLE = "Bundle";
    private FXMLLoader fxmlLoader;
    private static File fileToLoad;
    private static JEditFXController controller;

    /**
     * Listen for OPEN_FILE events while the application is running.
     */
    static {
        if (java.awt.Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
            Desktop.getDesktop().setOpenFileHandler(event -> {
                fileToLoad = event.getFiles().get(0);                
                Platform.runLater(() -> {
                    controller.setFile(event.getFiles().get(0));
                    controller.openFileAction(null);
                });
            });
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Pull the saved preferences and set the stage size and start location

        Preferences pref = Preferences.userRoot().node(NODE_NAME);
        double x = pref.getDouble(WINDOW_POSITION_X, DEFAULT_X);
        double y = pref.getDouble(WINDOW_POSITION_Y, DEFAULT_Y);
        double width = pref.getDouble(WINDOW_WIDTH, DEFAULT_WIDTH);
        double height = pref.getDouble(WINDOW_HEIGHT, DEFAULT_HEIGHT);
        boolean maximized = pref.getBoolean(WINDOW_MAXIMIZED, DEFAULT_MAXIMIZED);
        stage.setMaximized(true);
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setMaximized(maximized);

        stage.setOnCloseRequest((final WindowEvent event) -> {
            Preferences preferences = Preferences.userRoot().node(NODE_NAME);
            preferences.putDouble(WINDOW_POSITION_X, stage.getX());
            preferences.putDouble(WINDOW_POSITION_Y, stage.getY());
            preferences.putDouble(WINDOW_WIDTH, stage.getWidth());
            preferences.putDouble(WINDOW_HEIGHT, stage.getHeight());
            preferences.putBoolean(WINDOW_MAXIMIZED, stage.isMaximized());
            controller = fxmlLoader.getController();
            preferences.putBoolean("WRAPTEXT", controller.getWrapText());
            preferences.putInt("FONTSIZE", controller.getFontSize());
            controller.getExecutor().shutdownNow();
        });

        fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/jeditform.fxml"));
        root = (Parent) fxmlLoader.load();
        scene = new Scene(root, 640, 480);
        scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());

        controller = fxmlLoader.getController();
        controller.setWrapText(pref.getBoolean("WRAPTEXT", false));
        controller.setFontSize(pref.getInt("FONTSIZE", 11));        

        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/icon_256x256.png")));
        stage.show();

        Parameters params = getParameters();
        List<String> list = params.getRaw();
        if (!list.isEmpty()) {
            controller.setFile(new File(params.getRaw().get(0)));
            controller.openFileAction(null);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public static File getFileToLoad() {
        return fileToLoad;
    }

}
