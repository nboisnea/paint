package paint;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javax.imageio.ImageIO;

/**
 * A canvas specially made for user drawing and image displaying. It provides
 * tools for drawing various shapes using the mouse: lines, rectangles, squares,
 * circles, a pen tool for free drawing and an eraser. It also allows to insert
 * personalized text on the drawing. The PaintCanvas can open all of the most
 * common image formats: Jpeg, PNG, Bitmap, GIF and can save in these formats as
 * well. A selection tool allows parts of an image to be moved across the
 * canvas. Drawing color, stroke width, text size can be changed. A color picker
 * tool allows the drawing color to be changed according to a selected pixel in
 * the image. Graphical User Interface for the PaintCanvas can be added by using
 * a PaintToolbar.
 *
 * @author Nathan Boisneault
 */
public class PaintCanvas extends Pane {

    //default dimension values
    private static final double defaultWidth = 800.;
    private static final double defaultHeight = 800.;

    //color to apply for drawing
    private Color color;

    //stroke width to use
    private double width;

    //font size for text
    private double fontSize;

    //the text to display in text mode
    private String text = "";

    //keeps track of modification. Set to true avery time something changes on the canvas. Set to false when saving
    private boolean changed = false;

    //switch between drawing tools
    private Pen pen = Pen.LINE;

    //switch between drawing, erasing and selecting modes
    private Mode mode = Mode.SELECT;

    //a stack to store the undone actions
    private Stack<Shape> undo = new Stack<Shape>();

    //selection graphics
    private SelectionRectangle selection = new SelectionRectangle();

    //background
    private Rectangle background;

    //listeners for color picking events
    private List<PickListener> colorPickedListeners = new ArrayList<PickListener>();

    /**
     * PaintCanvas Pen for drawings. Set a PaintCanvas' pen field using one of
     * those Pens. Each pen correspond to a unique shape to be added when
     * drawing on the canvas. FREE_DRAWING is for hand drawing and TEXT is used
     * to add text on the image. Note that when using TEXT, the PaintCanvas will
     * first prompt a dialog so as to type the text before adding it to the
     * image.
     */
    public enum Pen {
        FREE_DRAWING, LINE, RECT, SQUARE, CIRCLE, TEXT
    }

    /**
     * PaintCanvas interaction modes. Set a PaintCanvas' mode field using one of
     * those Modes. Different mode will make the PaintCanvas react differently
     * to mouse interaction with the image. DRAW is used to draw shapes on the
     * image. Shape can be chosen by setting the Pen field and the Pen
     * enumeration. ERASE is used to erase the image with the mouse. SELECT is
     * used for rectangular selection. Selection can be moved across the image.
     * PICK is used to pick a color. When clicking on the image, the pixel's
     * color at the pointer's position will be captured and set as the new
     * drawing color.
     */
    public enum Mode {
        DRAW, ERASE, SELECT, PICK
    }

    /**
     * Creates a new PaintCanvas pane. Default settings are applied and events
     * are created to handle later modifications on the image.
     */
    public PaintCanvas() {
        this(PaintCanvas.defaultWidth, PaintCanvas.defaultHeight);
    }

    /**
     * Creates a new PaintCanvas pane, with the provided width and height.
     * Default settings are applied and events are created to handle later
     * modifications on the image.
     *
     * @param w canvas' width
     * @param h canvas' height
     */
    public PaintCanvas(double w, double h) {
        super();

        //default settings
        this.color = Color.RED;
        this.width = 1;
        this.fontSize = 40;

        //reset the canvas
        this.reset(w, h);

        /**
         * Mouse pressed event listener: start selection or drawing. In
         * selection mode, reset the selection and add the selection rectangle
         * to the canvas' children to be displayed. In color picking mode,
         * capture the selected pixel's color and raise the colorPicked event
         * for the listeners. In drawing mode, create a new shape according to
         * the current settings (color, stroke, type of shape), and add it the
         * the canvas' children to be displayed.
         */
        this.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                //check the current mode
                if (mode == Mode.SELECT) {

                    //check if selecting or clicking on existing selection
                    if (selection.getIsHolding()) {

                        //if starting to move current selection, place empty rectangle instead of selection before selection moves
                        if (!selection.hasMoved()) {
                            Rectangle r = new Rectangle(selection.getAbsX(), selection.getAbsY(), selection.getWidth(), selection.getHeight());
                            r.setFill(Color.WHITE);
                            getChildren().add(getChildren().indexOf(selection), r);
                        }
                    } else {
                        //if clicking outside the current selection, remove current selection rectangle and start a new selection

                        //if in selection mode, print the content of previous selection
                        if (selection.getContent() != null) {
                            getChildren().remove(selection);
                            getChildren().add(selection.getContent());
                        }

                        //start a new selection where the user clicked
                        selection.reset(event.getX(), event.getY());
                    }
                } else if (mode == Mode.PICK) {
                    //in picking mode, 
                    //get the pixel's color at the mouse position
                    Color col = snapshot(new SnapshotParameters(), null).getPixelReader().getColor((int) event.getX(), (int) event.getY());

                    //update drawing color
                    color = col;

                    //call the event listeners for color picking events
                    for (PickListener listener : colorPickedListeners) {
                        listener.colorPicked(col);
                    }
                } else {
                    //if in drawing or erasing mode, start a new drawing as a new shape we add the pane

                    //check if in text mode and text has not been set yet
                    if (mode == Mode.DRAW && pen == Pen.TEXT && text == "") {
                        //In text mode, at the first click on the canvas, ask the user for the text to display
                        TextInputDialog dialog = new TextInputDialog("Text");
                        dialog.setTitle("Text");
                        dialog.setHeaderText("Enter a text to add to the image.\n Click OK to place it on the canvas");
                        text = dialog.showAndWait().get();
                    } else {
                        //otherwise, start a new shape according to the current settings

                        Shape newShape;

                        //if in erase mode, use free drawing, otherwise use custom pen
                        Pen p = mode == Mode.ERASE ? Pen.FREE_DRAWING : pen;

                        switch (p) {
                            case LINE:
                                newShape = new Line(event.getX(), event.getY(), event.getX(), event.getY());
                                break;
                            case RECT:
                            case SQUARE:
                                newShape = new BetterRectangle(event.getX(), event.getY(), 0, 0);
                                break;
                            case CIRCLE:
                                newShape = new Circle(event.getX(), event.getY(), 0);
                                break;
                            case TEXT:
                                newShape = new Text(event.getX(), event.getY(), text);
                                ((Text) newShape).setFont(new Font(fontSize));
                                text = ""; //reset for next drawing
                                break;
                            default:
                                newShape = new Path();
                                newShape.setStrokeLineCap(StrokeLineCap.ROUND);
                                newShape.setStrokeLineJoin(StrokeLineJoin.ROUND);
                                ((Path) newShape).getElements().add(new MoveTo(event.getX(), event.getY()));
                                break;
                        }

                        if (mode == Mode.ERASE) {
                            //in erase mode, pen should be a large, white free drawing
                            newShape.setStroke(Color.WHITE);
                            newShape.setStrokeWidth(width * 5);
                        } else {
                            //setup the shape with the pane's settings
                            newShape.setStroke(color);

                            if (pen == Pen.TEXT) {
                                //if adding text to the image, should use a with of 1 and fill the inside
                                newShape.setStrokeWidth(1);
                                newShape.setFill(color);
                            } else {
                                //for any other type of drawing, use custom width
                                newShape.setStrokeWidth(width);
                                newShape.setFill(Color.TRANSPARENT);
                            }
                        }

                        //finalize and add the new shape to the parent
                        getChildren().add(newShape);
                    }
                }

                //In any case, the canvas has changed and smart save should be triggered when necessary
                changed = true;
                undo.clear();
            }
        });

        /**
         * Mouse dragged event listener: drawing preview. In selection or
         * drawing modes, update the shape being drawn as to match the new mouse
         * position. Used to preview the shape or selection until the user
         * releases the mouse.
         */
        this.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                //check the current mode
                if (mode == Mode.SELECT) {

                    //if the user is not moving the selection, update the selection rectangle
                    if (!selection.getIsHolding()) {

                        selection.setRelativeWidth(event.getX() - selection.getX());
                        selection.setRelativeHeight(event.getY() - selection.getY());

                        //if the selection rectangle is not visible yet, add it to the pane's children
                        if (!getChildren().contains(selection)) {
                            getChildren().add(selection);
                        }
                    }
                } else if (mode != Mode.PICK) {
                    //if in drawing or erasing mode, update the shape being drawn so as to preview it

                    //retrieve last shape from the canvas' children, it is the one being drawn
                    Shape shape = (Shape) getChildren().get(getChildren().size() - 1);

                    //get mouse position
                    double mouseX = event.getX();
                    double mouseY = event.getY();

                    //if in erase mode, use free drawing, otherwise use custom pen
                    Pen p = mode == Mode.ERASE ? Pen.FREE_DRAWING : pen;

                    //otherwise, use the custom pen
                    switch (p) {
                        case LINE:
                            Line l = (Line) shape;
                            l.setEndX(mouseX);
                            l.setEndY(mouseY);
                            break;
                        case RECT:
                            BetterRectangle r = (BetterRectangle) shape;
                            r.setRelativeWidth(mouseX - r.getX());
                            r.setRelativeHeight(mouseY - r.getY());
                            break;
                        case SQUARE:
                            BetterRectangle sq = (BetterRectangle) shape;
                            sq.setRelativeHeight(mouseY - sq.getY());

                            //for the square, set the width to be equals to the height qnd orient the shqpe to be on the pointer's side
                            if (mouseX >= sq.getX() && mouseY >= sq.getY() || mouseX < sq.getX() && mouseY < sq.getY()) {
                                sq.setRelativeWidth(mouseY - sq.getY());
                            } else {
                                sq.setRelativeWidth(sq.getY() - mouseY);
                            }

                            break;
                        case CIRCLE:
                            Circle c = (Circle) shape;
                            c.setRadius(sqrt(pow(mouseX - c.getCenterX(), 2) + pow(mouseY - c.getCenterY(), 2)));
                            resize(800, 800);
                            break;
                        case TEXT:
                            Text t = (Text) shape;
                            t.setX(mouseX);
                            t.setY(mouseY);
                            break;
                        default:
                            //free drawing or eraser
                            Path path = (Path) shape;
                            path.getElements().add(new LineTo(mouseX, mouseY));
                            break;
                    }
                }
            }
        }
        );

        /**
         * Mouse released event listener: capture selection. In selection mode,
         * if releasing the mouse for the first time after selecting a part of
         * the image, capture the content of the image. releases the mouse.
         */
        this.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                //in selection mode, if this is the first time releasing the mouse, capture the content
                if (mode == Mode.SELECT && selection.getContent() == null) {
                    selection.select();
                }
            }
        });
    }

    /**
     * Erases the current content of the canvas to start a new drawing. All
     * drawings on the canvas will be erased and the background set back to
     * white. All modifications are removed from the drawing history, so no redo
     * will be possible afterward. As well, whatever the selection rectangle
     * contained will be discarded. The canvas will be resized to its default
     * width and height.
     */
    public void reset() {
        this.reset(PaintCanvas.defaultWidth, PaintCanvas.defaultHeight, Color.WHITE);
    }

    /**
     * Erases the current content of the canvas to start a new drawing. All
     * drawings on the canvas will be erased and the background set back to
     * white. All modifications are removed from the drawing history, so no redo
     * will be possible afterward. As well, whatever the selection rectangle
     * contained will be discarded. The canvas will be resized according to the
     * specified width and height. Use when creating a new file.
     *
     * @param w canvas' new width.
     * @param h canvas' new height.
     */
    public void reset(double w, double h) {
        this.reset(w, h, Color.WHITE);
    }

    /**
     * Erases the current content of the canvas and start anew drawing with the
     * specified background paint. All drawings on the canvas will be erased and
     * the new background will be added. All modifications are removed from the
     * drawing history, so no redo will be possible afterward. As well, whatever
     * the selection rectangle contained will be discarded. The canvas will be
     * resized according to the specified width and height. Use when opening an
     * existing image file, giving the ImagePaint as background paint.
     *
     * @param w canvas' new width.
     * @param h canvas' new height.
     * @param background the paint to use as a background to the new canvas.
     */
    public void reset(double w, double h, Paint background) {
        this.getChildren().clear();
        this.selection.empty();
        this.background = new Rectangle(0, 0, w, h);
        this.background.setFill(background);
        this.getChildren().add(this.background);
        this.undo = new Stack();
        this.changed = false;
    }

    /**
     * Sets the color used for drawings. This color will apply to all future
     * shapes, free drawings and texts. This color will not apply to eraser and
     * will not affect previous drawings.
     *
     * @param col new drawing color.
     */
    public void setColor(Color col) {
        this.color = col;
    }

    /**
     * Gets the color that is currently in use for drawings.
     *
     * @return Color the currently selected color.
     */
    public Color getColor() {
        return this.color;
    }

    /**
     * Sets the line stroke thickness used for drawings. This will apply to all
     * future shapes and free drawings. Eraser tool will be set ten times this
     * stroke thickness. This will not affect text size and will not apply to
     * previous drawings.
     *
     * @param val new line width.
     */
    public void setLineWidth(double val) {
        this.width = val;
    }

    /**
     * Gets the line stroke thickness that is currently in use for drawings.
     * This is the line width used when drawing shapes and free drawing. This is
     * not the text size.
     *
     * @return double the current line width.
     */
    public double getLineWidth() {
        return this.width;
    }

    /**
     * Sets the font size to use for the text tool. This will only affect future
     * texts and not the ones that are already on the image.
     *
     * @param val the new text size.
     */
    public void setFontSize(double val) {
        this.fontSize = val;
    }

    /**
     * Gets the font size that is currently in use for the text tool.
     *
     * @return double the font size
     */
    public double getFontSize() {
        return this.fontSize;
    }

    /**
     * Sets the Pen to use for future drawing. The pen sets which type of shape
     * to draw (line, rectangle, square, circle). It can also be set to hand
     * drawing mode or text adding.
     *
     * @param p PaintCanvas.Pen corresponding to the desired shape or object to
     * draw.
     */
    public void setPen(Pen p) {
        this.pen = p;
    }

    /**
     * Gets the Pen that is currently in use for drawings. The pen could be
     * either drawing a shape (line, rectangle, square, circle), free drawing or
     * text adding.
     *
     * @return PaintCanvas.Pen the currently used Pen.
     */
    public Pen getPen() {
        return this.pen;
    }

    /**
     * Sets the interaction mode for the canvas. The mode can be either
     * selecting parts of the image, eraser tool, dropper tool or regular
     * drawing. If setting the mode to something else than selecting mode, the
     * current selection will be printed to the current selection position. The
     * selection rectangle will also erased and will disappear from the screen.
     *
     * @param m PaintCanvas.Mode corresponding to the desired mode.
     */
    public void setMode(Mode m) {
        this.mode = m;

        //hide the selection rectangle
        if (m != Mode.SELECT && this.getChildren().contains(selection)) {

            //if in selection mode, print the content of previous selection
            if (selection.getContent() != null) {
                this.getChildren().add(selection.getContent());
                this.selection.empty();
            }

            this.getChildren().remove(selection);
        }
    }

    /**
     * Gets the Mode that is currently in use. The mode can be either selecting
     * parts of the image, eraser tool, dropper tool or regular drawing.
     *
     * @return PaintCanvas.Mode the currently selected mode
     */
    public Mode getMode() {
        return this.mode;
    }

    /**
     * Adds a PickListener which will be notified whenever a color is picked
     * from the canvas using the dropper tool. If the same listener is added
     * more than once, then it will be notified more than once. That is, no
     * check is made to ensure uniqueness.
     *
     * @param listener the listener to register.
     */
    void addListener(PickListener listener) {
        this.colorPickedListeners.add(listener);
    }

    /**
     * Undoes the last action. Removes the last shape that was added to the
     * children list. If the last action is a selection move, the selection will
     * be removed from the canvas. The shape is saved in a stack and can be
     * redrawn using Redo().
     */
    public void undo() {

        //check if there are more shapes than only the background (cannot undo the background)
        if (this.undoAvailable()) {

            //the shape to remove is the last one in the pane's children list
            Shape toRemove = (Shape) this.getChildren().get(this.getChildren().size() - 1);

            //we push the undone shape into the undo stack, for an eventual redo
            this.undo.push(toRemove);

            //the shape is removed, it is considered as a change
            this.getChildren().remove(toRemove);
            this.changed = true;
        }
    }

    /**
     * Redoes the last undone action. Add the last undone shape to the children
     * list if there is one available in undone action stack. It can be any
     * drawing or the content from a selection. The shape is then popped out of
     * the stack.
     */
    public void redo() {

        //check if there is something to redo
        if (this.redoAvailable()) {

            //the shape to redo is the last to have been pushed onto the undo stack
            Shape toAdd = this.undo.pop();

            //the shape is added to the pane's children again, it is considered a change
            this.getChildren().add(toAdd);
            this.changed = true;
        }
    }

    /**
     * Check if there is any action that could be undone. An action that can be
     * undone is any previous shape drawing, free drawing, text adding,
     * selection or selection move. Use to check if an action can be undone
     * before calling Undo().
     *
     * @return true if undo is possible, false otherwise.
     */
    public boolean undoAvailable() {
        //can undo last action only if there is more shapes than only the background
        return this.getChildren().size() > 1;
    }

    /**
     * Check if there is any action that could be redone. An action that can be
     * redone is any previous shape drawing, free drawing, text adding,
     * selection or selection move that has been canceled by calling Undo(). Use
     * to check if an action can be redone before calling Redo().
     *
     * @return true if redo is possible, false otherwise.
     */
    public boolean redoAvailable() {
        //can redo last undone action only if there is something in the stack
        return this.undo.size() > 0;
    }

    /**
     * Checks if any modification has been added. This value is set to true
     * whenever the content of the canvas changes. That means any shape drawing,
     * free drawing, text adding, selection or selection moving. This value is
     * reset to false as soon as the content of the canvas is saved by calling
     * saveImageAs()
     *
     * @return true if the image has been modified since last save, false
     * otherwise
     */
    public boolean hasChanged() {
        return this.changed;
    }

    /**
     * Displays an image from a file inside the canvas. The file has to be an
     * image file of any accepted format : Jpeg, PNG, GIF or Bitmap. The canvas
     * will then be reset and the image will be open.
     *
     * @param file the file from which to get the image
     */
    public void openImageFromFile(File file) {

        //getting the image from the file
        Image img = new Image(file.toURI().toString());

        //reset the canvas and drawing the image inside
        this.reset(img.getWidth(), img.getHeight(), new ImagePattern(img));
    }

    /**
     * Saves the canvas' content as an image in the provided file. The content
     * will be saved as it is when calling this function. The selection will be
     * removed if it has not been added to the content and the selection
     * rectangle will be removed from the canvas.
     *
     * @param file the file in which to save the image
     */
    public void saveImageAs(File file) {

        if (this.getChildren().contains(selection)) {
            this.getChildren().remove(selection);
        }

        if (file != null) {
            try {
                //getting the destination file extension so as to retrieve the file type
                String extension = file.getName().split("\\.")[1];

                //generating the image (jpeg problem fix)
                BufferedImage snapshot = SwingFXUtils.fromFXImage(this.snapshot(new SnapshotParameters(), null), null);
                BufferedImage img = new BufferedImage(snapshot.getWidth(), snapshot.getHeight(), BufferedImage.TYPE_INT_RGB);

                for (int x = 0; x < snapshot.getWidth(); x++) {

                    for (int y = 0; y < snapshot.getHeight(); y++) {
                        img.setRGB(x, y, snapshot.getRGB(x, y));
                    }
                }

                //writing the file
                ImageIO.write(img, extension, file);
                this.changed = false;
            } catch (IOException ex) {
                System.out.println(ex.toString());
            }
        }
    }
}
