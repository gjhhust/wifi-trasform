package com.zjh.btim;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class ExampleApp extends Application {

    private static List<Activity> mActivitys = Collections.synchronizedList(new LinkedList<Activity>());
    //记录Activity的总个数
    public int count = 0;
    private static ExampleApp instance;

    @Override
    public void onCreate() {
        super.onCreate();

        instance = this;

        //注册记录Activity
        registerActivityListener();

    }

    public static ExampleApp getInstance(){
        return instance;
    }

    private void registerActivityListener() {
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (null == mActivitys) {
                    return;
                }
                mActivitys.add(activity);
            }

            @Override
            public void onActivityStarted(Activity activity) {
                if (count == 0) { //后台切换到前台
                }
                count++;
            }

            @Override
            public void onActivityResumed(Activity activity) {

            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {
                count--;
                if (count == 0) { //前台切换到后台
                }
            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                if (null == activity && mActivitys.isEmpty()) {
                    return;
                }
                if (mActivitys.contains(activity)) {
                    mActivitys.remove(activity);
                }
            }
        });
    }

    public static void finishAllActivity() {
        if (mActivitys == null || mActivitys.isEmpty()) {
            return;
        }
        for (Activity activity : mActivitys) {
            activity.finish();
        }
    }
    public static Activity getCurrentActivity() {
        if (mActivitys == null || mActivitys.isEmpty()) {
            return null;
        }
        Activity activity = mActivitys.get(mActivitys.size() - 1);
        return activity;
    }

}
