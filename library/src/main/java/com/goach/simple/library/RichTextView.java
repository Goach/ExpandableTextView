package com.goach.simple.library;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.text.Html;
import android.util.AttributeSet;

import com.goach.simple.library.utils.DeviceUtil;

/**
 * author: Goach.zhong
 * Date: 2018/11/15 11:02.
 * Des:TextView支持img标签
 */
public class RichTextView extends AppCompatTextView {

    private GlideImageGetter mGlideImageGetter;

    public RichTextView(Context context) {
        this(context, null);
    }

    public RichTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RichTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    /**
     * 设置富文本
     */
    public void setHtml(final String source) {
        if(mGlideImageGetter == null){
            mGlideImageGetter = new GlideImageGetter(getContext(),RichTextView.this,
                    DeviceUtil.instance().getScreenPixelsWidth(getContext())
                            - DeviceUtil.instance().dip2px(30,getContext()));
        }
        CharSequence htmlText = Html.fromHtml(source, mGlideImageGetter, null);
        setText(htmlText);
    }
    public void clear(){
        if(mGlideImageGetter != null){
            mGlideImageGetter.clear();
        }
    }
}
