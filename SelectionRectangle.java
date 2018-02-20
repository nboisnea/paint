package paint;

import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Rectangle;

/**
 * A rectangle designed for selection content as an image in its parent. The
 * user can select a rectangle area inside the parent's node. The content is
 * captured as an image and held by the Selection Rectangle until it is replaced
 * or emptied. The selected area can be moved across the image. Dragging the
 * selection is automatically handled by the SelectionRectangle object.
 *
 * @author Nathan Boisneault
 */
public class SelectionRectangle extends BetterRectangle {

    //track the mouse position when starting a selection
    private double mouseX, mouseY;

    //true if the user is dragging the selection
    private boolean holding = false;

    //updated as once as the selection moves from its original position
    private boolean moved = false;

    //the content of the selection
    private WritableImage content;

    /**
     * Creates a new SelectionRectangle object. The style is set automatically
     * to a dashed rectangle but can be later modified. Default settings and
     * events are created to handle the selection's drawing and dragging.
     */
    SelectionRectangle() {
        super();

        //default graphics for the selection rectangle
        this.setSmooth(false);
        this.setFill(Color.TRANSPARENT);
        this.setStroke(Color.BLACK);
        this.setStrokeWidth(1);
        this.getStrokeDashArray().addAll(2d, 2d);

        /**
         * On mouse pressed: capture the mouse position before the selection
         * moves.
         */
        this.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //capture the mouse position relative to the selection rectangle before the selection moves
                mouseX = event.getX() - getX();
                mouseY = event.getY() - getY();
                holding = true;
            }
        });

        /**
         * On mouse dragged: make the selection follow the pointer.
         */
        this.setOnMouseDragged(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //the selection is no longer in its original position
                moved = true;

                //moves the selection to the new mouse position
                //using the original mouse position as an offset so the cursor has is always on the same point of the selection
                setX(event.getX() - mouseX);
                setY(event.getY() - mouseY);

                //by default, an ImagePattern stays fixed even if the rectangle moves, so we need to make the content move as well
                if (content != null) {
                    setFill(new ImagePattern(content, getX(), getY(), getWidth(), getHeight(), false));
                }
            }
        });

        /**
         * On mouse released: end selection dragging.
         */
        this.setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                //when dragging is over, user is no longer holding the selection
                holding = false;
            }
        });
    }

    /**
     * Resets the selection rectangle to a null selection, at a default
     * position. The content of the selection will be moved and the rectangle
     * will be moved to the specified position. Its width and height will be set
     * to 0.
     *
     * @param x new x position
     * @param y new y position
     */
    public void reset(double x, double y) {
        this.setX(x);
        this.setY(y);
        this.setWidth(0);
        this.setHeight(0);
        this.empty();
        this.setFill(Color.TRANSPARENT);
        moved = false;
    }

    /**
     * Checks if the user is currently holding the selection. That means if the
     * selection is currently under a dragging operation.
     *
     * @return true if the user is dragging the selection, false otherwise
     */
    public boolean getIsHolding() {
        return holding;
    }

    /**
     * Checks if the selection has moved since its creation. The selection has
     * moved if its current coordinates are different from its original
     * coordinates.
     *
     * @return false if the selection is still in its original position, true if
     * it has moved
     */
    public boolean hasMoved() {
        return moved;
    }

    /**
     * Capture the content of the parent node inside the selection rectangle.
     * Takes a snapshot of the content of SelectionRectangle object's parent
     * node inside selection area. The SelectionRectangle will be filled with
     * the resulting image. The content can be retrieved using getContent().
     */
    public void select() {

        //coordinates of the selection, without the selection rectangle's borders
        int x = (int) this.getAbsX() + 1;
        int y = (int) this.getAbsY() + 1;
        int w = (int) this.getWidth() - 2;
        int h = (int) this.getHeight() - 2;

        //check if the selection is big enough to have a content (not only borders)
        if (w > 0 && h > 0) {
            Node parent = this.getParent();

            //take a snapshot of the parent, and retrieve only the part that is directly behind the selection rectangle
            this.content = new WritableImage(parent.snapshot(new SnapshotParameters(), null).getPixelReader(), x, y, w, h);
            this.setFill(new ImagePattern(content, getX(), getY(), getWidth(), getHeight(), false));
        }
    }

    /**
     * Returns the content that has been previously captured. Content from the
     * parent node can be captured by calling select(). The content is returned
     * as a Rectangle filled with an ImagePattern created with the captured
     * content. If nothing has already been captured or if the content is empty,
     * null will be returned.
     *
     * @return Shape.Rectangle a rectangle filled with the content or null if no
     * content available.
     */
    public Rectangle getContent() {

        if (this.content != null) {
            int x = (int) this.getAbsX() + 1;
            int y = (int) this.getAbsY() + 1;

            //create a new rectangle and fills it with the content of the selection
            Rectangle r = new Rectangle(x, y, this.content.getWidth(), this.content.getHeight());
            r.setFill(new ImagePattern(this.content, x, y, this.content.getWidth(), this.content.getHeight(), false));
            r.setStrokeWidth(0);

            return r;
        } else {
            //if no content has been captured, return null
            return null;
        }
    }

    /**
     * Empty the selection's content. Whatever has been captured previously is
     * discarded. The content is set to null.
     */
    public void empty() {
        this.content = null;
    }
}
