package paint;

import javafx.scene.shape.Rectangle;

/**
 * A more efficient rectangle shape that can handle negative dimensions. JavaFX'
 * default Rectangle does not support negative width and height! This one does!
 * It works exactly the same as a regular Rectangle object. To set the
 * rectangle's width or height, use setRelativeWidth() or setRelativeHeight()
 * instead of the usual setWidth() and setHeight(). TranslateX and TranslateY
 * will be automatically modified so as the rectangle looks like having negative
 * dimensions. X and Y position will not move, but it is possible to have the
 * top-left point coordinates by calling getAbsX() and getAbsY().
 *
 * @author Nathan Boisneault
 */
public class BetterRectangle extends Rectangle {

    /**
     * Creates a new empty BetterRectangle
     */
    BetterRectangle() {
        super();
    }

    /**
     * Creates a new empty BetterRectangle with the given position and
     * dimensions.
     *
     * @param x Rectangle's x position
     * @param y Rectangle's y position
     * @param w Rectangle's width
     * @param h Rectangle's height
     */
    BetterRectangle(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    /**
     * Change rectangle's width. Accepts negative values. If the given width is
     * negative, the rectangle will be translated on the X axis and the absolute
     * width will be applied.
     *
     * @param w the new width.
     */
    public void setRelativeWidth(double w) {

        if (w >= 0) {
            this.setWidth(w);
            this.setTranslateX(0);
        } else {
            this.setWidth(-w);
            this.setTranslateX(w);
        }
    }

    /**
     * Change rectangle's height. Accepts negative values. If the given height
     * is negative, the rectangle will be translated on the Y axis and the
     * absolute height will be applied.
     *
     * @param h the new height.
     */
    public void setRelativeHeight(double h) {

        if (h >= 0) {
            this.setHeight(h);
            this.setTranslateY(0);
        } else {
            this.setHeight(-h);
            this.setTranslateY(h);
        }
    }

    /**
     * Get the X coordinate of the top-left corner. To get the regular X
     * position, use getX().
     *
     * @return rectangle's top-left corner x position
     */
    public double getAbsX() {
        return this.getX() + this.getTranslateX();
    }

    /**
     * Get the Y coordinate of the top-left corner. To get the regular Y
     * position, use getY().
     *
     * @return rectangle's top-left corner y position
     */
    public double getAbsY() {
        return this.getY() + this.getTranslateY();
    }
}
