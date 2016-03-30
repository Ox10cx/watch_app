package com.watch.customer.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.uacent.watchapp.R;
import com.watch.customer.passlock.AbstractAppLock;
import com.watch.customer.passlock.AppLockManager;
import com.watch.customer.util.CommonUtil;
import com.watch.customer.util.DialogUtil;
import com.watch.customer.util.HttpUtil;
import com.watch.customer.util.JsonUtil;
import com.watch.customer.util.ThreadPoolManager;
import com.watch.customer.util.UpdateManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Administrator on 16-3-7.
 */
public class SettingActivity  extends BaseActivity {
    SharedPreferences mSharedPreferences;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            closeLoadingDialog();
            String result = msg.obj.toString();
            try {
                final JSONObject json = new JSONObject(result);
                if (json.getInt(JsonUtil.CODE) == 1) {
                    Log.e("hjq", "msg is = " + json.getString(JsonUtil.MSG));
                    showLongToast(json.getString(JsonUtil.MSG));
                } else {
                    final String path = json.getString(JsonUtil.PATH);
                    final String updatemsg = json.getString(JsonUtil.MSG);

                    DialogUtil.showDialog(SettingActivity.this, "发现新版本！",
                            json.getString(JsonUtil.MSG) + ", 是否要更新？",
                            getString(R.string.system_sure),
                            getString(R.string.system_cancel),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface arg0, int arg1) {
                                    // TODO Auto-generated method stub
                                    UpdateManager mUpdateManager = new UpdateManager(
                                            SettingActivity.this,
                                            updatemsg,
                                            HttpUtil.SERVER + path);
                                    mUpdateManager.showDownloadDialog();
                                }
                            }, null, true);
                }
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.setting_activity);

        Switch sw = (Switch) findViewById(R.id.switchNotDisturb);
        sw.setOnClickListener(this);

        ImageView iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(this);

        LinearLayout ll_password = (LinearLayout)findViewById(R.id.ll_password);
        ll_password.setOnClickListener(this);

        LinearLayout ll_map = (LinearLayout)findViewById(R.id.ll_map);
        ll_map.setOnClickListener(this);

        LinearLayout ll_dblclick = (LinearLayout)findViewById(R.id.ll_dblclick);
        ll_dblclick.setOnClickListener(this);

        LinearLayout ll_recordlist = (LinearLayout)findViewById(R.id.ll_recordlist);
        ll_recordlist.setOnClickListener(this);

        LinearLayout ll_userinfo = (LinearLayout)findViewById(R.id.ll_userinfo);
        ll_userinfo.setOnClickListener(this);

        TextView txtStatus = (TextView) findViewById(R.id.textstatus);

        AbstractAppLock appLock = AppLockManager.getInstance().getCurrentAppLock();
        if (!appLock.isPasswordLocked()) {
            txtStatus.setText("Closed");
        } else {
            txtStatus.setText("Open");
        }
        mSharedPreferences = getSharedPreferences("watch_app", 0);

        int disturb = mSharedPreferences.getInt("disturb_status", 0);
        if (disturb == 0) {
            sw.setChecked(false);
        } else {
            sw.setChecked(true);
        }

        Button button = (Button) findViewById(R.id.bt_checkupdate);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoadingDialog("正在检查版本..");
                ThreadPoolManager.getInstance().addTask(new Runnable() {

                    @Override
                    public void run() {
                        // TODO Auto-generated method stub
                        Log.e("hjq", "version=" + CommonUtil.getVersionName(SettingActivity.this));
                        String result = HttpUtil.post(HttpUtil.URL_ANDROIDUPDATE,
                                new BasicNameValuePair(JsonUtil.VERSION, CommonUtil.getVersionName(SettingActivity.this)));
                        Message msg = new Message();
                        msg.obj = result;
                        mHandler.sendMessage(msg);
                    }
                });
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.switchNotDisturb: {
                int disturb = mSharedPreferences.getInt("disturb_status", 0);
                if (disturb == 0) {
                    disturb = 1;
                } else {
                    disturb = 0;
                }

                SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                mEditor.putInt("disturb_status", disturb);
                mEditor.apply();

                break;
            }

            case R.id.ll_password: {
                Intent intent = new Intent(SettingActivity.this, PasswordSettingActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }

            case R.id.ll_map: {
                Intent intent = new Intent(SettingActivity.this, MapSelectActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.ll_userinfo: {
                Intent intent = new Intent(SettingActivity.this, PersonInfoActivity.class);
                startActivity(intent);
                break;
            }

            case R.id.ll_dblclick:
                break;

            case R.id.ll_recordlist:
                break;

            case R.id.iv_back: {
                finish();
                break;
            }

            default:
                break;
        }

        super.onClick(v);
    }

    @Override
    protected void onResume() {
        super.onResume();

        TextView txtStatus = (TextView) findViewById(R.id.textstatus);
        AbstractAppLock appLock = AppLockManager.getInstance().getCurrentAppLock();
        if (!appLock.isPasswordLocked()) {
            txtStatus.setText("Closed");
        } else {
            txtStatus.setText("Open");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
