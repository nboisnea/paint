package paint;

import java.util.EventListener;
import javafx.scene.paint.Color;

/**
 * A listener to color picking events in a PaintCanvas. Whenever a color is
 * picked using the color dropper tool, a PaintCanvas call the colorPicked()
 * method in all listeners so that actions can been taken.
 *
 * @author Nathan Boisneault
 */
public interface PickListener extends EventListener {

    /**
     * Called when a color is picked. The color that has been picked is given as
     * a parameter.
     *
     * @param c Color the picked color
     */
    void colorPicked(Color c);
}
