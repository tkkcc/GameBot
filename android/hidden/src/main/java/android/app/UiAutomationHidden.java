package android.app;

import android.os.Looper;

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

    public int getConnectionId() {
        return 0;
    }

    public void connect(int flag) {
        throw new RuntimeException("Stub!");
    }

    public void disconnect() {
        throw new RuntimeException("Stub!");
    }


    //    private IAccessibilityServiceClient mClient;
    public int mConnectionId = 0;

}
