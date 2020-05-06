package flandre.cn.novel.view;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.Date;
import java.util.List;

/**
 * 上下滚动翻页
 * 2020.5.6
 */
public class ScrollPageView extends NormalPageView {
    private static final int SEPARATOR = 50;

    private int nowPos = 0;  // 第一行的位置
    private int offset = 0;  // 第一行的偏移
    private boolean isShowNotice = false;  // 是否显示通知
    private int yp;  // 当前手指的y
    private int lastYP;  // 上次的手指位置
    private long time;  // 当前时间
    private long lastTime;  // 上次时间
    private int speed;  // 速度
    private int pos;

    public ScrollPageView(Context context) {
        super(context);
    }

    public ScrollPageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ScrollPageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!pageEnable) return true;
        stopScroll();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = (int) event.getX();
                y = (int) event.getY();
                yp = (int) event.getY();
                time = new Date().getTime();
                break;
            case MotionEvent.ACTION_MOVE:
                // 处理滚动
                int y = (int) event.getY();
                lastYP = yp;
                scroll(y - lastYP);
                this.yp = y;
                lastTime = time;
                time = new Date().getTime();
                postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                int r = (int) (event.getY() - this.y);
                // 先处理点击事件
                if (Math.abs(r) < height / 20 && Math.abs(x - event.getX()) < width / 20) {
                    if (this.y > height / 2 + height / 10)
                        nextPage();
                    else if (this.y < height / 2 - height / 10)
                        if (alwaysNext) nextPage();
                        else lastPage();
                    else getPageTurn().onShowAction();
                    return true;
                }
                // 不是点击事件时自动滚动
                auto();
                break;
        }
        return true;
    }

    // 自动滚动
    private Runnable autoScroll = new Runnable() {
        @Override
        public void run() {
            scroll(speed);
            postInvalidate();
            speed /= 1.2;
            if (Math.abs(speed) < getRowHeight() / 5) return;
            postDelayed(autoScroll, SEPARATOR);
        }
    };

    private void auto() {
        int max = pageCount * getRowHeight() - getRowHeight();
        int sepTime = (int) (time - lastTime);
        if (sepTime == 0) sepTime = 1;
        speed = (int) (((float) (yp - lastYP)) / sepTime * 50);
        if (speed > max) speed = max;
        else if (speed < -max) speed = -max;
        startScroll();
    }

    private void startScroll() {
        postDelayed(autoScroll, SEPARATOR);
    }

    private void stopScroll() {
        removeCallbacks(autoScroll);
    }

    /**
     * 滚动文本
     * @param addY 文本位置增量
     */
    private void scroll(int addY) {
        offset += addY;
        if (offset > 0) {
            // 向上提一行, 把offset变成负的, 让他自己滚下来
            int t = (offset / getRowHeight() + 1);
            offset -= t * getRowHeight();
            nowPos -= t;
            if (nowPos < 0) {
                // 第一页的时候不能滚动
                if (now == 0) {
                    nowPos = 0;
                    offset = 0;
                    // 防止疯狂发信息
                    if (isShowNotice) return;
                    isShowNotice = true;
                    last();
                    return;
                } else nowPos = pageCount + nowPos;
                last();
            }
            isShowNotice = false;
        } else if (now + 1 == listPosition[6]) {
            // 最后一页时停止滚动
            nowPos = 0;
            offset = 0;
            if (isShowNotice) return;
            isShowNotice = true;
            next();
        } else if (Math.abs(offset) >= getRowHeight()) {
            // 当滚动了一行时, 向下提一行
            nowPos -= offset / getRowHeight();
            if (nowPos >= pageCount) {
                nowPos -= pageCount;
                next();
            }
            offset %= getRowHeight();
            isShowNotice = false;
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(color);
        // 文字没加载好就直接退出
        if (drawText == null) return;
        int i = 1;  // 当前文字的行数
        // 写当前页的内容
        String text = drawText[position].getText();
        List<String> strings = textPosition.get(now);
        for (int j = nowPos + 1; j <= pageCount; j++) {
            if (j > strings.size()) {
                i++;
                continue;
            }
            String[] mark = strings.get(j - 1).split(":");
            canvas.drawText(text, Integer.valueOf(mark[0]), Integer.valueOf(mark[1]), getLeftPadding(), getCowPosition(i) + offset, getPaint());
            i++;
        }
        // 如果不是最后一页, 写下一页的内容
        if (now + 1 != listPosition[position] || position != 6) {
            int pageCount = offset != 0 ? this.pageCount + 1 : this.pageCount;
            if (now + 1 == listPosition[position]) text = drawText[position + 1].getText();
            strings = textPosition.get(now + 1);
            for (int j = 1; i <= pageCount; i++) {
                if (j > strings.size()) continue;
                String[] mark = strings.get(j - 1).split(":");
                canvas.drawText(text, Integer.valueOf(mark[0]), Integer.valueOf(mark[1]), getLeftPadding(), getCowPosition(i) + offset, getPaint());
                j++;
            }
        }
        drawChapter(canvas, now);
    }

    @Override
    public void lastPage() {
        for (int i = 1; i <= pageCount / 2; i++) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    pos++;
                    scroll(getRowHeight() * 2);
                    postInvalidate();
                    if (pos == pageCount / 2) {
                        last();
                    }
                }
            }, i * SEPARATOR);
        }
    }

    @Override
    public void nextPage() {
        for (int i = 1; i <= pageCount / 2; i++) {
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    pos++;
                    scroll(-getRowHeight() * 2);
                    postInvalidate();
                    if (pos == pageCount / 2) {
                        next();
                    }
                }
            }, i * SEPARATOR);
        }
    }
}
