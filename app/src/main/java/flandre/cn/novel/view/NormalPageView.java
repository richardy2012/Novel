package flandre.cn.novel.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;

/**
 * 正常的翻页
 * 2020.4
 */
public class NormalPageView extends PageView {
    public NormalPageView(Context context) {
        this(context, null);
    }

    public NormalPageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NormalPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public void onUpdateText() {
        switch (mode){
            case NEXT:
                nextPage();
                break;
            case LAST:
                lastPage();
                break;
            case REDIRECT:
                postInvalidate();
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (pageEnable) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    lastPage();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    nextPage();
                    return true;
            }
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!pageEnable) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = (int) event.getX();
                y = (int) event.getY();
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                action(event, this.x);
                break;
        }
        return true;
    }

    @Override
    public void nextPage() {
        next();
        if (!pageEnable) return;
        postInvalidate();
    }

    @Override
    public void lastPage() {
        last();
        if (!pageEnable) return;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(color);
        if (drawText != null)
            drawText(canvas, now);
    }
}