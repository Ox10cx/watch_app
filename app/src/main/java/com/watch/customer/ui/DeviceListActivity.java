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
import android.media.AudioManager;
import android.media.MediaPlayer;
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
import com.watch.customer.dao.BtDeviceDao;
import com.watch.customer.device.BluetoothAntiLostDevice;
import com.watch.customer.device.BluetoothLeClass;
import com.watch.customer.model.BtDevice;
import com.watch.customer.service.BleComService;
import com.watch.customer.util.CommonUtil;
import com.watch.customer.util.ImageLoaderUtil;
import com.watch.customer.util.PreventAntiLostCore;
import com.watch.customer.util.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private MediaPlayer mPlayer;

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
    }

    protected LayoutAnimationController getAnimationController() {
        int duration=300;
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
                        playAlertRingtone(d);
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

    private void collapse(final View v, Animation.AnimationListener al) {
        final int initialHeight = v.getMeasuredHeight();

        Animation anim = new Animation() {
            @Override
            protected void applyTransformation(float interpolatedTime, Transformation t) {
                if (interpolatedTime == 1) {
                    v.setVisibility(View.GONE);
                }
                else {
                    v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
                    v.requestLayout();
                }
            }

            @Override
            public boolean willChangeBounds() {
                return true;
            }
        };

        if (al != null) {
            anim.setAnimationListener(al);
        }

        anim.setDuration(500);
        v.startAnimation(anim);
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
    }

    private void playAlertRingtone(BtDevice d) {
        if (mPlayer != null) {
            //mPlayer.release();
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
                showLoadingDialog(getResources().getString(R.string.waiting));
                try {
                    mService.scanBtDevices(true);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.testkey: {
                Log.d(TAG, "test key");
                Toast.makeText(this,  R.string.prompt, Toast.LENGTH_SHORT).show();
                break;
            }

            default:
                break;
        }
    }

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void addDevice(final String address, final String name, final int rssi) throws RemoteException {
            //Log.d("hjq", "addDevice called");

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
                        d = new BtDevice();
                        d.setAddress(address);
                        d.setName(name);
                        d.setRssi(rssi);

                        mDeviceDao.insert(d);
                        mListData.add(d);
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
                    try {
                        mService.setAntiLost(false);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
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
                    checkAntiLost();
                }
            });
        }

        @Override
        public void onRead(String address, byte[] val) throws RemoteException {
            Log.d("hjq", "onRead called");
        }

        @Override
        public void onSignalChanged(String address, int rssi) throws RemoteException {
            Log.d("hjq", "onSignalChanged called address = " + address + " rssi = " + rssi);

            for (int i = 0; i < mListData.size(); i++) {
                BtDevice d = mListData.get(i);
                if (d.getAddress().equals(address)) {
                    d.setRssi(rssi);
                }
            }
        }

        public void onPositionChanged(String address, int position) throws RemoteException {
            Log.d("hjq", "onPositionChanged called address = " + address + " newpos = " + position);

            for (int i = 0; i < mListData.size(); i++) {
                BtDevice d = mListData.get(i);
                if (d.getAddress().equals(address)) {
                    d.setPosition(position);
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
                checkAntiLost();
                mService.scanBtDevices(true);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    };

    boolean checkAntiLost() {
        boolean ret = false;

        for (BtDevice d : mListData) {
            if (d.isAntiLostSwitch()) {
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
