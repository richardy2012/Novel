package flandre.cn.novel.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.R;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.activity.NovelDetailActivity;
import flandre.cn.novel.adapter.RankAdapter;
import flandre.cn.novel.Tools.RankAsyncTask;

/**
 * 排行榜
 * 2019.??
 */
public class DataRankFragment extends AttachFragment implements RankAdapter.RankClick {
    private RankAdapter adapter;
    private int rankType;  // 更新的类型
    private boolean loadEnable;  // 加载排行榜是否可用
    private View data;  // 排行榜界面
    private SwipeRefreshLayout refresh;
    private FrameLayout frameLayout;  // 界面容器

    public RankAdapter getAdapter() {
        return adapter;
    }

    int getRankType() {
        return rankType;
    }

    public SwipeRefreshLayout getRefresh() {
        return refresh;
    }

    /**
     * 传递创建的类型
     */
    public static DataRankFragment newInstance(int type) {
        DataRankFragment fragment = new DataRankFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("type", type);
        fragment.setArguments(bundle);
        return fragment;
    }

    /**
     * 当用户第一次看见时更新数据
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (loadEnable && isVisibleToUser) {
            loadEnable = false;
            updateData();
        }
    }

    boolean isLoadEnable() {
        return loadEnable;
    }

    void setLoadEnable(boolean loadEnable) {
        this.loadEnable = loadEnable;
    }

    public FrameLayout getFrameLayout() {
        return frameLayout;
    }

    public View getData() {
        return data;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        loadEnable = true;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void OnClickListen(View view, int pos) {
        // 点击排行榜上的Item时进入查看详细界面
        NovelInfo novelInfo = adapter.getData().get(pos);
        Intent intent = new Intent(mContext, NovelDetailActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("NovelInfo", novelInfo.copy());
        intent.putExtras(bundle);
        startActivity(intent);
    }

    public void updateData() {
        RankAsyncTask task = new RankAsyncTask(getContext(), getAdapter(), this);
        task.execute(getRankType());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rankType = getArguments().getInt("type");
        SQLiteNovel.getSqLiteNovel(mContext.getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rank_detail_layout, container, false);
        frameLayout = view.findViewById(R.id.base);
        loadRankUI(inflater);
        // 加载加载界面
        View load = inflater.inflate(R.layout.loading_layout, frameLayout, false);
        load.setTag("loading");
        ImageView load_img = load.findViewById(R.id.load_img);
        load_img.setBackground(mContext.getResources().getDrawable(
                NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY ? R.drawable.loading_day : R.drawable.loading_night));
        TextView textView = load.findViewById(R.id.load_txt);
        textView.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        // 让图片旋转
        RotateAnimation animation = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        animation.setRepeatCount(Animation.INFINITE);
        animation.setRepeatMode(Animation.RESTART);
        animation.setInterpolator(new LinearInterpolator());
        load_img.setAnimation(animation);
        animation.setDuration(1000);
        frameLayout.addView(load);
        return view;
    }

    private void loadRankUI(LayoutInflater inflater) {
        // 加载排行榜的UI, 等数据拿到时添加到frameLayout容器
        data = inflater.inflate(R.layout.rank_data, frameLayout, false);
        data.setTag("rankData");
        refresh = data.findViewById(R.id.fresh);

        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateData();
            }
        });
        RecyclerView recyclerView = data.findViewById(R.id.data);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayout.VERTICAL);
        recyclerView.setLayoutManager(manager);
        adapter = new RankAdapter(null);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new Decoration(mContext));
        recyclerView.setHasFixedSize(false);
        adapter.setListen(this);
    }

    void changeTheme() {
        if (frameLayout == null) return;
        if (frameLayout.findViewWithTag("loading") != null) {
            View view = frameLayout.findViewWithTag("loading");
            view.findViewById(R.id.load_img).setBackground(mContext.getResources().getDrawable(
                    NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY ? R.drawable.loading_day : R.drawable.loading_night));
            ((TextView) view.findViewById(R.id.load_txt)).setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        } else if (frameLayout.findViewWithTag("IOError") != null) {
            ((TextView) frameLayout.findViewWithTag("IOError")).setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
        } else {
            adapter.notifyDataSetChanged();
        }
    }
}
