/*
 *
 * MediaCodecActivity.java
 * 
 * Created by Wuwang on 2016/10/26
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya.media;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.aiyaapp.aiya.AutoTest;
import com.aiyaapp.aiya.EffectSelectActivity;
import com.aiyaapp.aiya.R;
import com.aiyaapp.aiya.camera.LogUtils;
import com.aiyaapp.aiya.util.UriUtil;

//import com.aiyaapp.aiya.AutoTest;

/**
 * Description:
 */
public class MediaActivity extends EffectSelectActivity {

    private VideoView mVideoView;
    private boolean isCenterCrop=false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mediacodec);
        initData();
        mVideoView= (VideoView)findViewById(R.id.mVideoView);
        if(AutoTest.getInstance().auto){
            mVideoView.setVideoResource(AutoTest.getInstance().video);
        }else{
            //mVideoView.setVideoResource("http://i.snssdk.com/neihan/video/playback/?vide" +
            //    "o_id=3f1e8efb569643d797371636ef45f596&quality=480p&line=0&is_gif=0.mp4");
            mVideoView.setVideoResource("assets/test/test.mp4");
        }
        mVideoView.startPlay();
    }

    @Override
    public void onClick(View view){
        super.onClick(view);
        switch (view.getId()){
            case R.id.mCenter:
                Intent intent=new Intent();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("video/*");
                startActivityForResult(intent,1);
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mVideoView!=null){
            mVideoView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mVideoView!=null){
            mVideoView.onPause();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.media,menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.mShowType:
                isCenterCrop=!isCenterCrop;
                mVideoView.setCenterCrop(isCenterCrop);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(data!=null){
            LogUtils.e("player","data-->"+data.toString());
            String path= UriUtil.getPath(this,data.getData());
            LogUtils.e("player","path-->"+path);
            if(path!=null){
                mVideoView.setVideoResource(path);
                mVideoView.startPlay();
            }
        }
    }

}
