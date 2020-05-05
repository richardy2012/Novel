package flandre.cn.novel.crawler;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelTools;
import flandre.cn.novel.info.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 爬虫的基类
 * 创建源时继承该类
 * 2019.12.8
 */
public abstract class BaseCrawler {
    public static final int DAY_RANK = 0;  // 周榜
    public static final int MONTH_RANK = 1;  // 月榜
    public static final int TOTAL_RANK = 2;  // 总榜

    static final int SEARCH_COUNT = 8;  // 搜索时查找的Item大小
    static final int RANK_COUNT = 8;  // 拿排行榜时查找的Item大小

    static final int SEARCH_TIMEOUT = 30;  // 搜索时的Timeout时间
    static final int RANK_TIMEOUT = 60;  // 排行榜的Timeout时间

    public static final int MAX_THREAD_COUNT = 4;  // 允许开的最大线程数
    public static final int MIDDLE_THREAD_COUNT = 2;  // 允许一般的线程数量
    public static final int MIN_THREAD_COUNT = 1;  // 允许开的最小线程数

    public static final int TIMEOUT = 30 * 1000;

    public static final String BR_REPLACEMENT = "0x0a";

    String CHARSET;  // 网页的编码
    String DOMAIN;  // 网页的域名
    public int THREAD_COUNT = MAX_THREAD_COUNT;  // 使用的线程数量

    private WeakReference<Handler> handler;
    private WeakReference<Context> mContext;

    public BaseCrawler(Activity activity, Handler handler) {
        this.mContext = new WeakReference<>((Context) activity);
        this.handler = new WeakReference<>(handler);
    }

    void configureConn(HttpURLConnection connection) throws IOException {
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/65.0.3325.181 Safari/537.36");
//        connection.addRequestProperty("Connection", "keep-alive");
        connection.addRequestProperty("Connection", "close");
        connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.addRequestProperty("Referer", DOMAIN);
        connection.addRequestProperty("Origin:", DOMAIN);
        connection.setInstanceFollowRedirects(false);
        connection.setConnectTimeout(TIMEOUT);
        connection.setReadTimeout(TIMEOUT);
        connection.connect();
    }

    private Document crawlerPOST(String Url, String data, boolean rediect) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(Url);
            byte[] bytes = data.getBytes(CHARSET);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setUseCaches(Boolean.FALSE);
            connection.setDoOutput(Boolean.TRUE);
            connection.setDoInput(Boolean.TRUE);
            connection.addRequestProperty("Content-Length", String.valueOf(bytes.length));
            configureConn(connection);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
            if (connection.getResponseCode() == 301 || connection.getResponseCode() == 302) {
                String location = connection.getHeaderField("Location");
                if (location.startsWith("/")) location = DOMAIN + location.substring(1);
                return crawlerPOST(location, data, true);
            }
            InputStream inputStream = connection.getInputStream();
            Document document = Jsoup.parse(inputStream, CHARSET, DOMAIN);
            document.setBaseUri(Url);
            inputStream.close();
            return document;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    /**
     * 发送POST请求
     *
     * @return Document
     */
    Document crawlerPOST(String Url, String data) {
        return crawlerPOST(Url, data, false);
    }

    private Document crawlerGET(String Url, boolean redirect) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(Url);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(Boolean.FALSE);
            configureConn(connection);
            if (connection.getResponseCode() == 301 || connection.getResponseCode() == 302) {
                String location = connection.getHeaderField("Location");
                return crawlerGET(location, true);
            }
            if (connection.getResponseCode() >= 400) {
                Log.e(String.valueOf(connection.getResponseCode()), Url);
            }
            InputStream inputStream = connection.getInputStream();
            Document document = Jsoup.parse(inputStream, CHARSET, DOMAIN);
            inputStream.close();
            document.setBaseUri(Url);
            return document;
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) connection.disconnect();
        }
        return null;
    }

    /**
     * 发送GET请求
     *
     * @return Document
     */
    Document crawlerGET(String Url) {
        return crawlerGET(Url, false);
    }

    private HttpURLConnection crawlerImage(String Url) {
        try {
            URL imageURL = new URL(Url);
            HttpURLConnection conn = (HttpURLConnection) imageURL.openConnection();
            conn.setRequestMethod("GET");
            conn.setUseCaches(Boolean.FALSE);
            conn.setDoInput(true);
            configureConn(conn);
            return conn;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 搜索小说
     *
     * @param s 用户输入的搜索词
     */
    public List<NovelInfo> search(final String s) {
        try {
            return run_search(setUnicode(s));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * name: 书名
     * author: 作者
     * status: 状态
     * url: 目录网站
     * image: bitmap对象
     * imageUrl: 图片url
     * source: 来源
     * chapter: 最新章节
     * introduce: 介绍
     */
    abstract List<NovelInfo> run_search(final String s);

    /**
     * 给小说搜索结果排序
     *
     * @param elements 小说列表
     * @param selector 每个列表获取小说名的解析式
     * @param name     搜索的小说名
     */
    Elements orderBy(Elements elements, String selector, String name) {
        return orderBy(elements, selector, null, name);
    }

    Elements orderBy(Elements elements, String selector, Pattern pattern, String name) {
        try {
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                String text = element.select(selector).text();
                if (pattern != null) {
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find() && matcher.groupCount() > 0) text = matcher.group(1);
                }
                if (setUnicode(text).equals(name)) {
                    elements.add(0, element.clone());
                    elements.remove(i + 1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return elements;
    }

    /**
     * 把Br转换成空格
     * @return 转换后的文本
     */
    String withBr(Elements element, String select){
        return withBr(element, select, "", "");
    }

    String withBr(Elements elements, String select, String extra, String rep){
        elements.select(select + " br").append(BR_REPLACEMENT);
        elements.select(select + " p").append(BR_REPLACEMENT);
        return elements.select(select).text().replace(BR_REPLACEMENT + extra, "\n" + rep);
    }

    String withBr(Element element, String select){
        return withBr(element, select, "", "");
    }

    String withBr(Element element, String select, String extra, String rep){
        element.select(select + " br").append(BR_REPLACEMENT);
        element.select(select + " p").append(BR_REPLACEMENT);
        return element.select(select).text().replace(BR_REPLACEMENT + extra, "\n" + rep);
    }

    /**
     * 获取章节信息
     */
    public void list(final String URL) {
        new ListTask(this).execute(URL);
    }

    static class ListTask extends AsyncTask<String, Void, Void> {
        private BaseCrawler mCrawler;

        ListTask(BaseCrawler mCrawler) {
            this.mCrawler = mCrawler;
        }

        @Override
        protected Void doInBackground(String... URL) {
            Message message = new Message();
            try {
                message.obj = mCrawler.run_list(URL[0]);
            } catch (Exception e) {
                e.printStackTrace();
                message.obj = null;
            }
            message.what = 0x300;
            mCrawler.handler.get().sendMessage(message);
            return null;
        }
    }

    /**
     * url: 文章url
     * chapter: 章节名
     */
    abstract List<NovelTextItem> run_list(final String URL);

    /**
     * 获取文本
     */
    public AsyncTask<Void, Void, Void> text(String URL, int i, String table) {
        return new TextTask(this, URL, i, table);
    }

    /**
     * 开个新线程爬文本
     */
    static class TextTask extends AsyncTask<Void, Void, Void> {
        private BaseCrawler mCrawler;
        private String URL;
        private int i;
        private String table;

        TextTask(BaseCrawler mCrawler, String URL, int i, String table) {
            this.mCrawler = mCrawler;
            this.URL = URL;
            this.i = i;
            this.table = table;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Message message = new Message();
            WrapperNovelText wrapperNovelText = new WrapperNovelText();
            try {
                wrapperNovelText.setNovelText(mCrawler.run_text(URL));
            } catch (Exception e) {
                NovelText novelText = new NovelText();
                novelText.setText("网络出现异常");
                novelText.setChapter("网络出现异常");
                wrapperNovelText.setNovelText(novelText);
                message.arg2 = 1;
                e.printStackTrace();
            }
            wrapperNovelText.setTable(table);
            message.what = 0x302;
            message.obj = wrapperNovelText;
            message.arg1 = i;
            mCrawler.handler.get().sendMessage(message);
            return null;
        }
    }

    /**
     * text: 文本
     * chapter: 章节名
     */
    abstract NovelText run_text(String URL);

    /**
     * 更新小说
     *
     * @param id    数据库novel的id
     * @param newId 最新章节的id
     * @return Thread, 用于join
     */
    public Runnable update(final String URL, final int id, final int newId, final UpdateFinish updateFinish) {
        return new Runnable() {
            @Override
            public void run() {
                List<NovelTextItem> list = run_update(URL, newId);
                updateFinish.onUpdateFinish(id, list);
            }
        };
    }

    /**
     * 更新小说
     *
     * @param URL   小说章节目录的URL
     * @param newId 当前最新章节的id
     * @return 新的章节列表
     */
    private List<NovelTextItem> run_update(String URL, int newId) {
        try {

            List<NovelTextItem> list = run_list(URL);
            if (newId > 0) {
                list.subList(0, newId).clear();
            }

            return list;
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
        return null;
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

    /**
     * 缓存章节
     *
     * @param URL 章节文本的URL
     * @param id  章节id
     * @return 爬取章节文本的线程对象
     */
    public Runnable download(final String URL, final int id, final String table, final DownloadFinish downloadFinish) {
        return new Runnable() {
            @Override
            public void run() {
                NovelText obj = run_download(URL);
                downloadFinish.onDownloadFinish(obj, table, id);
            }
        };
    }

    private NovelText run_download(String URL) {
        try {
            return run_text(URL);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

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

    /**
     * 获取小说的排行版
     */
    public List<NovelInfo> rank(int type) {
        return run_rank(type);
    }

    /**
     * @param type 类型, 有{@link BaseCrawler#DAY_RANK} 这个是小说的周榜, {@link BaseCrawler#MONTH_RANK}
     *             这个是小说的月榜, {@link BaseCrawler#TOTAL_RANK} 这个是小说的总榜
     * @return 返回一个键值对, 里面的数据和Search的一样
     */
    public abstract List<NovelInfo> run_rank(int type);

    /**
     * 获取搜索界面的提示
     */
    public List<NovelRemind> remind() {
        return run_remind();
    }

    /**
     * @return 一个键值对
     * name -> 小说的名字
     * chapter -> 小说的最新章节或作者的名字
     */
    public abstract List<NovelRemind> run_remind();

    /**
     * 为线程池设置超时时间
     *
     * @param fixedThreadPool 线程池对象
     * @param timeout         超时的时间
     */
    void checkTimeOut(ExecutorService fixedThreadPool, int timeout) {
        try {
            fixedThreadPool.shutdown();
            if (!fixedThreadPool.awaitTermination(timeout, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 转码, 把输入的字符串转换成 {@link BaseCrawler#CHARSET} 这个编码
     *
     * @param s 要转码的字符串
     */
    private String setUnicode(String s) {
        try {
            s = URLEncoder.encode(s, CHARSET);
            return s;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return s;
    }

    /**
     * 获取图片
     *
     * @param imageUrl 要获取图片的HTTP的URL
     * @return 没有错误返回URL的图片, 出错返回本地的一张图片
     */
    private Bitmap getImage(String imageUrl) {
        HttpURLConnection conn = crawlerImage(imageUrl);
        InputStream inputStream = null;
        try {
            inputStream = conn.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap bitmap = null;
        try {
            if (inputStream != null) {
                bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
            }
            if (bitmap == null) {
                bitmap = BitmapFactory.decodeResource(mContext.get().getResources(), R.drawable.not_found);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn.disconnect();
        return bitmap;
    }

    /**
     * 多线程类
     * 如果需要使用多线程,继承该类
     */
    abstract class BaseThread {
        NovelInfo novelInfo;
        String imgUrl;
        String detailUrl;
        Document doc;
        int rank = -1;

        BaseThread(NovelInfo novelInfo, String imgUrl, String detailUrl) {
            this.novelInfo = novelInfo;
            this.imgUrl = imgUrl;
            this.detailUrl = detailUrl;
        }

        BaseThread(Document doc, String imgUrl, NovelInfo novelInfo) {
            this.doc = doc;
            this.imgUrl = imgUrl;
            this.novelInfo = novelInfo;
        }

        public BaseThread setRank(int rank) {
            this.rank = rank;
            return this;
        }

        void getImage() {
            String image = "FL" + NovelTools.md5(imgUrl) + ".jpg";
            File file = new File(mContext.get().getExternalFilesDir(null), "img");
            File path = new File(file, image);
            novelInfo.setImagePath(path.getAbsolutePath());
            novelInfo.setBitmap(BaseCrawler.this.getImage(imgUrl));
        }

        public abstract Runnable run();
    }
}
