package paint;

import java.io.File;
import java.util.Optional;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/**
 *
 * @author Nathan Boisneault
 * @version 1.4
 */
public class Paint extends Application {

    public static Stage primaryStage;

    //The path of the currently open file
    private String fileUrl = "";

    //The main Canvas on which to draw
    private PaintCanvas canvas = new PaintCanvas(800, 800);

    @Override
    public void start(Stage stage) {
        //the rool element of the scene
        BorderPane root = new BorderPane();

        //this scrollpane will contain the canvas and show scrollbars if necessary
        ScrollPane content = new ScrollPane();

        //this will contain the menu bar and the tool bar
        VBox top = new VBox();

        //the tool bar for the canvas (drawing tools, color)
        PaintToolbar toolbar = new PaintToolbar(canvas);

        //the menu bar and the menus
        MenuBar menuBar = new MenuBar();
       
        Menu menuFile = new Menu("File");
        MenuItem menuNew = new MenuItem("New image");
        MenuItem menuOpen = new MenuItem("Open image");
        MenuItem menuSave = new MenuItem("Save image");
        MenuItem menuSaveAs = new MenuItem("Save image as");
        MenuItem menuClose = new MenuItem("Quit");

        Menu menuEdit = new Menu("Edit");
        MenuItem menuUndo = new MenuItem("Undo");
        MenuItem menuRedo = new MenuItem("Redo");

        /**
         * Menu New: clear the canvas and start over. Checks if modifications
         * have been made and ask the user to save previous work to prevent
         * loss.
         */
        menuNew.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {

                //check if current file not saved
                if (smartSave()) {
                    canvas.reset();
                    fileUrl = "";
                }
            }
        });

        /**
         * Menu Open: shows a FileChooser and displays the image from a file.
         * Checks if modifications have been made and ask the user to save
         * previous work to prevent loss. The canvas is cleared and the content
         * replaced with the image from the selected file.
         */
        menuOpen.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                FileChooser fc = new FileChooser();
                fc.setTitle("Open image");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.png", "*.gif", "*.bmp"));

                //show file chooser
                File file = fc.showOpenDialog(stage);

                //check file exists
                if (file != null) {

                    //check if current file not saved
                    if (smartSave()) {
                        //reset canvas and load image
                        canvas.openImageFromFile(file);
                        fileUrl = file.getPath();
                    }
                }
            }
        });

        //Save Menu: save the image in the current FileUrl
        /**
         * Menu Save: saves the image in the currently open file.
         */
        menuSave.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                save();
            }
        });

        /**
         * Menu Save As: saves the image in a new image file.
         */
        menuSaveAs.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                saveAs();
            }
        });

        /**
         * Menu Close: fires a close request.
         */
        menuClose.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
            }
        });

        /**
         * Close request: close the application. Checks if modifications have
         * been made and asks the user to save previous work to prevent loss.
         */
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent we) {

                if (!smartSave()) {
                    we.consume();
                }
            }
        });

        /**
         * Menu Undo: undo last action. Last drawing on the canvas is removed,
         * but able to be redone.
         */
        menuUndo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                canvas.undo();
            }
        });

        /**
         * Menu Redo: redo last action. Last drawing to have been undone is add
         * on the canvas again.
         */
        menuRedo.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                canvas.redo();
            }
        });

        //Keyboard shortcuts for the menus
        menuNew.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        menuOpen.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        menuSave.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        menuSaveAs.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN));
        menuUndo.setAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.CONTROL_DOWN));
        menuRedo.setAccelerator(new KeyCodeCombination(KeyCode.Y, KeyCombination.CONTROL_DOWN));

        //Adding the menus to the MenuBar
        menuFile.getItems().addAll(menuNew, menuOpen, menuSave, menuSaveAs, menuClose);
        menuEdit.getItems().addAll(menuUndo, menuRedo);
        menuBar.getMenus().addAll(menuFile, menuEdit);

        //Window layout
        content.setContent(canvas);
        top.getChildren().addAll(menuBar, toolbar);
        root.setTop(top);
        root.setCenter(content);

        Scene scene = new Scene(root, 800, 800);

        stage.setTitle("Pain(t)");
        stage.setScene(scene);
        stage.show();

        this.primaryStage = stage;
    }

    /**
     * Check if canvas has been modified since last time being saved, and ask
     * whether to save or not before continuing. If canvas has been modified, a
     * dialog box is prompted asking the user if they want to save their work.
     * If <b><Yes</b>, the save() function is called. If <b>No</b>, content is
     * not saved. The returned value is a boolean set to true if smart saving
     * completed successfully or was not necessary (no modifications were made).
     * The returned value is false if the user decided to cancel whatever action
     * was started before calling smart saving, or if saving failed.
     *
     * @return  <code>true</code> if save completed or not wanted,
     * <code>false</code> if user clicked <b>Cancel</b>, or saving failed.
     */
    private boolean smartSave() {

        //check for modification
        if (canvas.hasChanged()) {

            //create a new smart save dialog, display it and wait for the user to select an option
            Optional<ButtonType> response = new SmartSaveAlert().showAndWait();

            //if user clicked Yes, save the canvas' content
            //and return whether the saving succeeded or not
            if (response.get() == ButtonType.YES) {
                return save();
            }

            //in other cases, if the user did not click Cancel, return true as saving is not wanted
            if (response.get() != ButtonType.CANCEL) {
                return true;
            } else {
                //if user wiches to cancel, return false
                return false;
            }

        } else {
            //if smart saving is not necessary, return true;
            return true;
        }
    }

    /**
     * Saves the main Canvas' content as an image file. The destination file's
     * path is contained in fileUrl. If fileUrl is empty, a file chooser will be
     * displayed. The function returns a boolean indicating whether saving was
     * successful or not.
     *
     * @return true if save completed, false otherwise
     */
    private boolean save() {

        //check if fileUrl is empty (new file)
        if (fileUrl == "") {
            //call saveAs() to ask user where to save file
            return saveAs();
        } else {
            File file = new File(fileUrl);

            //check if file exists
            if (file != null) {
                //save the image into the file and return true
                canvas.saveImageAs(file);
                return true;
            } else {
                //if problem with the file, return false
                return false;
            }
        }
    }

    /**
     * Saves the content of the main Canvas as an image file. A file chooser
     * will be displayed in order for the user to chose the saving location. The
     * function returns a boolean indicating whether saving was successful or
     * not.
     *
     * @return true if save completed, false otherwise
     */
    private boolean saveAs() {
        File file;
        FileChooser fc = new FileChooser();
        fc.setTitle("Save image as");

        //extension filters to restrain to only image files
        fc.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("jpeg image", "*.jpg"),
                new FileChooser.ExtensionFilter("png image", "*.png"),
                new FileChooser.ExtensionFilter("gif image", "*.gif"),
                new FileChooser.ExtensionFilter("bitmap image", "*.bmp"));

        //show the file chooser
        file = fc.showSaveDialog(Paint.primaryStage);

        //check if file exists
        if (file != null) {
            //save the image into the file, set the fileUrl to be the new path and return true
            canvas.saveImageAs(file);
            fileUrl = file.getPath();
            return true;
        } else {
            //if problem with the file, return false
            return false;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
