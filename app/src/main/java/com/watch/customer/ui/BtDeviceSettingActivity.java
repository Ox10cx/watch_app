package com.watch.customer.ui;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.uacent.watchapp.R;
import com.watch.customer.dao.BtDeviceDao;
import com.watch.customer.model.BtDevice;
import com.watch.customer.service.BleComService;
import com.watch.customer.util.CommonUtil;
import com.watch.customer.util.ImageLoaderUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 16-3-10.
 */
public class BtDeviceSettingActivity extends BaseActivity {
    static public final String TAG = "hjq";

    ListView mList;
    EditText mEdit;
    protected static final int SELECT_PICTURE = 0;
    protected static final int SELECT_CAMER = 1;
    private final int editmsg_what = 1;
    private final int editimage_what = 2;
    private Bitmap bmp;
    private BtDevice mDevice;
    private BtDevice mOld;
    private ImageView ivIcon;
    private BtDeviceDao mDeviceDao;

    private IService mService;

    /***
     * 使用照相机拍照获取图片
     */
    public static final int SELECT_PIC_BY_TACK_PHOTO = 1;
    /***
     * 使用相册中的图片
     */
    public static final int SELECT_PIC_BY_PICK_PHOTO = 2;

    private static final int CUT_PHOTO = 3;

    public static final int CHANGE_ANTI_LOST_SETTING = 4;

    public static final int CHANGE_FIND_ME_SETTING = 5;

    private Uri photoUri;
    /**
     * 通过centerIndex来决定采用那种存储方式
     **/
    private int centerIndex;

    private static final int[] text_array = {R.string.str_anti_lost, R.string.str_find_me, R.string.str_disconnect};
    private static final int[] icon_array = {R.drawable.antilost_service_icon, R.drawable.found_service_icon, R.drawable.service};

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            String result = msg.obj.toString();
            Log.e("hjq", result);
            switch (msg.what) {
                case editmsg_what:
//                    try {
//                            mUserDao.update(mUser);
//                            showLongToast("修改完成");
//                            text_name.setText(mUser.getName());
//                            String sexstr = mUser.getSex().equals("1") ? "男" : "女";
//                            text_sex.setText(sexstr);
//                        }

                    break;

                case editimage_what:
                    mDevice.setThumbnail(result);
                    String path = CommonUtil.getImageFilePath(result);
                    if (path != null) {
                        ImageLoaderUtil.displayImage("file://" + path, ivIcon, BtDeviceSettingActivity.this);
                    } else {

                    }

                    break;

                default:
                    break;
            }
        }

        ;
    };
    private ICallback.Stub mCallback = new ICallback.Stub() {

        @Override
        public void onConnect(String address) throws RemoteException {

        }

        @Override
        public void onDisconnect(String address) throws RemoteException {

        }

        @Override
        public boolean onRead(String address, byte[] val) throws RemoteException {
            return false;
        }

        @Override
        public boolean onWrite(String address, byte[] val) throws RemoteException {
            return false;
        }

        @Override
        public void onSignalChanged(String address, int rssi) throws RemoteException {

        }

        public void onPositionChanged(String address, int rssi) throws RemoteException {

        }

        @Override
        public void onAlertServiceDiscovery(String address, boolean support) throws RemoteException {

        }

    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected2");
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected2");
            mService = IService.Stub.asInterface(service);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "", e);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_btdevice_setting);
        Intent i = getIntent();
        mDevice = (BtDevice) i.getSerializableExtra("device");
        mOld = mDevice.copy();

        ImageView ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        ivIcon = (ImageView) findViewById(R.id.imageView);
        ivIcon.setOnClickListener(this);

        String path = CommonUtil.getImageFilePath(mDevice.getThumbnail());
        if (path != null) {
            ImageLoaderUtil.displayImage("file://" + path, ivIcon, BtDeviceSettingActivity.this);
        } else {

        }

        TextView tv = (TextView) findViewById(R.id.device_text);
        tv.setText(mDevice.getName());
        mEdit = (EditText) findViewById(R.id.editText);
        mEdit.setText(mDevice.getName());

        mList = (ListView) findViewById(R.id.ls_listview);
        mList.setAdapter(new SimpleAdapter(this, getData(), R.layout.list_item2,
                new String[]{"icon", "text"},
                new int[]{R.id.img_icon, R.id.list_text}));

        mList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,
                                    long id) {

                Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(pos);

                if (pos == 0) {
                    Intent i = new Intent(BtDeviceSettingActivity.this, AntiLostSettingActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable("device", mDevice);
                    i.putExtras(b);
                    startActivityForResult(i, CHANGE_ANTI_LOST_SETTING);
                } else if (pos == 1) {
                    Intent i = new Intent(BtDeviceSettingActivity.this, FindmeSettingActivity.class);
                    Bundle b = new Bundle();
                    b.putSerializable("device", mDevice);
                    i.putExtras(b);
                    startActivityForResult(i, CHANGE_FIND_ME_SETTING);
                } else if (pos == 2) {
                    try {
                        mService.disconnect(mDevice.getAddress());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mDeviceDao = new BtDeviceDao(this);

        Intent intent = new Intent(this, BleComService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        for (int i = 0; i < text_array.length; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("text", getString(text_array[i]));
            map.put("icon", icon_array[i]);
            list.add(map);
        }

        return list;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    void goBack() {
        int val;

        mDevice.setName(mEdit.getText().toString());
        Log.d("hjq", "mDevice = " + mDevice);
        Log.d("hjq", "mOld = " + mOld);

        if (mDevice.equals(mOld)) {
            val = 0;
        } else {
            val = 1;
        }

        Log.d("hjq", "val = " + val);

        Intent intent = new Intent();
        Bundle b = new Bundle();
        b.putSerializable("device", mDevice);
        b.putInt("ret", val);
        intent.putExtras(b);

        ContentValues values = new ContentValues();
        values.put("name", mDevice.getName());
        values.put("thumbnail", mDevice.getThumbnail());

        int index = mDeviceDao.update(mDevice, values);
        Log.d("hjq", "index = " + index + " d = " + mDevice);

        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back: {
                goBack();
                break;
            }

            case R.id.imageView:
//                Intent i = new Intent(BtDeviceSettingActivity.this, SelectPicPopupWindow.class);
//                startActivity(i);
                getimage();
                break;
        }


        super.onClick(v);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            Log.e("hjq", "onBackPressed");

            goBack();
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onStart() {
        super.onStart();

    }

    @Override
    protected void onDestroy() {
        if (mConnection != null) {
            try {
                Log.i(TAG, "onDestroy->>unregisterCallback");
                mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        unbindService(mConnection);
        Log.i(TAG, "onDestroy->>unbindService");
        super.onDestroy();
    }

    public void getimage() {
        CharSequence[] items = {"相册", "相机"}; // 设置显示选择框的内容
        new AlertDialog.Builder(this).setTitle("选择图片来源")
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == SELECT_PICTURE) {
                            Intent choosePictureIntent = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(choosePictureIntent, SELECT_PIC_BY_PICK_PHOTO);
                        } else {
                            takePhoto();
                        }
                    }
                }).create().show();
    }

    private void takePhoto() {
        // TODO Auto-generated method stub
        // 执行拍照前，应该先判断SD卡是否存在
        String SDState = Environment.getExternalStorageState();
        if (!SDState.equals(Environment.MEDIA_MOUNTED)) {
            showShortToast("内存卡不存在");
            return;
        }
        try {
            photoUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    new ContentValues());
            if (photoUri != null) {
                Intent i = new Intent(
                        android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(i, SELECT_PIC_BY_TACK_PHOTO);

            } else {
                showShortToast("发生意外，无法写入相册");
            }
        } catch (Exception e) {
            // TODO: handle exception
            e.printStackTrace();
            showShortToast("发生意外，无法写入相册");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_PIC_BY_TACK_PHOTO:
                    // 选择自拍结果
                    beginCrop(photoUri);
                    break;

                case SELECT_PIC_BY_PICK_PHOTO:
                    // 选择图库图片结果
                    beginCrop(data.getData());
                    break;

                case CUT_PHOTO:
                    handleCrop(data);
                    break;

                case CHANGE_ANTI_LOST_SETTING:
                case CHANGE_FIND_ME_SETTING: {
                    Bundle bundle = data.getExtras();
                    BtDevice device = (BtDevice) bundle.getSerializable("device");
                    mDevice = device;
                    break;
                }
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onChoosePhoto() {
        // TODO Auto-generated method stub
        // 从相册中取图片
        Intent choosePictureIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(choosePictureIntent, SELECT_PIC_BY_PICK_PHOTO);
    }

    /**
     * 裁剪图片方法实现
     *
     * @param uri
     */
    public void beginCrop(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true");
        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        // outputX outputY 是裁剪图片宽高，注意如果return-data=true情况下,其实得到的是缩略图，并不是真实拍摄的图片大小，
        // 而原因是拍照的图片太大，所以这个宽高当你设置很大的时候发现并不起作用，就是因为返回的原图是缩略图，但是作为头像还是够清晰了
        intent.putExtra("outputX", 150);
        intent.putExtra("outputY", 150);
        //返回图片数据
        intent.putExtra("return-data", true);
        startActivityForResult(intent, CUT_PHOTO);
    }

    /**
     * 保存裁剪之后的图片数据
     *
     * @param result
     */
    private void handleCrop(Intent result) {
        Log.e("hjq", "handleCrop");
        Bundle extras = result.getExtras();
        if (extras != null) {
            bmp = extras.getParcelable("data");
            try {
                String filename = CommonUtil.generateShortUuid();
                CommonUtil.saveMyBitmap(bmp, filename);

                Message msg = new Message();
                msg.obj = filename;
                msg.what = editimage_what;
                mHandler.sendMessage(msg);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
