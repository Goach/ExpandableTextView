package com.goach.simple.library;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.Html;
import android.view.Gravity;
import android.widget.TextView;

import com.bumptech.glide.*;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.goach.simple.library.utils.DrawableWrapper;
import com.goach.simple.library.utils.GlideUtils;

import java.util.ArrayList;
import java.util.Collection;

/**
 * author: Goach.zhong
 * Date: 2018/11/15 10:44.
 * Des:为了TextView 支持img标签，Html.fromHtml的时候使用
 */

public class GlideImageGetter implements Html.ImageGetter, Drawable.Callback {
    private final Collection<Target> imageTargets = new ArrayList<>();
    private final TextView targetView;
    private final int width;
    private final Context mCtx;

    public GlideImageGetter(Context ctx, TextView targetView, int width) {
        this.mCtx = ctx;
        this.targetView = targetView;
        this.width = width;
        targetView.setTag(this);
    }

    @Override
    public Drawable getDrawable(String url) {
        WrapperTarget imageTarget = new WrapperTarget(width);
        Drawable asyncWrapper = imageTarget.getLazyDrawable();
        asyncWrapper.setCallback(this);
        GlideUtils.getInstance().loadDrawable(mCtx,url,imageTarget);
        imageTargets.add(imageTarget);
        return asyncWrapper;
    }

    public void clear() {
        for (Target target : imageTargets) {
            Glide.clear(target);
        }
    }

    public static void clear(TextView view) {
        view.setText(null);
        Object tag = view.getTag();
        if (tag instanceof GlideImageGetter) {
            ((GlideImageGetter)tag).clear();
            view.setTag(null);
        }
    }
    @Override
    public void invalidateDrawable(@NonNull Drawable who) {
        targetView.invalidate();
    }
    @Override
    public void scheduleDrawable(@NonNull Drawable who, @NonNull Runnable what, long when) {

    }
    @Override
    public void unscheduleDrawable(@NonNull Drawable who, @NonNull Runnable what) {

    }

    public class WrapperTarget extends SimpleTarget<GlideDrawable> {
        private final ColorDrawable nullObject = new ColorDrawable(Color.TRANSPARENT);
        private final DrawableWrapper wrapper = new DrawableWrapper(null);
        private int mWidth;
        public WrapperTarget(int width) {
            super();
            setDrawable(null);
            this.mWidth = width;
        }

        public Drawable getLazyDrawable() {
            return wrapper;
        }

        @Override
        public void onLoadStarted(Drawable placeholder) {
            setDrawable(placeholder);
        }


        @Override
        public void onLoadFailed(Exception e, Drawable errorDrawable) {
            setDrawable(errorDrawable);
        }

        @Override
        public void onResourceReady(GlideDrawable glideDrawable, GlideAnimation glideAnimation) {
            glideDrawable.setLoopCount(GlideDrawable.LOOP_FOREVER);
            glideDrawable.start();
            float scale = 1;
            if(glideDrawable.getIntrinsicWidth() > 0){
                scale = glideDrawable.getIntrinsicHeight()*1.0f/glideDrawable.getIntrinsicWidth();
            }
            wrapper.setBounds(0,0,mWidth, (int) (mWidth*scale));//这里设置防止和文字重叠
            if(targetView.getParent() instanceof ExpandableTextView){
                ((ExpandableTextView)targetView.getParent()).resetMeasure();
            }
            setDrawable(glideDrawable);
        }

        @Override
        public void onLoadCleared(Drawable placeholder) {
            setDrawable(placeholder);
        }

        private void setDrawable(Drawable drawable) {
            if (drawable == null) {
                drawable = nullObject;
            }
            drawable.setBounds(calcBounds(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM));
            wrapper.setWrappedDrawable(drawable);
            wrapper.invalidateSelf();
        }

        private Rect calcBounds(int gravity) {
            Rect bounds = new Rect();
            Rect container = wrapper.getBounds();
            int w = container.width();
            int h = container.height();
            Gravity.apply(gravity, w, h, container, bounds);
            return bounds;
        }
    }
}
