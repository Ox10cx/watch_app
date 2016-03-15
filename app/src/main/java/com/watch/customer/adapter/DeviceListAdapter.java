package com.watch.customer.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.uacent.watchapp.R;
import com.watch.customer.app.MyApplication;
import com.watch.customer.device.BluetoothLeClass;
import com.watch.customer.model.BtDevice;
import com.watch.customer.model.Shop;
import com.watch.customer.ui.BtDeviceSettingActivity;
import com.watch.customer.ui.DeviceListActivity;
import com.watch.customer.util.CommonUtil;
import com.watch.customer.util.ImageLoaderUtil;

import java.util.ArrayList;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<BtDevice> data;
    private int mId;

    public DeviceListAdapter(Context context, ArrayList<BtDevice> list)
    {
        this.context = context;
        data = list;
        mId = 0;
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public int getmId() {
        return mId;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holderView;

        if (convertView == null) {
            holderView = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.device_item, null);
            holderView.image = (ImageView) convertView.findViewById(R.id.device_image);
            holderView.name = (TextView) convertView.findViewById(R.id.device_name);

            holderView.status = (TextView) convertView.findViewById(R.id.device_status);

            holderView.button = (Button) convertView.findViewById(R.id.device_button);

            holderView.right_arrow = (ImageView) convertView.findViewById(R.id.right_arrow);

            convertView.setTag(holderView);
        } else {
            holderView = (ViewHolder)convertView.getTag();
        }

        String path =  CommonUtil.getImageFilePath(data.get(position).getThumbnail());
       // Log.e("hjq", "path = " + path);
        if (path != null) {
            ImageLoaderUtil.displayImage("file://" + path, holderView.image, context);
        }  else {
            Drawable d = context.getResources().getDrawable(R.drawable.device_icon);
            holderView.image.setImageDrawable(d);
        }

        holderView.name.setText(data.get(position).getName());


        if (data.get(position).getStatus() == BluetoothLeClass.BLE_STATE_CONNECTED ||
                data.get(position).getStatus() == BluetoothLeClass.BLE_STATE_ALERTING) {
            holderView.status.setText(R.string.connected);
        } else if (data.get(position).getStatus() == BluetoothLeClass.BLE_STATE_CONNECTING) {
            holderView.status.setText(R.string.connecting);
        } else {
            holderView.status.setText(R.string.disconnected);
        }

        holderView.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("hjq", "fffif");
                Button v = (Button) view;

                final String address = data.get(position).getAddress();
                if (address == null) {
                    return;
                }

                DeviceListActivity activity = (DeviceListActivity) context;
                mId = position;
                int status = data.get(position).getStatus();

                Log.d("hjq", "status = " + status);
                switch (status) {
                    case BluetoothLeClass.BLE_STATE_CONNECTED: {
                        activity.turnOnImmediateAlert();
                        v.setText(R.string.stop_alert);
                        data.get(position).setStatus(BluetoothLeClass.BLE_STATE_ALERTING);
                        break;
                    }

                    case BluetoothLeClass.BLE_STATE_ALERTING: {
                        activity.turnOffImmediateAlert();
                        data.get(position).setStatus(BluetoothLeClass.BLE_STATE_CONNECTED);
                        v.setText(R.string.alert);
                        break;
                    }

                    case BluetoothLeClass.BLE_STATE_CONNECTING: {
                        break;
                    }

                    default:
                    case BluetoothLeClass.BLE_STATE_INIT:{
                        if (activity.connectBLE(address)) {
                            v.setText(R.string.disconnect);
                            data.get(position).setStatus(BluetoothLeClass.BLE_STATE_CONNECTING);
                        }
                        break;
                    }
                }

                notifyDataSetChanged();
            }
        });

        int status2 = data.get(position).getStatus();

      //  Log.d("hjq", "status2 = " + status2);
        switch (status2) {
            case BluetoothLeClass.BLE_STATE_CONNECTED: {
                holderView.button.setText(R.string.alert);
                break;
            }

            case BluetoothLeClass.BLE_STATE_ALERTING: {
                holderView.button.setText(R.string.stop_alert);
                break;
            }

            case BluetoothLeClass.BLE_STATE_CONNECTING: {
                break;
            }

            case BluetoothLeClass.BLE_STATE_INIT:
            default: {
                holderView.button.setText(R.string.connect);
                break;
            }
        }

        holderView.right_arrow.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  Log.d("hjq", "gggg");

                  mId = position;
                  Intent i = new Intent(context, BtDeviceSettingActivity.class);
                  BtDevice d = data.get(position);
                  Bundle b = new Bundle();
                  b.putSerializable("device", d);
                  i.putExtras(b);
                  context.startActivity(i);
              }
          }
        );

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    public void updateDataSet(int position) {
        Log.i("hjq", "update position =" + position);
        data.remove(position);
        notifyDataSetChanged();
    }

    private final static class ViewHolder {
        public ImageView image;
        public TextView name;
        public TextView status;
        public Button button;
        public ImageView right_arrow;
    }

}
