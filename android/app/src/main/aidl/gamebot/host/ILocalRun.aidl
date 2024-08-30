// ILocalRun.aidl
package gamebot.host;

// Declare any non-default types here with import statements

interface ILocalRun {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void call();
    void startTask();
    void showTask();
    void toast(in String text);
}