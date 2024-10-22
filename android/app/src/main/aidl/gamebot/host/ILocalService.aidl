// ILocalService.aidl
package gamebot.host;

// Declare any non-default types here with import statements

interface ILocalService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void toast(String text);
    void start();
    void updateConfigUI(in String name, in ParcelFileDescriptor pfd);
    ParcelFileDescriptor waitConfigUIEvent(in String name) ;
    void clearConfigUI(in String name);
    void sendEmptyConfigUIEvent(in String name);
    String cacheDir();
    void updateDownload(in String path, in float progress, in float bytePerSecond);

//    void startPackage(in String packageName);
//    void startActivity(in String packageName, in String className);
}