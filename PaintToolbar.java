package paint;

import java.util.HashMap;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * A toolbar designed for interacting with a PaintCanvas. It offers tools to
 * select what to draw on the canvas (shapes, hand drawing, text), and to switch
 * between drawing, erasing, selecting and color dropper tools. It contains a
 * color picker to change the drawing color and a field to set the stroke
 * thickness and the text size values. It is linked to a PaintCanvas so that it
 * is synchronized with it: any action on the toolbar is effective immediately
 * on the canvas, and any change on the canvas will automatically update the
 * information displayed on the toolbar.
 *
 * @author Nathan Boisneault
 */
public class PaintToolbar extends HBox {

    private PaintCanvas canvas;

    /**
     * Create a new toolbar associated with a PaintCanvas. The tool bar is built
     * and linked to the provided PaintCanvas. Events to handle user actions on
     * the toolbar's element are created.
     *
     * @param c PaintCanvas the Paint Canvas to be linked to the toolbar.
     */
    PaintToolbar(PaintCanvas c) {

        //initialize the associated PaintCanvas
        this.canvas = c;

        //build the tool bar
        //add the default components
        //Color picker with initial value set to canvas' default color
        final ColorPicker col = new ColorPicker();
        col.setValue(canvas.getColor());

        col.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {

                //change the canvas' selected color
                canvas.setColor(col.getValue());
            }
        });

        //a listener to know when a color is picked from the canvas using the dropper tool
        c.addListener(new PickListener() {
            @Override
            public void colorPicked(Color c) {
                col.setValue(c);
            }
        });

        //Width/text size selector set to canvas' initial value
        Label lblWidth = new Label("Width: ");
        Spinner<Double> spinner = new Spinner<Double>();
        SpinnerValueFactory<Double> val = new SpinnerValueFactory.DoubleSpinnerValueFactory(.5, 100., canvas.getLineWidth(), .5);
        spinner.setValueFactory(val);
        spinner.setEditable(true);
        spinner.setMaxWidth(80);

        //whenever the value is changed, update the canvas' line width or text size according to what is currently used
        spinner.valueProperty().addListener(new ChangeListener<Double>() {
            @Override
            public void changed(ObservableValue<? extends Double> observable, Double oldValue, Double newValue) {

                //change the canvas' font size if in text mode
                //otherwise, change the stroke width value
                if (canvas.getPen() == PaintCanvas.Pen.TEXT) {
                    canvas.setFontSize(newValue);
                } else {
                    canvas.setLineWidth(newValue);
                }

            }
        });

        //these buttons allow the user to switch between drawing and selecting
        ToggleButton btnDraw = new ToggleButton("Draw");
        ToggleButton btnSelect = new ToggleButton("Select");
        ToggleButton btnErase = new ToggleButton("Erase");
        ToggleButton btnDrop = new ToggleButton("Pick"); //the dropper tool

        ToggleGroup toggleMode = new ToggleGroup();
        btnDraw.setToggleGroup(toggleMode);
        btnSelect.setToggleGroup(toggleMode);
        btnErase.setToggleGroup(toggleMode);
        btnDrop.setToggleGroup(toggleMode);

        //use canvas' default setting
        switch (canvas.getMode()) {
            case SELECT:
                btnSelect.setSelected(true);
                break;
            case DRAW:
                btnSelect.setSelected(true);
                break;
            case ERASE:
                btnSelect.setSelected(true);
                break;
        }

        //when clicking, setting the canvas to draw mode
        btnDraw.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                canvas.setMode(PaintCanvas.Mode.DRAW);
            }
        });

        //when clicking, setting the canvas to selection mode
        btnSelect.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                canvas.setMode(PaintCanvas.Mode.SELECT);
            }
        });

        //when clicking, setting the canvas to eraser: same as white free_drawing
        btnErase.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                canvas.setMode(PaintCanvas.Mode.ERASE);
            }
        });

        //when clicking, setting the canvas to pick mode (dropper tool)
        btnDrop.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                canvas.setMode(PaintCanvas.Mode.PICK);
            }
        });

        //Pen selector
        ComboBox cbPen = new ComboBox();
        HashMap<PaintCanvas.Pen, String> items = new HashMap<PaintCanvas.Pen, String>();
        items.put(PaintCanvas.Pen.FREE_DRAWING, "Free drawing");
        items.put(PaintCanvas.Pen.LINE, "Line");
        items.put(PaintCanvas.Pen.RECT, "Rectangle");
        items.put(PaintCanvas.Pen.SQUARE, "Square");
        items.put(PaintCanvas.Pen.CIRCLE, "Circle");
        items.put(PaintCanvas.Pen.TEXT, "Text");
        cbPen.getItems().addAll(items.values());

        //select the default pen that tha canvas is using at startup
        cbPen.setValue(items.get(canvas.getPen()));

        cbPen.valueProperty().addListener(new ChangeListener() {

            @Override
            public void changed(ObservableValue observable, Object oldValue, Object newValue) {
                int i = 0;
                PaintCanvas.Pen pen;

                //find which mode is selected and apply it to the canvas
                do {
                    pen = PaintCanvas.Pen.values()[i];
                    i++;
                } while (items.get(pen) != cbPen.getValue() && i < PaintCanvas.Pen.values().length);

                //set canvas to drawing mode
                canvas.setMode(PaintCanvas.Mode.DRAW);
                canvas.setPen(pen);

                if (pen == PaintCanvas.Pen.TEXT) {
                    lblWidth.setText("Text size: ");
                    spinner.getValueFactory().setValue(canvas.getFontSize());
                } else {
                    lblWidth.setText("Width: ");
                    spinner.getValueFactory().setValue(canvas.getLineWidth());
                }

                toggleMode.selectToggle(btnDraw);
            }
        });

        this.setAlignment(Pos.CENTER_LEFT);
        this.getChildren().addAll(btnDrop, col, new Separator(Orientation.VERTICAL), lblWidth, spinner, new Separator(Orientation.VERTICAL), cbPen, btnDraw, btnErase, btnSelect);
        this.setSpacing(5);
    }

}
