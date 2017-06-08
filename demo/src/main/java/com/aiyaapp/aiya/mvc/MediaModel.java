/*
 *
 * MediaModel.java
 * 
 * Created by Wuwang on 2017/3/4
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya.mvc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import com.aiyaapp.aiya.media.SurfaceTexturePlayer;
import com.aiyaapp.camera.sdk.base.Log;
import com.aiyaapp.camera.sdk.base.Renderer;
import com.aiyaapp.camera.sdk.filter.MatrixUtils;
import com.aiyaapp.camera.sdk.widget.AiyaController;
import com.aiyaapp.camera.sdk.widget.AiyaModel;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Description:
 */
public class MediaModel implements AiyaModel {

    private SurfaceTexturePlayer mPlayer;
    private int mDataWidth;
    private int mDataHeight;
    private AiyaController mController;

    public MediaModel(Context context){
        mPlayer=new SurfaceTexturePlayer(context);
        mPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                mDataWidth=width;
                mDataHeight=height;
                if(mController!=null){
                    mController.setDataSize(mDataWidth,mDataHeight);
                    mController.setShowType(MatrixUtils.TYPE_CENTERINSIDE);
                    mController.surfaceChanged(mController.getWindowSize().x,
                        mController.getWindowSize().y);
                }
            }
        });
    }

    public void setResource(String resource){
        mPlayer.setVideoSource(resource);
    }

    public void start(){
        mPlayer.start();
    }

    public void stop(){
        mPlayer.stop();
    }

    public SurfaceTexturePlayer getPlayer(){
        return mPlayer;
    }

    @Override
    public void attachToController(AiyaController controller) {
        mController=controller;
        mController.setRenderer(new Renderer() {
            @Override
            public void onDestroy() {
                mPlayer.stop();
            }

            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                Log.e("MediaModel","media onSurfaceCreated");
                mPlayer.setSurfaceTexture(mController.getTexture());
                mController.getTexture().setOnFrameAvailableListener(new SurfaceTexture
                    .OnFrameAvailableListener() {
                    @Override
                    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                        mController.requestRender();
                    }
                });
            }

            @Override
            public void onSurfaceChanged(GL10 gl, int width, int height) {
            }

            @Override
            public void onDrawFrame(GL10 gl) {

            }
        });
    }
}
