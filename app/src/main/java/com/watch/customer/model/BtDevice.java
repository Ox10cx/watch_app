package com.watch.customer.model;

import android.bluetooth.BluetoothDevice;
import android.media.Image;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Administrator on 16-3-7.
 */
public class BtDevice implements Serializable {
   // private String name;
    private Image thumbnail;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setAntiLostSwitch(boolean antiLostSwitch) {
        this.antiLostSwitch = antiLostSwitch;
    }

    public void setLostAlertSwitch(boolean lostAlertSwitch) {
        this.lostAlertSwitch = lostAlertSwitch;
    }

    public void setAlertDistance(int alertDistance) {
        this.alertDistance = alertDistance;
    }

    public void setAlertVolume(int alertVolume) {
        this.alertVolume = alertVolume;
    }

    public void setAlertRingtone(String alertRingtone) {
        this.alertRingtone = alertRingtone;
    }

    public void setFindAlertSwitch(boolean findAlertSwitch) {
        this.findAlertSwitch = findAlertSwitch;
    }

    public void setFindAlertVolume(int findAlertVolume) {
        this.findAlertVolume = findAlertVolume;
    }

    public void setFindAlertRingtone(String findAlertRingtone) {
        this.findAlertRingtone = findAlertRingtone;
    }

    private String name;
    private String address;

    public String getAddress() {
        return address;
    }

    public boolean isAntiLostSwitch() {
        return antiLostSwitch;
    }

    public boolean isLostAlertSwitch() {
        return lostAlertSwitch;
    }

    public int getAlertDistance() {
        return alertDistance;
    }

    public int getAlertVolume() {
        return alertVolume;
    }

    public String getAlertRingtone() {
        return alertRingtone;
    }

    public boolean isFindAlertSwitch() {
        return findAlertSwitch;
    }

    public int getFindAlertVolume() {
        return findAlertVolume;
    }

    public String getFindAlertRingtone() {
        return findAlertRingtone;
    }

    // Anti lost setting
    private boolean antiLostSwitch;
    private boolean lostAlertSwitch;
    private int alertDistance;

    public final static int ALERT_DISTANCE_NEAR = 0;
    public final static int ALERT_DISTANCE_MIDDLE = 0;
    public final static int ALERT_DISTANCE_FAR = 0;

    private int alertVolume;
    private String alertRingtone;

    //
    private boolean findAlertSwitch;
    private int findAlertVolume;
    private String findAlertRingtone;
    private int rssi;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    private int status;

    public int getRssi() {
        return rssi;
    }
    public void setThumbnail(Image image) { this.thumbnail = image;}
    public String getName() { return name; }
    public Image getThumbnail() { return thumbnail; }
    public void setRssi(int s) { rssi = s; }

}
