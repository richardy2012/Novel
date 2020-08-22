package flandre.cn.novel.activity;

import android.content.Intent;
import android.os.*;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.*;
import flandre.cn.novel.fragment.AlarmTriggerDialogFragment;
import flandre.cn.novel.info.NovelChapter;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.service.NovelService;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.fragment.LoadDialogFragment;
import flandre.cn.novel.fragment.TextPopupFragment;
import flandre.cn.novel.view.page.PageAnimation;
import flandre.cn.novel.view.page.PageView;
import flandre.cn.novel.view.page.PageViewTextManager;

import java.util.*;

/**
 * 2019.12.7
 */
public class TextActivity extends BaseActivity implements PageViewTextManager.LoadTextListener {
    private NovelInfo novelInfo;
    private SQLiteNovel sqLiteNovel;
    private PageView pageView;
    private FrameLayout popup;  // 点击中间时的弹出菜单
    private LoadDialogFragment loadDialogFragment;  // 加载页面
    public TextPopupFragment fragment;
    private AlarmTriggerDialogFragment dialogFragment;

    private String table;  // 表名
    private PageViewTextManager pageViewTextManager;

    public NovelInfo getNovelInfo() {
        return novelInfo;
    }

    public String getTable() {
        return table;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);
        setupNovelService();
        setupMusicService();

        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(getApplicationContext());

        NovelConfigureManager.getConfigure(getApplicationContext());

        setUpView(savedInstanceState);

        novelInfo = pageViewTextManager.getNovelInfo();
        table = pageViewTextManager.getTable();

        if (table != null) SQLTools.setStartTime(String.valueOf(novelInfo.getId()), sqLiteNovel, this);
    }

    public NovelService getService() {
        return mService;
    }

    private void setUpView(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            fragment = new TextPopupFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.popup, fragment, "TextPopupFragment");
            transaction.commit();
        } else {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragment = (TextPopupFragment) getSupportFragmentManager().findFragmentByTag("TextPopupFragment");
            fragmentTransaction.attach(fragment);
        }
        dialogFragment = new AlarmTriggerDialogFragment();
        loadDialogFragment = new LoadDialogFragment();
        loadDialogFragment.setCancelable(false);

        popup = findViewById(R.id.popup);
        RelativeLayout relativeLayout = findViewById(R.id.viewPage);
        pageView = new PageView(this);
        PageAnimation pageAnimation = NovelConfigureManager.getPageView(pageView);
        pageView.setPageAnimation(pageAnimation);

        pageViewTextManager = new PageViewTextManager(pageView, this);
        pageViewTextManager.init(savedInstanceState, this);

        pageView.setOnPageTurnListener(pageViewTextManager);
        pageView.setPadding(26, 20, 23, 20);
        relativeLayout.addView(pageView);
    }

    @Override
    void onServiceConnected(int service) {
        if (service == NOVEL_SERVICE_CONNECTED) pageViewTextManager.setService(mService);
        pageViewTextManager.onServiceConnected(service);
    }

    public List<? extends NovelChapter> getList() {
        return pageViewTextManager.getList();
    }

    public int getChapter() {
        return pageViewTextManager.getChapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 更改主题
        changeTheme();
        pageView.setTime(new Date().getTime());
    }

    public void choiceChapter(int i) {
        popup.setVisibility(View.GONE);
        if (i != 0) {
            pageViewTextManager.choiceChapter(i);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        pageViewTextManager.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    /**
     * 音量键换页
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return pageView.onKeyDown(keyCode, event);
        }
        return super.onKeyDown(keyCode, event);
    }

    public void changeTheme() {
        pageView.setTextSize(NovelConfigureManager.getConfigure().getTextSize());
        pageView.setTextColor(NovelConfigureManager.getConfigure().getTextColor());
        pageView.setDescriptionColor(NovelConfigureManager.getConfigure().getChapterColor());
        pageView.setColor(NovelConfigureManager.getConfigure().getBackgroundColor());
        pageView.setAlwaysNext(NovelConfigureManager.getConfigure().isAlwaysNext());
        pageView.update();
        if (!pageView.getPageAnimation().getClass().getName().equals(NovelConfigureManager.getConfigure().getNowPageView())) {
            pageView.setPageAnimation(NovelConfigureManager.getPageView(pageView));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (table != null) {
            SQLTools.setTime(novelInfo.getId(), sqLiteNovel);
            sendBroadcast(new Intent(IndexActivity.LOAD_DATA));
        }
    }

    @Override
    public void showLoadDialog() {
        loadDialogFragment.show(getSupportFragmentManager(), "LoadDialog");
    }

    @Override
    public void cancelLoadDialog() {
        loadDialogFragment.dismiss();
    }

    @Override
    public void showActionBar() {
        popup.setVisibility(View.VISIBLE);
    }

    @Override
    public void cancelActionBar() {
        popup.setVisibility(View.GONE);
    }

    @Override
    public void showAlarmDialog() {
        dialogFragment.setForce(NovelConfigureManager.getConfigure().isAlarmForce());
        dialogFragment.show(getSupportFragmentManager(), "AlarmTrigger");
    }
}
