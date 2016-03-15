package com.watch.customer.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.uacent.watchapp.R;
import com.watch.customer.device.BluetoothAntiLostDevice;
import com.watch.customer.device.BluetoothLeClass;
import com.watch.customer.ui.AntiLostSettingActivity;
import com.watch.customer.ui.ICallback;
import com.watch.customer.ui.IService;
import com.watch.customer.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 16-3-15.
 */
public class BleComService extends Service {
    private static final String  TAG = "hjq";
    private static final int SCAN_PERIOD = 10000;
    private BluetoothAntiLostDevice mBLE;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();

    private RemoteCallbackList<ICallback> mCallbacks = new RemoteCallbackList<ICallback>();

    public class LocalBinder extends Binder {
        public BleComService getService() {
            return BleComService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        initialize();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        mCallbacks.kill();

        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();

        return super.onUnbind(intent);
    }

    private IService.Stub mBinder = new IService.Stub() {
        @Override
        public boolean initialize() throws RemoteException {
            return false;
        }

        @Override
        public boolean connect(String addr) throws RemoteException {
            return connectBtDevice(addr);
        }

        @Override
        public void disconnect() throws RemoteException {
            disconnectBtDevice();
        }

        public void unregisterCallback(ICallback cb){
            if (cb != null) {
                mCallbacks.unregister(cb);
            }
        }

        public void registerCallback(ICallback cb){
            if (cb != null) {
                mCallbacks.register(cb);
            }
        }

        public void turnOffImmediateAlert() {
            mBLE.turnOffImmediateAlert();
        }

        public void turnOnImmediateAlert() {
            mBLE.turnOnImmediateAlert();
        }

        public void scanBtDevices(boolean enable) {
            scanLeDevice(enable);
        }
    };

    boolean initialize() {
        mBLE = new BluetoothAntiLostDevice(BleComService.this);

        mBLE.initialize();

        //发现BLE终端的Service时回调
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        //收到BLE终端数据交互的事件
        mBLE.setOnDataAvailableListener(mOnDataAvailable);
        mBLE.setOnConnectListener(mOnConnectListener);
        mBLE.setOnDisconnectListener(mOnDisconnectListener);

        BluetoothManager mBluetoothManager;
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return false;
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();

        return true;
    }


    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    boolean connectBtDevice(String address) {
        boolean ret;

        ret = mBLE.connect(address);
        if (ret)  {
            Log.d(TAG, "connect to " + address + " success");
        } else {
            Log.d(TAG, "connect to " + address + " failed");
        }

        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    void disconnectBtDevice() {
        mBLE.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    void close() {
        mBLE.close();
    }

    private BluetoothLeClass.OnDisconnectListener mOnDisconnectListener = new BluetoothLeClass.OnDisconnectListener() {
        @Override
        public void onDisconnect(BluetoothGatt gatt) {
            int n = mCallbacks.beginBroadcast();
            Log.d("hjq", "n = " + n);
            try {
                int i;
                for (i = 0; i < n; i++) {
                    mCallbacks.getBroadcastItem(i).onDisconnect(gatt.getDevice().getAddress());
                }
            } catch (RemoteException e) {
                Log.e(TAG, "remote call exception", e);
            }
            mCallbacks.finishBroadcast();
        }
    };

    private BluetoothLeClass.OnConnectListener mOnConnectListener = new BluetoothLeClass.OnConnectListener() {
        @Override
        public void onConnect(BluetoothGatt gatt) {
                int n = mCallbacks.beginBroadcast();
                Log.d("hjq", "n = " + n);
                try {
                        int i;
                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).onConnect(gatt.getDevice().getAddress());
                        }
                } catch (RemoteException e) {
                    Log.e(TAG, "remote call exception", e);
                }
                mCallbacks.finishBroadcast();
        }

    };

    /**
     * 搜索到BLE终端服务的事件
     */
    private BluetoothLeClass.OnServiceDiscoverListener mOnServiceDiscover = new BluetoothLeClass.OnServiceDiscoverListener(){
        @Override
        public void onServiceDiscover(BluetoothGatt gatt) {
            displayGattServices(mBLE.getSupportedGattServices());
        }
    };

    /**
     * 收到BLE终端数据交互的事件
     */
    private BluetoothLeClass.OnDataAvailableListener mOnDataAvailable = new BluetoothLeClass.OnDataAvailableListener(){

        /**
         * BLE终端数据被读的事件
         */
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS)
                Log.e(TAG,"onCharRead "+gatt.getDevice().getName()
                        +" read "
                        +characteristic.getUuid().toString()
                        +" -> "
                        + Utils.bytesToHexString(characteristic.getValue()));
            //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }

        /**
         * 收到BLE终端写入数据回调
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic) {

            byte[] value = characteristic.getValue();

            Log.e(TAG, "onCharWrite " + gatt.getDevice().getName()
                    + " write "
                    + characteristic.getUuid().toString()
                    + " -> "
                    + Utils.bytesToHexString(value));

        }
    };

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    int n = mCallbacks.beginBroadcast();
                    Log.d("hjq", "n = " + n);
                    try {
                        int i;
                        for (i = 0; i < n; i++) {
                            mCallbacks.getBroadcastItem(i).addDevice(device.getAddress(), device.getName(),  rssi);
                        }
                    } catch (RemoteException e) {
                        Log.e(TAG, "remote call exception", e);
                    }
                    mCallbacks.finishBroadcast();
                }
            };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }

        for (BluetoothGattService gattService : gattServices) {
            //-----Service的字段信息-----//
            int type = gattService.getType();
            final UUID serviceUUID = gattService.getUuid();
            Log.e(TAG,"-->service type:"+Utils.getServiceType(type));
            Log.e(TAG,"-->includedServices size:"+gattService.getIncludedServices().size());
            Log.e(TAG,"-->service uuid:"+gattService.getUuid());

            //-----Characteristics的字段信息-----//
            List<BluetoothGattCharacteristic> gattCharacteristics =gattService.getCharacteristics();
            for (final BluetoothGattCharacteristic  gattCharacteristic: gattCharacteristics) {
                Log.e(TAG,"---->char uuid:"+gattCharacteristic.getUuid());

                int permission = gattCharacteristic.getPermissions();
                Log.e(TAG,"---->char permission:"+Utils.getCharPermission(permission));

                int property = gattCharacteristic.getProperties();
                Log.e(TAG,"---->char property:"+Utils.getCharPropertie(property));

                byte[] data = gattCharacteristic.getValue();
                if (data != null && data.length > 0) {
                    Log.e(TAG,"---->char value:"+new String(data));
                }

                //-----Descriptors的字段信息-----//
                List<BluetoothGattDescriptor> gattDescriptors = gattCharacteristic.getDescriptors();
                for (BluetoothGattDescriptor gattDescriptor : gattDescriptors) {
                    Log.e(TAG, "-------->desc uuid:" + gattDescriptor.getUuid());
                    int descPermission = gattDescriptor.getPermissions();
                    Log.e(TAG,"-------->desc permission:"+ Utils.getDescPermission(descPermission));

                    byte[] desData = gattDescriptor.getValue();
                    if (desData != null && desData.length > 0) {
                        Log.e(TAG, "-------->desc value:"+ new String(desData));
                    }
                }
            }
        }
    }
}
