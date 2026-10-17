package com.opendeta.Mail.Services;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/** A three-dot (overflow) button drawn entirely with code - no drawable resources. */
public class ThreeDotView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ThreeDotView(Context context) {
        super(context);
        init();
    }

    public ThreeDotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThreeDotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float radius = Math.min(w, h) * 0.105f;
        float cx = w / 2f;
        canvas.drawCircle(cx, h * 0.22f, radius, paint);
        canvas.drawCircle(cx, h * 0.50f, radius, paint);
        canvas.drawCircle(cx, h * 0.78f, radius, paint);
    }
}
