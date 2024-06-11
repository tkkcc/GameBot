// IGameKeeperShizukuService.aidl
package gamebot.host;

// Declare any non-default types here with import statements

interface IGameKeeperShizukuService {
    void destroy() = 16777114; // Destroy method defined by Shizuku server

    void exit() = 1; // Exit method defined by user

    IBinder getRemoteRun() = 3;
}