package com.watch.customer.ui;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;

import com.uacent.watchapp.R;
import com.watch.customer.dao.BtDeviceDao;
import com.watch.customer.model.BtDevice;
import com.watch.customer.util.SwitchButton;

/**
 * Created by Administrator on 16-3-11.
 */
public class FindmeSettingActivity extends BaseActivity {

    private static final int SELECT_RINGTONE = 1;
    BtDevice mDevice;
    BtDeviceDao mDao;
    private AudioManager mAudioManager;
    private SwitchButton lightSwitch;
    private SeekBar mVolumeBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_findme_setting);

        Intent i = getIntent();
        Bundle b = i.getExtras();

        mDevice  = (BtDevice) b.getSerializable("device");
        Log.d("hjq", "mDevice = " + mDevice);

        ImageView ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        lightSwitch = (SwitchButton) findViewById(R.id.switchLight);
        if (mDevice.isFindAlertSwitch()) {
            lightSwitch.setChecked(true);
        } else {
            lightSwitch.setChecked(false);
        }

        lightSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.d("hjq", "lightswitch is checked = " + isChecked);
                mDevice.setFindAlertSwitch(isChecked);
            }
        });

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        Log.d("hjq", "max volume = " + maxVolume);
        mVolumeBar = (SeekBar) findViewById(R.id.seekBarVolume);
        mVolumeBar.setMax(maxVolume);
        mVolumeBar.setProgress(mDevice.getAlertVolume());
        mVolumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if (seekBar == mVolumeBar) {
                    Log.d("hjq", "value in seekbar = " + i);
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, i, 0);
                    mDevice.setFindAlertVolume(i);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        View ll_ringtone = findViewById(R.id.ll_ringtone);
        ll_ringtone.setOnClickListener(this);

        mDao = new BtDeviceDao(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back: {
                Intent intent = new Intent();
                Bundle b = new Bundle();
                b.putSerializable("device", mDevice);
                intent.putExtras(b);
                //设置返回数据
                setResult(RESULT_OK, intent);
                finish();
                break;
            }

            case R.id.ll_ringtone: {
                Intent i = new Intent(FindmeSettingActivity.this, RingtoneSelectActivity.class);
                int index = -1;
                if (mDevice.getFindAlertRingtone() > 0) {
                    index = RingtoneSelectActivity.getIndexFromResid(mDevice.getFindAlertRingtone());
                }

                if (index < 0) {
                    index = 0;
                }

                Log.d("hjq", "index = " + index);
                i.putExtra("index", index);

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
                    int id = bundle.getInt("audio_id");

                    //Toast.makeText(this, "audio id = " + id, Toast.LENGTH_SHORT).show();
                    mDevice.setFindAlertRingtone(id);

                    mDao.deleteById(mDevice.getId());
                    Log.d("hjq", "d = " + mDevice);
                    mDao.insert(mDevice);

                    break;
                }

                default:
                    break;
            }

            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
