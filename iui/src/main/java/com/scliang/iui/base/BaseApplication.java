package com.scliang.iui.base;

import android.content.Context;

import androidx.multidex.MultiDexApplication;

import com.scliang.iui.utils.LogUtils;

import java.lang.reflect.Field;

public abstract class BaseApplication extends MultiDexApplication {
    public static final class BasicApplication extends BaseApplication { }

    public static BaseApplication instance() { return ins; }

    public BaseApplication() { ins = this; }

    @Override
    public final void onCreate() {
        super.onCreate();
        final Context context = instance().getApplicationContext();
        try {
            Class<?> clz = Class.forName(context.getPackageName() + ".BuildConfig");
            Field field = clz.getField("BUILD_TYPE");
            String BuildType = (String) field.get(null);
            LogUtils.init(BuildType);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {
            LogUtils.init("release");
        }
        onCreateHere();
    }

    public void onCreateHere() {
    }

    private static BaseApplication ins;
}
