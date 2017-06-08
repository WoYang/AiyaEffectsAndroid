/*
 *
 * AutoTest.java
 * 
 * Created by Wuwang on 2017/2/28
 * Copyright © 2016年 深圳哎吖科技. All rights reserved.
 */
package com.aiyaapp.aiya;

import android.content.Context;
import android.util.JsonReader;
import com.aiyaapp.aiya.camera.LogUtils;
import com.aiyaapp.camera.sdk.AiyaEffects;
import com.aiyaapp.camera.sdk.base.ISdkManager;
import com.aiyaapp.camera.sdk.base.Log;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Description:
 */
public class AutoTest {

    private static AutoTest instance;
    public String video=null;
    private int beauty=0;
    private long[] time;
    public boolean auto=false;
    private boolean isRandom=true;

    private ArrayList<Effect> mEffects;

    private int effectIndex=-1;

    private WeakReference<Context> mContext;

    private AutoTest(){
        mEffects=new ArrayList<>();
    }

    public static AutoTest getInstance(){
        if(instance==null){
            synchronized (AutoTest.class){
                if(instance==null){
                    instance=new AutoTest();
                }
            }
        }
        return instance;
    }

    public void init(Context context){
        this.mContext=new WeakReference<>(context);
    }

    public void config(String file,boolean isFile){
        LogUtils.e("test","start config:"+file);
        try {
            JsonReader reader;
            if(isFile){
                if(file.startsWith("assets/")){
                    reader=new JsonReader(new InputStreamReader(mContext.get().getAssets().open
                        (file.substring(7))));
                }else{
                    reader=new JsonReader(new FileReader(file));
                }
            }else{
                reader=new JsonReader(new StringReader(file));
            }
            reader.beginObject();
            LogUtils.e("test","start reader");
            while (reader.hasNext()){
                String name=reader.nextName();
                LogUtils.e("test","has name:"+name);
                switch (name){
                    case "effects":
                        reader.beginArray();
                        while (reader.hasNext()){
                            Effect effect;
                            if(mEffects.size()==0){
                                effect=new Effect();
                            }else{
                                effect=new Effect(mEffects.get(mEffects.size()-1));
                            }
                            reader.beginObject();
                            while (reader.hasNext()){
                                String name1=reader.nextName();
                                Log.e("test",name1);
                                switch (name1){
                                    case "effect":
                                        String path=reader.nextString();
                                        if("".equals(path)){
                                            effect.path=null;
                                        }else if(path.endsWith("/meta.json")){
                                            effect.path=path;
                                        }else{
                                            effect.path="assets/modelsticker/"
                                                +path+"/meta.json";
                                        }
                                        break;
                                    case "time":
                                        effect.time=reader.nextInt();
                                        break;
                                    case "beauty":
                                        effect.beautyLevel=reader.nextInt();
                                        break;
                                }
                            }
                            reader.endObject();
                            mEffects.add(effect);
                        }
                        reader.endArray();
                        break;
                    case "video":
                        video=reader.nextString();
                        LogUtils.e("test",video);
                        break;
                }
            }
            reader.endObject();
            auto=true;
        } catch (IOException e) {
            LogUtils.e("test","err:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public void start(){
        if(auto){
            AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_LEVEL,beauty);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (auto){
                        effectIndex++;
                        if(effectIndex>=mEffects.size()){
                            effectIndex=0;
                        }
                        Effect effect=mEffects.get(effectIndex);
                        if(effect.beautyLevel>=0){
                            AiyaEffects.getInstance().set(ISdkManager.SET_BEAUTY_LEVEL,
                                effect.beautyLevel);
                        }
                        AiyaEffects.getInstance().setEffect(effect.path);
                        Log.e("log_analyse","effect "+effect.path+"/"+effect.beautyLevel);
                        try {
                            Thread.sleep(effect.time);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        }
    }

    public void release(){
        auto=false;
        mEffects.clear();
        video=null;
        beauty=0;
    }

    class Effect{
        public String path;
        public int time;
        public int beautyLevel;

        public Effect() {
        }

        public Effect(Effect effect){
            if(effect!=null){
                this.path=effect.path;
                this.time=effect.time;
                this.beautyLevel=effect.beautyLevel;
            }
        }
    }

}
