package paint;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * A standard alert box asking for saving before continuing. Answer can be
 * either Yes, No or Cancel.
 *
 * @author natha
 */
public class SmartSaveAlert extends Alert {

    /**
     * Create a new Alter box asking the user if they would like to save their
     * work.
     */
    public SmartSaveAlert() {
        super(Alert.AlertType.WARNING);
        this.setTitle("Save changes?");
        this.setHeaderText("Would you like to save your image?");
        this.getButtonTypes().clear();
        this.getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
    }

}
