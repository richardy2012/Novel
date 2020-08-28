package flandre.cn.novel.crawler;

import android.content.Context;
import android.os.Handler;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.info.NovelRemind;
import flandre.cn.novel.info.NovelText;
import flandre.cn.novel.info.NovelTextItem;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.StrictMath.min;

public class Sourceaixiatxt extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourceaixiatxt(Context context, Handler handler) {
        super(context, handler);
        DOMAIN = "http://www.ixiatxt.com/";
        CHARSET = "UTF8";
        THREAD_COUNT = MIN_THREAD_COUNT;
    }

    @Override
    public List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN + "search.php?s=&searchkey=" + s);
        Elements elements = document.select("body > div:nth-child(4) > div.list > div > ul > li");
        Pattern pattern = Pattern.compile("《(.*?)》");
        elements = orderBy(elements, "a", pattern, s);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = DOMAIN + elements.get(i).select("a").get(0).attr("href").substring(1);
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).run());
        }
        checkTimeOut(fixedThreadPool, SEARCH_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelTextItem> run_list(String URL) {
        List<NovelTextItem> list = new ArrayList<>();
        Document document = crawlerGET(URL);
        Element element = document.select("#info").get(2);
        Elements items = element.select("div > ul > li");
        for (Element item : items) {
            NovelTextItem novelTextItem = new NovelTextItem();
            novelTextItem.setChapter(item.select("a").text());
            novelTextItem.setUrl(URL + item.select("a").attr("href"));
            list.add(novelTextItem);
        }
        return list;
    }

    @Override
    public NovelText run_text(String URL) {
        return run_text(URL, 1);
    }

    private NovelText run_text(String URL, int times) {

        Document document = crawlerGET(URL);

        if (document == null && times < 4) {
            return run_text(URL, ++times);
        }
        document.select("#center_tip").remove();
        NovelText novelText = new NovelText();
        novelText.setText(withBr(document, "#content1", " ", "").replace(BR_REPLACEMENT, ""));
        novelText.setChapter(document.select("#info > div > h1").text());
        return novelText;
    }

    @Override
    public List<NovelInfo> run_rank(int type) {
        list = new ArrayList<>();
        for (int i = 0; i < 8; i++) list.add(null);
        Document document = crawlerGET(DOMAIN);
        Elements elements = null;
        switch (type) {
            case DAY_RANK:
                elements = document.select("body > div:nth-child(3) > div.mpLeft > div:nth-child(2) > div > ul:nth-child(2) > li");
                break;
            case MONTH_RANK:
                elements = document.select("body > div:nth-child(3) > div.mpLeft > div:nth-child(2) > div > ul:nth-child(3) > li");
                break;
            case TOTAL_RANK:
                elements = document.select("body > div:nth-child(3) > div.mpLeft > div:nth-child(2) > div > ul:nth-child(4) > li");
                break;
        }
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < Math.min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = DOMAIN + elements.get(i).select("a").attr("href").substring(1);
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        document.select("body > div:nth-child(3) > div.mpRight > div.fic_type_box > div.fic_type_tabcont > div > ul > li:nth-child(1)").remove();
        Elements elements = document.select("body > div:nth-child(3) > div.mpRight > div.fic_type_box > div.fic_type_tabcont > div > ul > li");
        Pattern pattern = Pattern.compile("《(.*?)》");
        for (Element element : elements) {
            NovelRemind novelRemind = new NovelRemind();
            Matcher matcher = pattern.matcher(element.select("a.name").text());
            if (matcher.find() && matcher.groupCount() > 0)
                novelRemind.setName(matcher.group(1));
            else continue;
            novelRemind.setChapter(element.select("a:nth-child(4)").text());
            list.add(novelRemind);
        }
        return list;
    }

    @Override
    public BaseThread getNovelInfo(String addr, NovelInfo novelInfo) {
        Document document = crawlerGET(addr);
        addr = DOMAIN + document.select("#info > dd > a:nth-child(3)").attr("href").substring(1);
        return new MapThread(novelInfo, null, addr);
    }

    public class MapThread extends BaseThread {

        MapThread(NovelInfo novelInfo, String imgUrl, String detailUrl) {
            super(novelInfo, imgUrl, detailUrl);
        }

        private void run(int times) {
            doc = crawlerGET(detailUrl);

            if (doc == null && times < 4) {
                run(++times);
                return;
            }

            try {
                Elements elements = doc.select("body > div:nth-child(4) > div.show > div:nth-child(1) > div");
                imgUrl = DOMAIN + elements.select("div.detail_pic > img").attr("src").substring(1);
                Matcher matcher = Pattern.compile("《(.*?)》").matcher(elements.select("div.detail_info > div > h1").text());
                if (matcher.find() && matcher.groupCount() > 0)
                    novelInfo.setName(matcher.group(1));
                else return;
                novelInfo.setAuthor(elements.select("div.detail_info > div > ul > li:nth-child(6)").text().substring(5));
                novelInfo.setComplete(elements.select("div.detail_info > div > ul > li:nth-child(5)").text().contains("完本") ? 1 : 0);
                novelInfo.setChapter(elements.select("div.detail_info > div > ul > li:nth-child(8) > a").text());
                novelInfo.setIntroduce(doc.select("body > div:nth-child(4) > div.show > div:nth-child(2) > div.showInfo > p").text());
                novelInfo.setSource(Sourceaixiatxt.class.getName());
                novelInfo.setUrl(DOMAIN + doc.select("body > div:nth-child(4) > div.show > div:nth-child(4) " +
                        "> div.showDown > ul > li:nth-child(1) > a").attr("href").substring(1));

                getImage();

                if (list == null) return;
                if (rank != -1) list.set(rank, novelInfo);
                else list.add(novelInfo);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public Runnable run() {
            return new Runnable() {
                @Override
                public void run() {
                    MapThread.this.run(1);
                }
            };
        }
    }
}
