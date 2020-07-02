package flandre.cn.novel.view.page;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MotionEvent;
import flandre.cn.novel.info.NovelText;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 滚动动画
 * <p>
 * 2020.6.24
 */
public class ScrollPageAnimation extends NormalPageAnimation {
    private static final int SEPARATOR = 10;
    private static final int MOVE_PAGE_SEPARATOR = 1;

    private int nowPos = 0;  // 第一行的位置
    private int offset = 0;  // 第一行的偏移
    private boolean isShowNotice = false;  // 是否显示通知
    private int yp;  // 当前手指的y
    private int lastYP;  // 上次的手指位置
    private long time;  // 当前时间
    private long lastTime;  // 上次时间
    private float speed;  // 速度
    private int pos;
    private int x, y;
    private boolean isFirst = true;
    private Bitmap mBitmap;
    private Canvas mCanvas;

    public ScrollPageAnimation(PageView view) {
        super(view);
    }

    @Override
    public void onLoad(int width, int height) {
        super.onLoad(width, height);
        mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    @Override
    public void onPaintChange() {
        if (mPageView.getLoad()) {
            drawScroll(mCanvas);
            mPageView.postInvalidate();
        }
    }

    @Override
    public void onUpdateText() {
        if (mPageView.mode == PageView.REDIRECT) drawScroll(mCanvas);
        super.onUpdateText();
    }

    @Override
    public boolean onTouch(MotionEvent event) {
        if (!mPageView.pageEnable) return true;
        stopScroll();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = (int) event.getX();
                y = (int) event.getY();
                yp = (int) event.getY();
                time = System.currentTimeMillis();
                break;
            case MotionEvent.ACTION_MOVE:
                // 处理滚动
                int y = (int) event.getY();
                lastYP = yp;
                scroll(y - lastYP);
                this.yp = y;
                lastTime = time;
                time = System.currentTimeMillis();
                drawScroll(mCanvas);
                mPageView.postInvalidate();
                break;
            case MotionEvent.ACTION_UP:
                int r = (int) (event.getY() - this.y);
                // 先处理点击事件
                if (Math.abs(r) < height / 20 && Math.abs(x - event.getX()) < width / 20) {
                    if (this.y > height / 2 + height / 10)
                        nextPage();
                    else if (this.y < height / 2 - height / 10)
                        if (mPageView.alwaysNext) nextPage();
                        else lastPage();
                    else mPageView.getPageTurn().onShowAction();
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
            int height = mPageView.getRowHeight() * 2;
            scroll(Math.abs(speed) > height ? speed > 0 ? height : -height : (int) speed);
            drawScroll(mCanvas);
            mPageView.postInvalidate();
            speed /= 1.05;
            if (Math.abs(speed) < mPageView.getRowHeight() / 10) return;
            mPageView.postDelayed(autoScroll, SEPARATOR);
        }
    };

    private void auto() {
        int max = (mPageView.pageCount - 1) * mPageView.getRowHeight() / 2;
        int sepTime = (int) (time - lastTime);
        if (sepTime == 0) sepTime = 1;
        speed = (((float) (yp - lastYP)) / sepTime * 50);
        if (speed > max) speed = max;
        else if (speed < -max) speed = -max;
        startScroll();
    }

    private void startScroll() {
        mPageView.postDelayed(autoScroll, SEPARATOR);
    }

    private void stopScroll() {
        mPageView.removeCallbacks(autoScroll);
    }

    /**
     * 滚动文本
     *
     * @param addY 文本位置增量
     */
    private void scroll(int addY) {
        offset += addY;
        if (offset > 0) {
            // 向上提一行, 把offset变成负的, 让他自己滚下来
            int t = (offset / mPageView.getRowHeight() + 1);
            offset -= t * mPageView.getRowHeight();
            nowPos -= t;
            if (nowPos < 0) {
                // 第一页的时候不能滚动
                if (mPageView.now == 0) {
                    nowPos = 0;
                    offset = 0;
                    // 防止疯狂发信息
                    if (isShowNotice) return;
                    isShowNotice = true;
                    mPageView.last();
                    return;
                } else nowPos = mPageView.pageCount + nowPos;
                mPageView.last();
            }
            isShowNotice = false;
        } else if (mPageView.now + 1 == mPageView.listPosition[6]) {
            // 最后一页时停止滚动
            nowPos = 0;
            offset = 0;
            if (isShowNotice) return;
            isShowNotice = true;
            mPageView.next();
        } else if (Math.abs(offset) >= mPageView.getRowHeight()) {
            // 当滚动了一行时, 向下提一行
            nowPos -= offset / mPageView.getRowHeight();
            if (nowPos >= mPageView.pageCount) {
                nowPos -= mPageView.pageCount;
                mPageView.next();
            }
            offset %= mPageView.getRowHeight();
            isShowNotice = false;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (isFirst) {
            if (mPageView.drawText != null)
                drawScroll(mCanvas);
            else mCanvas.drawColor(mPageView.color);
            isFirst = false;
        }
        canvas.drawBitmap(mBitmap, 0, 0, null);
    }

    private void drawScroll(Canvas canvas) {
        int position = mPageView.position;
        NovelText[] drawText = mPageView.drawText;
        ArrayList<ArrayList<String>> textPosition = mPageView.textPosition;
        int now = mPageView.now;
        Integer[] listPosition = mPageView.listPosition;

        canvas.drawColor(mPageView.color);
        // 文字没加载好就直接退出
        if (mPageView.drawText == null) return;
        int i = 1;  // 当前文字的行数
        // 写当前页的内容
        String text = drawText[position].getText();
        List<String> strings = textPosition.get(now);
        for (int j = nowPos + 1; j <= mPageView.pageCount; j++) {
            if (j > strings.size()) {
                i++;
                continue;
            }
            String[] mark = strings.get(j - 1).split(":");
            canvas.drawText(text, Integer.valueOf(mark[0]), Integer.valueOf(mark[1]), mPageView.getLeftPadding(), mPageView.getCowPosition(i) + offset, mPageView.getPaint());
            i++;
        }
        // 如果不是最后一页, 写下一页的内容
        if (now + 1 != listPosition[position] || position != 6) {
            int pageCount = offset != 0 ? mPageView.pageCount + 1 : mPageView.pageCount;
            pageCount = mPageView.heightRest > mPageView.getRowHeight() / 1.5 ? pageCount + 1 : pageCount;
            if (now + 1 == listPosition[position]) text = drawText[position + 1].getText();
            strings = textPosition.get(now + 1);
            for (int j = 1; i <= pageCount; i++) {
                if (j > strings.size()) continue;
                String[] mark = strings.get(j - 1).split(":");
                canvas.drawText(text, Integer.valueOf(mark[0]), Integer.valueOf(mark[1]), mPageView.getLeftPadding(), mPageView.getCowPosition(i) + offset, mPageView.getPaint());
                j++;
            }
        }
        mPageView.drawChapter(canvas, now, position);
    }

    @Override
    public void nextPage() {
        for (int i = 1; i <= mPageView.pageCount / MOVE_PAGE_SEPARATOR; i++) {
            mPageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pos++;
                    scroll(-mPageView.getRowHeight() * MOVE_PAGE_SEPARATOR);
                    drawScroll(mCanvas);
                    mPageView.postInvalidate();
                    if (pos == mPageView.pageCount / MOVE_PAGE_SEPARATOR) {
                        mPageView.next();
                    }
                }
            }, i * SEPARATOR);
        }
    }

    @Override
    public void lastPage() {
        for (int i = 1; i <= mPageView.pageCount / MOVE_PAGE_SEPARATOR; i++) {
            mPageView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    pos++;
                    scroll(mPageView.getRowHeight() * MOVE_PAGE_SEPARATOR);
                    drawScroll(mCanvas);
                    mPageView.postInvalidate();
                    if (pos == mPageView.pageCount / MOVE_PAGE_SEPARATOR) {
                        mPageView.last();
                    }
                }
            }, i * SEPARATOR);
        }
    }
}
