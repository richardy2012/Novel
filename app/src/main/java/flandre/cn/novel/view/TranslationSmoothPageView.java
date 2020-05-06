package flandre.cn.novel.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;

/**
 * 左右平移翻页
 * 2020.5.6
 */
public class TranslationSmoothPageView extends SmoothPageView {
    public TranslationSmoothPageView(Context context) {
        super(context);
    }

    public TranslationSmoothPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TranslationSmoothPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (drawMode){
            case NORMAL_DRAW:
                canvas.drawBitmap(getBitmap().get(1), 0, 0, null);
                break;
            case SMOOTH_LAST_DRAW:
                canvas.drawBitmap(getBitmap().get(0), left, 0, null);
                canvas.drawBitmap(getBitmap().get(1), left + width, 0, null);
                break;
            case SMOOTH_NEXT_DRAW:
                canvas.drawBitmap(getBitmap().get(1), left, 0, null);
                canvas.drawBitmap(getBitmap().get(2), left + width, 0, null);
                break;
        }
    }
}
