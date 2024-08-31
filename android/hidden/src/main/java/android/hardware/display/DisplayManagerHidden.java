package android.hardware.display;

import android.view.Surface;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(DisplayManager.class)
public class DisplayManagerHidden {
    public static VirtualDisplay createVirtualDisplay(String name, int width, int height,
                                                      int displayIdToMirror, Surface surface) {
        return null;
    }
}
