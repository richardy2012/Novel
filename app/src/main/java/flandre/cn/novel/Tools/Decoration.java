package flandre.cn.novel.Tools;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import flandre.cn.novel.Tools.NovelAttr;
import flandre.cn.novel.Tools.NovelConfigureManager;

public class Decoration extends RecyclerView.ItemDecoration {

    private final int[] ATTRS = new int[]{
            android.R.attr.listDivider
    };
    private Drawable mDivider;
    Paint paint;

    private int mOrientation;

    public Decoration(Context context) {
        // 拿到ListView的属性
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        paint = new Paint();
        mDivider = a.getDrawable(0);
        a.recycle();
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getPaddingLeft();
        final int right = parent.getWidth() - parent.getPaddingRight();
        paint.setColor(NovelConfigureManager.getConfigure().getIntroduceTheme() & 0x22FFFFFF | 0x22000000);

        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount - 1; i++) {
            final View child = parent.getChildAt(i);
            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int top = child.getBottom() + params.bottomMargin;
            final int bottom = top + mDivider.getIntrinsicHeight();
            c.drawRect(left, top, right, bottom, paint);
//            mDivider.setBounds(left, top, right, bottom);
//            mDivider.draw(c);
        }

    }

    @Override
    public void getItemOffsets(Rect outRect, int itemPosition, RecyclerView parent) {
        // 横屏或竖屏
        outRect.set(0, 0, 0, mDivider.getIntrinsicHeight());
    }
}
