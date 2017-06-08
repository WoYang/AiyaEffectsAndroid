/*
 *
 * VideoView.java
 * 
 * Created by Wuwang on 2017/2/24
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya.media;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import com.aiyaapp.aiya.camera.LogUtils;
import com.aiyaapp.camera.sdk.filter.AFilter;
import com.aiyaapp.camera.sdk.filter.AiyaEffectFilter;
import com.aiyaapp.camera.sdk.filter.MatrixUtils;
import com.aiyaapp.camera.sdk.filter.NoFilter;
import java.util.Arrays;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Description:
 */
public class VideoView extends GLSurfaceView implements GLSurfaceView.Renderer {

    private SurfaceTexturePlayer mPlayer;
    private AiyaEffectFilter mFilter;
    private int width=0,height=0;
    private int videoWidth,videoHeight,aiyaWidth,aiyaHeight;
    private AFilter mShowFilter;

    private float[] SM=new float[16];

    public VideoView(Context context) {
        this(context,null);
    }

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init(){
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        setPreserveEGLContextOnPause(true);
        mShowFilter=new NoFilter(getResources());
        mFilter=new AiyaEffectFilter(getResources());
        mPlayer=new SurfaceTexturePlayer(getContext());
        mPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            @Override
            public void onVideoSizeChanged(MediaPlayer mp, int vWidth, int vHeight) {
                videoWidth=vWidth;
                videoHeight=vHeight;
                if(width!=0&&height!=0){
                    surfaceChanged(null,0,width,height);
                }
            }
        });
    }

    public void setVideoResource(String resource){
        mPlayer.setVideoSource(resource);
    }

    public void startPlay(){
        mPlayer.start();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mFilter.create();
        mShowFilter.create();
        mPlayer.setSurfaceTexture(mFilter.getTexture());
        mFilter.getTexture().setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
            @Override
            public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                requestRender();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mPlayer.pause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPlayer.tryStartPlay();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        this.width=width;
        this.height=height;
        if(videoWidth>0&&videoHeight>0&&aiyaWidth!=videoWidth&&aiyaHeight!=videoHeight){
            this.aiyaWidth=videoWidth;
            this.aiyaHeight=videoHeight;
            mFilter.setSize(aiyaWidth,aiyaHeight);
            MatrixUtils.getMatrix(SM, MatrixUtils.TYPE_CENTERINSIDE,videoWidth,videoHeight, width, height);
            mShowFilter.setMatrix(SM);
            LogUtils.e("player","matrix data:"+ videoWidth+"/"+videoHeight+"/"+width+"/"+height);
            LogUtils.e("player","set show filter matrix"+ Arrays.toString(SM));
        }
    }

    public void setCenterCrop(boolean isCenterCrop){
        if(videoWidth>0&&videoHeight>0&&width>0&&height>0&&mShowFilter!=null){
            if(isCenterCrop){
                MatrixUtils.getMatrix(SM, MatrixUtils.TYPE_CENTERCROP,videoWidth,videoHeight, width,height);
            }else{
                MatrixUtils.getMatrix(SM, MatrixUtils.TYPE_CENTERINSIDE,videoWidth,videoHeight, width, height);
            }
            mShowFilter.setMatrix(SM);
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if(aiyaWidth>0&&aiyaHeight>0){
            mFilter.draw();
            
            GLES20.glViewport(0,0,width,height);
            mShowFilter.setTextureId(mFilter.getOutputTexture());
            mShowFilter.draw();
        }
    }
}
