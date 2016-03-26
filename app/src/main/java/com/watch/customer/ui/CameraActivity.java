package com.watch.customer.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Camera;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
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
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.uacent.watchapp.R;
import com.watch.customer.service.BleComService;
import com.watch.customer.ui.CameraInterface.CamOpenOverCallback;
import com.watch.customer.util.DisplayUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    ImageButton flashBtn;
    Handler mHandler;
    Handler myHandler;
    HandlerThread mHandlerThread;

    int mDegree;

    List<String> mSupportedFlashMode;

    int mCount;
    int mInterval;

    private IService mService;
    SharedPreferences mSharedPreferences;

    float previewRate = -1f;
    private PowerManager.WakeLock wakeLock;
    private boolean iswakeLock = true;

    boolean mCapturing = false;
    final Object mSync = new Object();

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
        flashBtn.setOnClickListener(new BtnListeners());

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
        flashBtn = (ImageButton) findViewById(R.id.ib_flash);
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

        surfaceView.setVisibility(View.VISIBLE);
        mDegree = getCameraDisplayOrientation(this, CameraInterface.getInstance().getCameraId());
        myHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraInterface.getInstance().doOpenCamera(CameraActivity.this);
            }
        });
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "cameraactivity onstop");



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

        myHandler.post(new Runnable() {
            @Override
            public void run() {
                CameraInterface.getInstance().doStopCamera();
                Log.d("hjq", "run in thread");
            }
        });
        surfaceView.setVisibility(View.INVISIBLE);
        if (wakeLock != null) {
            wakeLock.release();
        }

        super.onPause();
    }

    void takePhoto() {
        mCount = mSharedPreferences.getInt("continous", 0);
        mInterval = mSharedPreferences.getInt("interval", 0);   // mInterval 单位为0.1s

        synchronized (mSync) {
            mCapturing = true;
        }

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
        } else {
            synchronized (mSync) {
                mCapturing = false;
            }
        }

        Log.e("hjq", "mcapturing = " + mCapturing);
    }

    private class BtnListeners implements OnClickListener{
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            synchronized (mSync) {
                if (mCapturing) {
                    Toast.makeText(CameraActivity.this, "camera is busy", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

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
                    mDegree = getCameraDisplayOrientation(CameraActivity.this, CameraInterface.getInstance().getCameraId());
                    myHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.e("hjq", "here1");
                            CameraInterface.getInstance().doStopCamera();
                            Log.e("hjq", "here2");
                            CameraInterface.getInstance().doOpenCamera(CameraActivity.this);
                        }
                    });
                    break;
                }

                case R.id.btn_setting: {
                    showDialog();
                    break;
                }

                case R.id.ib_flash:{
                    if (mSupportedFlashMode == null) {
                        break;
                    }

                    LinearLayout ll = (LinearLayout) findViewById(R.id.ll_flash_second);
                    if (ll.getVisibility() == View.VISIBLE) {
                        ll.setVisibility(View.GONE);
                        break;
                    }

                    ll.setVisibility(View.VISIBLE);
                    Map<String, Boolean> map = new HashMap<>();

                    for (String mode : mSupportedFlashMode) {
                        Log.e("hjq", "mode = " + mode);
                        map.put(mode, true);
                    }

                    ImageButton ib_flash_auto = (ImageButton) ll.findViewById(R.id.ib_flash_auto);
                    if (map.get("auto") != null) {
                        ib_flash_auto.setOnClickListener(new BtnListeners());
                        ib_flash_auto.setVisibility(View.VISIBLE);
                    } else {
                        ib_flash_auto.setVisibility(View.GONE);
                    }

                    ImageButton ib_flash_on = (ImageButton) ll.findViewById(R.id.ib_flash_on);
                    if (map.get("on") != null) {
                        ib_flash_on.setOnClickListener(new BtnListeners());
                        ib_flash_on.setVisibility(View.VISIBLE);
                    } else {
                        ib_flash_on.setVisibility(View.GONE);
                    }

                    ImageButton ib_flash_off = (ImageButton) ll.findViewById(R.id.ib_flash_off);
                    if (map.get("off") != null) {
                        ib_flash_off.setOnClickListener(new BtnListeners());
                        ib_flash_off.setVisibility(View.VISIBLE);
                    } else {
                        ib_flash_off.setVisibility(View.GONE);
                    }

                    break;
                }

                case R.id.ib_flash_off:
                case R.id.ib_flash_auto:
                case R.id.ib_flash_on: {
                    changeFlashMode(v.getId());
                    LinearLayout ll = (LinearLayout) findViewById(R.id.ll_flash_second);
                    ll.setVisibility(View.INVISIBLE);

                    break;
                }

                default:
                    break;
            }
        }
    }

    void changeFlashMode(int resid) {
        String mode = "off";
        int drawid = R.drawable.flash_off;;
        if (resid ==  R.id.ib_flash_off) {
            mode = "off";
            drawid = R.drawable.flash_off;
        } else if (resid == R.id.ib_flash_auto) {
            mode = "auto";
            drawid = R.drawable.flash_auto;
        }  else if (resid == R.id.ib_flash_on) {
            mode = "on";
            drawid = R.drawable.flash_on;
        }

        flashBtn.setBackgroundResource(drawid);
        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
        mEditor.putString("flash_mode", mode);
        mEditor.apply();

        CameraInterface.getInstance().setFlashMode(mode);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "camera activity onDestroy");
        mHandlerThread.quit();
        unbindService(mConnection);
        super.onDestroy();
    }

    public static int getCameraDisplayOrientation(Activity activity,
                                                   int cameraId
                                                   ) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();

        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    @Override
    public void cameraHasOpened() {
        // TODO Auto-generated method stub
        SurfaceHolder holder = surfaceView.getSurfaceHolder();
        CameraInterface.getInstance().doStartPreview(holder, previewRate, mDegree);

        Log.e("hjq", "here 3");

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSupportedFlashMode = CameraInterface.getInstance().getSupportedFlashModes();
                if (mSupportedFlashMode == null) {
                    Log.e("hjq", " supported flash mode is null?");
                    return;
                } else {
                    Log.e("hjq", "list size = " + mSupportedFlashMode.size());
                }

                if (mSupportedFlashMode.size() > 0) {
                    String flash_mode = mSharedPreferences.getString("flash_mode", mSupportedFlashMode.get(0));
                    if (!mSupportedFlashMode.contains(flash_mode)) {
                        flash_mode = mSupportedFlashMode.get(0);
                        SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                        mEditor.putString("flash_mode", flash_mode);
                        mEditor.apply();
                    }

                    if ("auto".equals(flash_mode)) {
                        flashBtn.setBackgroundResource(R.drawable.flash_auto);
                    } else if ("on".equals(flash_mode)) {
                        flashBtn.setBackgroundResource(R.drawable.flash_on);
                    } else if ("off".equals(flash_mode)) {
                        flashBtn.setBackgroundResource(R.drawable.flash_off);
                    }
                } else {
                    flashBtn.setVisibility(View.INVISIBLE);
                }
                Log.e("hjq", "here 4");
                flashBtn.invalidate();
            }
        });
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
