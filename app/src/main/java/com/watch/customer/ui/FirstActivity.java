package com.watch.customer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;

import com.uacent.watchapp.R;
import com.watch.customer.dao.UserDao;
import com.watch.customer.model.User;
import com.watch.customer.util.HttpUtil;
import com.watch.customer.util.JsonUtil;
import com.watch.customer.util.PreferenceUtil;
import com.watch.customer.util.ThreadPoolManager;

import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import cn.jpush.android.api.JPushInterface;

public class FirstActivity extends BaseActivity {
	private String phone;
	private String password;
	private final String TAG = FirstActivity.class.getName();
    private final int MSG_LOGIN = 0;

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			String result = msg.obj.toString();
			closeLoadingDialog();
			Log.e(TAG, result);
			switch (msg.what) {
				case MSG_LOGIN: {
                    try {
                        JSONObject json = new JSONObject(result);
                        if (JsonUtil.getInt(json, JsonUtil.CODE) != 1) {
                            showLongToast(JsonUtil.getStr(json, JsonUtil.MSG));
                        } else {
                            JSONObject userobj = json.getJSONObject("user");
                            String id = userobj.getString(JsonUtil.ID);
                            String name = userobj.getString(JsonUtil.NAME);
                            String phone = userobj.getString(JsonUtil.PHONE);
                            String sex = userobj.getString(JsonUtil.SEX);
                            String password = userobj.getString(JsonUtil.PASSWORD);
                            String create_time = userobj.getString(JsonUtil.CREATE_TIME);
                            String image_thumb = userobj.getString(JsonUtil.IMAGE_THUMB);
                            String image = userobj.getString(JsonUtil.IMAGE);
                            User user = new User(id, name, phone, sex, password, create_time, image_thumb, image);
                            new UserDao(FirstActivity.this).insert(user);
                            showLongToast("µÇÂ¼³É¹¦");
                            PreferenceUtil.getInstance(FirstActivity.this).setUid(user.getId());
                            PreferenceUtil.getInstance(FirstActivity.this).getString(PreferenceUtil.PHONE, user.getPhone());

                            startActivity(new Intent(FirstActivity.this, MainActivity.class));
                        }
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    break;
                }

                default:
                    break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_first);

        ArrayList<User> list = new UserDao(this).queryAll();
		User user = null;

        if (list != null && !list.isEmpty())
        {
            user = list.get(0);
        }
		if (user != null) {
			phone = user.getPhone();
			password = user.getPassword();

			ThreadPoolManager.getInstance().addTask(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					String result = HttpUtil.post(HttpUtil.URL_LOGIN,
							new BasicNameValuePair(JsonUtil.PHONE, phone),
							new BasicNameValuePair(JsonUtil.PASSWORD,
									password));
					Log.e(TAG, result);

					Message msg = new Message();
					msg.obj = result;
					msg.what = MSG_LOGIN;
					mHandler.sendMessage(msg);
				}
			});

			showLoadingDialog();
		} else {
			new Handler().postDelayed(new Runnable() {
				@Override
				public void run() {
					Log.i(TAG, "postDelayed");
					finish();
					startActivity(new Intent(FirstActivity.this, AuthLoginActivity.class));
				}
			}, 3000);
		}

		ThreadPoolManager.getInstance().addTask(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				HttpUtil.get(HttpUtil.URL_INITAPP);
			}
		});
	}
}
