// ILocalService.aidl
package gamebot.host;

// Declare any non-default types here with import statements

interface ILocalService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void toast(String text);
    void test();
    void updateConfigUI(int token, in ParcelFileDescriptor pfd);
    ParcelFileDescriptor waitConfigUIEvent(int token) ;
    void stopConfigUI(int token);
    void sendEmptyConfigUIEvent(int token);

//    void startPackage(in String packageName);
//    void startActivity(in String packageName, in String className);
}