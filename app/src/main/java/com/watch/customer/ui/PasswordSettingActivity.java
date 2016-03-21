package com.watch.customer.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.uacent.watchapp.R;


/**
 * Created by Administrator on 16-3-21.
 */
public class PasswordSettingActivity extends BaseActivity {
    private static final int SETUP_PASSWORD = 1;
    private static final int VERIFY_PASSWORD = 2;
    private static final int TURN_OFF_PASSWORD = 3;

    private SharedPreferences mSharedPreferences;
    private TextView txtChangePassword;
    private TextView txtTurnOnPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.password_list_activity);

        mSharedPreferences = getSharedPreferences("watch_app_preference", 0);

        RelativeLayout rl_password_status = (RelativeLayout) findViewById(R.id.ll_turn_password);
        rl_password_status.setOnClickListener(this);

        txtTurnOnPassword = (TextView) findViewById(R.id.text_turn_on_password);
        txtChangePassword = (TextView) findViewById(R.id.text_change_password);
        int val = mSharedPreferences.getInt("password_status", 0);
        if (val == 0) {
            txtChangePassword.setTextColor(getResources().getColor(R.color.TextColorDisable));
            txtTurnOnPassword.setText("Turn On Password");
        } else {
            txtTurnOnPassword.setText("Turn Off Password");
            RelativeLayout rl_change_password = (RelativeLayout) findViewById(R.id.ll_change_password);
            rl_change_password.setOnClickListener(this);
            txtChangePassword.setTextColor(getResources().getColor(R.color.TextColorBlack));
        }

        ImageView iv_back = (ImageView) findViewById(R.id.iv_back);
        iv_back.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_turn_password: {
                int val = mSharedPreferences.getInt("password_status", 0);

                if (val == 0) {
                    Intent intent = new Intent(this, InputPasswordActivity.class);
                    intent.putExtra("mode", InputPasswordActivity.MODE_INPUT);
                    intent.putExtra("title", txtTurnOnPassword.getText());
                    startActivityForResult(intent, SETUP_PASSWORD);
                } else {
                    Intent intent = new Intent(this, InputPasswordActivity.class);
                    intent.putExtra("mode", InputPasswordActivity.MODE_VERIFY);
                    intent.putExtra("title", txtTurnOnPassword.getText());
                    startActivityForResult(intent, TURN_OFF_PASSWORD);
                }

                break;
            }

            case R.id.ll_change_password: {
                Intent intent = new Intent(this, InputPasswordActivity.class);
                intent.putExtra("mode", InputPasswordActivity.MODE_VERIFY);
                intent.putExtra("title", txtChangePassword.getText());
                startActivityForResult(intent, VERIFY_PASSWORD);
                break;
            }

            case R.id.iv_back: {
                finish();
                break;
            }

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            if (requestCode == SETUP_PASSWORD) {
                int val = data.getIntExtra("password_status", 0);

                if (val == 0) {
                    txtChangePassword.setTextColor(getResources().getColor(R.color.TextColorDisable));
                    txtTurnOnPassword.setText("Turn On Password");
                } else {
                    txtTurnOnPassword.setText("Turn Off Password");
                    RelativeLayout rl_change_password = (RelativeLayout) findViewById(R.id.ll_change_password);
                    rl_change_password.setOnClickListener(this);
                    txtChangePassword.setTextColor(getResources().getColor(R.color.TextColorBlack));
                }
            } else if (requestCode == VERIFY_PASSWORD) {
                boolean val = data.getBooleanExtra("verify_status", false);

                if (val) {
                    Intent intent = new Intent(this, InputPasswordActivity.class);
                    intent.putExtra("mode", InputPasswordActivity.MODE_INPUT);
                    startActivityForResult(intent, SETUP_PASSWORD);
                } else {
                    Toast.makeText(this, "Your password is incorrect!", Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == TURN_OFF_PASSWORD) {
                boolean val = data.getBooleanExtra("verify_status", false);

                if (val) {
                    SharedPreferences.Editor mEditor = mSharedPreferences.edit();
                    mEditor.putInt("password_status", 0);
                    mEditor.apply();

                    txtTurnOnPassword = (TextView) findViewById(R.id.text_turn_on_password);
                    txtChangePassword = (TextView) findViewById(R.id.text_change_password);

                    txtTurnOnPassword.setText("Turn on Password");
                    RelativeLayout rl_change_password = (RelativeLayout) findViewById(R.id.ll_change_password);
                    rl_change_password.setOnClickListener(this);
                    txtChangePassword.setTextColor(getResources().getColor(R.color.TextColorBlack));
                } else {
                    Toast.makeText(this, "Your password is incorrect!", Toast.LENGTH_SHORT).show();
                }
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
