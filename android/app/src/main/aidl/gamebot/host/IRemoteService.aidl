// IRemoteService.aidl
package gamebot.host;

// Declare any non-default types here with import statements

interface IRemoteService {
    void setLocalRunBinder(in IBinder binder)=0;
    void start()=1;
    void destroy() = 16777114; // this is need for shizuku debuging
    void callback(in String msg)=2;
    void startGuest(in String name)=3;
    void stopGuest(in String name)=4;
    String startDownload(in String url, in String path,in String sha256sum, in boolean isRepo)=5;
    void stopDownload(in String path)=6;
}