// IRemoteService.aidl
package gamebot.host;

// Declare any non-default types here with import statements

interface IRemoteService {
    void setLocalRunBinder(in IBinder binder);
    void start();
}