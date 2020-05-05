package flandre.cn.novel.activity;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.support.v4.app.FragmentTransaction;
import android.view.*;
import android.widget.*;
import flandre.cn.novel.database.SharedTools;
import flandre.cn.novel.fragment.AlarmDialogFragment;
import flandre.cn.novel.fragment.AlarmTriggerDialogFragment;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.info.NovelText;
import flandre.cn.novel.info.NovelTextItem;
import flandre.cn.novel.info.WrapperNovelText;
import flandre.cn.novel.service.NovelService;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.crawler.BaseCrawler;
import flandre.cn.novel.fragment.LoadDialogFragment;
import flandre.cn.novel.fragment.TextPopupFragment;
import flandre.cn.novel.serializable.SelectList;
import flandre.cn.novel.view.PageView;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * 小说文本界面
 * 爬取下一章：当在{@link TextActivity#textPosition}=3且进入下一章时加载章节,
 * {@link TextActivity#crawlPosition}爬取位置是当前章节{@link TextActivity#chapter}-2, 加载7章.
 * 爬取上一章：当在{@link TextActivity#textPosition}=1且进入上一章时加载章节,
 * {@link TextActivity#crawlPosition}爬取位置时当前章节{@link TextActivity#chapter}-5, 加载7章.
 * 当全部文本加载好时, 会把当前的文本与章节替换, 并重设{@link TextActivity#textPosition}
 * 2019.12.7
 */
public class TextActivity extends BaseActivity implements PageView.PageTurn {
    public static final int BufferChapterCount = 7;

    private NovelInfo novelInfo;
    public List<NovelTextItem> list = null;  // 当小说没有被收藏时, 小说的章节以及网址
    private SQLiteNovel sqLiteNovel;
    private PageView pageView;
    private FrameLayout popup;  // 点击中间时的弹出菜单
    private LoadDialogFragment loadDialogFragment;  // 加载页面
    public TextPopupFragment fragment;
    private AlarmTriggerDialogFragment dialogFragment;

    private NovelText[] novelTexts = new NovelText[BufferChapterCount];
    private int textPosition = 0;  // 当前的text位置
    private int chapter;  // 当前章节

    private int flushCount = 0;  // 当前已经更新的章节数量
    private int crawlPosition;  // 爬取的开始章节
    private int crawlChapter;  // 前往爬取时当前章节的位置
    private int lastChapter = 0;  // 最后一章的id

    private String table;  // 表名

    private boolean emptyTable = false;  // 是否为空表
    private boolean flushFinish = true;  // 是否可以重新加载text
    private boolean showActionBar = true;  // 是否能展示ActionBar
    private boolean redirect = false;  // 是否从外部跳转而来
    private boolean loadIsAdd = false;  // 加载对话框是否加载
    private boolean showLoad = true;  // 是否展示加载对话框(true时, 在合适的时候展示)

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case 0x300:
                    loadText(message);
                    break;
                case 0x302:
                    replaceText(message);
                    break;
                case 0x303:
                    redirectChapter(message);
                    break;
            }
            return true;
        }
    });

    public Handler getHandler() {
        return handler;
    }

    public NovelInfo getNovelInfo() {
        return novelInfo;
    }

    public int getChapter() {
        return chapter;
    }

    public String getTable() {
        return table;
    }

    /**
     * 更新text
     * 如果load为true,更新完后显示出来
     */
    private void loadText(Message message) {
        if (showLoad) {
            setLoad();
        }
        BaseCrawler crawler = null;
        try {
            if (novelInfo.getSource() == null) {
                crawler = (BaseCrawler) NovelConfigureManager.getConstructor().newInstance(this, this.handler);
            } else {
                crawler = (BaseCrawler) Class.forName(novelInfo.getSource()).
                        getConstructor(Activity.class, Handler.class).newInstance(this, this.handler);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        // 当没有收藏图书时,获取列表数据,然后爬取text
        if (table == null) {
            // 第一次加载时, 会加载章节
            if (list == null) {
                if (message.obj == null) {
                    Toast.makeText(TextActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                list = (List<NovelTextItem>) message.obj;
            }
            // 爬取章节=当前章节-2, 爬取章节等于总章节-2, 表示当前章节是最后一章
            if (crawlPosition == list.size() - 2 && !redirect) {
//                Toast.makeText(TextActivity.this, "最后一章了", Toast.LENGTH_SHORT).show();
                flushFinish = true;
                return;
            }
            if (crawlPosition > list.size() - 6) {
                crawlPosition = list.size() - 6;
                handler.sendEmptyMessage(0x300);
                return;
            }
            for (int i = 0; i < 7; i++) {
                crawler.text(list.get(crawlPosition - 1 + i).getUrl(), i, table).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else {
            // 收藏情况下的文本处理
            handleTableText(crawler, message);
        }
    }

    private void setLoad() {
        if (!loadIsAdd) {
            loadDialogFragment.show(getSupportFragmentManager(), "LoadDialog");
            loadIsAdd = true;
        }
    }

    /**
     * 更新一条text文本
     * message.arg1: 更新的位置
     * message.obj: 更新的文本与章节
     */
    private void replaceText(Message message) {
        String table = null;
        int position = message.arg1;
        NovelText novelText;
        if (message.obj instanceof WrapperNovelText) {
            table = ((WrapperNovelText) message.obj).getTable();
            novelText = ((WrapperNovelText) message.obj).getNovelText();
        } else {
            novelText = (NovelText) message.obj;
        }
        // 如果obj时null表示小说未收藏时Activity的恢复状态
        if (message.obj != null) {
            novelTexts[position] = novelText;
            flushCount++;
        }
        // 当它们相等时, 表示此次text更新完成
        if (flushCount == BufferChapterCount) {
            // 首次加载最后章节
            if (lastChapter == 0) {
                if (this.table != null) {
                    Cursor cursor = sqLiteNovel.getReadableDatabase().query(this.table, null, null,
                            null, null, null, null);
                    lastChapter = cursor.getCount();
                    cursor.close();
                } else {
                    lastChapter = list.size();
                }
                pageView.setLastChapter(lastChapter);
            }
            // 更新章节文件
            pageView.updateText(novelTexts, chapter - crawlPosition);
            // 当展示了对话框且对话框已经在展示了把对话框去掉
            if (showLoad && loadIsAdd) {
                loadDialogFragment.dismiss();
                showLoad = false;
                loadIsAdd = false;
            }
            if (redirect) {
                pageView.flashWatch();
                redirect = false;
            }
            flushFinish = true;
        }
        if (message.arg2 != 1 && sqLiteNovel.freeStatus && table != null) {
            ContentValues values = new ContentValues();
            values.put("text", novelTexts[position].getText());
            sqLiteNovel.getReadableDatabase().update(table, values, "id=?", new String[]{String.valueOf(crawlPosition + position)});
        }
    }

    /**
     * 章节跳转
     * message.obj: 跳转的位置
     */
    private void redirectChapter(Message message) {
        int id = (Integer) message.obj;
        crawlChapter = chapter = id;

        if (chapter != 1) {
            crawlPosition = crawlChapter - 1;
        } else {
            crawlPosition = crawlChapter;
        }

        flushCount = 0;
        showActionBar = true;
        flushFinish = false;
        redirect = true;
        showLoad = true;
        pageView.setWatch(1);
        pageView.setPageEnable(false);
        pageView.setMode(PageView.REDIRECT);

        Message msg = new Message();
        msg.what = 0x300;
        handler.sendMessage(msg);
    }

    private void handleTableText(BaseCrawler crawler, Message message) {
        if (emptyTable) {
            // 若表为空时,把本书的所有章节都添加进数据库(不包括文本)
            if (message.obj == null) {
                Toast.makeText(TextActivity.this, "网络错误", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            list = (List<NovelTextItem>) message.obj;
            sqLiteNovel.freeStatus = false;
            new NewTableTask().execute();
            emptyTable = false;
            // 爬取文本
            for (int i = 0; i < 7; i++) {
                crawler.text(list.get(crawlPosition - 1 + i).getUrl(), i, table).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        } else {
            // 拿到表数据若有文字了,就直接使用,没有从网上爬取
            Cursor cursor = sqLiteNovel.getReadableDatabase().query(table, new String[]{"url", "chapter", "text", "id"},
                    "id>=?", new String[]{crawlPosition + ""}, null, null,
                    null, String.valueOf(BufferChapterCount));
            // 表里面是否存在该章节
            if (cursor.moveToNext()) {
                int count = -1;
                List<Object> data = new ArrayList<>();
                do {
                    count++;
                    if (cursor.getString(2) == null) {
                        NovelTextItem novelTextItem = new NovelTextItem();
                        novelTextItem.setUrl(cursor.getString(0));
                        novelTextItem.setChapter(cursor.getString(1));
                        data.add(novelTextItem);
                    } else {
                        NovelText novelText = new NovelText();
                        novelText.setText(cursor.getString(2));
                        novelText.setChapter(cursor.getString(1));
                        data.add(novelText);
                    }
                } while (cursor.moveToNext());
                // 最后一章时阻止溢出
                if (count == 2 && !redirect) {
                    // Toast.makeText(TextActivity.this, "最后一章了", Toast.LENGTH_SHORT).show();
                    // SQLTools.setFinishTime((String) map.get("id"), sqLiteNovel, this);
                    flushFinish = true;
                    return;
                }
                // 使章节缓存总是够7章(当前=末尾-2->当前=末尾-6), BUG(当小说总体章数不足以7章时会发生什么)
                if (count < BufferChapterCount - 1) {
                    crawlPosition = crawlPosition - (BufferChapterCount - count - 1);
                    handler.sendEmptyMessage(0x300);
                    return;
                }
                // 如果存在文本直接拿, 没有从网上爬
                for (int i = 0; i < data.size(); i++) {
                    if (data.get(i) instanceof NovelTextItem) {
                        crawler.text(((NovelTextItem) data.get(i)).getUrl(), i, table).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    } else {
                        Message msg = new Message();
                        msg.what = 0x302;
                        msg.arg1 = i;
                        msg.arg2 = 1;
                        msg.obj = data.get(i);
                        handler.sendMessage(msg);
                    }
                }
            } else {
                // 表为空时从网上爬取目录进行填充
                emptyTable = true;
                crawler.list(novelInfo.getUrl());
            }
            cursor.close();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text);

        // getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(getApplicationContext());

        NovelConfigureManager.getConfigure(getApplicationContext());

        unpack(savedInstanceState);
        setUpView(savedInstanceState);

        if (table != null) SQLTools.setStartTime(String.valueOf(novelInfo.getId()), sqLiteNovel, this);

        redirect = true;
        setUpText(savedInstanceState);
        setupNovelService();
        setupMusicService();
    }

    private void unpack(Bundle savedInstanceState) {
        Bundle bundle;
        if (savedInstanceState == null)
            bundle = getIntent().getExtras();
        else bundle = savedInstanceState;

        String name = bundle.getString("name");
        String author = bundle.getString("author");
        if (bundle.get("url") == null) {
            novelInfo = SQLTools.getNovelOneData(sqLiteNovel, name, author);
            table = novelInfo.getTable();
        } else {
            novelInfo = new NovelInfo();
            novelInfo.setName(name);
            novelInfo.setAuthor(author);
            novelInfo.setSource(bundle.getString("source"));
            novelInfo.setUrl(bundle.getString("url"));
            table = null;
        }
    }

    public NovelService getService() {
        return mService;
    }

    private void setUpView(Bundle savedInstanceState) {
        popup = findViewById(R.id.popup);
        RelativeLayout relativeLayout = findViewById(R.id.viewPage);
        pageView = NovelConfigureManager.getPageView(this);
        pageView.setOnPageTurnListener(this);
        pageView.setPadding(26, 20, 29, 20);
        relativeLayout.addView(pageView);
        if (savedInstanceState == null) {
            fragment = new TextPopupFragment();
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.popup, fragment, "TextPopupFragment");
            transaction.commit();
        }else {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragment = (TextPopupFragment) getSupportFragmentManager().findFragmentByTag("TextPopupFragment");
            fragmentTransaction.attach(fragment);
        }
        dialogFragment = new AlarmTriggerDialogFragment();
        loadDialogFragment = new LoadDialogFragment();
        loadDialogFragment.setCancelable(false);
    }

    private void setUpText(Bundle savedInstanceState) {
        String watch;
        String[] split;
        if (table == null) {
            if (savedInstanceState == null) {
                split = new String[]{"1", "1"};
                crawlChapter = chapter = Integer.parseInt(split[0]);
            } else {
                split = ((String) savedInstanceState.get("watch")).split(":");
                crawlChapter = Integer.parseInt(split[0]);
                chapter = Integer.parseInt(split[2]);
                list = ((SelectList) savedInstanceState.get("list")).getList();
                novelTexts = (NovelText[]) savedInstanceState.getSerializable("NovelText");
                crawlPosition = Integer.parseInt(split[3]);
            }
            pageView.setWatch(Integer.parseInt(split[1]));
            // 当Activity处于恢复时, 不需要再次从网上下载信息, 直接使用以前的
            if (list != null) {
                showLoad = false;
                flushFinish = false;
                flushCount = 7;
                handler.obtainMessage(0x302, null).sendToTarget();
                return;
            }
            crawlPosition = crawlChapter;
            try {
                // 获取目录列表
                setLoad();
                ((BaseCrawler) NovelConfigureManager.getConstructor().newInstance(this, this.handler)).list(novelInfo.getUrl());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            if (savedInstanceState == null) {
                split = (novelInfo.getWatch()).split(":");
                crawlChapter = chapter = Integer.parseInt(split[0]);
                crawlPosition = crawlChapter > 1 ? crawlChapter - 1 : crawlChapter;
            } else {
                split = ((String) savedInstanceState.get("watch")).split(":");
                crawlChapter = Integer.parseInt(split[0]);
                chapter = Integer.parseInt(split[2]);
                crawlPosition = Integer.parseInt(split[3]);
            }
            pageView.setWatch(Integer.parseInt(split[1]));

            Message message = new Message();
            message.what = 0x300;
            handler.sendMessage(message);
        }
        pageView.setChapter(chapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 更改主题
        changeTheme();
        pageView.setTime(new Date().getTime());
    }

    public void choiceChapter(int i){
        popup.setVisibility(View.GONE);
        if (i != 0) {
            Message message = new Message();
            message.obj = i;
            message.what = 0x303;
            handler.sendMessage(message);
        }
    }

    /**
     * 把当前观看进度放入数据库,并更新页面
     */
    @Override
    public synchronized void onUpdateWatch(long addTime, int watch) {
        SharedTools sharedTools = new SharedTools(this);
        sharedTools.setReadTime(addTime);
        long alarm = sharedTools.getAlarm();
        // 存在闹钟时
        if (alarm != AlarmDialogFragment.NO_ALARM_STATE)
            if (alarm - addTime <= 0) {
                sharedTools.setAlarm(AlarmDialogFragment.NO_ALARM_STATE);
                dialogFragment.setForce(NovelConfigureManager.getConfigure().isAlarmForce());
                dialogFragment.show(getSupportFragmentManager(), "AlarmTrigger");
            } else sharedTools.setAlarm(alarm - addTime);
        sharedTools.setTodayRead(addTime);
        if (sqLiteNovel.freeStatus && table != null) {
            SQLTools.setRead(String.valueOf(novelInfo.getId()), sqLiteNovel, addTime, chapter, watch);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        // 把观看的进度保存了
        outState.putString("watch", crawlChapter + ":" + pageView.getWatch() + ":" + chapter + ":" + crawlPosition);
        outState.putBoolean("star", table != null);
        outState.putString("name", novelInfo.getName());
        outState.putString("author", novelInfo.getAuthor());
        if (table == null) {
            outState.putSerializable("NovelText", pageView.getDrawText());
            SelectList<NovelTextItem> selectList = new SelectList<>();
            selectList.setList(list);
            outState.putSerializable("list", selectList);
            outState.putString("source", novelInfo.getSource());
            outState.putString("url", novelInfo.getUrl());
        }
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (table != null) SQLTools.setTime(novelInfo.getId(), sqLiteNovel);
    }

    @Override
    public boolean onLastPage(int position, boolean isPositionChange, boolean isPageEnable, boolean isNowOverflow) {
        if (isPositionChange) {
            chapter--;
        }
        textPosition = position;
        // 当textPosition为1时(上面有textPosition--), 加载文本
        if (position < 1 && flushFinish && crawlPosition != 1) {
            flushCount = 0;
            if (chapter > BufferChapterCount - 2) {
                crawlChapter = chapter;
                crawlPosition = crawlChapter - BufferChapterCount + 2;
            } else {
                crawlPosition = 1;
            }
            Message msg = new Message();
            msg.what = 0x300;
            handler.sendMessage(msg);
            flushFinish = false;
        }
        boolean ret = chapter == 1 && isNowOverflow;
        if (!isPageEnable && !ret) {
            setLoad();
            showLoad = true;
        }
        return ret;
    }

    @Override
    public boolean onNextPage(int position, boolean isPositionChange, boolean isPageEnable, boolean isNowOverflow) {
        // 如果翻页了把这里的章节进行增加
        if (isPositionChange) {
            chapter++;
        }
        // 同步文本位置, 让下面的判断更为准确, 这里不能放到上面的条件里面,
        textPosition = position;
        // 当观看的位置超过了限制时, 且刷新已经完成时, 进行章节缓冲的更换
        if (textPosition > BufferChapterCount - 3 && flushFinish) {
            flushCount = 0;
            crawlChapter = chapter;
            crawlPosition = crawlChapter - 2;
            Message message = new Message();
            message.what = 0x300;
            handler.sendMessage(message);
            flushFinish = false;
        }
        boolean ret = chapter == lastChapter && isNowOverflow;
        // 当不是最后一章但是看到最后一页时, 显示加载中对话框
        if (!isPageEnable && !ret) {
            setLoad();
            // 设置加载完马上显示
            showLoad = true;
        }
        if (ret) SQLTools.setFinishTime(String.valueOf(novelInfo.getId()), sqLiteNovel, this);
        return ret;
    }

    @Override
    public void onShowAction() {
        if (showActionBar) {
            popup.setVisibility(View.VISIBLE);
            showActionBar = false;
        } else {
            popup.setVisibility(View.GONE);
            showActionBar = true;
        }
    }

    class NewTableTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            SQLiteDatabase database = sqLiteNovel.getReadableDatabase();
            database.beginTransaction();
            for (NovelTextItem textItem : list) {
                database.execSQL("insert into " + table +
                        " (url, chapter) values (?, ?)", new String[]{textItem.getUrl(), textItem.getChapter()});
            }
            database.setTransactionSuccessful();
            database.endTransaction();
            sqLiteNovel.freeStatus = true;
            return null;
        }
    }
}
