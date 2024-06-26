// IRemoteService.aidl
package gamebot.host;

// Declare any non-default types here with import statements

interface IRemoteService {
    void setLocalRunBinder(in IBinder binder)=0;
    void start()=1;
    void destroy() = 16777114; // this is need for shizuku debuging
    void callback(in String msg)=2;
}