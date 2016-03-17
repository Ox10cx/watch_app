// ICallback.aidl
package com.watch.customer.ui;

// Declare any non-default types here with import statements
interface ICallback {
       void addDevice(String address, String name, int rssi);
       void onConnect(String address);
       void onDisconnect(String address);
       void onRead(String address, in byte[] val);
       void onSignalChanged(String address, int rssi);
       void onPositionChanged(String address, int position);
}
