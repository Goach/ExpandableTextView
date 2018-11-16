package com.goach.simple.library.utils;

import android.content.Context;
import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.load.resource.transcode.BitmapToGlideDrawableTranscoder;
import com.bumptech.glide.request.target.SimpleTarget;

/**
 * author: Goach.zhong.
 * Date: 2018/7/18 15:14.
 * Des: Glide 加载图片的帮助类
 */

public class GlideUtils {
    private static volatile GlideUtils mGlideUtils;

    private GlideUtils() {
    }

    public static GlideUtils getInstance() {
        if (null == mGlideUtils) {
            synchronized (GlideUtils.class) {
                if (null == mGlideUtils) {
                    mGlideUtils = new GlideUtils();
                }
            }
        }
        return mGlideUtils;
    }

    public void loadDrawable(Context ctx, String mUrl, SimpleTarget<GlideDrawable> target){
        createGlide(ctx)
                .fromString()
                .asBitmap()
                .transcode(new BitmapToGlideDrawableTranscoder(ctx), GlideDrawable.class)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .load(mUrl)
                .into(target);
    }

    private RequestManager createGlide(Context ctx) {
        return Glide.with(ctx);
    }
}
