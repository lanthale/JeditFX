package at.itarchitects.jeditfx;

import java.io.File;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
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
    private FXMLLoader fxmlLoader;
    private static File fileToLoad;
    private static JEditFXController controller;

    public App() {
        com.sun.glass.ui.Application glassApp = com.sun.glass.ui.Application.GetApplication();
        glassApp.setEventHandler(new com.sun.glass.ui.Application.EventHandler() {
            @Override
            public void handleOpenFilesAction(com.sun.glass.ui.Application app, long time, String[] filenames) {
                if (filenames.length > 1) {
                    super.handleOpenFilesAction(app, time, filenames);
                    Logger.getLogger("at.itarchitects.jeditfx").log(Level.SEVERE, "Parameters size " + filenames.length);
                    if (controller == null) {
                        Logger.getLogger("at.itarchitects.jeditfx").log(Level.SEVERE, "Getting File to load " + fileToLoad);
                        fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/jeditform.fxml"));
                        try {
                            root = (Parent) fxmlLoader.load();
                        } catch (IOException ex) {
                            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        //scene = new Scene(root, 640, 480);
                        controller = fxmlLoader.getController();
                        Logger.getLogger("at.itarchitects.jeditfx").log(Level.ALL, "Getting File to load " + filenames[0]);
                        Logger.getLogger("at.itarchitects.jeditfx").log(Level.ALL, "Getting File to load " + filenames[1]);                        
                        File targetFile = new File(filenames[1]);
                        Logger.getLogger("at.itarchitects.jeditfx").log(Level.SEVERE, "Getting File to load " + filenames[0]);
                        if (targetFile.isDirectory() == false) {
                            controller.setFile(targetFile);
                            controller.openFileAction(null);
                        }
                    }
                }
            }
        });
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
            saveSettings(stage, controller);
        });

        if (fxmlLoader == null) {
            fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/jeditform.fxml"));
        }
        if (root == null) {
            root = (Parent) fxmlLoader.load();
        }
        if (scene == null) {
            scene = new Scene(root, 640, 480);
        }
        scene.getStylesheets().add(getClass().getResource("/fxml/style.css").toExternalForm());

        if (controller == null) {
            controller = fxmlLoader.getController();
        }
        controller.setWrapText(pref.getBoolean("WRAPTEXT", false));
        controller.setFontSize(pref.getInt("FONTSIZE", 11));

        stage.setScene(scene);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/img/icon_256x256.png")));
        stage.show();

        Parameters params = getParameters();
        List<String> list = params.getRaw();
        if (!list.isEmpty()) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Loading from cmd as param");
            controller.setFile(new File(params.getRaw().get(0)));
            controller.openFileAction(null);
        }
    }

    public static void saveSettings(Stage stage, JEditFXController controller) {
        Preferences preferences = Preferences.userRoot().node(NODE_NAME);
        preferences.putDouble(WINDOW_POSITION_X, stage.getX());
        preferences.putDouble(WINDOW_POSITION_Y, stage.getY());
        preferences.putDouble(WINDOW_WIDTH, stage.getWidth());
        preferences.putDouble(WINDOW_HEIGHT, stage.getHeight());
        preferences.putBoolean(WINDOW_MAXIMIZED, stage.isMaximized());
        preferences.putBoolean("WRAPTEXT", controller.getWrapText());
        preferences.putInt("FONTSIZE", controller.getFontSize());
        controller.getExecutor().shutdownNow();
        try {
            preferences.flush();
        } catch (BackingStoreException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static File getFileToLoad() {
        return fileToLoad;
    }

    /**
     * Returns the application data path, path is returned with ending /
     *
     * @return
     */
    public static String getAppData() {
        String path = "";
        String OS = System.getProperty("os.name").toUpperCase();
        if (OS.contains("WIN")) {
            path = System.getenv("APPDATA");
        } else if (OS.contains("MAC")) {
            path = System.getProperty("user.home") + "/Library/Application Support";
        } else if (OS.contains("NUX")) {
            path = System.getProperty("user.home");
        } else {
            path = System.getProperty("user.dir");
        }

        path = path + File.separator + "JEditFX";
        if (new File(path).exists() == false) {
            new File(path).mkdirs();
        }

        return path;
    }

    public static void main(String[] args) {
        /*Platform.runLater(() -> {
            if (java.awt.Desktop.getDesktop().isSupported(Desktop.Action.APP_OPEN_FILE)) {
                Desktop.getDesktop().setOpenFileHandler(event -> {
                    fileToLoad = event.getFiles().get(0);
                    Logger.getLogger("at.itarchitects.jeditfx").log(Level.SEVERE, "Getting File to load " + fileToLoad);
                    controller.setFile(event.getFiles().get(0));
                    controller.openFileAction(null);
                });
            }
        });*/
        try {
            String appData = getAppData();
            Logger logger = Logger.getLogger("at.itarchitects.jeditfx");
            File logFile = new File(appData + File.separator + "jeditfx.log");
            Handler handler = new FileHandler(logFile.getAbsolutePath(), 50000, 1, false);
            logger.addHandler(handler);
            logger.setLevel(Level.ALL);
            handler.setFormatter(new SimpleFormatter());
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "Starting app");
            launch(args);
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, "finished");
        } catch (IOException | SecurityException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
