/*
 *
 * FwEncoder.java
 * 
 * Created by Wuwang on 2016/11/26
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya.media;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Description:
 */
public class FwEncoder {

    private static final String TAG="fwencoder";
    private MediaCodec mEnc;
    private MediaCodec.BufferInfo mInfo;
    private String mime="video/avc";
    private int rate=125000;
    private int frameRate=24;
    private int frameInterval=1;

    private HandlerThread mThread;
    private Handler mHandler;

    private boolean mInputFlag;
    private boolean mOutputFlag;
    private int width;
    private int height;
    private Queue<byte[]> mEmptyData;
    private byte[] mHeadInfo=null;

    private FileOutputStream fos;
    private String mSavePath;

    public FwEncoder(){
        mEmptyData=new ConcurrentLinkedQueue<>();
        mInfo=new MediaCodec.BufferInfo();
    }

    public void setMime(String mime){
        this.mime=mime;
    }

    public void setRate(int rate){
        this.rate=rate;
    }

    public void setFrameRate(int frameRate){
        this.frameRate=frameRate;
    }

    public void setFrameInterval(int frameInterval){
        this.frameInterval=frameInterval;
    }

    public void setSavePath(String path){
        this.mSavePath=path;
    }

    public void prepareAndStart(int width,int height){
        mHeadInfo=null;
        this.width=width;
        this.height=height;
        try {
            File file=new File(mSavePath);
            if(file.exists()){
                boolean b=file.delete();
            }
            fos=new FileOutputStream(mSavePath);
            MediaFormat format=MediaFormat.createVideoFormat(mime,width,height);
            format.setInteger(MediaFormat.KEY_BIT_RATE,rate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE,frameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,frameInterval);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities
                .COLOR_FormatYUV420Planar);
            mEnc=MediaCodec.createEncoderByType(mime);
            mEnc.configure(format,null,null,MediaCodec.CONFIGURE_FLAG_ENCODE);
            mEnc.start();
            mThread=new HandlerThread("Encoder Thread");
            mThread.start();
            mHandler=new Handler(mThread.getLooper());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    mEnc.stop();
                    mEnc.release();
                    fos.flush();
                    fos.close();
                } catch (Exception e){
                    e.printStackTrace();
                }
                if(mThread!=null&&mThread.isAlive()){
                    mThread.getLooper().quit();
                }
            }
        });
    }

    public void feedData(final byte[] data, final long timeStep){
        if(mHandler!=null){
            final byte[] dts= getData(data);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    try {
                        readOutputData(dts,timeStep);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    public byte[] getData(byte[] data){
        byte[] c=mEmptyData.poll();
        if(c==null){
            c=Arrays.copyOf(data,data.length);
        }else{
            if(c.length!=data.length){
                c=new byte[data.length];
            }
            System.arraycopy(data,0,c,0,data.length);
        }
        return c;
    }

    private ByteBuffer getInputBuffer(int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEnc.getInputBuffer(index);
        }else{
            return mEnc.getInputBuffers()[index];
        }
    }

    private ByteBuffer getOutputBuffer(int index){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return mEnc.getOutputBuffer(index);
        }else{
            return mEnc.getOutputBuffers()[index];
        }
    }

    //TODO 定时调用，如果没有新数据，就用上一个数据
    private void readOutputData(byte[] data,long timeStep) throws IOException {
        int index=mEnc.dequeueInputBuffer(-1);
        if(index>=0){
            if(yuv==null){
                yuv=new byte[width*height*3/2];
            }
            ByteBuffer buffer=getInputBuffer(index);
            rgbaToYuv(data,width,height,yuv);
            mEmptyData.add(data);
            buffer.clear();
            buffer.put(yuv);
            mEnc.queueInputBuffer(index,0,yuv.length,timeStep,0);
        }
        MediaCodec.BufferInfo mInfo=new MediaCodec.BufferInfo();
        int outIndex=mEnc.dequeueOutputBuffer(mInfo,0);
        while (outIndex>=0){
            ByteBuffer outBuf=getOutputBuffer(outIndex);
            byte[] temp=new byte[mInfo.size];
            outBuf.get(temp);
            if(mInfo.flags==MediaCodec.BUFFER_FLAG_CODEC_CONFIG){
                Log.e(TAG,"start frame");
                mHeadInfo=new byte[temp.length];
                mHeadInfo=temp;
            }else if(mInfo.flags%8==MediaCodec.BUFFER_FLAG_KEY_FRAME){
                Log.e(TAG,"key frame");
                byte[] keyframe = new byte[temp.length + mHeadInfo.length];
                System.arraycopy(mHeadInfo, 0, keyframe, 0, mHeadInfo.length);
                System.arraycopy(temp, 0, keyframe, mHeadInfo.length, temp.length);
                Log.e(TAG,"other->"+mInfo.flags);
                fos.write(keyframe,0,keyframe.length);
            }else if(mInfo.flags==MediaCodec.BUFFER_FLAG_END_OF_STREAM){
                Log.e(TAG,"end frame");
            }else{
                fos.write(temp,0,temp.length);
            }
            mEnc.releaseOutputBuffer(outIndex,false);
            outIndex=mEnc.dequeueOutputBuffer(mInfo,0);
            Log.e("wuwang","outIndex-->"+outIndex);
        }

    }

    private void fosWrite(){

    }

    private void readOutputDataFinish(){

    }

    byte[] yuv;
    private void rgbaToYuv(byte[] rgba,int width,int height,byte[] yuv){
        final int frameSize = width * height;

        int yIndex = 0;
        int uIndex = frameSize;
        int vIndex = frameSize + frameSize/4;

        int R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                index = j * width + i;
                if(rgba[index*4]>127||rgba[index*4]<-128){
                    Log.e("color","-->"+rgba[index*4]);
                }
                R = rgba[index*4]&0xFF;
                G = rgba[index*4+1]&0xFF;
                B = rgba[index*4+2]&0xFF;

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv[yIndex++] = (byte) ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv[uIndex++] = (byte) ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                    yuv[vIndex++] = (byte) ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                }
            }
        }
    }

}
