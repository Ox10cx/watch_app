package com.watch.customer.ui;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import com.watch.customer.app.MyApplication;
import com.watch.customer.model.Shop;
import com.watch.customer.util.DialogUtil;

public class BaseActivity extends Activity implements OnClickListener {
	private Dialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		MyApplication.getInstance().addActivity(this);
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		MyApplication.getInstance().removeActivity(this);
	}

	public void showLongToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_LONG).show();
	}

	public void showShortToast(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}

	public void showLoadingDialog() {
		dialog = DialogUtil.createLoadingDialog(this, "正在加载中..");
		dialog.setCancelable(true);
		dialog.show();
	}

	public void showLoadingDialog(String msg) {
		dialog = DialogUtil.createLoadingDialog(this, msg);
		dialog.setCancelable(true);
		dialog.show();
	}

	public boolean closeLoadingDialog() {
		if (dialog != null && dialog.isShowing()) {
			dialog.dismiss();

			return true;
		} else {
			return false;
		}
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

	}
	public void toActivity(Context packageContext, Class<?> cls,Shop shop){
		Intent intent=new Intent(packageContext,cls);
		intent.putExtra("object", shop);
		startActivity(intent);
	}
}
