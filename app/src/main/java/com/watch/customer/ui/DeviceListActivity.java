package com.watch.customer.ui;

import android.app.ActionBar;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.uacent.watchapp.R;
import com.watch.customer.SlideDeleteListView;
import com.watch.customer.adapter.DeviceListAdapter;
import com.watch.customer.dao.BtDeviceDao;
import com.watch.customer.device.BluetoothAntiLostDevice;
import com.watch.customer.device.BluetoothLeClass;
import com.watch.customer.model.BtDevice;
import com.watch.customer.service.BleComService;
import com.watch.customer.util.CommonUtil;
import com.watch.customer.util.ImageLoaderUtil;
import com.watch.customer.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
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

    private Handler mHandler;
    private BtDeviceDao mDeviceDao;

    private IService mService;

    private final String TAG = "hjq";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_devicelist);

        ImageView seachbtn;
        ImageView testbtn;

        seachbtn = (ImageView) findViewById(R.id.search);
        testbtn = (ImageView) findViewById(R.id.testkey);

        seachbtn.setOnClickListener(this);
        testbtn.setOnClickListener(this);

        mHandler = new Handler();
        mDeviceDao = new BtDeviceDao(this);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }


        mDeviceList = (SlideDeleteListView)findViewById(R.id.devicelist);

        mDeviceList.setRemoveListener(new SlideDeleteListView.RemoveListener() {
            @Override
            public void removeItem(SlideDeleteListView.RemoveDirection direction, int position) {
                Toast.makeText(DeviceListActivity.this, "Item " + position + " has deleted",
                        Toast.LENGTH_SHORT).show();
                BtDevice d = mListData.get(position);
                mDeviceDao.deleteById(d.getAddress());

                mDeviceListAdapter.updateDataSet(position - mDeviceList.getHeaderViewsCount());

            }
        });

        mDeviceList.setOnItemClickListener(DeviceListActivity.this);

        fillListData();

        Intent i = new Intent(this, BleComService.class);
        getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void fillListData() {
        mListData = mDeviceDao.queryAll();

        mDeviceListAdapter = new DeviceListAdapter(
                DeviceListActivity.this, mListData);
        mDeviceList.setAdapter(mDeviceListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // update the setting.
        if (mListData.size() != 0) {
            int position = mDeviceListAdapter.getmId();
            BtDevice old = mListData.get(position);
            BtDevice d = mDeviceDao.queryById(old.getId());

            d.setStatus(old.getStatus());
            d.setRssi(old.getRssi());

            Log.d("hjq", "d = " + d);
            mListData.set(position, d);

            mDeviceListAdapter.notifyDataSetChanged();
        }
    }

    private void rescanDevice()
    {
        mListData.clear();
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void turnOnImmediateAlert() {
        try {
            mService.turnOnImmediateAlert();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void turnOffImmediateAlert() {
        try {
            mService.turnOffImmediateAlert();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        Log.d(TAG, "v = " + v);
        switch (v.getId()) {
            case R.id.search:

                break;

            case R.id.testkey:
                Log.d(TAG, "test key");
                break;

            default:
                break;
        }
    }

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void addDevice(final String address, final String name, final int rssi) throws RemoteException {
            Log.d("hjq", "addDevice called");

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    boolean deviceFound = false;
                    int i;
                    BtDevice d;

                    for (i = 0; i < mListData.size(); i++) {
                        if (mListData.get(i).getAddress().equals(address)) {
                            deviceFound = true;
                            break;
                        }
                    }

                    Log.d("hjq", "found device = " + deviceFound);

                    if (deviceFound) {
                        d = mListData.get(i);
                        d.setRssi(rssi);
                    } else {
                        d = new BtDevice();
                        d.setAddress(address);
                        d.setName(name);
                        d.setRssi(rssi);

                        mDeviceDao.insert(d);
                        mListData.add(0, d);
                    }

                    mDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onConnect(String address) throws RemoteException {
            Log.d("hjq", "onConnect called");
            int position = mDeviceListAdapter.getmId();
            mListData.get(position).setStatus(BluetoothAntiLostDevice.BLE_STATE_CONNECTED);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            Log.d("hjq", "onDisconnect called");
            int position = mDeviceListAdapter.getmId();
            mListData.get(position).setStatus(BluetoothAntiLostDevice.BLE_STATE_INIT);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onRead(String address, byte[] val) throws RemoteException {
            Log.d("hjq", "onRead called");
        }

        @Override
        public void onSignalChanged(String address, int rssi) throws RemoteException {
            Log.d("hjq", "onSignalChanged called");
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected");
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected");
            mService = IService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
                mService.scanBtDevices(true);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    };

    public boolean connectBLE(String address)
    {
        boolean ret = false;

        try {
            ret = mService.connect(address);

            if (ret) {
                Log.d(TAG, "connect to " + address + " success");
            } else {
                Log.d(TAG, "connect to " + address + " failed");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Log.d("hjq", "xxx id = " + id);
    }

}
