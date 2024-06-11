package android.app;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.RemoteException;

/**
 * @author xjunz 2021/6/23 0:52
 */
public class UiAutomationConnection extends IUiAutomationConnection.Stub {
    public UiAutomationConnection() {
        throw new RuntimeException("Stub!");
    }

    public Bitmap takeScreenshot(int x, int y){
        return null;
    }
    public Bitmap takeScreenshot(Rect rect){
        return null;
    }
}
