package com.watch.customer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import com.uacent.watchapp.R;
import com.watch.customer.util.HttpUtil;
import com.watch.customer.util.ThreadPoolManager;

public class FirstActivity extends BaseActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_first);
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				Log.i("hjq", "postDelayed");
				finish();
				startActivity(new Intent(FirstActivity.this, MainActivity.class));
			}
		}, 3000);
	ThreadPoolManager.getInstance().addTask(new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			HttpUtil.get(HttpUtil.URL_INITAPP);
		}
	});
	}

	
}
