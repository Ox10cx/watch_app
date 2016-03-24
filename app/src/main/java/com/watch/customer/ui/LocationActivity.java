package com.watch.customer.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.BMapManager;

import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.GroundOverlayOptions;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.google.android.maps.MapController;
import com.google.android.maps.MyLocationOverlay;
import com.uacent.watchapp.R;
import com.watch.customer.model.LocationRecord;

import java.util.ArrayList;

/**
 * Created by Administrator on 16-3-7.
 */
public class LocationActivity  extends BaseActivity {
    private static final int LOCATION_GET_POSITION = 1;
    // 定位相关
    LocationClient mLocClient;
    public MyLocationListenner myListener = new MyLocationListenner();
    private MyLocationConfiguration.LocationMode mCurrentMode;
    BitmapDescriptor mCurrentMarker;
    private static final int accuracyCircleFillColor = 0xAAFFFF88;
    private static final int accuracyCircleStrokeColor = 0xAA00FF00;

    MapView mMapView;
    BaiduMap mBaiduMap;

    // UI相关
    RadioGroup.OnCheckedChangeListener radioButtonListener;
    Button requestLocButton;
    boolean isFirstLoc = true; // 是否首次定位
    private final String TAG = "hjq";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "location activity onCreate");

        setContentView(R.layout.activity_location);

        // 地图初始化
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        // 开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
        // 定位初始化

        Button bt_lost_history = (Button) findViewById(R.id.bt_lost_history);
        bt_lost_history.setOnClickListener(this);

        Button bt_loc_history = (Button) findViewById(R.id.bt_location_history);
        bt_loc_history.setOnClickListener(this);

        mLocClient = new LocationClient(this);
        mLocClient.registerLocationListener(myListener);
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("bd09ll"); // 设置坐标类型
        option.setScanSpan(50);
        mLocClient.setLocOption(option);
        mLocClient.start();
    }

    void addMarker(float longitude, float latitude) {
        //定义Maker坐标点
        LatLng point = new LatLng(latitude, longitude);
        //构建Marker图标
        BitmapDescriptor bitmap = BitmapDescriptorFactory
                .fromResource(R.drawable.icon_gcoding);
        //构建MarkerOption，用于在地图上添加Marker
        OverlayOptions option = new MarkerOptions()
                .position(point)
                .icon(bitmap);
        //在地图上添加Marker，并显示
        mBaiduMap.addOverlay(option);
    }

    /**
     * 定位SDK监听函数
     */
    public class MyLocationListenner implements BDLocationListener {

        @Override
        public void onReceiveLocation(BDLocation location) {
            // map view 销毁后不在处理新接收的位置
            Log.e("hjq", "onReceiveLocation");
            if (location == null || mMapView == null) {
                return;
            }
            MyLocationData locData = new MyLocationData.Builder()
                    .accuracy(location.getRadius())
                            // 此处设置开发者获取到的方向信息，顺时针0-360
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);

            String addr = location.getAddrStr();
            Log.e("hjq","addr2="+addr);
            Log.e("hjq","lon2="+location.getLongitude());
            Log.e("hjq", "lat2=" + location.getLatitude());

            if (isFirstLoc) {
                isFirstLoc = false;
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                builder.target(ll).zoom(18.0f);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
            }
        }

        public void onReceivePoi(BDLocation poiLocation) {
        }
    }

    @Override
    protected void onPause() {
        mMapView.onPause();
        Log.d(TAG, "location activity onpause");
        mMapView.setVisibility(View.INVISIBLE);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "location activity onResume");
        mMapView.onResume();
        mMapView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_location_history: {
                Intent intent = new Intent(LocationActivity.this, LocationRecordList.class);
                intent.putExtra("status", LocationRecord.FOUND);
                startActivityForResult(intent, LOCATION_GET_POSITION);
                break;
            }

            case R.id.bt_lost_history: {
                Intent intent = new Intent(LocationActivity.this, LocationRecordList.class);
                intent.putExtra("status", LocationRecord.LOST);
                startActivityForResult(intent, LOCATION_GET_POSITION);
                break;
            }

            default:
                break;
        }

        super.onClick(v);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "location activity onDestroy");
        // 退出时销毁定位
        mLocClient.stop();
        // 关闭定位图层
        mBaiduMap.setMyLocationEnabled(false);
        mMapView.onDestroy();
        mMapView = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == LOCATION_GET_POSITION) {
                float longitude = data.getFloatExtra("longitude", 0f);
                float latitude = data.getFloatExtra("latitude", 0f);
                Log.e("hjq", "long = " + longitude + " latitude =" + latitude);
                addMarker(longitude, latitude);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
}
