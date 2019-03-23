package com.example.xieyo.lifepieces;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MsgAdapter extends BaseAdapter {
    private List<Msg> mList = new ArrayList<>();
    private Msg data;
    private Context mContext;
    private LayoutInflater inflater;

    //定义常量,区分收发信息
    public static final int chat_left = 1;//收
    public static final int chat_right = 2;//发

    //构造器
    public MsgAdapter(Context mContext,List<Msg> mList) {
        this.mContext = mContext;
        this.mList = mList;
        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public Object getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolderleft viewHolderleft = null;
        ViewHolderright viewHolderright = null;
        int type = getItemViewType(i);
        if(view==null){
            //加载布局
            //判断左右信息，即是收到还是发出
            switch (type){
                case chat_left:
                    viewHolderleft = new ViewHolderleft();
                    view = inflater.inflate(R.layout.chat_dialog_left_item,null);
                    viewHolderleft.textView_left = (TextView) view.findViewById(R.id.tv_left_text);
                    view.setTag(viewHolderleft);
                    break;
                case chat_right:
                    viewHolderright = new ViewHolderright();
                    view = inflater.inflate(R.layout.chat_dialog_right_item,null);
                    viewHolderright.textView_right = (TextView) view.findViewById(R.id.tv_right_text);
                    view.setTag(viewHolderright);
                    break;
            }

        }else{
            //判断左右信息，即是收到还是发出
            switch (type){
                case chat_left:
                    viewHolderleft = (ViewHolderleft) view.getTag();
                    break;
                case chat_right:
                    viewHolderright = (ViewHolderright) view.getTag();
                    break;
            }

        }


        //赋值
        Msg data = mList.get(i);
        //判断左右信息，即是收到还是发出
        switch (data.getType()){
            case chat_left:
                viewHolderleft.textView_left.setText(data.getText());
                break;
            case chat_right:
                viewHolderright.textView_right.setText(data.getText());
                break;
        }
        return view;
    }

    //获取当前Item的类型
    @Override
    public int getItemViewType(int position) {
        Msg chatData= mList.get(position);
        int type = chatData.getType();
        return type;
    }

    //左边消息控件缓存
    class ViewHolderleft{
        private TextView textView_left;
    }

    //右边消息控件缓存
    class ViewHolderright{
        private TextView textView_right;
    }
    //返回所有Layout数据
    @Override
    public int getViewTypeCount() {
        return 3;
    }

}