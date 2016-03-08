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

            holderView.btnDel = (Button) convertView.findViewById(R.id.delete_button);

            convertView.setTag(holderView);
        } else {
            holderView = (ViewHolder)convertView.getTag();
        }

        convertView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                final ViewHolder holder = (ViewHolder) v.getTag();
                //当按下时处理
                if (event.getAction() == MotionEvent.ACTION_DOWN) {

                    //                    //设置背景为选中状态
                    //                    v.setBackgroundResource(R.drawable.mm_listitem_pressed);
                    //获取按下时的x轴坐标
                    x = event.getX();
                    Log.d("hjq", "x = " + x);
                    //判断之前是否出现了删除按钮如果存在就隐藏
                    if (curDel_btn != null) {
                        if(curDel_btn.getVisibility() == View.VISIBLE){
                            curDel_btn.setVisibility(View.GONE);
                            return true;
                        }
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {// 松开处理
                    //设置背景为未选中正常状态
                    //v.setBackgroundResource(R.drawable.mm_listitem_simple);
                    //获取松开时的x坐标
                    ux = event.getX();
                    Log.d("hjq", "ux = " + ux);
                    //判断当前项中按钮控件不为空时
                    if (holder.btnDel != null) {
                        //按下和松开绝对值差当大于20时显示删除按钮，否则不显示
                        if (Math.abs(x - ux) > 20) {
                            holder.btnDel.setVisibility(View.VISIBLE);
                            curDel_btn = holder.btnDel;
                            return true;
                        }
                    }
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {//当滑动时背景为选中状态
                    Log.d("hjq", "motion move ");
                    return true;
                    //v.setBackgroundResource(R.drawable.mm_listitem_pressed);

                } else {//其他模式
                    //设置背景为未选中正常状态
                    //v.setBackgroundResource(R.drawable.mm_listitem_simple);
                }

                return true;
            }
        });

        Drawable d = context.getResources().getDrawable(R.drawable.defaultpic);
        holderView.image.setImageDrawable(d);

        holderView.name.setText(data.get(position).getName());
        holderView.status.setText("not working");

        holderView.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, "button postion " + position, Toast.LENGTH_SHORT).show();
                Log.d("hjq", "fffif");

                final BluetoothDevice device = data.get(position).getDevice();

                if (device == null) return;

                DeviceListActivity activity = (DeviceListActivity)context;
                activity.connectBLE(device.getAddress());
            }
        });

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
        public Button btnDel;
    }

}
