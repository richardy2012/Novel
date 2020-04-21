package flandre.cn.novel.crawler;

import android.app.Activity;
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

import static java.lang.Math.min;

public class Sourcewangshugu extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourcewangshugu(Activity activity, Handler handler) {
        super(activity, handler);
        this.DOMAIN = "http://www.wangshugu.com/";
        this.CHARSET = "UTF8";
        this.THREAD_COUNT = MIDDLE_THREAD_COUNT;
    }

    @Override
    List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();
        Document document = crawlerPOST(DOMAIN + "search/", "searchkey=" + s);
        document.select("#content > dd > table > tbody > tr:nth-child(1)").remove();
        Elements elements = document.select("#content > dd > table > tbody > tr");
        elements = orderBy(elements, "td:nth-child(1) > a", s);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            Element element = elements.get(i);
            String url = element.select("td:nth-child(1) > a").attr("href");
            url = url.substring(0, url.length() - 1);
            url = DOMAIN + "books/book" + url.substring(url.lastIndexOf("/") + 1) + ".html";
            fixedThreadPool.execute(new MapThread(novelInfo, null, url).run());
        }
        checkTimeOut(fixedThreadPool, SEARCH_TIMEOUT);
        return list;
    }

    @Override
    List<NovelTextItem> run_list(String URL) {
        List<NovelTextItem> list = new ArrayList<>();
        Document document = crawlerGET(URL);
        Elements elements = document.select("#at > tbody > tr");
        for (Element element : elements) {
            Elements items = element.select("td > a");
            for (Element item : items) {
                NovelTextItem textItem = new NovelTextItem();
                textItem.setChapter(item.text());
                textItem.setUrl(URL + item.attr("href"));
                list.add(textItem);
            }
        }
        return list;
    }

    @Override
    NovelText run_text(String URL) {
        return run_text(URL, 1);
    }

    private NovelText run_text(String URL, int times) {
        Document document = crawlerGET(URL);
        if (document == null && times < 4) {
            return run_text(URL, ++times);
        }
        NovelText novelText = new NovelText();
        novelText.setChapter(document.select("#amain > dl > dd:nth-child(2) > h1").get(0).text());
        novelText.setText(withBr(document, "#contents", " ", "")
                .replace(BR_REPLACEMENT, "").replace("\n\n", "\n"));
        return novelText;
    }

    @Override
    public List<NovelInfo> run_rank(int type) {
        list = new ArrayList<>();
        for (int i = 0; i < 8; i++) list.add(null);
        Document document = null;
        switch (type) {
            case BaseCrawler.DAY_RANK:
                document = crawlerGET(DOMAIN + "books/toplist/weekvote-1.html");
                break;
            case BaseCrawler.MONTH_RANK:
                document = crawlerGET(DOMAIN + "books/toplist/monthvote-1.html");
                break;
            case BaseCrawler.TOTAL_RANK:
                document = crawlerGET(DOMAIN + "books/toplist/allvote-1.html");
                break;
        }
        document.select("#content > dd > table > tbody > tr:nth-child(1)").remove();
        Elements elements = document.select("#content > dd > table > tbody > tr");
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < Math.min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = elements.get(i).select("td:nth-child(1) > a").attr("href");
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        document.select("#centeri > div > div.blockcontent > ul > li.more").remove();
        Elements elements = document.select("#centeri > div > div.blockcontent > ul > li");
        for (Element element : elements) {
            NovelRemind novelRemind = new NovelRemind();
            novelRemind.setName(element.select("p.ul1 > a.poptext").text());
            novelRemind.setChapter(element.select("p.ul2 > a").text());
            list.add(novelRemind);
        }
        return list;
    }

    class MapThread extends BaseCrawler.BaseThread {

        MapThread(NovelInfo novelInfo, String imgUrl, String detailUrl) {
            super(novelInfo, imgUrl, detailUrl);
        }

        private void run(int times) {
            doc = crawlerGET(detailUrl);
            if (doc == null && detailUrl != null && times < 4) {
                run(++times);
                return;
            }

            try {
                novelInfo.setName(doc.select("#content > dd:nth-child(2) > h1").get(0).text().split(" ")[0]);
                novelInfo.setAuthor(doc.select("#at > tbody > tr:nth-child(1) > td:nth-child(4)").get(0).text().substring(1));
                novelInfo.setComplete(doc.select("#at > tbody > tr:nth-child(1) > td:nth-child(6)").get(0).text().contains("连载") ? 0 : 1);
                novelInfo.setIntroduce(doc.select("#content > dd:nth-child(7) > p:nth-child(3)").get(0).text());
                novelInfo.setUrl(doc.select("#content > dd:nth-child(3) > div:nth-child(2) > p.btnlinks > a.read").attr("href"));
                novelInfo.setSource(Sourcewangshugu.this.getClass().getName());
                novelInfo.setChapter(doc.select("#content > dd:nth-child(7) > p:nth-child(7) > a").get(0).text());
                if (imgUrl == null) {
                    imgUrl = doc.select("#content > dd:nth-child(3) > div:nth-child(1) > a > img").attr("src");
                }
                getImage();
                if (rank != -1) list.set(rank, novelInfo);  // 因为是排行榜, 所以要确保位置不变
                else list.add(novelInfo);
            } catch (NullPointerException e) {
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
