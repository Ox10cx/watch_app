package com.watch.customer.model;

import android.bluetooth.BluetoothDevice;
import android.media.Image;

import java.io.Serializable;

/**
 * Created by Administrator on 16-3-7.
 */
public class BtDevice implements Serializable {
   // private String name;
    private Image thumbnail;
    private BluetoothDevice device;
    private int rssi;

    public void setThumbnail(Image image) { this.thumbnail = image;}
    public void setDevice(BluetoothDevice d) { device = d; }
    public BluetoothDevice getDevice() { return device; }

    public String getName() { return device.getName(); }
    public Image getThumbnail() { return thumbnail; }
    public void setRssi(int s) { rssi = s; }

}
