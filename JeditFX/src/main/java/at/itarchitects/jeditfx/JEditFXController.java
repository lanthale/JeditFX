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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.Paragraph;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
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
    private int startViewFileLine;
    private int endViewFileLine;

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
    private VirtualizedScrollPane<CodeArea> panev;
    private File file;
    private long filePos;
    @FXML
    private StackPane mainview;
    @FXML
    private StackPane progressInfo;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    private SimpleDoubleProperty doubleProgress;
    private SimpleStringProperty stringProperty;
    @FXML
    private TabPane tabbedPane;
    @FXML
    private Label charsetLabel;
    private String encoding;
    private int maxLineReadAhead;
    private int lastDeletedLineQTY;
    private ExecutorService executor;
    @FXML
    private MenuBar menuBar;
    private long linesToRead;

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
        //maxLineReadAhead = 1000;
        maxLineReadAhead = 10000000;
        lastDeletedLineQTY = 0;
        startViewFileLine = 1;
        endViewFileLine = 1;
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
        textarea.wrapTextProperty().bindBidirectional(wrapTextCheckBox.selectedProperty());
        textarea.setParagraphGraphicFactory(LineNumberFactory.get(textarea));
        textarea.plainTextChanges().observe((t) -> {
            if (file != null) {
                isChanged.setValue(Boolean.TRUE);
                if (!tabbedPane.getTabs().get(0).getText().contains("*")) {
                    tabbedPane.getTabs().get(0).setText("*" + tabbedPane.getTabs().get(0).getText());
                }
            }
        });
        panev = new VirtualizedScrollPane<>(textarea);
        iconview = new FontIcon("fa-arrow-circle-left:12");
        iconviewDelete = new FontIcon("fa-trash:12");

        mainview.getChildren().add(panev);
        filePos = 0;
        doubleProgress = new SimpleDoubleProperty();
        stringProperty = new SimpleStringProperty();
        progressBar.progressProperty().bindBidirectional(doubleProgress);
        stringProperty.addListener((o) -> {
            Platform.runLater(() -> {
                progressLabel.setText(stringProperty.getValue());
            });
        });
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
                //progressBar.setProgress(0);
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
                            readText(maxLineReadAhead, 0, "END");
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

        Task<Object> task = new Task<Object>() {
            @Override
            protected Object call() throws Exception {
                if (file != null) {
                    Platform.runLater(() -> {
                        tabbedPane.getTabs().get(0).setText(file.getName());
                        tabbedPane.getTabs().get(0).setTooltip(new Tooltip(file.getAbsolutePath()));
                    });
                    readText(maxLineReadAhead, 0, "END");
                }
                return true;
            }
        };
        task.setOnSucceeded((t) -> {
            progressInfo.setVisible(false);
        });
        task.setOnFailed((t) -> {
            progressInfo.setVisible(false);
        });
        progressBar.progressProperty().bind(task.progressProperty());
        executor.submit(task);
    }

    private void readText(int linesToRead, long start, String insertPos) throws IOException {
        String actualReadLine;
        if (start == 0) {
            encoding = UniversalDetector.detectCharset(file);
            if (encoding == null) {
                encoding = "ISO-8859-1";
            }
            Platform.runLater(() -> {
                charsetLabel.setText(encoding);
            });
            Platform.runLater(() -> {
                progressBar.setVisible(true);
                progressLabel.setText("Building file map...");
            });
            createFileMap();
        }
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName(encoding))) {
            reader.skip(start);
            int count = 0;
            while ((actualReadLine = reader.readLine()) != null) {
                if (count >= linesToRead) {
                    return;
                } else {
                    filePos = filePos + actualReadLine.length() + "\n".length();
                    final String txt = actualReadLine;
                    if (linesToRead > 1) {
                        final double pVal = (double) count / linesToRead;
                        final String pstr = 100 * (double) count / linesToRead + "%";

                    }
                    Platform.runLater(() -> {
                        if (insertPos.equalsIgnoreCase("END")) {
                            textarea.appendText(txt + "\n");
                        }
                        if (insertPos.equalsIgnoreCase("BEGIN")) {
                            startViewFileLine = startViewFileLine - 1;
                            textarea.insertText(0, txt + "\n");
                        }
                    });
                }
                count = count + 1;
            }
        }

    }

    private void createFileMap() throws IOException {
        String actualReadLine;

        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), Charset.forName(encoding))) {
            int count = 1;
            linesToRead = reader.lines().count();
            Platform.runLater(() -> {
                sizeLabel.setText("Lines: " + linesToRead + "");
                String fSize = String.format("%,d", (file.length() / 1024));
                linesLabel.setText("Size: " + fSize + " KB");
            });
            while ((actualReadLine = reader.readLine()) != null) {

                mapFileLines.put(count, actualReadLine.length() + "\n".length());

                //doubleProgress.setValue((double) count / linesToRead);
                //stringProperty.setValue(100 * (double) count / linesToRead + "%");
                try {
                    Thread.sleep(5);
                } catch (InterruptedException ex) {
                    return;
                }
                count = count + 1;
            }
        }
    }

    @FXML
    private void closeFileAction(ActionEvent event) {
        file = null;
        isChanged.setValue(Boolean.FALSE);
        filePos = 0;
        startViewFileLine = 1;
        endViewFileLine = 1;
        textarea.clear();
        sizeLabel.setText("Lines: ");
        linesLabel.setText("Size: ");
        charsetLabel.setText("DEFAULT");
        encoding = null;
        tabbedPane.getTabs().get(0).setText("Unnamed-1");
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
            Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
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
                textarea.setStyleSpans(0, (StyleSpans<Collection<String>>) t.getSource().getValue());
                for (Paragraph<Collection<String>, String, Collection<String>> p : textarea.getParagraphs()) {
                    if (p.getStyleSpans().getSpanCount() > 1) {
                        textarea.showParagraphInViewport(textarea.getParagraphs().indexOf(p));
                        textarea.moveTo(textarea.getParagraphs().indexOf(p), 0);
                        break;
                    }
                }
            });
            task.setOnFailed((WorkerStateEvent t) -> {
                progressLabel.setVisible(false);
            });
            executor.submit(task);
        }
    }

    private StyleSpans<Collection<String>> computeHighlighting(Pattern patter, String text) {
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
    }

    @FXML
    private void ScrollNextAction(ActionEvent event) {
        int currentParagraph = textarea.getCurrentParagraph() + 1;
        if (currentParagraph > textarea.getParagraphs().size()) {
            currentParagraph = textarea.getParagraphs().size();
        }
        for (int i = currentParagraph; i < textarea.getParagraphs().size(); i++) {
            Paragraph<Collection<String>, String, Collection<String>> p = textarea.getParagraphs().get(i);
            if (p.getStyleSpans().getSpanCount() > 1) {
                textarea.showParagraphInViewport(textarea.getParagraphs().indexOf(p));
                textarea.moveTo(textarea.getParagraphs().indexOf(p), 0);
                break;
            }
        }
    }

    @FXML
    private void ScrollPrevAction(ActionEvent event) {
        int currentParagraph = textarea.getCurrentParagraph() - 1;
        if (currentParagraph == -1) {
            currentParagraph = 0;
        }
        for (int i = currentParagraph; i < textarea.getParagraphs().size(); i--) {
            if (i < 0) {
                break;
            }
            Paragraph<Collection<String>, String, Collection<String>> p = textarea.getParagraphs().get(i);
            if (p.getStyleSpans().getSpanCount() > 1) {
                textarea.showParagraphInViewport(textarea.getParagraphs().indexOf(p));
                textarea.moveTo(textarea.getParagraphs().indexOf(p), 0);
                break;
            }
        }
    }

}
