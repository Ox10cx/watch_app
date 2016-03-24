package com.watch.customer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.uacent.watchapp.R;
import com.watch.customer.SlideDeleteListView;
import com.watch.customer.adapter.LocationRecordAdapter;
import com.watch.customer.dao.LocationDao;
import com.watch.customer.model.LocationRecord;

import java.util.ArrayList;

/**
 * Created by Administrator on 16-3-24.
 */
public class LocationRecordList extends BaseActivity implements  AdapterView.OnItemClickListener{
    private SlideDeleteListView mDeviceList;
    private ArrayList<LocationRecord> mListData;
    private LocationRecordAdapter mAdapter;
    private LocationDao mDao;
    private int mStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.loc_recordlist_activity);

        Intent intent = getIntent();
        mStatus = intent.getIntExtra("status", LocationRecord.LOST);

        ImageView ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        mDeviceList = (SlideDeleteListView)findViewById(R.id.loclist);
        mDeviceList.setRemoveListener(new SlideDeleteListView.RemoveListener() {
            @Override
            public void removeItem(SlideDeleteListView.RemoveDirection direction, int position) {

            }
        });

        mDao = new LocationDao(LocationRecordList.this);
        mDeviceList.setOnItemClickListener(LocationRecordList.this);
        fillListData();
    }

    private void fillListData() {
        mListData = mDao.queryAll(mStatus);
        mAdapter = new LocationRecordAdapter(LocationRecordList.this, mListData);
        mDeviceList.setAdapter(mAdapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        LocationRecord r = mListData.get(position);
        String longlat = r.getLong_lat();
        String[] array = longlat.split(",");
        float longitude = 0f;
        float latitude = 0f;
        if (array.length == 2) {
            longitude = Float.parseFloat(array[0]);
            latitude = Float.parseFloat(array[1]);

            Intent intent = new Intent();
            intent.putExtra("longitude", longitude);
            intent.putExtra("latitude", latitude);

            setResult(RESULT_OK, intent);
        }

        finish();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back: {
                finish();
                break;
            }

            default:
                break;
        }


        super.onClick(v);
    }
}
