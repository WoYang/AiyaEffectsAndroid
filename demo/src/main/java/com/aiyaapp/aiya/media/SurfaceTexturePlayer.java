/*
 *
 * SurfaceTexturePlayer.java
 * 
 * Created by Wuwang on 2017/2/25
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya.media;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.aiyaapp.aiya.camera.LogUtils;
import java.io.IOException;

/**
 * Description:
 */
public class SurfaceTexturePlayer implements MediaPlayer.OnPreparedListener,
    MediaPlayer.OnInfoListener,MediaPlayer.OnCompletionListener,MediaPlayer.OnErrorListener,
    MediaPlayer.OnSeekCompleteListener,MediaPlayer.OnVideoSizeChangedListener,
    MediaPlayer.OnBufferingUpdateListener{

    private MediaPlayer mPlayer;
    private MediaPlayer.OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private String path;

    private boolean isUserWantToStart=false;
    private boolean isLoop=true;
    private boolean isPrepared=false;
    private Surface mSurface;
    private int width;
    private int height;

    private Context mContext;

    public SurfaceTexturePlayer(Context context) {
        mContext = context;
    }

    public void setSurfaceTexture(SurfaceTexture texture){
        if(texture!=null){
            this.mSurface=new Surface(texture);
            if(path!=null){
                setVideoSource(path);
            }
            tryStartPlay();
        }else{
            mSurface=null;
            stop();
            release();
        }
    }

    public void setSurfaceHolder(SurfaceHolder holder){
        if(holder!=null){
            this.mSurface=holder.getSurface();
            if(path!=null){
                setVideoSource(path);
            }
            tryStartPlay();
        }else{
            mSurface=null;
            stop();
            release();
        }
    }

    public void setSurface(Surface surface){
        this.mSurface=surface;
        if(path!=null){
            setVideoSource(path);
        }
        tryStartPlay();
    }

    private void createPlayerIfNeeded(){
        LogUtils.e("player","mPlayer null:"+(mPlayer==null)+"/"+"surface null:"+(mSurface==null));
        if(mPlayer==null&&mSurface!=null){
            mPlayer=new MediaPlayer();
            mPlayer.setSurface(mSurface);
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnErrorListener(this);
            mPlayer.setOnInfoListener(this);
            mPlayer.setOnSeekCompleteListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnBufferingUpdateListener(this);
            mPlayer.setOnVideoSizeChangedListener(this);
            LogUtils.e("player","player created->");
        }
    }

    public void setVideoSource(String path){
        Log.e("player","setVideoSource->"+path);
        this.path=path;
        try {
            Log.e("player","try prepared");
            createPlayerIfNeeded();
            isPrepared=false;
            if(mPlayer!=null){
                mPlayer.reset();
                if(path.startsWith("assets/")){
                    AssetFileDescriptor f=mContext.getAssets()
                        .openFd(path.substring(7));
                    mPlayer.setDataSource(f.getFileDescriptor(),f.getStartOffset(),f.getLength());
                    LogUtils.e("player","path->"+path.substring(7));
                }else{
                    mPlayer.setDataSource(path);
                }
                mPlayer.prepareAsync();
                LogUtils.e("player","prepareAsync");
            }else{
                LogUtils.e("player","player is null");
            }
        } catch (IOException e) {
            LogUtils.e("player",e.getMessage());
            e.printStackTrace();
        }
    }

    public void start(){
        isUserWantToStart=true;
        tryStartPlay();
    }

    public void stop(){
        isUserWantToStart=false;
        tryStop();
    }

    public void release(){
        if(mPlayer!=null){
            mPlayer.release();
            mPlayer=null;
        }
    }

    public void reset(){
        mPlayer.reset();
    }

    public void tryStartPlay(){
        createPlayerIfNeeded();
        Log.e("player","try start play->is " +
            "Prepared:"+isPrepared+"/isUserWantToStart:"+isUserWantToStart);
        if(mPlayer!=null&&isPrepared&&isUserWantToStart){
            mPlayer.start();
        }
    }

    private void tryStop(){
        if(mPlayer!=null){
            mPlayer.stop();
        }
    }

    public void pause(){
        if(mPlayer!=null){
            mPlayer.pause();
        }
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
//        LogUtils.e("player","onBufferingUpdate->"+percent);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        LogUtils.e("player","onCompletion->");
        if(isLoop){
            tryStartPlay();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        LogUtils.e("player","onError->"+what+"/"+extra);
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        LogUtils.e("player","onInfo->"+what+"/"+extra);
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        LogUtils.e("player","onPrepared->");
        isPrepared=true;
        tryStartPlay();
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        LogUtils.e("player","onSeekComplete->");
    }

    @Override
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        LogUtils.e("player","onVideoSizeChanged->"+width+"/"+height);
        this.width=width;
        this.height=height;
        if(mOnVideoSizeChangedListener!=null){
            mOnVideoSizeChangedListener.onVideoSizeChanged(mp, width, height);
        }
    }

    public void setOnVideoSizeChangedListener(MediaPlayer.OnVideoSizeChangedListener listener){
        this.mOnVideoSizeChangedListener=listener;
    }
}
