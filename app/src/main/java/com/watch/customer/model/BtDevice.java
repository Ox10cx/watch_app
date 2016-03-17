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
    private String thumbnail;

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

    public void setAlertRingtone(int alertRingtone) {
        this.alertRingtone = alertRingtone;
    }

    public void setFindAlertSwitch(boolean findAlertSwitch) {
        this.findAlertSwitch = findAlertSwitch;
    }

    public void setFindAlertVolume(int findAlertVolume) {
        this.findAlertVolume = findAlertVolume;
    }

    public void setFindAlertRingtone(int findAlertRingtone) {
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

    public int getAlertRingtone() {
        return alertRingtone;
    }

    public boolean isFindAlertSwitch() {
        return findAlertSwitch;
    }

    public int getFindAlertVolume() {
        return findAlertVolume;
    }

    public int getFindAlertRingtone() {
        return findAlertRingtone;
    }

    // Anti lost setting
    private boolean antiLostSwitch;
    private boolean lostAlertSwitch;
    private int alertDistance;

    public final static int ALERT_DISTANCE_NEAR = 1;
    public final static int ALERT_DISTANCE_MIDDLE = 2 ;
    public final static int ALERT_DISTANCE_FAR = 3;

    private int alertVolume;
    private int alertRingtone;

    //
    private boolean findAlertSwitch;
    private int findAlertVolume;
    private int findAlertRingtone;
    private int rssi;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    private int position;

    static public final int  LOST = 0;
    static public final int  FAR = 1;
    static public final int  MIDDLE = 2;
    static public final int  NEAR = 3;
    static public final int  OK = 4;

    public boolean isLostAlert() {
        return lostAlert;
    }

    public void setLostAlert(boolean lostAlert) {
        this.lostAlert = lostAlert;
    }

    private boolean lostAlert;

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
    public void setThumbnail(String image) { thumbnail = image;}
    public String getName() { return name; }
    public String getThumbnail() { return thumbnail; }
    public void setRssi(int s) { rssi = s; }

    @Override
    public String toString() {
        return "BtDevice{" +
                "thumbnail='" + thumbnail + '\'' +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", antiLostSwitch=" + antiLostSwitch +
                ", lostAlertSwitch=" + lostAlertSwitch +
                ", alertDistance=" + alertDistance +
                ", alertVolume=" + alertVolume +
                ", alertRingtone='" + alertRingtone + '\'' +
                ", findAlertSwitch=" + findAlertSwitch +
                ", findAlertVolume=" + findAlertVolume +
                ", findAlertRingtone='" + findAlertRingtone + '\'' +
                ", rssi=" + rssi +
                ", status=" + status +
                '}';
    }

    public String getId() { return address; }

    public BtDevice(String thumbnail, String name, String address, boolean antiLostSwitch,
                    boolean lostAlertSwitch, int alertDistance, int alertVolume,
                    int alertRingtone, boolean findAlertSwitch, int findAlertVolume, int findAlertRingtone) {
        this.thumbnail = thumbnail;
        this.name = name;
        this.address = address;
        this.antiLostSwitch = antiLostSwitch;
        this.lostAlertSwitch = lostAlertSwitch;
        this.alertDistance = alertDistance;
        this.alertVolume = alertVolume;
        this.alertRingtone = alertRingtone;
        this.findAlertSwitch = findAlertSwitch;
        this.findAlertVolume = findAlertVolume;
        this.findAlertRingtone = findAlertRingtone;

        rssi = -9999;
        status = -1;
        lostAlert = false;
        position = -1;

        if (name == null) {
            this.name = "unkown";
        }
    }

    public BtDevice()
    {
        thumbnail = "";
        name = "";
        address = "";
        antiLostSwitch = false;
        lostAlertSwitch = false;
        alertDistance = ALERT_DISTANCE_FAR;
        alertVolume = 0;
        alertRingtone = 0;
        findAlertSwitch = false;
        findAlertVolume = 0;
        findAlertRingtone = 0;

        position = -1;
        lostAlert = false;
    }

}
