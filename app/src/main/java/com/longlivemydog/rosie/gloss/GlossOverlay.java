package com.longlivemydog.rosie.gloss;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 *
 */
public class GlossOverlay extends FrameLayout {

    public GlossOverlay(Context context) {
        super(context);
    }

    public GlossOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GlossOverlay(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
