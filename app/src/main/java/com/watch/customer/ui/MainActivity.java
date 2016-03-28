package com.watch.customer.ui;

import android.app.AlertDialog;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TabHost;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.location.LocationClientOption.LocationMode;
import com.uacent.watchapp.R;
import com.watch.customer.app.MyApplication;
import com.watch.customer.passlock.InputPasswordActivity;
import com.watch.customer.util.DialogUtil;
import com.watch.customer.util.PreferenceUtil;

@SuppressWarnings("deprecation")
public class MainActivity extends TabActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private TabHost mTabHost;
    private RadioGroup mTabButtonGroup;
    public static final String TAB_MAIN = "SHOPLIST_ACTIVITY";
    public static final String TAB_BOOK = "ORDER_ACTIVITY";
    public static final String TAB_CATEGORY = "PERSON_ACTIVITY";

    public static final String TAB_DEVICE = "DEVICELIST_ACTIVITY";
    public static final String TAB_CAMERA = "CAMERA_ACTIVITY";
    public static final String TAB_LOCATION = "LOCATION_ACTIVITY";
    public static final String TAB_SETTING = "SETTING_ACTIVITY";
    public static final String TAB_INFO = "INFO_ACTIVITY";


    public static final String ACTION_TAB = "tabaction";
    private RadioButton rButton1, rButton2, rButton3;
    private RadioButton rButtonDevice;
    private RadioButton rButtonCamera;
    private RadioButton rButtonLocation;
    private RadioButton rButtonSetting;
    private RadioButton rButtonInfo;

    public LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    public static boolean isForeground = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById();
        initView();

        MyApplication.getInstance().addActivity(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_TAB);
        registerReceiver(TabReceiver, intentFilter);
        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationMode.Hight_Accuracy);
        option.setCoorType("bd09ll");
        option.setScanSpan(0);
        option.setNeedDeviceDirect(true);
        option.setIsNeedAddress(true);
        mLocClient.setLocOption(option);
        mLocClient.start();

        confirmBluetooth();

//		if (!PreferenceUtil.getInstance(this).getUid().equals("")) {
//			JPushInterface.setDebugMode(false); 	// 设置开启日志,发布时请关闭日志
//		    JPushInterface.init(this);
//		}
    }

    void confirmBluetooth(){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "本机没有找到蓝牙硬件或驱动！", Toast.LENGTH_SHORT).show();
            finish();
        }
        // 如果本地蓝牙没有开启，则开启
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        }
    }


    private void selectTab(int index) {
        // TODO Auto-generated method stub
        switch (index) {
            case 1:
                mTabHost.setCurrentTabByTag(TAB_MAIN);
                mTabButtonGroup.check(R.id.home_tab_device);
                break;

            case 2:
                mTabHost.setCurrentTabByTag(TAB_BOOK);
                mTabButtonGroup.check(R.id.home_tab_camera);
                break;

            case 3:
                mTabHost.setCurrentTabByTag(TAB_CATEGORY);
                mTabButtonGroup.check(R.id.home_tab_location);
                break;

            case 4:
                mTabHost.setCurrentTabByTag(TAB_SETTING);
                mTabButtonGroup.check(R.id.home_tab_setting);
                break;

            case 5:
                mTabHost.setCurrentTabByTag(TAB_INFO);
                mTabButtonGroup.check(R.id.home_tab_info);
                break;

            default:
                break;
        }
        changeTextColor(index);

    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();

        Log.e("hjq", "onResume," + "index=" + MyApplication.getInstance().type);
        selectTab(MyApplication.getInstance().type);
        MyApplication.getInstance().type = 0;
        isForeground = true;

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        isForeground = false;
    }

    private void findViewById() {
        mTabButtonGroup = (RadioGroup) findViewById(R.id.home_radio_button_group);


        rButtonDevice = (RadioButton) findViewById(R.id.home_tab_device);
        rButtonCamera = (RadioButton) findViewById(R.id.home_tab_camera);
        rButtonLocation = (RadioButton) findViewById(R.id.home_tab_location);
        rButtonSetting = (RadioButton) findViewById(R.id.home_tab_setting);
        rButtonInfo = (RadioButton) findViewById(R.id.home_tab_info);

        if (PreferenceUtil.getInstance(MainActivity.this).getUid().equals("")) {
            mTabButtonGroup.check(R.id.home_tab_device);
        }
    }

    private void initView() {
        mTabHost = getTabHost();

        Intent i_device = new Intent(this, DeviceListActivity.class);
        Intent i_camera = new Intent(this, CameraActivity.class);
        Intent i_location = new Intent(this, LocationActivity.class);
        Intent i_setting = new Intent(this, SettingActivity.class);
        Intent i_info = new Intent(this, InfoActivity.class);

        mTabHost.addTab(mTabHost.newTabSpec(TAB_DEVICE).setIndicator(TAB_DEVICE)
                .setContent(i_device));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_CAMERA).setIndicator(TAB_CAMERA)
                .setContent(i_camera));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_LOCATION).setIndicator(TAB_LOCATION)
                .setContent(i_location));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_SETTING).setIndicator(TAB_SETTING)
                .setContent(i_setting));
        mTabHost.addTab(mTabHost.newTabSpec(TAB_INFO).setIndicator(TAB_INFO)
                .setContent(i_info));

        mTabButtonGroup
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(RadioGroup group, int checkedId) {
                        switch (checkedId) {

                            case R.id.home_tab_device:
                                mTabHost.setCurrentTabByTag(TAB_DEVICE);
                                changeTextColor(1);
                                break;

                            case R.id.home_tab_camera:
                                mTabHost.setCurrentTabByTag(TAB_CAMERA);
                                changeTextColor(2);
                                break;

                            case R.id.home_tab_location:
                                mTabHost.setCurrentTabByTag(TAB_LOCATION);
                                changeTextColor(3);
                                break;

                            case R.id.home_tab_setting:
                                mTabHost.setCurrentTabByTag(TAB_SETTING);
                                changeTextColor(4);
                                break;

                            case R.id.home_tab_info:
                                mTabHost.setCurrentTabByTag(TAB_INFO);
                                changeTextColor(5);
                                break;

                            default:
                                break;
                        }
                    }
                });
    }

    private void changeTextColor(int index) {
        switch (index) {
            case 1:
                rButtonDevice.setTextColor(getResources().getColor(
                        R.color.textcolor_select));
                rButtonCamera.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonLocation.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonSetting.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonInfo.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                break;

            case 2:
                rButtonDevice.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonCamera.setTextColor(getResources().getColor(
                        R.color.textcolor_select));
                rButtonLocation.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonSetting.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonInfo.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                break;

            case 3:
                rButtonDevice.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonCamera.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonLocation.setTextColor(getResources().getColor(
                        R.color.textcolor_select));
                rButtonSetting.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonInfo.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                break;

            case 4:
                rButtonDevice.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonCamera.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonLocation.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonSetting.setTextColor(getResources().getColor(
                        R.color.textcolor_select));
                rButtonInfo.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                break;


            case 5:
                rButtonDevice.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonCamera.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonLocation.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonSetting.setTextColor(getResources().getColor(
                        R.color.textcolor_normal));
                rButtonInfo.setTextColor(getResources().getColor(
                        R.color.textcolor_select));
                break;

            default:
                break;
        }
    }

    /**
     * 含有标题、内容、两个按钮的对话框
     **/
    protected void showAlertDialog(String title, String message,
                                   String positiveText,
                                   DialogInterface.OnClickListener onPositiveClickListener,
                                   String negativeText,
                                   DialogInterface.OnClickListener onNegativeClickListener) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton(positiveText, onPositiveClickListener)
                .setNegativeButton(negativeText, onNegativeClickListener)
                .show();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            // 具体的操作代码
            Log.e("hjq", "onBackPressed");
            DialogUtil.showNoTitleDialog(MainActivity.this,
                    R.string.system_sureifexit, R.string.system_sure, R.string.system_cancel,
                    new OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            MyApplication.getInstance().exit();
                        }
                    }, new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub

                        }
                    }, true);
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        MyApplication.getInstance().removeActivity(this);
        unregisterReceiver(TabReceiver);
    }

    public class MyLocationListenner implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            Log.e("hjq", "onReceiveLocation");
            if (location == null) {
                return;
            }

            String addr = location.getAddrStr();
            Log.e("hjq", "addr=" + addr);
            Log.e("hjq", "lon=" + location.getLongitude());
            Log.e("hjq", "lat=" + location.getLatitude());

            if (addr != null) {
                String[] adds = getLocalMsg(addr);
                PreferenceUtil.getInstance(MainActivity.this).setString(PreferenceUtil.CITY, adds[1]);
                PreferenceUtil.getInstance(MainActivity.this).setString(PreferenceUtil.LOCATION, addr);
                PreferenceUtil.getInstance(MainActivity.this).setString(PreferenceUtil.LAT, "" + location.getLatitude());
                PreferenceUtil.getInstance(MainActivity.this).setString(PreferenceUtil.LON, "" + location.getLongitude());
                MyApplication.getInstance().islocation = 1;
                //sendBroadcast(new Intent(ShopListActivity.REFRESH_CITY));
            } else {
                Log.e("hjq", "location no found!!!");
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    public String[] getLocalMsg(String address) {
        int first, second;
        String[] str = new String[2];
        first = address.indexOf("省") + 1;
        if (first == 0) {
            first = address.indexOf("市") + 1;
            str[0] = address.substring(0, first - 1);
            str[1] = str[0];
        } else {
            second = address.indexOf("市") + 1;
            str[0] = address.substring(0, first);
            str[1] = address.substring(first, second - 1);
        }

        return str;
    }

    BroadcastReceiver TabReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            MyApplication.getInstance().type = 2;

        }
    };
}
