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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
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
import com.watch.customer.app.MyApplication;
import com.watch.customer.dao.BtDeviceDao;
import com.watch.customer.dao.LocationDao;
import com.watch.customer.device.BluetoothAntiLostDevice;
import com.watch.customer.device.BluetoothLeClass;
import com.watch.customer.model.BtDevice;
import com.watch.customer.model.LocationRecord;
import com.watch.customer.service.BleComService;
import com.watch.customer.util.CommonUtil;
import com.watch.customer.util.ImageLoaderUtil;
import com.watch.customer.util.PreferenceUtil;
import com.watch.customer.util.PreventAntiLostCore;
import com.watch.customer.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListActivity  extends BaseActivity  implements View.OnClickListener,
        AdapterView.OnItemClickListener, DeviceListAdapter.OnItemClickCallback {
    private static final int CHANGE_BLE_DEVICE_SETTING = 1;
    private SlideDeleteListView mDeviceList;
    private DeviceListAdapter mDeviceListAdapter;
    private ArrayList<BtDevice> mListData;
    private Handler mHandler;
    private BtDeviceDao mDeviceDao;
    private IService mService;
    private MediaPlayer mPlayer;
    boolean mScanningStopped;
    LocationDao mLocationDao;
    SharedPreferences mSharedPreferences;

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
        mLocationDao = new LocationDao(this);
        mSharedPreferences = getSharedPreferences("watch_app_preference", 0);

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
                BtDevice d = mListData.get(position);
                mDeviceDao.deleteById(d.getAddress());
                stopAnimation(position);
                mDeviceListAdapter.updateDataSet(position - mDeviceList.getHeaderViewsCount());
            }
        });

        mDeviceList.setOnItemClickListener(DeviceListActivity.this);
        mDeviceList.setLayoutAnimation(getAnimationController());
        fillListData();

        Intent i = new Intent(this, BleComService.class);
        getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        showLoadingDialog(getResources().getString(R.string.waiting));
    }

    @Override
    public boolean closeLoadingDialog() {
        boolean ret;

        ret = super.closeLoadingDialog();

        if (!mScanningStopped) {
            scanLeDevice(false);
        }

        checkAntiLost();
        return ret;
    }

    protected LayoutAnimationController getAnimationController() {
        int duration = 300;
        AnimationSet set = new AnimationSet(true);

        Animation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(duration);
        set.addAnimation(animation);

        animation = new TranslateAnimation(Animation.RELATIVE_TO_SELF, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
                -1.0f, Animation.RELATIVE_TO_SELF, 0.0f);
        animation.setDuration(duration);
        set.addAnimation(animation);

        LayoutAnimationController controller = new LayoutAnimationController(set, 0.5f);
        controller.setOrder(LayoutAnimationController.ORDER_NORMAL);
        return controller;
    }


    void checkUI()
    {
        final Runnable fnCheck = new Runnable() {
            @Override
            public void run() {
                boolean restart = false;

                for (int i = 0; i < mListData.size(); i++) {
                    BtDevice d = mListData.get(i);
                    if (d.isLostAlert() && d.isAntiLostSwitch()) {
                        int disturb = mSharedPreferences.getInt("disturb_status", 0);
                        if (disturb == 0) {     // 免打扰模式没有打开，播放声音
                            playAlertRingtone(d);
                        }
                        startAnimation(i);
                        restart = true;
                    } else {
                        stopAnimation(i);
                    }
                }

                if (restart) {
                    mHandler.postDelayed(this, 3000);
                }
            }
        };

        mHandler.postDelayed(fnCheck, 3000);
    }

    private void showItemViewAnimation(final View v, final int index) {
        Animation myAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha_anim);
        v.startAnimation(myAnimation);
    }

    int getActualPosition(int pos) {
        int firstPosition = mDeviceList.getFirstVisiblePosition() - mDeviceList.getHeaderViewsCount(); // This is the same as child #0
        int wantedChild = pos - firstPosition;
        // Say, first visible position is 8, you want position 10, wantedChild will now be 2
        // So that means your view is child #2 in the ViewGroup:
        if (wantedChild < 0 || wantedChild >= mDeviceList.getChildCount()) {
            Log.w("hjq", "Unable to get view for desired position, because it's not being displayed on screen.");
            return  -1;
        }
        return wantedChild;
    }

    void stopAnimation(final int position) {
        int wantedChild;

        wantedChild = getActualPosition(position);
        View wantedView = mDeviceList.getChildAt(wantedChild);
        if (wantedView != null) {
            wantedView.setTag(R.id.tag_second, true);
            wantedView.setBackgroundColor(getResources().getColor(R.color.text_white));
            wantedView.clearAnimation();
        }
    }

    void startAnimation(final int position) {
        int wantedChild;

        wantedChild = getActualPosition(position);
        View wantedView = mDeviceList.getChildAt(wantedChild);
        if (wantedView != null) {
            wantedView.setTag(R.id.tag_second, false);
            wantedView.setBackgroundColor(getResources().getColor(R.color.textbg_red));
            showItemViewAnimation(wantedView, position);
        }

        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        long [] pattern = {100, 400, 100, 400}; // 停止 开启 停止 开启
        vibrator.vibrate(pattern, -1); //重复两次上面的pattern 如果只想震动一次，index设为-1
    }

    private void playAlertRingtone(BtDevice d) {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            Log.d(TAG, "the player is busy now");
            return;
        }

        AudioManager mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, d.getAlertVolume(), 0);

        mPlayer = MediaPlayer.create(this, d.getAlertRingtone());
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mPlayer.release();
                mPlayer = null;
            }
        });

        mPlayer.setVolume(1.0f, 1.0f);
        mPlayer.start();
    }

    private void fillListData() {
      //  mListData = mDeviceDao.queryAll();
        mListData = new ArrayList<BtDevice>(10);

        mDeviceListAdapter = new DeviceListAdapter(
                DeviceListActivity.this, mListData, this);
        mDeviceList.setAdapter(mDeviceListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
    }

    private void scanLeDevice(final boolean enable) {
        BluetoothManager mBluetoothManager;
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (mBluetoothManager == null) {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return;
        }
        final BluetoothAdapter mBluetoothAdapter = mBluetoothManager.getAdapter();

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    Log.d("hjq", "stop scanning");
                    mScanningStopped = true;
                    closeLoadingDialog();
                }
            }, 10 * 1000);

            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mScanningStopped = false;
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanningStopped = true;
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
                    addDevice(device.getAddress(), device.getName(), rssi);
                }
            };

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void turnOnImmediateAlert(String addr) {
        try {
            mService.turnOnImmediateAlert(addr);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void turnOffImmediateAlert(String addr) {
        try {
            mService.turnOffImmediateAlert(addr);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        switch (v.getId()) {
            case R.id.search:
                showLoadingDialog(getResources().getString(R.string.waiting));
                scanLeDevice(true);
                break;

            case R.id.testkey: {
                Toast.makeText(this,  R.string.prompt, Toast.LENGTH_SHORT).show();
                break;
            }

            default:
                break;
        }
    }

    public void addDevice(final String address, final String name, final int rssi) {
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
                if (deviceFound) {
                    d = mListData.get(i);
                    d.setRssi(rssi);
                } else {
                    d = mDeviceDao.queryById(address);
                    if (d == null) {
                        d = new BtDevice();
                        d.setAddress(address);
                        d.setName(name);
                        d.setRssi(rssi);
                        mDeviceDao.insert(d);
                    } else {
                        d.setRssi(rssi);
                    }

                    mListData.add(d);
                }
                mDeviceListAdapter.notifyDataSetChanged();
            }
        });
    }

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
            synchronized (mListData) {
                int position = mDeviceListAdapter.getmId();
                Log.d("hjq", "onConnect called position = " + position);
                mListData.get(position).setStatus(BluetoothAntiLostDevice.BLE_STATE_CONNECTED);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
            synchronized (mListData) {
                Log.d("hjq", "onDisconnect called");
                int position = mDeviceListAdapter.getmId();
                mListData.get(position).setStatus(BluetoothAntiLostDevice.BLE_STATE_INIT);
                mListData.get(position).setPosition(BtDevice.LOST);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mDeviceListAdapter.notifyDataSetChanged();
                    checkAntiLost();
                }
            });
        }

        @Override
        public void onRead(String address, byte[] val) throws RemoteException {
            Log.d("hjq", "onRead called");
        }

        @Override
        public void onWrite(String address, byte[] val) throws RemoteException {
            Log.d("hjq", "onWrite called");
        }

        @Override
        public void onSignalChanged(String address, int rssi) throws RemoteException {
            synchronized (mListData) {
                Log.d("hjq", "onSignalChanged called address = " + address + " rssi = " + rssi);

                for (int i = 0; i < mListData.size(); i++) {
                    BtDevice d = mListData.get(i);
                    if (d.getAddress().equals(address)) {
                        d.setRssi(rssi);
                    }
                }
            }
        }

        public void onPositionChanged(String address, int position) throws RemoteException {
            synchronized (mListData) {
                Log.d("hjq", "onPositionChanged called address = " + address + " newpos = " + position);

                for (int i = 0; i < mListData.size(); i++) {
                    BtDevice d = mListData.get(i);
                    if (d.getAddress().equals(address)) {
                        d.setPosition(position);
                    }
                }
            }
            checkAntiLost();
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
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    scanLeDevice(true);
                }
            });
        }
    };

    boolean checkAntiLost() {
        boolean ret = false;
        boolean oldstatus;

        for (BtDevice d : mListData) {
            if (d.isAntiLostSwitch()) {
                oldstatus = d.isLostAlert();

                switch (d.getPosition()) {
                    case BtDevice.LOST:
                    case BtDevice.FAR: {
                        d.setLostAlert(true);
                        break;
                    }

                    case BtDevice.OK: {
                        d.setLostAlert(false);
                        break;
                    }

                    default:
                        break;
                }

                Log.d("hjq", "oldstatus = " + oldstatus + " lostalsert =" + d.isLostAlert());
                // 丢失状态变化了，记录这个变化
                if (oldstatus ^ d.isLostAlert()) {
                    recordLostHistory(d);
                }
                ret = true;
            }

        }

        if (ret) {
            try {
                mService.setAntiLost(true);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            checkUI();
        } else {
            try {
                mService.setAntiLost(false);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    private void recordLostHistory(BtDevice d) {
        if (MyApplication.getInstance().islocation == 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    showShortToast("waiting for positioning!");
                }
            });
            return;
        }

        String address = PreferenceUtil.getInstance(DeviceListActivity.this).getString(PreferenceUtil.LOCATION, "广东省深圳市");
        String longitude = PreferenceUtil.getInstance(DeviceListActivity.this).getString(PreferenceUtil.LON, "22");
        String latitude = PreferenceUtil.getInstance(DeviceListActivity.this).getString(PreferenceUtil.LAT, "105");
        long datetime = new Date().getTime();
        int status;
        if (d.isLostAlert()) {
            status = LocationRecord.LOST;
        } else {
            status = LocationRecord.FOUND;
        }

        LocationRecord r = new LocationRecord(-1, d.getAddress(), longitude + "," + latitude, address, datetime, status);
        int id = mLocationDao.insert(r);
        r.setId(id);
    }

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

    @Override
    public void onButtonClick(View view, int position) {
        Button v = (Button)view;
        BtDevice device = mListData.get(position);
        int status = device.getStatus();

        Log.d("hjq", "status = " + status);
        switch (status) {
            case BluetoothLeClass.BLE_STATE_CONNECTED: {
                turnOnImmediateAlert(device.getAddress());
                v.setText(R.string.stop_alert);
                device.setStatus(BluetoothLeClass.BLE_STATE_ALERTING);
                break;
            }

            case BluetoothLeClass.BLE_STATE_ALERTING: {
                turnOffImmediateAlert(device.getAddress());
                device.setStatus(BluetoothLeClass.BLE_STATE_CONNECTED);
                v.setText(R.string.alert);
                break;
            }

            case BluetoothLeClass.BLE_STATE_CONNECTING: {
                break;
            }

            default:
            case BluetoothLeClass.BLE_STATE_INIT: {
                synchronized (mListData) {
                    if (connectBLE(device.getAddress())) {
                        v.setText(R.string.disconnect);
                        device.setStatus(BluetoothLeClass.BLE_STATE_CONNECTING);
                    }
                }

                break;
            }
        }

       mDeviceListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRightArrowClick(int position) {
        BtDevice d = mListData.get(position);

        if (d.getStatus() == BluetoothLeClass.BLE_STATE_CONNECTED || d.getStatus() == BluetoothLeClass.BLE_STATE_ALERTING) {
            Intent i = new Intent(this, BtDeviceSettingActivity.class);
            Bundle b = new Bundle();
            b.putSerializable("device", d);
            i.putExtras(b);
            startActivityForResult(i, CHANGE_BLE_DEVICE_SETTING);
        } else {
            Toast.makeText(this, "connect device first", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == CHANGE_BLE_DEVICE_SETTING) {
                Bundle b = data.getExtras();
                int changed = b.getInt("ret", 0);

                Log.d("hjq", "changed = " + changed);

                if (changed == 1) {
                    int i;
                    BtDevice d = (BtDevice)b.getSerializable("device");
                    for (i = 0; i < mListData.size(); i++) {
                        if (mListData.get(i).getAddress().equals(d.getAddress())) {
                            break;
                        }
                    }
                    Log.d("hjq", "i = " + i + " ,d = " + d);
                    if (i != mListData.size()) {
                        mListData.remove(i);
                        mListData.add(i, d);
                        mDeviceListAdapter.notifyDataSetChanged();
                    }

                    checkAntiLost();
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
