package flandre.cn.novel.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.adapter.FragmentPagerAdapter;
import flandre.cn.novel.crawler.Crawler;
import flandre.cn.novel.crawler.Crawler;

import java.util.ArrayList;
import java.util.List;

/**
 * 小说排行榜
 * 2019.??
 */
public class RankFragment extends AttachFragment {
    public static final String TAG = "RankFragment";
    private DataRankFragment dayFragment;
    private DataRankFragment monthFragment;
    private DataRankFragment totalFragment;
    private TabLayout tabLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.rank_fragment_layout, container, false);
        ViewPager viewPager = view.findViewById(R.id.pager);
        tabLayout = view.findViewById(R.id.tab);
        Adapter adapter = new Adapter(getChildFragmentManager());
        if (savedInstanceState == null) {
            dayFragment = DataRankFragment.newInstance(Crawler.DAY_RANK);
            monthFragment = DataRankFragment.newInstance(Crawler.MONTH_RANK);
            totalFragment = DataRankFragment.newInstance(Crawler.TOTAL_RANK);
        }else {
            dayFragment = (DataRankFragment) getChildFragmentManager().findFragmentByTag("DayFragment");
            monthFragment = (DataRankFragment) getChildFragmentManager().findFragmentByTag("MonthFragment");
            totalFragment = (DataRankFragment) getChildFragmentManager().findFragmentByTag("TotalFragment");
        }
        adapter.addItem(dayFragment, "周榜", "DayFragment");
        adapter.addItem(monthFragment, "月榜", "MonthFragment");
        adapter.addItem(totalFragment, "总榜", "TotalFragment");
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setSelectedTabIndicatorColor(Color.parseColor("#88000000"));
        return view;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (isVisibleToUser && dayFragment.isLoadEnable()){
            dayFragment.updateData();
            dayFragment.setLoadEnable(false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayout.setSelectedTabIndicatorColor(NovelConfigureManager.getConfigure().getMode() != NovelConfigure.NIGHT
                ? NovelConfigureManager.getConfigure().getMainTheme() : NovelConfigureManager.getConfigure().getNameTheme());
        tabLayout.setTabTextColors(NovelConfigureManager.getConfigure().getAuthorTheme(), NovelConfigureManager.getConfigure().getNameTheme());
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void changeTheme(){
        if (tabLayout == null) return;
        tabLayout.setSelectedTabIndicatorColor(NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY
                ? NovelConfigureManager.getConfigure().getMainTheme() : NovelConfigureManager.getConfigure().getNameTheme());
        tabLayout.setTabTextColors(NovelConfigureManager.getConfigure().getAuthorTheme(), NovelConfigureManager.getConfigure().getNameTheme());
        dayFragment.changeTheme();
        monthFragment.changeTheme();
        totalFragment.changeTheme();
    }

    class Adapter extends FragmentPagerAdapter {
        List<Fragment> fragments = new ArrayList<>();
        List<String> title = new ArrayList<>();

        Adapter(FragmentManager fm) {
            super(fm);
        }

        void addItem(Fragment fragment, String name, String tag){
            fragments.add(fragment);
            title.add(name);
            addTag(tag);
        }

        @Override
        public Fragment getItem(int i) {
            return fragments.get(i);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return title.get(position);
        }

        @Override
        public int getCount() {
            return fragments.size();
        }
    }
}
