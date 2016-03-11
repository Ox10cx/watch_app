package com.watch.customer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.uacent.watchapp.R;
import com.watch.customer.model.BtDevice;

/**
 * Created by Administrator on 16-3-11.
 */
public class AntiLostSettingActivity extends BaseActivity {
    private int button_selected = 1;
    Button mBtnear;
    Button mBtmiddle;
    Button mBtfar;
    final static int SELECT_RINGTONE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_antilost_setting);

        ImageView ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        mBtnear = (Button) findViewById(R.id.btn_near);
        mBtnear.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           mBtmiddle.setBackgroundColor(getResources().getColor(R.color.text_white));
                                           mBtfar.setBackgroundColor(getResources().getColor(R.color.text_white));
                                           mBtnear.setBackgroundColor(getResources().getColor(R.color.btnbg_orange));
                                       }
                                   }
        );
        mBtmiddle = (Button) findViewById(R.id.btn_middle);
        mBtmiddle.setOnClickListener(new View.OnClickListener() {
                                         @Override
                                         public void onClick(View view) {
                                             mBtnear.setBackgroundColor(getResources().getColor(R.color.text_white));
                                             mBtfar.setBackgroundColor(getResources().getColor(R.color.text_white));
                                             mBtmiddle.setBackgroundColor(getResources().getColor(R.color.btnbg_orange));

                                         }
                                     }
        );
        mBtfar = (Button) findViewById(R.id.btn_far);
        mBtfar.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View view) {
                                          mBtnear.setBackgroundColor(getResources().getColor(R.color.text_white));
                                          mBtmiddle.setBackgroundColor(getResources().getColor(R.color.text_white));
                                          mBtfar.setBackgroundColor(getResources().getColor(R.color.btnbg_orange));

                                      }
                                  }
        );

        mBtfar.setBackgroundColor(getResources().getColor(R.color.btnbg_orange));

        View ll_ringtone = findViewById(R.id.ll_ringtone);
        ll_ringtone.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back: {
                finish();
                break;
            }

            case R.id.ll_ringtone: {
                Intent i = new Intent(AntiLostSettingActivity.this, RingtoneSelectActivity.class);
                startActivityForResult(i, SELECT_RINGTONE);
                break;
            }
        }

        super.onClick(v);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case SELECT_RINGTONE: {
                    Bundle bundle = data.getExtras();
                    String path = bundle.getString("path");

                    Toast.makeText(this, "path = " + path, Toast.LENGTH_SHORT).show();
                    break;
                }
                default:
                    break;

            }
            super.onActivityResult(requestCode, resultCode, data);
        }

    }
}
