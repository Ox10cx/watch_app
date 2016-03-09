package com.watch.customer.adapter;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.media.Image;
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
import com.watch.customer.model.BtDevice;
import com.watch.customer.model.Shop;
import com.watch.customer.ui.DeviceListActivity;

import java.util.ArrayList;

/**
 * Created by Administrator on 16-3-7.
 */
public class DeviceListAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<BtDevice> data;
    private Button curDel_btn;
    private float x,ux;

    public DeviceListAdapter(Context context, ArrayList<BtDevice> list)
    {
        this.context = context;
        data = list;
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

    @Override
    public View getView(final int position, View convertView, ViewGroup viewGroup) {
        ViewHolder holderView = null;

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

        Drawable d = context.getResources().getDrawable(R.drawable.defaultpic);
        holderView.image.setImageDrawable(d);

        holderView.name.setText(data.get(position).getName());

        if (data.get(position).getStatus() == BtDevice.CONNECTED) {
            holderView.status.setText(R.string.connected);
        } else {
            holderView.status.setText(R.string.disconnected);
        }

        holderView.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("hjq", "fffif");
                Button v = (Button) view;

                final BluetoothDevice device = data.get(position).getDevice();

                if (device == null) return;

                DeviceListActivity activity = (DeviceListActivity) context;

                if (data.get(position).getStatus() == BtDevice.CONNECTED) {
                    activity.disconnectBLE();
                    v.setText(R.string.connect);
                    data.get(position).setStatus(BtDevice.DISCONNECTED);

                } else {
                    if (activity.connectBLE(device.getAddress())) {
                        v.setText(R.string.disconnect);
                        data.get(position).setStatus(BtDevice.CONNECTED);
                    }
                }

                notifyDataSetChanged();
            }
        });

        if (data.get(position).getStatus() == BtDevice.CONNECTED) {
            holderView.button.setText(R.string.disconnect);
        } else {
            holderView.button.setText(R.string.connect);
        }

        holderView.right_arrow.setOnClickListener(new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                  Toast.makeText(context, "right arrow postion " + position, Toast.LENGTH_SHORT).show();
                  Log.d("hjq", "gggg");
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
