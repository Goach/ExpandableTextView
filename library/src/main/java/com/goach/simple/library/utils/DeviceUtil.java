package com.goach.simple.library.utils;

import android.content.Context;

/**
 * author: Goach.
 * Date: 2018/11/16 15:53.
 * Des:
 */
public class DeviceUtil {
    private static volatile DeviceUtil singleton = null;

    private DeviceUtil() {
    }
    public synchronized static DeviceUtil instance() {
        if (null == singleton) {
            singleton = new DeviceUtil();
        }
        return singleton;
    }
    public int dip2px(float dipValue, Context activity) {
        return (int) (dipValue * getScreenDensity(activity) + 0.5f);
    }
    public float getScreenDensity(Context activity) {
        try {
            return activity.getResources().getDisplayMetrics().density;
        } catch (Exception e) {
            return 1;
        }
    }
    public int getScreenPixelsWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }
}
