package com.watch.customer.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.uacent.watchapp.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 16-3-11.
 */
public class RingtoneSelectActivity extends BaseActivity {
    private ListView mListView;
    private int lastPos;
    static final int NR_RINGTONE = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ringtone_select);

        ImageView ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);

        mListView = (ListView) findViewById(R.id.listview);
        mListView.setAdapter(new SimpleAdapter(this, getData(), R.layout.list_item3,
                new String[]{"text", "icon"},
                new int[]{R.id.list_text, R.id.img_icon}));

        lastPos = 0;

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int pos,
                                    long id) {

                Map<String, Object> item = (Map<String, Object>) parent.getItemAtPosition(pos);
                int wantedChild;
                ImageView iv;

                if (lastPos != pos) {
                    wantedChild = getActualPosition(lastPos);
                    View wantedView = mListView.getChildAt(wantedChild);
                    iv = (ImageView) wantedView.findViewById(R.id.img_icon);
                    iv.setVisibility(View.INVISIBLE);
                }

                wantedChild = getActualPosition(pos);
                View wantedView = mListView.getChildAt(wantedChild);
                iv = (ImageView) wantedView.findViewById(R.id.img_icon);
                iv.setVisibility(View.VISIBLE);
                lastPos = pos;
            }
        });

//        int wantedChild;
//        ImageView iv;
//        wantedChild = getActualPosition(lastPos);
//        View wantedView = mListView.getChildAt(wantedChild);
//        iv = (ImageView) wantedView.findViewById(R.id.img_icon);
//        iv.setVisibility(View.VISIBLE);
    }

    int getActualPosition(int pos) {
        int wantedPosition = pos; // Whatever position you're looking for
        int firstPosition = mListView.getFirstVisiblePosition() - mListView.getHeaderViewsCount(); // This is the same as child #0
        int wantedChild = wantedPosition - firstPosition;
        // Say, first visible position is 8, you want position 10, wantedChild will now be 2
        // So that means your view is child #2 in the ViewGroup:
        if (wantedChild < 0 || wantedChild >= mListView.getChildCount()) {
            Log.w("hjq", "Unable to get view for desired position, because it's not being displayed on screen.");
            return  -1;
        }

        return wantedChild;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_back: {
                finish();
                break;
            }

        }

        super.onClick(v);
    }


    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

        for (int i = 1; i <= NR_RINGTONE; i++) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("text", "Alarmtone " + i);
            map.put("icon", R.drawable.check_true);
            list.add(map);
        }

        return list;
    }

}
