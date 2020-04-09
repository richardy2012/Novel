package flandre.cn.novel.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Date;

public class SmoothPageView extends PageView {
    private static final int NORMAL_DRAW = 0;  // 普通作画
    private static final int SMOOTH_LAST_DRAW = 1;  // 上一个页面作画
    private static final int SMOOTH_NEXT_DRAW = 2;  // 下一个页面作画

    private static final long POST_DELAY = 20;  // post延迟时间(毫秒)
    private static final int DISTANCE = 8;  // 每次移动的距离(width/this)

    private int drawMode = NORMAL_DRAW;  // 作画的模式

    private int left;  // bitmap的x坐标
    private int x, y;
    private ArrayList<Bitmap> mBitmap = new ArrayList<>();
    private ArrayList<Canvas> mCanvas = new ArrayList<>();

    public SmoothPageView(Context context) {
        super(context);
    }

    public SmoothPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmoothPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLoad() {
        // 加载完成时创建bitmap
        for (int i = 0; i < 3; i++) {
            mBitmap.add(Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888));
            mCanvas.add(new Canvas(mBitmap.get(i)));
            mCanvas.get(i).drawColor(color);
        }
        // 如果已经设置了drawText就可以直接开始作画
        if (drawText != null) {
            drawBitmap();
        }
    }

    /**
     * 给三张bitmap都进行作画
     */
    private void drawBitmap() {
        if (drawText == null) return;
        for (Canvas canvas : mCanvas) canvas.drawColor(color);
        if (now > 0)
            drawText(mCanvas.get(0), now - 1, getPosition(now - 1));
        drawText(mCanvas.get(1), now);
        if (now < listPosition[6] - 1)
            drawText(mCanvas.get(2), now + 1, getPosition(now + 1));
    }

    /**
     * @param now 页面位置
     * @return 对应的文本位置
     */
    private int getPosition(int now) {
        int position = 0;
        for (int p : listPosition) {
            if (now >= p) position++;
            else break;
        }
        return position;
    }

    /**
     * 仅对一章bitmap进行作画
     *
     * @param mode true是下一页, false是上一页
     */
    private void changeBitmap(boolean mode) {
        if (mode) {
            Bitmap bitmap = mBitmap.get(0);
            Canvas canvas = mCanvas.get(0);
            mBitmap.remove(0);
            mCanvas.remove(0);
            mBitmap.add(bitmap);
            mCanvas.add(canvas);
            canvas.drawColor(color);
            if (now < listPosition[6] - 1) drawText(canvas, now + 1, getPosition(now + 1));
        } else {
            Bitmap bitmap = mBitmap.get(2);
            Canvas canvas = mCanvas.get(2);
            mBitmap.remove(2);
            mCanvas.remove(2);
            mBitmap.add(0, bitmap);
            mCanvas.add(0, canvas);
            canvas.drawColor(color);
            if (now > 0) drawText(canvas, now - 1, getPosition(now - 1));
        }
    }

    /**
     * 计算x坐标的位置, 并设置页面的移动方式
     *
     * @param x 当前手指的坐标
     */
    private void smooth(int x) {
        if (x - this.x > 0) {
            left = Math.abs(x - this.x) - width;
            drawMode = SMOOTH_LAST_DRAW;
        } else {
            left = 0 - Math.abs(x - this.x);
            drawMode = SMOOTH_NEXT_DRAW;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        switch (drawMode) {
            case NORMAL_DRAW:
                canvas.drawBitmap(mBitmap.get(1), 0, 0, null);
                break;
            case SMOOTH_LAST_DRAW:
                canvas.drawBitmap(mBitmap.get(1), 0, 0, null);
                canvas.drawBitmap(mBitmap.get(0), left, 0, null);
//                canvas.drawLine(left + width, 0, left + width, height, getPaint());
                break;
            case SMOOTH_NEXT_DRAW:
                canvas.drawBitmap(mBitmap.get(2), 0, 0, null);
                canvas.drawBitmap(mBitmap.get(1), left, 0, null);
//                canvas.drawLine(left + width, 0, left + width, height, getPaint());
                break;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!pageEnable) return true;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = (int) event.getX();
                y = (int) event.getY();
            case MotionEvent.ACTION_MOVE:
                // 页面随着手的变化而变化
                int x = (int) event.getX();
                smooth(x);
                postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                // 如果时点击时, 设置好数据
                if (Math.abs(event.getX() - this.x) < width / 10) {
                    // 如果使用全屏点击下一页或者点的位置在右边, 使用下一页动画
                    if (alwaysNext || this.x > width / 2) {
                        left = 0;
                        drawMode = SMOOTH_NEXT_DRAW;
                    } else {
                        left = -width;
                        drawMode = SMOOTH_LAST_DRAW;
                    }
                }
                action(event, this.x);
                break;
        }
        return true;
    }

    @Override
    public void onUpdateText() {
        switch (mode) {
            case NEXT:
                left = 0;
                drawMode = SMOOTH_NEXT_DRAW;
                nextPage();
                break;
            case LAST:
                left = -width;
                drawMode = SMOOTH_LAST_DRAW;
                lastPage();
                break;
            case REDIRECT:
                drawBitmap();
                postInvalidate();
                break;
        }
    }

    @Override
    public void setColor(int color) {
        this.color = color;
        if (getLoad()) {
            drawBitmap();
            postInvalidate();
        }
    }

    @Override
    public void nextPage() {
        // 开启翻页动画, 不给用户移动, 直到动画结束
        pageEnable = false;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveNext();
            }
        }, POST_DELAY);
    }

    private void moveNext() {
        if (now < listPosition[6] - 1) {
            // 如果不是最后一页, 进行下一页操作
            // 当left<-width时表示页面已经翻走了, 可以结束动画进行收尾工作
            if (left > -width) {
                left -= width / DISTANCE;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        moveNext();
                    }
                }, POST_DELAY);
                // 最后的改变交给NORMAL_DRAW就可以了
                if (left <= -width) return;
            } else {
                drawMode = NORMAL_DRAW;
                finishNext();
            }
        } else {
            // 如果时最后一页, 把页面翻回来
            if (left < 0) {
                left += width / DISTANCE;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        moveNext();
                    }
                }, POST_DELAY);
                if (left >= 0) return;
            } else {
                drawMode = NORMAL_DRAW;
                finishNext();
            }
        }
        postInvalidate();
    }

    private void finishNext() {
        // 设置为用户可移动
        pageEnable = true;
        // 未进行页面处理时, 是否为最后一页, 如果是最后一页, 就不需要改变bitmap
        boolean change = now >= listPosition[6] - 1;
        next();
        if (!pageEnable || change) return;
        changeBitmap(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (pageEnable) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    left = -width;
                    drawMode = SMOOTH_LAST_DRAW;
                    lastPage();
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    left = 0;
                    drawMode = SMOOTH_NEXT_DRAW;
                    nextPage();
                    return true;
            }
        }
        return true;
    }

    @Override
    public void lastPage() {
        // 进行翻页动画
        pageEnable = false;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveLast();
            }
        }, POST_DELAY);
    }

    private void moveLast() {
        // 不是第一页就翻到下一页, 是第一页就退回来
        if (now > 0) {
            if (left < 0) {
                left += width / DISTANCE;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        moveLast();
                    }
                }, POST_DELAY);
                if (left >= 0) return;
            } else {
                drawMode = NORMAL_DRAW;
                finishLast();
            }
        } else {
            if (left > -width) {
                left -= width / DISTANCE;
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        moveLast();
                    }
                }, POST_DELAY);
                if (left <= -width) return;
            } else {
                drawMode = NORMAL_DRAW;
                finishLast();
            }
        }
        postInvalidate();
    }

    private void finishLast() {
        pageEnable = true;
        // 未翻页前是否是第一页, 是第一页就没必要changeBitmap
        boolean change = now <= 0;
        last();
        if (!pageEnable || change) return;
        changeBitmap(false);
    }
}
