// IRemoteRun.aidl
package bilabila.gamebot.host;

// Declare any non-default types here with import statements
import android.graphics.Point;

interface IRemoteRun {
    void setOverrideDisplaySize(in Point point);
    Point getOverrideDisplaySize();
    int getOverrideDisplayDensity();
    int getPhysicalDisplayDensity();
    void setLocalRunBinder(in IBinder binder);
    void start();
    void stop();
    void test();
    void runTask(in String type);
    void testLargeFileTransfer(in ParcelFileDescriptor pfd);
}