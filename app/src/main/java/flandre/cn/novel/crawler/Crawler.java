package flandre.cn.novel.crawler;

import android.os.AsyncTask;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.info.NovelRemind;
import flandre.cn.novel.info.NovelText;
import flandre.cn.novel.info.NovelTextItem;

import java.util.List;

public interface Crawler {
    public int DAY_RANK = 0;  // 周榜
    public int MONTH_RANK = 1;  // 月榜
    public int TOTAL_RANK = 2;  // 总榜

    static int SEARCH_COUNT = 8;  // 搜索时查找的Item大小
    static int RANK_COUNT = 8;  // 拿排行榜时查找的Item大小

    static int SEARCH_TIMEOUT = 30;  // 搜索时的Timeout时间
    static int RANK_TIMEOUT = 60;  // 排行榜的Timeout时间

    public int MAX_THREAD_COUNT = 4;  // 允许开的最大线程数
    public int MIDDLE_THREAD_COUNT = 2;  // 允许一般的线程数量
    public int MIN_THREAD_COUNT = 1;  // 允许开的最小线程数

    public int TIMEOUT = 10 * 1000;

    /**
     * 搜索小说
     *
     * @param s 用户输入的搜索词
     */
    public List<NovelInfo> search(String s);

    /**
     * 获取章节信息
     */
    public void list(final String URL);

    /**
     * 获取文本
     */
    public AsyncTask<Void, Void, Void> text(String URL, int i, String table);

    /**
     * 更新小说
     *
     * @param id    数据库novel的id
     * @param newId 最新章节的id
     * @return Thread, 用于join
     */
    public Runnable update(final String URL, int id, int newId, UpdateFinish updateFinish);

    /**
     * 缓存章节
     *
     * @param URL 章节文本的URL
     * @param id  章节id
     * @return 爬取章节文本的线程对象
     */
    public Runnable download(final String URL, int id, String table, DownloadFinish downloadFinish);

    /**
     * 获取小说的排行版
     */
    public List<NovelInfo> rank(int type);

    /**
     * 获取搜索界面的提示
     */
    public List<NovelRemind> remind();

    public interface DownloadFinish {
        /**
         * 当一章下载好时回调(多线程中调用)
         *
         * @param novelText 文本数据
         * @param table     表名
         * @param id        下载文本所属行的id
         */
        public void onDownloadFinish(NovelText novelText, String table, int id);
    }

    public interface UpdateFinish {
        /**
         * 更新好一本小说时回调(多线程中调用)
         *
         * @param id   小说的id
         * @param list 更新的章节数据(里面没有文本)
         */
        public void onUpdateFinish(int id, List<NovelTextItem> list);
    }
}
