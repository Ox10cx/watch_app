package com.watch.customer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ImageButton;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;

import com.uacent.watchapp.R;
import com.watch.customer.ui.CameraInterface.CamOpenOverCallback;
import com.watch.customer.util.DisplayUtil;

/**
 * Created by Administrator on 16-3-7.
 */
public class CameraActivity  extends BaseActivity implements CamOpenOverCallback {
    private static final String TAG = "hjq";
    CameraSurfaceView surfaceView = null;
    ImageButton shutterBtn;
    ImageButton switchCameraBtn;
    ImageButton settingBtn;
    ImageButton albumBtn;

    Thread cameraThread;

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

        cameraThread.start();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "cameraactivity onstop");


        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "cameraactivity onPause");
        CameraInterface.getInstance().doStopCamera();
        cameraThread = null;

        if (wakeLock != null) {
            wakeLock.release();
        }

        super.onPause();
    }

    private class BtnListeners implements OnClickListener{

        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch(v.getId()){
                case R.id.btn_shutter: {
                    CameraInterface.getInstance().doTakePicture(CameraActivity.this);
                    break;
                }

                case R.id.btn_album: {
//                    Intent intent = new Intent(Intent.ACTION_VIEW, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    intent.setType("image/*");
//                    startActivity(intent);
                    Intent intent = new Intent(CameraActivity.this, LocationActivity.class);

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
}
