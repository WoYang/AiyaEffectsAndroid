/*
 *
 * EffectSelectActivity.java
 * 
 * Created by Wuwang on 2017/2/25
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.JsonReader;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.aiyaapp.aiya.camera.LogUtils;
import com.aiyaapp.aiya.camera.MenuAdapter;
import com.aiyaapp.aiya.camera.MenuBean;
import com.aiyaapp.aiya.util.ClickUtils;
import com.aiyaapp.camera.sdk.AiyaEffects;
import com.aiyaapp.camera.sdk.base.ISdkManager;
import com.aiyaapp.camera.sdk.base.Log;

/**
 * Description:
 */
public class EffectSelectActivity extends AppCompatActivity {

    private ArrayList<MenuBean> mStickerData;
    private ArrayList<MenuBean> mBeautyData;
    private RecyclerView mMenuView;
    private RecyclerView mBeautyMenuView;
    private MenuAdapter mStickerAdapter;
    private MenuAdapter mBeautyAdapter;
    private TextView mBtnStick,mBtnBeauty;
    private int mBeautyFlag=0;
    private final int TYPE_NONE=0;
    private final int TYPE_EFFECT=1;
    private final int TYPE_BEAUTY=2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    protected void initData(){
        mMenuView= (RecyclerView)findViewById(R.id.mMenuView);
        mBeautyMenuView=(RecyclerView) findViewById(R.id.mBeautyMenuView);
        mBtnStick= (TextView)findViewById(R.id.mLeft);
        mBtnBeauty= (TextView)findViewById(R.id.mRight);
        mBtnStick.setSelected(true);
        //refreshRightBtn();

        mStickerData=new ArrayList<>();
        mBeautyData=new ArrayList<>();
        mMenuView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false));
        mBeautyMenuView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL,false));
        mStickerAdapter=new MenuAdapter(this,mStickerData);
        mStickerAdapter.setOnClickListener(new ClickUtils.OnClickListener() {
            @Override
            public void onClick(View v, int type, int pos, int child) {
                MenuBean m=mStickerData.get(pos);
                String name=m.name;
                mStickerAdapter.checkPos=pos;
                v.setSelected(true);
                mStickerAdapter.notifyDataSetChanged();
                if (name.equals("原始")) {
                    AiyaEffects.getInstance().setEffect(null);
                }else{
                    AiyaEffects.getInstance().setEffect("assets/modelsticker/"+m.path);
                }
            }
        });
        mBeautyAdapter=new MenuAdapter(this,mBeautyData);
        mBeautyAdapter.setOnClickListener(new ClickUtils.OnClickListener() {
            @Override
            public void onClick(View v, int type, int pos, int child) {
                mBeautyAdapter.checkPos=pos;
                v.setSelected(true);
                mBeautyAdapter.notifyDataSetChanged();
                MenuBean m=mBeautyData.get(pos);
                if(m.arg1>0&&m.arg2>0){
                    AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_TYPE,m.arg1);
                    AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_LEVEL,m.arg2);
                }else{
                    AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_LEVEL,0);
                    AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_TYPE,0);
                }

            }
        });

        mMenuView.setAdapter(mStickerAdapter);
        mBeautyMenuView.setAdapter(mBeautyAdapter);
        initEffectMenu("modelsticker/stickers.json");
        initBeautyMenu();
    }

    //刷新美颜按钮
    public void refreshRightBtn(){
        if(mBeautyFlag==0){
            mBtnBeauty.setText("美颜关");
            mBtnBeauty.setSelected(false);
            showMenu(TYPE_NONE);
        }else{
            mBtnBeauty.setText("美颜"+mBeautyFlag);
            mBtnBeauty.setSelected(true);
            mBeautyMenuView.setVisibility(View.VISIBLE);
            showMenu(TYPE_BEAUTY);
        }
    }

    private void showMenu(int type){
        switch (type){
            case TYPE_EFFECT:
                mBeautyMenuView.setVisibility(View.GONE);
                mMenuView.setVisibility(View.VISIBLE);
                break;
            case TYPE_BEAUTY:
                mBeautyMenuView.setVisibility(View.VISIBLE);
                mMenuView.setVisibility(View.GONE);
                break;
            default:
                mMenuView.setVisibility(View.GONE);
                mBeautyMenuView.setVisibility(View.GONE);
                break;
        }
        mBtnBeauty.setSelected(mBeautyMenuView.getVisibility()==View.VISIBLE);
        mBtnStick.setSelected(mMenuView.getVisibility()==View.VISIBLE);
    }

    //初始化特效按钮菜单
    protected void initEffectMenu(String menuPath) {
        try {
            Log.e( "解析菜单->" +menuPath);
            JsonReader r = new JsonReader(new InputStreamReader(getAssets().open(menuPath)));
            r.beginArray();
            while (r.hasNext()) {
                MenuBean menu = new MenuBean();
                r.beginObject();
                String name;
                while (r.hasNext()) {
                    name = r.nextName();
                    if (name.equals("name")) {
                        menu.name = r.nextString();
                    } else if (name.equals("path")) {
                        menu.path = r.nextString();
                    }
                }
                mStickerData.add(menu);
                Log.e( "增加菜单->" + menu.name);
                r.endObject();
            }
            r.endArray();
            mStickerAdapter.notifyDataSetChanged();
        } catch (IOException e) {
            e.printStackTrace();
            mStickerAdapter.notifyDataSetChanged();
        }
    }

    protected void initBeautyMenu(){
        MenuBean menu0=new MenuBean();
        menu0.name="无美颜";
        menu0.arg1=0;
        menu0.arg2=0;
        mBeautyData.add(menu0);
        int[] type=new int[]{1,4,5};
        for (int i:type){
            for (int j=1;j<7;j++){
                MenuBean menu=new MenuBean();
                menu.name="B"+i+j;
                menu.arg1=i;
                menu.arg2=j;
                mBeautyData.add(menu);
            }
        }
        mBeautyAdapter.notifyDataSetChanged();
    }

    //View的点击事件处理
    public void onClick(View view){
        switch (view.getId()){
            case R.id.mLeft:
                if(view.isSelected()){
                    showMenu(TYPE_NONE);
                }else{
                    showMenu(TYPE_EFFECT);
                }
                //mMenuView.setVisibility(mMenuView.getVisibility()==View.VISIBLE?
                //    View.GONE:View.VISIBLE);
                //view.setSelected(mMenuView.getVisibility()==View.VISIBLE);
                break;
            case R.id.mRight:
                if(view.isSelected()){
                    showMenu(TYPE_NONE);
                }else{
                    showMenu(TYPE_BEAUTY);
                }
                //mBeautyFlag=++mBeautyFlag>=7?0:mBeautyFlag;
                //AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_LEVEL,mBeautyFlag);
                //refreshRightBtn();
                //view.setSelected(mMenuView.getVisibility()==View.VISIBLE);
                break;
        }
    }


    protected String getSD(){
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }


    public void saveBitmapAsync(final byte[] bytes,final int width,final int height){
        new Thread(new Runnable() {
            @Override
            public void run() {
                LogUtils.e("has take pic");
                Bitmap bitmap=Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
                ByteBuffer b=ByteBuffer.wrap(bytes);
                bitmap.copyPixelsFromBuffer(b);
                saveBitmap(bitmap);
                bitmap.recycle();
            }
        }).start();
    }

    //图片保存
    public void saveBitmap(Bitmap b){
        String path =  getSD()+ "/AiyaCamera/photo/";
        File folder=new File(path);
        if(!folder.exists()&&!folder.mkdirs()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(EffectSelectActivity.this, "无法保存照片", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        long dataTake = System.currentTimeMillis();
        final String jpegName=path+ dataTake +".jpg";
        try {
            FileOutputStream fout = new FileOutputStream(jpegName);
            BufferedOutputStream bos = new BufferedOutputStream(fout);
            b.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(EffectSelectActivity.this, "保存成功->"+jpegName, Toast.LENGTH_SHORT).show();
            }
        });

    }
}
