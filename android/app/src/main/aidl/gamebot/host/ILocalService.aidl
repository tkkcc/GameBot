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
    void stopConfigUIEvent(int token);
    void sendReRenderConfigUIEvent(int token);
    void sendExitConfigUIEvent(int token);
}