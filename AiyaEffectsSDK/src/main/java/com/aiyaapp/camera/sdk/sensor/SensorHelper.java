/*
 *
 * SensorHelper.java
 * 
 * Created by Wuwang on 2017/3/13
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.camera.sdk.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.aiyaapp.camera.sdk.AiyaCameraJni;
import com.aiyaapp.camera.sdk.base.Log;
import java.lang.Math;

import java.util.List;

/**
 * Description:
 */
public class SensorHelper {

    private SensorManager mSensorManager;
    private Sensor mAccSensor;
    private Sensor mMagSensor;
    private Sensor mOretation;
    private Sensor mOriSensor;
    private SensorListener listener;
    private float[] R = new float[16];
    private float[] I = new float[16];
    private float[] gravity=new float[3];       //加速度方向数据
    private float[] linear_acceleration = new float[3];
    private float   timeStamp = 0.0f;
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final float EPSILON = 1e-9f;

    private float[] magnetic=new float[3];   //磁场方向数据
    private float smoothFactor = 0.2f;

    private Sensor      mGyroSensor;
    private float[]     gyroscope = new float[3];
    private float[]     deltaRotationVector = new float[4];

    private float[] result=new float[10];        //方向数据
    private AiyaCameraJni mAiyaJni;

    public SensorHelper(Context context){
        mSensorManager= (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        android.util.Log.e("AYEFFECTS","sensorList:");
        List<Sensor> sensors= mSensorManager.getSensorList( Sensor.TYPE_ALL);
        android.util.Log.e("AYEFFECTS","sensorcount:"+sensors.size());
        for (Sensor sensor:sensors){
            android.util.Log.e("AYEFFECTS","sensorType:"+sensor.getName());
        }

        //方向传感器被废弃，目前采用加速度传感器和磁场传感器来计算方向
        //加速度传感器
        mAccSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //磁场传感器
        mMagSensor=mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        // 陀螺仪
        mGyroSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        // rotation vector
        mOriSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mAiyaJni=new AiyaCameraJni();
    }

    public void setSensorListener(SensorListener listener){
        this.listener=listener;
    }

    public void start(){
        mSensorManager.registerListener(mSensorListener,mAccSensor,SensorManager.SENSOR_DELAY_GAME);
        //mSensorManager.registerListener(mSensorListener,mMagSensor,SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(mSensorListener,mGyroSensor,SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(mSensorListener,mOriSensor,SensorManager.SENSOR_DELAY_GAME);
    }

    private SensorEventListener mSensorListener =new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(timeStamp!=0){
                switch (event.sensor.getType()){
                    case Sensor.TYPE_ACCELEROMETER:
                    //System.arraycopy(event.values,0,gravity,0,3);
                    final float alpha = 0.8f;
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

                    linear_acceleration[0] = event.values[0] - gravity[0];
                    linear_acceleration[1] = event.values[1] - gravity[1];
                    linear_acceleration[2] = event.values[2] - gravity[2];

                    final float dT = (event.timestamp - timeStamp) * NS2S;
                    linear_acceleration[0] *= dT;
                    linear_acceleration[1] *= dT;
                    linear_acceleration[2] *= dT;
                    break;
                /*
                case Sensor.TYPE_MAGNETIC_FIELD:
                    System.arraycopy(event.values,0,magnetic,0,3);
                    break;*/
                case Sensor.TYPE_GYROSCOPE:
                    System.arraycopy(event.values,0,gyroscope,0,3);
                    break;
                case Sensor.TYPE_ROTATION_VECTOR:
                    //float v4 = event.values[4];
                    //if(v4<-0.999999f){
                    //    android.util.Log.e("AYEFFECTS", "v4: " + v4);
                    //}
                    //else{
                        deltaRotationVector[0] = event.values[0];
                        deltaRotationVector[1] = event.values[1];
                        deltaRotationVector[2] = event.values[2];
                        deltaRotationVector[3] = event.values[3];
                    Log.e(event.values[0]+"/"+event.values[1]+"/"+event.values[2]);
                    //}
                    break;
                }
            }
            timeStamp = event.timestamp;

            System.arraycopy(linear_acceleration,0,result,0,3);
            System.arraycopy(gyroscope,0,result,3,3);
            System.arraycopy(deltaRotationVector, 0, result, 6, 4);
            mAiyaJni.sensorChanged(result);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };


    private void sensorChanged(float[] result){
        if(listener!=null){
            listener.onSensorChanged(result);
        }
    }

    public void stop(){
        mSensorManager.unregisterListener(mSensorListener);
    }

    public interface SensorListener{
        void onSensorChanged(float[] values);
    }
}
