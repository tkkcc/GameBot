package android.view;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.IBinder;


import dev.rikka.tools.refine.RefineAs;


// https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/view/SurfaceControl.java
@RefineAs(SurfaceControl.class)
public class SurfaceControlHidden {

    public static void screenshot(IBinder display, Surface consumer) {
//        screenshot(display, consumer, new Rect(), 0, 0, 0, 0, true, false);
    }
    public static native Bitmap nativeScreenshot(IBinder displayToken,
                                                  Rect sourceCrop, int width, int height, int minLayer, int maxLayer,
                                                  boolean allLayers, boolean useIdentityTransform, int rotation);
    public static native void nativeScreenshot(IBinder displayToken, Surface consumer,
                                                Rect sourceCrop, int width, int height, int minLayer, int maxLayer,
                                                boolean allLayers, boolean useIdentityTransform);





    public static IBinder createDisplay(String name, boolean secure) {
        throw new RuntimeException("Stub!");
    }

    public static void openTransaction() {
        throw new RuntimeException("Stub!");

    }

    public static void setDisplaySurface(IBinder displayToken, Surface surface) {
        throw new RuntimeException("Stub!");

    }

    public static void setDisplayProjection(IBinder displayToken,
                                            int orientation, Rect layerStackRect, Rect displayRect) {

    }

    public static void setDisplayLayerStack(IBinder displayToken, int layerStack) {
    }

    public static void destroyDisplay(IBinder displayToken) {
    }

    public static void setDisplaySize(IBinder displayToken, int width, int height) {

    }

    public static long getPrimaryPhysicalDisplayId() {
        throw new RuntimeException("Stub!");

    }

    public static native long nativeGetPrimaryPhysicalDisplayId();


    public static IBinder getPhysicalDisplayToken(long physicalDisplayId) {
        throw new RuntimeException("Stub!");

    }

    public static IBinder getBuiltInDisplay(int id) {
        throw new RuntimeException("Stub!");
    }


    public void release() {
    }

    public static void closeTransaction() {
        throw new RuntimeException("Stub!");

    }
    public static void closeTransactionSync() {
        throw new RuntimeException("Stub!");

    }
}