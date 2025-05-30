package at.itarchitects.jeditfx;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import jfx.incubator.scene.control.richtext.CodeArea;
import org.kordamp.ikonli.javafx.FontIcon;
import org.mozilla.universalchardet.UniversalDetector;

public class JEditFXController implements Initializable {

    private final FileChooser fileChooser = new FileChooser();
    private FontIcon iconview;
    private FontIcon iconviewDelete;
    private UtilityTools util;
    private int fontSize;
    private HashMap<Integer, Integer> mapFileLines;
    private LongProperty fileLineQuantity;
    private SimpleBooleanProperty isChanged;

    private ResourceBundle resources;
    @FXML
    private Label sizeLabel;
    @FXML
    private Label linesLabel;
    @FXML
    private AnchorPane pane;
    @FXML
    private Button openButton;
    @FXML
    private Button closeButton;
    @FXML
    private Button saveButton;
    @FXML
    private VBox vbox;
    private CodeArea textarea;

    private Stage stage;
    private File file;
    @FXML
    private StackPane mainview;
    @FXML
    private StackPane progressInfo;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private TabPane tabbedPane;
    @FXML
    private Label charsetLabel;
    private String encoding;
    private ExecutorService executor;
    @FXML
    private MenuBar menuBar;
    @FXML
    private CheckBox wrapTextCheckBox;
    @FXML
    private TextField searchField;
    @FXML
    private CheckBox expertSearch;
    @FXML
    private Label searchQuantityLabel;
    @FXML
    private VBox vboxSearchOptions;
    private long linesToRead;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        Logger.getLogger(JEditFXController.class.getName()).log(Level.SEVERE, "Init Controller");
        executor = Executors.newSingleThreadExecutor();
        menuBar.useSystemMenuBarProperty().set(true);
        mapFileLines = new HashMap<>();
        fileLineQuantity = new SimpleLongProperty();
        isChanged = new SimpleBooleanProperty(false);
        searchQuantityLabel.setVisible(false);
        vboxSearchOptions.setAlignment(Pos.CENTER);
        vboxSearchOptions.getChildren().remove(searchQuantityLabel);
        util = new UtilityTools();
        fontSize = 10;
        tabbedPane.getTabs().get(0).setText("Unnamed-1");
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser
                .getExtensionFilters()
                .addAll(
                        new FileChooser.ExtensionFilter("All Files", "*.*"),
                        new FileChooser.ExtensionFilter("Text Files", "*.txt"));
        textarea = new CodeArea();
        textarea.setLineNumbersEnabled(true);
        textarea.setWrapText(wrapTextCheckBox.isSelected());

        iconview = new FontIcon("fa-arrow-circle-left:12");
        iconviewDelete = new FontIcon("fa-trash:12");

        mainview.getChildren().add(textarea);
        textarea.setOnDragOver((DragEvent event) -> {
            if (event.getGestureSource() != textarea
                    && event.getDragboard().hasFiles()) {
                /* allow for both copying and moving, whatever user chooses */
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        textarea.setOnDragDropped((t) -> {
            Dragboard db = t.getDragboard();
            if (db.hasFiles() == true) {
                this.file = db.getFiles().get(0);
                progressInfo.setVisible(true);
                progressLabel.setText("Start reading...");
                textarea.clear();

                Task<Object> task = new Task<Object>() {
                    @Override
                    protected Object call() throws Exception {
                        if (file != null) {
                            Platform.runLater(() -> {
                                tabbedPane.getTabs().get(0).setText(file.getName());
                                tabbedPane.getTabs().get(0).setTooltip(new Tooltip(file.getAbsolutePath()));
                            });
                            String actualReadLine;

                            encoding = UniversalDetector.detectCharset(file);
                            if (encoding == null) {
                                encoding = "ISO-8859-1";
                            }
                            Platform.runLater(() -> {
                                charsetLabel.setText(encoding);
                            });
                            updateMessage("Building file map...");
                            createFileMap();
                            updateMessage("Reading File...");
                            try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName(encoding))) {
                                int count = 0;
                                while ((actualReadLine = reader.readLine()) != null) {
                                    final String txt = actualReadLine;
                                    updateProgress(count, linesToRead);
                                    updateMessage("Line " + count);
                                    textarea.appendText(txt + "\n");
                                    count = count + 1;
                                }
                            }
                        }
                        return true;
                    }
                };
                task.setOnSucceeded((WorkerStateEvent th) -> {
                    isChanged.setValue(Boolean.FALSE);
                    tabbedPane.getTabs().get(0).setText(tabbedPane.getTabs().get(0).getText().substring(1));
                    progressInfo.setVisible(false);
                });
                task.setOnFailed((WorkerStateEvent th) -> {
                    progressInfo.setVisible(false);
                    util.showError("Error reading additional lines from fil " + file.getAbsolutePath(), new Exception(th.getSource().getException()));
                });
                executor.submit(task);
            }
        });
        progressInfo.setVisible(false);
        Logger.getLogger(JEditFXController.class.getName()).log(Level.SEVERE, "Init Controller finished");
    }

    public void init(Stage myStage) {
        this.stage = myStage;
    }

    @FXML
    public void exit() {
        if (textarea.getText().isEmpty()) {
            executor.shutdownNow();
            App.saveSettings((Stage) mainview.getScene().getWindow(), this);
            System.exit(0);
        } else {

            Alert alert = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Exit without saving?",
                    ButtonType.YES,
                    ButtonType.NO,
                    ButtonType.CANCEL
            );

            alert.setTitle("Confirm");
            alert.showAndWait();

            if (alert.getResult() == ButtonType.YES) {
                executor.shutdown();
                App.saveSettings((Stage) pane.getScene().getWindow(), this);
                System.exit(0);
            }
            if (alert.getResult() == ButtonType.NO) {
                saveFileAction(null);
                executor.shutdown();
                App.saveSettings((Stage) pane.getScene().getWindow(), this);
                System.exit(0);
            }
        }
    }

    @FXML
    public void openFileAction(ActionEvent event) {
        Logger.getLogger(JEditFXController.class.getName()).log(Level.SEVERE, "OpenFileAction");
        fileChooser.setTitle("Open File");
        if (file != null) {
            if (file.toString().contains("jeditfx.App")) {
                file = null;
                return;
            }
        }
        if (file == null) {
            file = fileChooser.showOpenDialog(stage);
        }
        progressInfo.setVisible(true);
        progressBar.setVisible(true);
        progressLabel.setText("Start reading...");
        textarea.clear();

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (file != null) {
                    Platform.runLater(() -> {
                        tabbedPane.getTabs().get(0).setText(file.getName());
                        tabbedPane.getTabs().get(0).setTooltip(new Tooltip(file.getAbsolutePath()));
                    });
                    String actualReadLine;

                    encoding = UniversalDetector.detectCharset(file);
                    if (encoding == null) {
                        encoding = "ISO-8859-1";
                    }
                    Platform.runLater(() -> {
                        charsetLabel.setText(encoding);
                    });
                    updateMessage("Building file map...");
                    createFileMap();
                    updateMessage("Reading File...");
                    try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName(encoding))) {
                        int count = 0;
                        while ((actualReadLine = reader.readLine()) != null) {
                            final String txt = actualReadLine;
                            updateProgress(count, linesToRead);
                            updateMessage("Line " + count);
                            textarea.appendText(txt + "\n");
                            count = count + 1;
                        }
                    }
                }
                return null;
            }
        };
        task.setOnSucceeded((t) -> {
            progressInfo.setVisible(false);
        });
        task.setOnFailed((t) -> {
            progressInfo.setVisible(false);
        });
        progressBar.progressProperty().bind(task.progressProperty());
        progressLabel.textProperty().bind(task.messageProperty());
        executor.submit(task);
    }    

    private void createFileMap() throws IOException {
        String actualReadLine;

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName(encoding))) {            
            linesToRead = reader.lines().count();
            Platform.runLater(() -> {
                sizeLabel.setText("Lines: " + linesToRead + "");
                String fSize = String.format("%,d", (file.length() / 1024));
                linesLabel.setText("Size: " + fSize + " KB");
            });            
        }
    }

    @FXML
    private void closeFileAction(ActionEvent event) {
        file = null;
        mainview.getChildren().remove(textarea);
        textarea = new CodeArea();
        textarea.setLineNumbersEnabled(true);
        textarea.setWrapText(wrapTextCheckBox.isSelected());        
        mainview.getChildren().add(textarea);                
        isChanged.setValue(Boolean.FALSE);        
        textarea.clear();
        sizeLabel.setText("Lines: ");
        linesLabel.setText("Size: ");
        charsetLabel.setText("DEFAULT");
        encoding = null;
        tabbedPane.getTabs().get(0).setText("Unnamed-1");
        progressLabel.textProperty().unbind();
        progressBar.progressProperty().unbind();
    }

    @FXML
    private void saveFileAction(ActionEvent event) {
        try {
            fileChooser.setTitle("Save As");
            progressInfo.setVisible(true);
            progressLabel.setText("Saving file...");
            if (file != null) {
                BufferedWriter out = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"), StandardOpenOption.WRITE);
                out.write(textarea.getText());
                out.close();
                Platform.runLater(() -> {
                    tabbedPane.getTabs().get(0).setText(file.getName());
                    tabbedPane.getTabs().get(0).setTooltip(new Tooltip(file.getAbsolutePath()));
                    charsetLabel.setText("UTF-8");
                });
            } else {
                file = fileChooser.showSaveDialog(stage);
                if (file != null) {
                    BufferedWriter out = Files.newBufferedWriter(file.toPath(), Charset.forName("UTF-8"));
                    out.write(textarea.getText());
                    out.close();
                    Platform.runLater(() -> {
                        tabbedPane.getTabs().get(0).setText(file.getName());
                        tabbedPane.getTabs().get(0).setTooltip(new Tooltip(file.getAbsolutePath()));
                        charsetLabel.setText("UTF-8");
                    });
                }
            }
        } catch (FileNotFoundException e) {
            util.showError("Error writing lines to file " + file.getAbsolutePath(), e);
        } catch (IOException e) {
            util.showError("Error writing lines to file " + file.getAbsolutePath(), e);
        }
        progressInfo.setVisible(false);
    }

    public CodeArea getTextarea() {
        return textarea;
    }

    @FXML
    private void FontIncreaseAction(ActionEvent event) {
        fontSize = fontSize + 1;
        String st = "-fx-font-size: " + fontSize + "pt;";
        textarea.setStyle(st);
    }

    @FXML
    private void FontDecreaseAction(ActionEvent event) {
        textarea.setStyle("");
        fontSize = fontSize - 1;
        textarea.setStyle("-fx-font-size: " + fontSize + "pt;");
    }

    public void setFile(File file) {
        this.file = file;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public boolean getWrapText() {
        return wrapTextCheckBox.isSelected();
    }

    public void setWrapText(boolean val) {
        wrapTextCheckBox.setSelected(val);
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
        String st = "-fx-font-size: " + fontSize + "pt;";
        textarea.setStyle(st);
    }

    @FXML
    private void searchAction(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER) {
            searchQuantityLabel.setVisible(false);
            progressLabel.setVisible(true);
            progressLabel.setText("Finding text...");
            /*Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
                @Override
                protected StyleSpans<Collection<String>> call() throws Exception {
                    Pattern pattern;
                    if (expertSearch.isSelected() == true) {
                        pattern = Pattern.compile(searchField.getText());
                    } else {
                        pattern = Pattern.compile("(?i:" + searchField.getText() + ")");
                    }
                    return computeHighlighting(pattern, textarea.getText());
                }
            };
            task.setOnSucceeded((WorkerStateEvent t) -> {
                progressLabel.setVisible(false);
                textarea2.setStyleSpans(0, (StyleSpans<Collection<String>>) t.getSource().getValue());
                for (Paragraph<Collection<String>, String, Collection<String>> p : textarea2.getParagraphs()) {
                    if (p.getStyleSpans().getSpanCount() > 1) {
                        textarea2.showParagraphInViewport(textarea2.getParagraphs().indexOf(p));
                        textarea2.moveTo(textarea2.getParagraphs().indexOf(p), 0);
                        break;
                    }
                }
            });
            task.setOnFailed((WorkerStateEvent t) -> {
                progressLabel.setVisible(false);
            });
            executor.submit(task);*/
        }
    }

    /*private StyleSpans<Collection<String>> computeHighlighting(Pattern patter, String text) {
        Matcher matcher = patter.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder
                = new StyleSpansBuilder<>();
        AtomicInteger m = new AtomicInteger(0);
        while (matcher.find()) {
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton("highlight"), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
            m.addAndGet(1);
        }
        Platform.runLater(() -> {
            if (!searchField.getText().equalsIgnoreCase("")) {
                vboxSearchOptions.getChildren().add(searchQuantityLabel);
                searchQuantityLabel.setVisible(true);
                vboxSearchOptions.setAlignment(Pos.CENTER_LEFT);
                searchQuantityLabel.setText(m + " found");
            } else {
                searchQuantityLabel.setVisible(false);
                vboxSearchOptions.getChildren().remove(searchQuantityLabel);
                vboxSearchOptions.setAlignment(Pos.CENTER);
            }
        });
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }*/
    @FXML
    private void ScrollNextAction(ActionEvent event) {
    }

    @FXML
    private void ScrollPrevAction(ActionEvent event) {
    }

}
