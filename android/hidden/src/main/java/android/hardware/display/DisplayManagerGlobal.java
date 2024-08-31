//package android.hardware.display;
package android.hardware.display;

import android.content.Context;
import android.media.projection.MediaProjection;

import java.util.concurrent.Executor;

public class DisplayManagerGlobal {
    public static DisplayManagerGlobal getInstance() {
        return null;
    }

    public VirtualDisplay createVirtualDisplay(Context context, MediaProjection projection,
                                               VirtualDisplayConfig virtualDisplayConfig, VirtualDisplay.Callback callback,
                                               Executor executor, Context windowContext) {
        return null;
    }
}
