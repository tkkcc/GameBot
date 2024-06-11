package android.app;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Looper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.rikka.tools.refine.RefineAs;

/**
 * @author xjunz 2021/6/23 0:48
 */


// https://android.googlesource.com/platform/frameworks/base.git/+/master/core/java/android/app/UiAutomation.java
@RefineAs(UiAutomation.class)
public class UiAutomationHidden {
//    public final IUiAutomationConnection mUiAutomationConnection;

    public UiAutomationHidden(Looper looper, IUiAutomationConnection connection) {
        throw new RuntimeException("Stub!");
    }

    public void connect() {
        throw new RuntimeException("Stub!");
    }

    public void connect(int flag) {
        throw new RuntimeException("Stub!");
    }

    public void disconnect() {
        throw new RuntimeException("Stub!");
    }

}
