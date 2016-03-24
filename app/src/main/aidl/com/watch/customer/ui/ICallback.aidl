// ICallback.aidl
package com.watch.customer.ui;

// Declare any non-default types here with import statements
interface ICallback {
       void onConnect(String address);
       void onDisconnect(String address);
       void onRead(String address, in byte[] val);
       void onWrite(String address, out byte[] val);
       void onSignalChanged(String address, int rssi);
       void onPositionChanged(String address, int position);
}
