/*
 *
 * FwDecoder.java
 * 
 * Created by Wuwang on 2016/10/26
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya.media;

import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import com.aiyaapp.aiya.camera.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

import static android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM;

/**
 * Description:
 */
public class FwDecoder {

    private final Object LOCK=new Object();

    private String path;
    private MediaCodec mediaCodec;
    private MediaExtractor mExtractor;
    private SurfaceTexture texture;

    private ByteBuffer[] buffers;       //输入buffer

    private String mime;
    private int width,height;
    private int fps;
    private MediaCodec.BufferInfo currentOutputBuffer;

    private CountDownLatch mSignal;
    private boolean flagInput=false,flagOutPut=false;
    private Thread inThread,outThread;

    public FwDecoder(){

    }

    public void setPath(String path){
        this.path=path;
    }

    public void setSurfaceTexture(SurfaceTexture texture){
        this.texture=texture;
    }

    public void prepare(){
        if(path==null)return;
        release();
        try {
            mExtractor=new MediaExtractor();
            mExtractor.setDataSource(path);
            mExtractor.seekTo(0,MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
            Log.e("wuwang","info-->"+mExtractor.getTrackCount());
            MediaFormat format=mExtractor.getTrackFormat(0);
            String mime=format.getString(MediaFormat.KEY_MIME);
            width=format.getInteger(MediaFormat.KEY_WIDTH);
            height=format.getInteger(MediaFormat.KEY_HEIGHT);
            Log.e("wuwang","info-->"+format.toString());
            mediaCodec=MediaCodec.createDecoderByType(mime);
            //0表示为解码器
            mediaCodec.configure(format,new Surface(texture),null,0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getWidth(){
        return width;
    }

    public int getHeight(){
        return height;
    }

    /**
     * MediaCodec开始
     */
    public void start(){
        if(mediaCodec!=null){
            mExtractor.selectTrack(0);
            mediaCodec.start();
            buffers=mediaCodec.getInputBuffers();
            flagInput=true;
            flagOutPut=true;
            Log.e("wuwang","开始解码");
            inThread=new Thread(new Runnable() {
                @Override
                public void run() {
                    while (flagInput){
                        feedInputData();
                    }

                }
            });
            outThread=new Thread(new Runnable() {
                @Override
                public void run() {
                    while (flagOutPut){
                        try {
                            readOutputData();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            inThread.start();
            outThread.start();
        }
    }

    public boolean isDecoding(){
        return flagInput&flagOutPut;
    }

    //向MediaCodec喂输入数据
    private void feedInputData(){
        int index=mediaCodec.dequeueInputBuffer(1000);
        if(index>=0){
            int size=mExtractor.readSampleData(buffers[index],0);
            long time=0;
            boolean isEnd;
            if(size<0){
                isEnd=true;
                flagInput=false;
                size=0;
            }else{
                time=mExtractor.getSampleTime();
                isEnd=false;
            }
//            Log.e("wuwang","isEnd?"+isEnd+"--time?"+time);
            mediaCodec.queueInputBuffer(index,0,size,time,isEnd?BUFFER_FLAG_END_OF_STREAM:0);
            if(!isEnd){
                mExtractor.advance();
            }else{
                Log.e("wuwang","in end--------------------------------------");
            }
        }
    }

    //输出数据
    private void readOutputData() throws InterruptedException {
        MediaCodec.BufferInfo info=new MediaCodec.BufferInfo();
        final int res=mediaCodec.dequeueOutputBuffer(info,1000);
        if(info.flags==BUFFER_FLAG_END_OF_STREAM){
            flagOutPut=false;
            Log.e("wuwang","out end--------------------------------");
        }
        if(res>=0) {
            boolean render = info.size >= 0;
            mSignal=new CountDownLatch(1);
            //Other thread deal with info,then release
            //releaseCurrentOutputBuffer();   //测试用
//            if(render){
//                currentOutputBuffer=new MediaCodec.BufferInfo();
//                currentOutputBuffer.set();
//            }
            mSignal.await();
            mediaCodec.releaseOutputBuffer(res,render);
        }
    }

    /**
     * 获得当前的output buffer信息
     */
    public MediaCodec.BufferInfo getCurrentOutputBuffer(){
        if(currentOutputBuffer!=null){
           return currentOutputBuffer;
        }
        return null;
    }

    /**
     * 释放当前的output buffer，若存在下一帧，则会自动取下一帧buffer
     */
    public void releaseCurrentOutputBuffer(){
        if(currentOutputBuffer!=null){
            currentOutputBuffer=null;
        }
        if(mSignal!=null){
            mSignal.countDown();
        }
    }

    public void release(){
        if(mediaCodec!=null){
            flagInput=false;
            flagOutPut=false;
            //不释放会导致解码线程一直等待
            releaseCurrentOutputBuffer();
            LogUtils.e("尝试停止解码");
            //保证线程中mediaCodec使用不会出错
            try{
                if(inThread!=null){
                    inThread.join();
                    LogUtils.e("解码Input线程停止");
                }
                if(outThread!=null){
                    outThread.join();
                    LogUtils.e("解码Output线程停止");
                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
            mediaCodec.stop();
            mediaCodec.release();
            mediaCodec=null;
            LogUtils.e("Codec销毁");
        }
        if(mExtractor!=null){
            mExtractor.release();
            mExtractor=null;
        }
    }

    public int getFrame(){
        return fps;
    }


}
