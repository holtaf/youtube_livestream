package com.google.android.apps.watchme.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TestDraw extends View {
    private float moveX = 0;
    private float moveY = 0;
    Paint paint = new Paint();

    public TestDraw(Context context) {
        super(context);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
    }

    public TestDraw(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
    }

    public TestDraw(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        paint.setAntiAlias(true);
        paint.setColor(Color.BLACK);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawCircle(moveX, moveY, 20, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                moveX = event.getX();
                moveY = event.getY();
                invalidate();
                break;
        }
        return true;
    }
}
