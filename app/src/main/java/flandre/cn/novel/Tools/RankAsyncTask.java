package flandre.cn.novel.Tools;

import android.content.Context;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.adapter.RankAdapter;
import flandre.cn.novel.crawler.BaseCrawler;
import flandre.cn.novel.fragment.DataRankFragment;
import flandre.cn.novel.info.NovelInfo;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class RankAsyncTask extends AsyncTask<Integer, Void, List<NovelInfo>> {
    private WeakReference<IndexActivity> mContext;
    private RankAdapter adapter;
    private DataRankFragment fragment;

    public RankAsyncTask(Context context, RankAdapter adapter, DataRankFragment fragment) {
        super();
        this.mContext = new WeakReference<>((IndexActivity) context);
        this.adapter = adapter;
        this.fragment = fragment;
    }

    @Override
    protected List<NovelInfo> doInBackground(Integer... voids) {
        BaseCrawler baseCrawler = NovelConfigureManager.getCrawler(mContext.get(), null);
        if (baseCrawler == null) return null;
        return baseCrawler.rank(voids[0]);
    }

    @Override
    protected void onPostExecute(List<NovelInfo> list) {
        super.onPostExecute(list);
        if (fragment.getFrameLayout().findViewWithTag("rankData") == null) {
            fragment.getFrameLayout().removeAllViews();
            fragment.getFrameLayout().addView(fragment.getData());
        }
        if (list != null) {
            TextView text = fragment.getFrameLayout().findViewWithTag("IOError");
            if (text != null) fragment.getFrameLayout().removeView(text);
            while (list.remove(null)) ;
            adapter.update(list);
        } else {
            adapter.update(null);
            TextView text = fragment.getFrameLayout().findViewWithTag("IOError");
            if (text != null) fragment.getFrameLayout().removeView(text);

            text = new TextView(fragment.getContext());
            text.setText("网络错误");
            text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            text.setGravity(Gravity.CENTER);
            text.setLayoutParams(params);
            text.setTag("IOError");
            text.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
            fragment.getFrameLayout().addView(text);
        }
        fragment.getRefresh().setRefreshing(false);
    }
}
