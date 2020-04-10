package flandre.cn.novel.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.*;
import android.widget.*;
import flandre.cn.novel.adapter.FragmentPagerAdapter;
import flandre.cn.novel.fragment.*;
import flandre.cn.novel.service.NovelService;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.adapter.PopUpAdapter;
import flandre.cn.novel.parse.FileParse;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.R;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.Tools.NovelAttr;
import flandre.cn.novel.Tools.Item;

import java.io.*;
import java.util.*;

/**
 * Index页面
 * 2019.12.4
 */
public class IndexActivity extends BaseActivity implements PopUpAdapter.OnPopUpClickListener, FileParse.OnfinishParse {
    // 底部的三个选择卡
    private TextView[] text;
    private LinearLayout[] select;
    private ImageView[] image;

    private ViewPager pager;
    private DrawerLayout drawerLayout;
    private LinearLayout popLeft;  // 侧面弹出菜单
    private Toolbar bar;
    private GridLayout gridLayout;
    private BookFragment bookFragment;
    private RankFragment rankFragment;
    private UserFragment userFragment;
    private List<Item> items;  // 适配器使用的数据
    private PopUpAdapter adapter;
    private ImageView imageView;  // 侧面弹出菜单头顶的图片
    private AlarmDialogFragment alarmDialogFragment;
    private MainAdapter mainAdapter;
    private boolean isPlayMusic = false;
    private boolean isPlaying = false;

    private Handler handler;  // UI线程消息处理

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("SaveTime", new Date().getTime());
        if (musicService != null)
            outState.putBoolean("isPlaying", isPlaying);
    }

    @Override
    void onServiceConnected(int service) {
        if (service == BaseActivity.MUSIC_SERVICE_CONNECTED && isPlayMusic) {
            try {
                musicService.play();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_index);
        unpackSave(savedInstanceState);
        setupMusicService();
        addMusicListener(this);

        // 加载配置文件
        NovelConfigureManager.getConfigure(getApplicationContext());

        // 数据库连接
        SQLiteNovel.getSqLiteNovel(getApplicationContext());

        //设置线程
        setupHandler();
        // 设置线程优先级
//        android.os.Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

        setupValues();
        setupTool();
        setupPager(savedInstanceState);
        setTextListener();
        setupPopLeft();
        changePartly();
        setupNovelService();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                receiveOut();
            }
        }, 500);
    }

    private void setupHandler() {
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                switch (message.what) {
                    case 0x103:
                        bookFragment.deleteBook(message);
                        break;
                }
                return true;
            }
        });
    }

    private void unpackSave(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            long time = new Date().getTime() - savedInstanceState.getLong("SaveTime");
            if (time < 10000 && savedInstanceState.getBoolean("isPlaying"))
                isPlayMusic = true;
        }
    }

    @Override
    public void onPlayMusic() {
        isPlaying = true;
    }

    @Override
    public void onPauseMusic() {
        isPlaying = false;
    }

    private void setupValues() {
        select = new LinearLayout[3];
        select[0] = findViewById(R.id.book_line);
        select[1] = findViewById(R.id.rank_line);
        select[2] = findViewById(R.id.user_line);

        text = new TextView[3];
        text[0] = findViewById(R.id.book);
        text[1] = findViewById(R.id.rank);
        text[2] = findViewById(R.id.user);

        image = new ImageView[3];
        image[0] = findViewById(R.id.book_image);
        image[1] = findViewById(R.id.rank_image);
        image[2] = findViewById(R.id.user_image);

        gridLayout = findViewById(R.id.tab);
        pager = findViewById(R.id.pager);
        popLeft = findViewById(R.id.left);
        bar = findViewById(R.id.tool);
        drawerLayout = findViewById(R.id.drawer);
        image[0].setSelected(true);
        text[0].setSelected(true);
    }

    private void setupTool() {
        setSupportActionBar(bar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    private void setupPager(Bundle savedInstanceState) {
        pager.setOffscreenPageLimit(2);
        mainAdapter = new MainAdapter(getSupportFragmentManager());
        if (savedInstanceState  == null) {
            bookFragment = new BookFragment();
            rankFragment = new RankFragment();
            userFragment = new UserFragment();
        }else {
            bookFragment = (BookFragment) getSupportFragmentManager().findFragmentByTag(BookFragment.TAG);
            rankFragment = (RankFragment) getSupportFragmentManager().findFragmentByTag(RankFragment.TAG);
            userFragment = (UserFragment) getSupportFragmentManager().findFragmentByTag(UserFragment.TAG);
        }
        mainAdapter.addFragment(bookFragment, BookFragment.TAG);
        mainAdapter.addFragment(rankFragment, RankFragment.TAG);
        mainAdapter.addFragment(userFragment, UserFragment.TAG);
        pager.setAdapter(mainAdapter);
        pager.setCurrentItem(0);
        pager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) {

            }

            @Override
            public void onPageSelected(int i) {
                switchTabs(i);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });
    }

    private void setTextListener() {
        select[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(0);
            }
        });

        select[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(1);
            }
        });

        select[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pager.setCurrentItem(2);
            }
        });
    }

    private void setupPopLeft() {
        alarmDialogFragment = new AlarmDialogFragment();
        LayoutInflater inflater = LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.pop_left_layput, popLeft, false);
        imageView = view.findViewById(R.id.novel_img);
        imageView.setBackground(getResources().getDrawable(NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY
                ? R.drawable.novel_top_day : R.drawable.novel_top_night));
        RecyclerView recyclerView = view.findViewById(R.id.novel_rec);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new Decoration(this));
        adapter = new PopUpAdapter(null);
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);
        popLeft.addView(view);
        // 把PopLeft事件消化完, 不会交给其他处理(不写这个可能会当做点击了小说进入阅读界面)
        popLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        addItem();
    }

    private void addItem() {
        items = new ArrayList<>();
        if (NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY) {
            addItem("阅读时长", R.drawable.read_time_day);
            addItem("书籍管理", R.drawable.book_manage_day);
//            addItem("阅读记录", R.drawable.read_record_day);
            addItem("更改主题", R.drawable.setting_day);
            addItem("设置来源", R.drawable.source_day);
            addItem("小说闹钟", R.drawable.alarm_day);
            addItem("夜间模式", R.drawable.night);
            addItem("退出程序", R.drawable.exit_day);
        } else {
            addItem("阅读时长", R.drawable.read_time_night);
            addItem("书籍管理", R.drawable.book_manage_night);
//            addItem("阅读记录", R.drawable.read_record_night);
            addItem("更改主题", R.drawable.setting_night);
            addItem("设置来源", R.drawable.source_night);
            addItem("小说闹钟", R.drawable.alarm_night);
            addItem("日间模式", R.drawable.day);
            addItem("退出程序", R.drawable.exit_night);
        }
        adapter.update(items);
    }

    private void addItem(String string, int id) {
        items.add(new Item(string, id));
    }

    private void changePartly() {
        pager.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        bar.setBackgroundColor(NovelConfigureManager.getConfigure().getMainTheme());
        popLeft.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        gridLayout.setBackgroundColor(NovelConfigureManager.getConfigure().getMainTheme());
    }

    private void receiveOut() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null && bundle.get("path") != null) {
            FileParse fileParse = new FileParse((String) bundle.get("path"), SQLiteNovel.getSqLiteNovel(this.getApplicationContext()), this);
            fileParse.setOnfinishParse(this);
            try {
                fileParse.parseFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFinishParse(int mode) {
        if (mode == FileParse.OK)
            this.getBookFragment().loadData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        userFragment.changeTheme();
        if (NovelAttr.changeThemeEnable) {
            changeTheme();
            NovelAttr.changeThemeEnable = false;
        }
        if (NovelAttr.loadDataEnable) {
            bookFragment.loadData();
            NovelAttr.loadDataEnable = false;
        }
    }

    private void switchTabs(int position) {
        for (int i = 0; i < select.length; i++) {
            if (i == position) {
                text[i].setSelected(true);
                image[i].setSelected(true);
            } else {
                text[i].setSelected(false);
                image[i].setSelected(false);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 在ActionBar上添加菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.index_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search:
                // 点击放大镜时去搜索页面
                Intent intent = new Intent(this, SearchActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
                break;
            case android.R.id.home:
                drawerLayout.openDrawer(Gravity.LEFT);
                break;
        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case 0x1:
                changeTheme();
                break;
            case 0x2:
                userFragment.handleFile(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void popUpClickListener(View view, int pos) {
        switch (pos) {
            case 0:
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(IndexActivity.this, ReadTimeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivityForResult(intent, 0x1);
                            }
                        }, 250);
                break;
            case 1:
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(IndexActivity.this, BookManageActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivityForResult(intent, 0x1);
                            }
                        }, 250);
                break;
            case 2:
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(IndexActivity.this, ConfigureThemeActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivityForResult(intent, 0x1);
                            }
                        }, 250);
                break;
            case 3:
                handler.postDelayed(
                        new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(IndexActivity.this, ConfigureSourceActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                                startActivity(intent);
                            }
                        }, 250);
                break;
            case 4:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        alarmDialogFragment.show(getSupportFragmentManager(), "AlarmDialog");
                    }
                }, 250);
                break;
            case 5:
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        changeConfigure();
                    }
                }, 250);
                break;
            case 6:
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
                finish();
                break;
        }
        drawerLayout.closeDrawers();
    }

    /**
     * 切换主题
     * 用户具有两套主题, 一套是默认的主题, 一套是夜间的主题
     */
    private void changeConfigure() {
        String name = NovelConfigureManager.getConfigure().getNowSourceKey();
        String source = NovelConfigureManager.getConfigure().getNowSourceValue();
        NovelConfigureManager.changeConfigure();
        NovelConfigureManager.getConfigure().setNowSourceKey(name);
        NovelConfigureManager.getConfigure().setNowSourceValue(source);
        try {
            NovelConfigureManager.saveConfigure(NovelConfigureManager.getConfigure(), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
        changeTheme();
    }

    /**
     * 根据配置文件马上更新主题
     */
    private void changeTheme() {
        changePartly();
        addItem();
        imageView.setBackground(getResources().getDrawable(NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY
                ? R.drawable.novel_top_day : R.drawable.novel_top_night));
        bookFragment.changeTheme();
        rankFragment.changeTheme();
        userFragment.changeTheme();
    }

    public BookFragment getBookFragment() {
        return bookFragment;
    }

    public Handler getHandler() {
        return handler;
    }

    public NovelService getService() {
        return mService;
    }

    public UserFragment getUserFragment() {
        return userFragment;
    }

    class MainAdapter extends FragmentPagerAdapter {
        List<AttachFragment> list = new ArrayList<>();

        MainAdapter(FragmentManager fm) {
            super(fm);
        }

        void addFragment(AttachFragment fragment, String tag) {
            list.add(fragment);
            addTag(tag);
        }

        @Override
        public Fragment getItem(int i) {
            return list.get(i);
        }

        @Override
        public int getCount() {
            return list.size();
        }
    }
}