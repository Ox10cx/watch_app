package com.watch.customer.ui;

import android.animation.StateListAnimator;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.uacent.watchapp.R;
import com.watch.customer.xlistview.ItemMainLayout;
import com.watch.customer.xlistview.Menu;
import com.watch.customer.xlistview.MenuItem;
import com.watch.customer.xlistview.SlideAndDragListView;
import com.watch.customer.adapter.DeviceListAdapter;
import com.watch.customer.app.MyApplication;
import com.watch.customer.dao.BtDeviceDao;
import com.watch.customer.dao.LocationDao;
import com.watch.customer.device.BluetoothAntiLostDevice;
import com.watch.customer.device.BluetoothLeClass;
import com.watch.customer.model.BtDevice;
import com.watch.customer.model.LocationRecord;
import com.watch.customer.service.BleComService;
import com.watch.customer.util.PreferenceUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListActivity  extends BaseActivity  implements View.OnClickListener,
        AdapterView.OnItemClickListener, DeviceListAdapter.OnItemClickCallback, SlideAndDragListView.OnListItemLongClickListener,
        SlideAndDragListView.OnDragListener, SlideAndDragListView.OnSlideListener,
        SlideAndDragListView.OnListItemClickListener, SlideAndDragListView.OnMenuItemClickListener,
        SlideAndDragListView.OnItemDeleteListener{
    private static final int CHANGE_BLE_DEVICE_SETTING = 1;
    private SlideAndDragListView mDeviceList;
    private DeviceListAdapter mDeviceListAdapter;
    private ArrayList<BtDevice> mListData;
    private Handler mHandler;
    private BtDeviceDao mDeviceDao;
    private IService mService;
    boolean mScanningStopped;
    LocationDao mLocationDao;
    SharedPreferences mSharedPreferences;

    private final String TAG = "hjq";
    private Menu mMenu;
    private Handler myHandler;
    HandlerThread mHandlerThread;

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

        mDeviceList = (SlideAndDragListView)findViewById(R.id.devicelist);

        mDeviceList.setOnItemClickListener(DeviceListActivity.this);
        mDeviceList.setLayoutAnimation(getAnimationController());

        initMenu();
        initUiAndListener();
        fillListData();


        Intent i = new Intent(this, BleComService.class);
        getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);

        showLoadingDialog(getResources().getString(R.string.waiting));

        mHandlerThread = new HandlerThread("torchThread");
        mHandlerThread.start();
        myHandler = new Handler(mHandlerThread.getLooper());
    }

    public void initMenu() {
        mMenu = new Menu(new ColorDrawable(Color.WHITE), true);
        mMenu.addItem(new MenuItem.Builder().setWidth((int) getResources().getDimension(R.dimen.slv_item_bg_btn_width) * 2)
                .setBackground(new ColorDrawable(Color.RED))
                .setText(getString(R.string.system_delete))
                .setDirection(MenuItem.DIRECTION_RIGHT)
                .setTextColor(Color.BLACK)
                .setTextSize((int) getResources().getDimension(R.dimen.txt_size))
                .build());
    }

    public void initUiAndListener() {
        mDeviceList.setMenu(mMenu);
        mDeviceList.setOnListItemLongClickListener(this);
        mDeviceList.setOnDragListener(this, mListData);
        mDeviceList.setOnListItemClickListener(this);
        mDeviceList.setOnSlideListener(this);
        mDeviceList.setOnMenuItemClickListener(this);
        mDeviceList.setOnItemDeleteListener(this);
    }


    @Override
    protected void onDialogCancel() {
        super.onDialogCancel();

        Log.d("hjq", "mScanningStopped = " + mScanningStopped);
        if (!mScanningStopped) {
            scanLeDevice(false);
        }
        checkAntiLost();
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
                        if (d.isLostAlert()) {
                            flashTorch();
                        }
                    } else {
                        stopAnimation(i);
                        stopAlertRingtone(d);
                        ensureStopTorch();
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
        if (v.getAnimation() != null) {
            Log.e("hjq", "animation is running");
            return;
        }

        final Animation myAnimation = AnimationUtils.loadAnimation(this, R.anim.alpha_anim);
        myAnimation.setAnimationListener(new Animation.AnimationListener() {
                                             @Override
                                             public void onAnimationStart(Animation animation) {

                                             }

                                             @Override
                                             public void onAnimationEnd(Animation animation) {
                                                 if (v.getAnimation() != null) {
                                                     v.startAnimation(myAnimation);
                                                 }
                                             }

                                             @Override
                                             public void onAnimationRepeat(Animation animation) {

                                             }
                                         }
        );
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
            ItemMainLayout layout = (ItemMainLayout)wantedView;
            View v = layout.getItemCustomLayout().getCustomView();
            v.setBackgroundColor(getResources().getColor(R.color.text_white));
            wantedView.clearAnimation();
        }
    }

    void startAnimation(final int position) {
        int wantedChild;

        wantedChild = getActualPosition(position);
        View wantedView = mDeviceList.getChildAt(wantedChild);
        Log.e("hjq", "view = " + wantedView);
        if (wantedView != null) {
            ItemMainLayout layout = (ItemMainLayout)wantedView;
            View v = layout.getItemCustomLayout().getCustomView();
            v.setBackgroundColor(getResources().getColor(R.color.textbg_red));

            showItemViewAnimation(wantedView, position);
        }

        Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
        long [] pattern = {100, 400, 100, 400}; // 停止 开启 停止 开启
        vibrator.vibrate(pattern, -1); //重复两次上面的pattern 如果只想震动一次，index设为-1
    }

    Map<String, MediaPlayer> mPlayer = new HashMap<String, MediaPlayer>();

    private void stopAlertRingtone(final BtDevice d) {
        MediaPlayer player = mPlayer.get(d.getAddress());
        if (player == null) {
            Log.e("hjq", "warning: mediaplayer some thing error!");
            return;
        }

        player.stop();
        player.release();

        mPlayer.remove(d.getAddress());
    }

    private void playAlertRingtone(final BtDevice d) {
        MediaPlayer player = mPlayer.get(d.getAddress());
        if (player != null) {
            Log.e("hjq", "media player is ringing");
            return;
        }

        AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // 优先防丢报警
        if (d.isLostAlert() && d.isAntiLostSwitch()) {
            player = MediaPlayer.create(this, d.getAlertRingtone());
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, d.getAlertVolume(), 0);
        } else /* if ( d.isReportAlert()) */{
            player = MediaPlayer.create(this, d.getFindAlertRingtone());
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, d.getFindAlertVolume(), 0);
        }

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if (d.isLostAlert() && d.isAntiLostSwitch() || d.isReportAlert()) {
                    int disturb = mSharedPreferences.getInt("disturb_status", 0);
                    if (disturb == 0) {     // 免打扰模式没有打开，播放声音
                        mediaPlayer.start();
                    }
                } else {
                    mediaPlayer.release();
                    mPlayer.remove(d.getAddress());
                }
            }
        });

        mPlayer.put(d.getAddress(), player);
        player.setVolume(1.0f, 1.0f);
        player.start();
    }

    private void fillListData() {
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
        if (mConnection != null) {
            getApplicationContext().unbindService(mConnection);
        }

        mHandlerThread.quit();

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

        ensureStopTorch();
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
                int i;
                BtDevice d;

                for (i = 0; i < mListData.size(); i++) {
                    d = mListData.get(i);
                    if (d.getAddress().equals(address)) {
                        d.setRssi(rssi);
                        return;
                    }
                }

                BtDevice device;

                device = mDeviceDao.queryById(address);
                if (device == null) {
                    return;
                }

                device.setRssi(rssi);
                mListData.add(device);

                mDeviceListAdapter.notifyDataSetChanged();
            }
        });
    }

    private boolean mKeydownFlag;
    private Runnable mTimeoutCheck;

    void registerDatabase(BtDevice device) {
        BtDevice d;
        d = mDeviceDao.queryById(device.getAddress());
        if (d == null) {
            mDeviceDao.insert(device);
        }
    }

    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
            final BtDevice d;
            synchronized (mListData) {
                int position = mDeviceListAdapter.getmId();
                Log.d("hjq", "onConnect called position = " + position);
                d = mListData.get(position);
                d.setStatus(BluetoothAntiLostDevice.BLE_STATE_CONNECTED);
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    registerDatabase(d);
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
        public boolean onRead(String address, byte[] val) throws RemoteException {
            Log.d("hjq", "onRead called");

            return false;
        }

        void startAlert(String address) {
            for (int i = 0; i < mListData.size(); i++) {
                BtDevice d = mListData.get(i);
                if (d.getAddress().equals(address)) {
                    int disturb = mSharedPreferences.getInt("disturb_status", 0);
                    if (disturb == 0) {     // 免打扰模式没有打开，播放声音
                        playAlertRingtone(d);
                        d.setReportAlert(true);
                    }
                    startAnimation(i);
                    if (d.isFindAlertSwitch()) {
                        flashTorch();
                    }
                }
            }
        }

        @Override
        public boolean onWrite(final String address, byte[] val) throws RemoteException {
            Log.d("hjq", "onWrite called");
            byte v = val[0];
            if (mTimeoutCheck == null) {
                mTimeoutCheck = new Runnable() {
                    @Override
                    public void run() {
                        synchronized (mTimeoutCheck) {
                            mKeydownFlag = false;
                        }
                        Log.e("hjq", "one key down detect!");
                        recordLocHistory(address);
                    }
                };
            }

            synchronized (mTimeoutCheck) {
                if (mKeydownFlag && v == 1) {
                    mHandler.removeCallbacks(mTimeoutCheck);
                    mKeydownFlag = false;
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                        /* turn on alert */
                            Log.e("hjq", "double key down detect!");
                            startAlert(address);

                        }
                    });
                    return true;
                }
                if (v == 1) {
                    mKeydownFlag = true;
                    mHandler.postDelayed(mTimeoutCheck, 1000);
                }
            }

            return true;
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

        @Override
        public void onAlertServiceDiscovery(String btaddr, boolean support) throws RemoteException {
            for (int i = 0; i < mListData.size(); i++) {
                BtDevice d = mListData.get(i);
                if (d.getAddress().equals(btaddr)) {
                    d.setAlertService(support);
                   mHandler.post(new Runnable() {
                       @Override
                       public void run() {
                            mDeviceListAdapter.notifyDataSetChanged();
                       }
                   });
                }
            }
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
                Log.d("hjq", "oldstatus = " + oldstatus + " lostalert =" + d.isLostAlert());
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

    private void recordLocHistory(String btaddr) {
        if (MyApplication.getInstance().islocation == 0) {
            showShortToast(getString(R.string.str_wait_for_position));
            return;
        }

        String address = PreferenceUtil.getInstance(DeviceListActivity.this).getString(PreferenceUtil.LOCATION, "广东省深圳市");
        String longitude = PreferenceUtil.getInstance(DeviceListActivity.this).getString(PreferenceUtil.LON, "22");
        String latitude = PreferenceUtil.getInstance(DeviceListActivity.this).getString(PreferenceUtil.LAT, "105");
        long datetime = new Date().getTime();
        int status = LocationRecord.FOUND;

        LocationRecord r = new LocationRecord(-1, btaddr, longitude + "," + latitude, address, datetime, status);
        int id = mLocationDao.insert(r);
        r.setId(id);

        showShortToast(getString(R.string.str_position_success));
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
            Toast.makeText(this, R.string.str_connect_first, Toast.LENGTH_SHORT).show();
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

    @Override
    public void onListItemLongClick(View view, int position) {
       // Toast.makeText(DeviceListActivity.this, "onItemLongClick   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onListItemLongClick   " + position);
    }

    @Override
    public void onDragViewStart(int position) {
       // Toast.makeText(DeviceListActivity.this, "onDragViewStart   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onDragViewStart   " + position);
    }

    @Override
    public void onDragViewMoving(int position) {
//        Toast.makeText(DemoActivity.this, "onDragViewMoving   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i("yuyidong", "onDragViewMoving   " + position);
    }

    @Override
    public void onDragViewDown(int position) {
        //Toast.makeText(DeviceListActivity.this, "onDragViewDown   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onDragViewDown   " + position);
    }

    @Override
    public void onListItemClick(View v, int position) {
       // Toast.makeText(DeviceListActivity.this, "onItemClick   position--->" + position, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onListItemClick   " + position);
        if (position < 0) {
            return;
        }

        stopAnimation(position);
        BtDevice d = mListData.get(position);
        d.setReportAlert(false);
        stopAlertRingtone(d);

        ensureStopTorch();
    }

    @Override
    public void onSlideOpen(View view, View parentView, int position, int direction) {
     //   Toast.makeText(DeviceListActivity.this, "onSlideOpen   position--->" + position + "  direction--->" + direction, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onSlideOpen   " + position);
    }

    @Override
    public void onSlideClose(View view, View parentView, int position, int direction) {
   //     Toast.makeText(DeviceListActivity.this, "onSlideClose   position--->" + position + "  direction--->" + direction, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "onSlideClose   " + position);
    }

    @Override
    public int onMenuItemClick(View v, int itemPosition, int buttonPosition, int direction) {
        Log.i(TAG, "onMenuItemClick   " + itemPosition + "   " + buttonPosition + "   " + direction);
        switch (direction) {
            case MenuItem.DIRECTION_LEFT:
                switch (buttonPosition) {
                    case 0:
                        return Menu.ITEM_NOTHING;
                    case 1:
                        return Menu.ITEM_SCROLL_BACK;
                }
                break;

            case MenuItem.DIRECTION_RIGHT:
                switch (buttonPosition) {
                    case 0: {
                        BtDevice d = mListData.get(itemPosition);
                        try {
                            mService.disconnect(d.getAddress());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        mDeviceDao.deleteById(d.getAddress());
                        stopAnimation(itemPosition);
                        stopAlertRingtone(d);
                        ensureStopTorch();

                        mDeviceListAdapter.updateDataSet(itemPosition - mDeviceList.getHeaderViewsCount());

                        return Menu.ITEM_SCROLL_BACK;
                    }

                    case 1: {
                        return Menu.ITEM_DELETE_FROM_BOTTOM_TO_TOP;
                    }
                }
        }

        return Menu.ITEM_NOTHING;
    }

    @Override
    public void onItemDelete(View view, int position) {

    }

    Boolean mOn = new Boolean("false");
    Object mSync = new Object();
    boolean mStop = false;
    Camera mCamera;

    Runnable flashRun = new Runnable() {
        @Override
        public void run() {
            if (mStop && mOn) {
                turnOffTorch();
                return;
            }

            if (!mOn) {
                if (!turnOnTorch()) {
                    return;
                }
            } else {
                turnOffTorch();
            }

            myHandler.postDelayed(this, 1000);
        }
    };

    void ensureStopTorch(){
        mStop = true;
        if (mOn) {
            myHandler.removeCallbacks(flashRun);
            myHandler.post(new Runnable() {
                @Override
                public void run() {
                    turnOffTorch();
                }
            });

            synchronized (mSync) {
                try {
                    Log.e("hjq", "wait 1");
                    mSync.wait();
                    Log.e("hjq", "wait 2");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    void flashTorch() {
        mStop = false;
        myHandler.postDelayed(flashRun, 1000);
    }

    boolean turnOnTorch() {
        Log.e("hjq", "turn on");
        try {
            mCamera = Camera.open(0);
            Camera.Parameters p = mCamera.getParameters();
            p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

            mCamera.setParameters(p);
            mCamera.startPreview();
            mOn = true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("hjq", "open camera error!");
            return false;
        }

        return true;
    }

    void turnOffTorch() {
        Log.e("hjq", "turn off");
        Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(p);
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
        mOn = false;
        Log.e("hjq", "notify 1");
        synchronized (mSync) {
            mSync.notify();
        }
        Log.e("hjq", "notify 2");
    }

}
