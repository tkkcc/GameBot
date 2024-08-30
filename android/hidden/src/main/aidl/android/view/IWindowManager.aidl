// IWindowManager.aidl
package android.view;

// Declare any non-default types here with import statements

interface IWindowManager {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void getInitialDisplaySize(int displayId, out Point size);
    void getBaseDisplaySize(int displayId, out Point size);
    void setForcedDisplaySize(int displayId, int width, int height);
    void clearForcedDisplaySize(int displayId);
    int getInitialDisplayDensity(int displayId);
    int getBaseDisplayDensity(int displayId);
    void setForcedDisplayDensityForUser(int displayId, int density, int userId);
    void setForcedDisplayDensity(int displayId, int density);
    void clearForcedDisplayDensityForUser(int displayId, int userId);
    void setForcedDisplayScalingMode(int displayId, int mode); // 0 = auto, 1 = disable

    int getDefaultDisplayRotation();
    int getRotation();

}