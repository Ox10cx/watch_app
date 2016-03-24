package com.watch.customer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.SeekBar;
import android.widget.TextView;

import com.uacent.watchapp.R;
import com.watch.customer.service.BleComService;
import com.watch.customer.ui.CameraInterface.CamOpenOverCallback;
import com.watch.customer.util.DisplayUtil;


/**
 * Created by Administrator on 16-3-7.
 */
public class CameraActivity  extends BaseActivity implements CamOpenOverCallback, CameraInterface.OnFinishCallback {
    private static final String TAG = "hjq";
    CameraSurfaceView surfaceView = null;
    ImageButton shutterBtn;
    ImageButton switchCameraBtn;
    ImageButton settingBtn;
    ImageButton albumBtn;
    Handler mHandler;
    Handler myHandler;
    HandlerThread mHandlerThread;

    int mCount;
    int mInterval;

    Thread cameraThread;
    private IService mService;
    SharedPreferences mSharedPreferences;

    float previewRate = -1f;
    private PowerManager.WakeLock wakeLock;
    private boolean iswakeLock = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "cameraactivity oncreate");
        setContentView(R.layout.activity_camera);
        initUI();
        initViewParams();

        shutterBtn.setOnClickListener(new BtnListeners());
        switchCameraBtn.setOnClickListener(new BtnListeners());
        albumBtn.setOnClickListener(new BtnListeners());
        settingBtn.setOnClickListener(new BtnListeners());

        cameraThread = new Thread(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                CameraInterface.getInstance().doOpenCamera(CameraActivity.this);
            }
        };

        cameraThread.start();

        mHandler = new Handler();
        mSharedPreferences = getSharedPreferences("watch_app_preference", 0);

        mHandlerThread = new HandlerThread("takphone");
        mHandlerThread.start();
        myHandler = new Handler(mHandlerThread.getLooper());

        Intent i = new Intent(CameraActivity.this, BleComService.class);
        getApplicationContext().bindService(i, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }

    private void initUI(){
        surfaceView = (CameraSurfaceView)findViewById(R.id.camera_surfaceview);
        shutterBtn = (ImageButton)findViewById(R.id.btn_shutter);

        switchCameraBtn = (ImageButton) findViewById(R.id.btn_camera_switch);
        albumBtn = (ImageButton) findViewById(R.id.btn_album);
        settingBtn = (ImageButton) findViewById(R.id.btn_setting);

    }

    private void initViewParams(){
        LayoutParams params = surfaceView.getLayoutParams();
        Point p = DisplayUtil.getScreenMetrics(this);
        params.width = p.x;
        params.height = p.y;
        previewRate = DisplayUtil.getScreenRate(this); //默认全屏的比例预览
        surfaceView.setLayoutParams(params);

        //手动设置拍照ImageButton的大小为120dip×120dip,原图片大小是64×64
        LayoutParams p2 = shutterBtn.getLayoutParams();
        p2.width = DisplayUtil.dip2px(this, 80);
        p2.height = DisplayUtil.dip2px(this, 80);;
        shutterBtn.setLayoutParams(p2);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "cameraactivity onresume");

        // TODO Auto-generated method stub
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                | PowerManager.ON_AFTER_RELEASE, "DPA");

        if (iswakeLock) {
            wakeLock.acquire();
        }

        if (cameraThread != null) {
            return;
        }

        cameraThread = new Thread(){
            @Override
            public void run() {
                // TODO Auto-generated method stub
                CameraInterface.getInstance().doOpenCamera(CameraActivity.this);
            }
        };

        surfaceView.setVisibility(View.VISIBLE);
        cameraThread.start();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "cameraactivity onstop");
        unbindService(mConnection);
        mHandlerThread.quit();

        super.onStop();
    }

    void showDialog() {
        LayoutInflater inflater = getLayoutInflater();

        View layout = inflater.inflate(R.layout.camera_setting_dialog,
                (ViewGroup) findViewById(R.id.camera_setting_dialog));

        mCount = mSharedPreferences.getInt("continous", 0);
        mInterval = mSharedPreferences.getInt("interval", 0);   // mInterval 单位为0.1s

        final SeekBar sb_continous = (SeekBar) layout.findViewById(R.id.sb_continue);
        final SeekBar sb_interval = (SeekBar) layout.findViewById(R.id.sb_interval);
        final TextView tv_continous = (TextView) layout.findViewById(R.id.tv_continue);
        final TextView tv_interval = (TextView) layout.findViewById(R.id.tv_interval);
        View btn_ok = layout.findViewById(R.id.rl_ok);

        sb_continous.setProgress(mCount);
        sb_interval.setProgress(mInterval);
        tv_interval.setText((float)mInterval / 10.0f + " " + "second");
        tv_continous.setText(mCount + " " + "stretch");

        final AlertDialog dialog = new AlertDialog.Builder(this).setView(layout).show();
        btn_ok.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = sb_continous.getProgress();
                int interval = sb_interval.getProgress();

                SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                mEditor.putInt("continous", count);
                mEditor.putInt("interval", interval);
                mEditor.apply();

                dialog.dismiss();
            }
        });

        sb_interval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float interval = progress / 10.0f;
                tv_interval.setText(interval + " " + "second");

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        sb_continous.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_continous.setText(progress + " " + "stretch");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "cameraactivity onPause");
        CameraInterface.getInstance().doStopCamera();
        surfaceView.setVisibility(View.INVISIBLE);
        cameraThread = null;

        if (wakeLock != null) {
            wakeLock.release();
        }

        super.onPause();
    }

    void takePhoto() {
        mCount = mSharedPreferences.getInt("continous", 0);
        mInterval = mSharedPreferences.getInt("interval", 0);   // mInterval 单位为0.1s

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                    Log.e("hjq", "take photo mcount = " + mCount);
                    CameraInterface.getInstance().doTakePicture(CameraActivity.this, CameraActivity.this);
            }
        });
    }

    @Override
    public void onFinish() {
        if (mCount != 0) {
            myHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    CameraInterface.getInstance().doTakePicture(CameraActivity.this, CameraActivity.this);
                    mCount--;
                    Log.e("hjq", "take photo mcount = " + mCount);
                }
            }, mInterval * 100);
        }
    }

    private class BtnListeners implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch(v.getId()){
                case R.id.btn_shutter: {
                    takePhoto();
                    break;
                }

                case R.id.btn_album: {
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    startActivity(intent);
                    break;
                }

                case R.id.btn_camera_switch: {
                    CameraInterface.getInstance().switchCamera();
                    CameraInterface.getInstance().doStopCamera();

                    cameraThread = new Thread(){
                        @Override
                        public void run() {
                            // TODO Auto-generated method stub
                            CameraInterface.getInstance().doOpenCamera(CameraActivity.this);
                        }
                    };

                    cameraThread.start();
                    break;
                }

                case R.id.btn_setting: {
                    showDialog();
                    break;
                }

                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "camera activity onDestroy");
        super.onDestroy();
    }

    @Override
    public void cameraHasOpened() {
        // TODO Auto-generated method stub
        SurfaceHolder holder = surfaceView.getSurfaceHolder();
        CameraInterface.getInstance().doStartPreview(holder, previewRate);
    }
    private ICallback.Stub mCallback = new ICallback.Stub() {
        @Override
        public void onConnect(String address) throws RemoteException {
                Log.d("hjq", "onConnect address = " + address);
        }

        @Override
        public void onDisconnect(String address) throws RemoteException {
                Log.d("hjq", "onDisconnect called");
        }

        @Override
        public void onRead(String address, byte[] val) throws RemoteException {
            Log.e("hjq", "onRead called in camera");
        }

        @Override
        public void onWrite(String address, byte[] val) throws RemoteException {
            Log.e("hjq", "onWrite called in camera");
            int key = val[0];
            if (key == 1) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        takePhoto();
                    }
                });
            }
        }

        @Override
        public void onSignalChanged(String address, int rssi) throws RemoteException {
                Log.d("hjq", "onSignalChanged called address = " + address + " rssi = " + rssi);
        }

        public void onPositionChanged(String address, int position) throws RemoteException {
                Log.d("hjq", "onPositionChanged called address = " + address + " newpos = " + position);
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected in camera");
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, " onServiceConnected in camera");
            mService = IService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
                Log.e(TAG, "", e);
            }

        }
    };
}
