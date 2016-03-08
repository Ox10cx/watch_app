package com.watch.customer.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.uacent.watchapp.R;
import com.watch.customer.SlideDeleteListView;
import com.watch.customer.adapter.DeviceListAdapter;
import com.watch.customer.device.BluetoothLeClass;
import com.watch.customer.model.BtDevice;
import com.watch.customer.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListActivity  extends BaseActivity  implements View.OnClickListener,
        AdapterView.OnItemClickListener {

    private SlideDeleteListView mDeviceList;
    private DeviceListAdapter mDeviceListAdapter;
    private ArrayList<BtDevice> mListData;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler;
    private BluetoothLeClass mBLE;
    private boolean mScanning;

    private final String TAG = "hjq";
    private static final long SCAN_PERIOD = 10000; //10 seconds

    public static final UUID TEMP_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    public static final UUID POWER_SERVICE_UUID = UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb");
    public static final UUID BATTERY_SERVICE_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
    public static final UUID KEY_SERVICE_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicelist);

        mHandler = new Handler();

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        mDeviceList = (SlideDeleteListView)findViewById(R.id.devicelist);

        mDeviceList.setRemoveListener(new SlideDeleteListView.RemoveListener() {
            @Override
            public void removeItem(SlideDeleteListView.RemoveDirection direction, int position) {
                Toast.makeText(DeviceListActivity.this, "Item " + position + " has deleted",
                        Toast.LENGTH_SHORT).show();
                mDeviceListAdapter.updateDataSet(position - mDeviceList.getHeaderViewsCount());
            }
        });



        mDeviceList.setOnItemClickListener(DeviceListActivity.this);

        mBLE = new BluetoothLeClass(this);
        if (!mBLE.initialize()) {
            Log.e(TAG, "Unable to initialize Bluetooth");
            finish();
        }
        //发现BLE终端的Service时回调
        mBLE.setOnServiceDiscoverListener(mOnServiceDiscover);
        //收到BLE终端数据交互的事件
        mBLE.setOnDataAvailableListener(mOnDataAvailable);
    }

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
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    addDevice(device, rssi);
                                }
                            });
                        }
                    });
                }
            };

    @Override
    protected void onResume() {
        super.onResume();


        mListData = new ArrayList<BtDevice>(10);
        scanLeDevice(true);

        // Initializes list view adapter.
        mDeviceListAdapter = new DeviceListAdapter(
                DeviceListActivity.this, mListData);
        mDeviceList.setAdapter(mDeviceListAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();

        scanLeDevice(false);
        mListData.clear();
        mBLE.disconnect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mBLE.close();
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.search:

                break;

            case R.id.delete:

                break;

            default:
                break;
        }
    }

    private void addDevice(BluetoothDevice device, int rssi) {
        boolean deviceFound = false;
        int i;
        BtDevice d;

        for (i = 0; i < mListData.size(); i++) {
            if (mListData.get(i).getDevice().getAddress().equals(device.getAddress())) {
                deviceFound = true;
                break;
            }
        }

        if (deviceFound) {
            d = mListData.get(i);
            d.setRssi(rssi);
        } else {
            d = new BtDevice();
            d.setDevice(device);
            mListData.add(d);
        }

        mDeviceListAdapter.notifyDataSetChanged();
    }

    public void connectBLE(String address)
    {
        if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
        }

        if (mBLE.connect(address))  {
            Log.d(TAG, "connect to " + address + " success");
        } else {
            Log.d(TAG, "connect to " + address + " failed");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.d("hjq", "xxx");
    }

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
                        +Utils.bytesToHexString(characteristic.getValue()));
        }

        /**
         * 收到BLE终端写入数据回调
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic) {
            Log.e(TAG, "onCharWrite " + gatt.getDevice().getName()
                    + " write "
                    + characteristic.getUuid().toString()
                    + " -> "
                    + new String(characteristic.getValue()));
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        for (BluetoothGattService gattService : gattServices) {
            //-----Service的字段信息-----//
            int type = gattService.getType();
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

                //UUID_KEY_DATA是可以跟蓝牙模块串口通信的Characteristic
                if(gattCharacteristic.getUuid().toString().equals(KEY_SERVICE_UUID)){
                    //测试读取当前Characteristic数据，会触发mOnDataAvailable.onCharacteristicRead()
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mBLE.readCharacteristic(gattCharacteristic);
                        }
                    }, 500);

                    //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发mOnDataAvailable.onCharacteristicWrite()
                    mBLE.setCharacteristicNotification(gattCharacteristic, true);
                    //设置数据内容
                    gattCharacteristic.setValue("send data->");
                    //往蓝牙模块写入数据
                    mBLE.writeCharacteristic(gattCharacteristic);
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
