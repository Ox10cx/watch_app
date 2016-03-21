package com.watch.customer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.uacent.watchapp.R;

/**
 * Created by Administrator on 16-3-7.
 */
public class SettingActivity  extends BaseActivity {
    private static final int CHANGE_PASSWORD_SETTING = 1;
    SharedPreferences mSharedPreferences;

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

        TextView txtStatus = (TextView) findViewById(R.id.textstatus);
        mSharedPreferences = getSharedPreferences("watch_app_preference", 0);
        int status = mSharedPreferences.getInt("password_status", 0);
        if (status == 0) {
            txtStatus.setText("Closed");
        } else {
            txtStatus.setText("Open");
        }

        int disturb = mSharedPreferences.getInt("disturb_status", 0);
        if (disturb == 0) {
            sw.setChecked(false);
        } else {
            sw.setChecked(true);
        }
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
                startActivityForResult(intent, CHANGE_PASSWORD_SETTING);
                break;
            }

            case R.id.ll_map:
                break;

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
        int status = mSharedPreferences.getInt("password_status", 0);
        if (status == 0) {
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
