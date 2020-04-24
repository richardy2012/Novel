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

public class Sourcembtxt extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourcembtxt(Activity activity, Handler handler) {
        super(activity, handler);
        DOMAIN = "https://www.mbtxt.cc/";
        CHARSET = "GBK";
        THREAD_COUNT = MIDDLE_THREAD_COUNT;
    }

    @Override
    List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();
        Document document = crawlerPOST(DOMAIN + "modules/article/search.php", "searchkey=" + s);
        if (document.select("dl.chapterlist").size() > 0) {
            MapThread mapThread = new MapThread(document, null, new NovelInfo());
            mapThread.run(1);
        } else {
            Elements elements = document.select("#fengtui > div.bookbox");
            elements = orderBy(elements, "div > div.bookinfo > h4 > a", s);
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
            for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
                NovelInfo novelInfo = new NovelInfo();
                String detailUrl = DOMAIN + elements.get(i).select("div > div.bookinfo > h4 > a").attr("href").substring(1);
                fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).run());
            }
            checkTimeOut(fixedThreadPool, SEARCH_TIMEOUT);
        }
        return list;
    }

    @Override
    List<NovelTextItem> run_list(String URL) {
        List<NovelTextItem> list = new ArrayList<>();
        Document document = crawlerGET(URL);
        Elements elements = document.select("#list-chapterAll > dd");
        for (Element element : elements) {
            NovelTextItem textItem = new NovelTextItem();
            textItem.setChapter(element.select("a").text());
            textItem.setUrl(URL + element.select("a").attr("href"));
            list.add(textItem);
        }
        return list;
    }

    private NovelText run_text(String URL, int times) {

        Document document = crawlerGET(URL);

        if (document == null && times < 4) {
            return run_text(URL, ++times);
        }

        document.select("body > div.container > div.content > div.book.read > div.readcontent > div").remove();
        document.select("body > div.container > div.content > div.book.read > div.readcontent > p").remove();
        document.select("body > div.container > div.content > div.book.read > h1 > small").remove();
        String text = withBr(document, "body > div.container > div.content > div.book.read > div.readcontent", " ", "")
                .replace(BR_REPLACEMENT, "").replace("\n\n", "\n");

        if (document.select("#linkNext").text().equals("下一页")) {
            text += run_text(document.select("#linkNext").attr("href"), 1).getText();
        }

        NovelText novelText = new NovelText();
        novelText.setChapter(document.select("body > div.container > div.content > div.book.read > h1").text());
        novelText.setText(text.replace("&n-->>bsp;", ""));
        return novelText;
    }

    @Override
    NovelText run_text(String URL) {
        return run_text(URL, 1);
    }

    @Override
    public List<NovelInfo> run_rank(int type) {
        list = new ArrayList<>();
        for (int i = 0; i < 8; i++) list.add(null);
        Document document = null;
        switch (type) {
            case BaseCrawler.DAY_RANK:
                document = crawlerGET(DOMAIN + "allvote.html");
                break;
            case BaseCrawler.MONTH_RANK:
                document = crawlerGET(DOMAIN + "monthvote.html");
                break;
            case BaseCrawler.TOTAL_RANK:
                document = crawlerGET(DOMAIN + "weekvisit.html");
                break;
        }
        Elements elements = document.select("#fengtui > div.bookbox");
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < Math.min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = elements.get(i).select("div > div.bookinfo > h4 > a").attr("href");
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        Elements elements = document.select("#gengxin > ul > li");
        for (Element element : elements) {
            NovelRemind novelRemind = new NovelRemind();
            novelRemind.setChapter(element.select("span.s3 > a").text());
            novelRemind.setName(element.select("span.s2 > a").text());
            list.add(novelRemind);
        }
        return list;
    }

    class MapThread extends BaseThread {

        MapThread(NovelInfo novelInfo, String imgUrl, String detailUrl) {
            super(novelInfo, imgUrl, detailUrl);
        }

        MapThread(Document doc, String imgUrl, NovelInfo novelInfo) {
            super(doc, imgUrl, novelInfo);
        }

        private void run(int times) {
            if (doc == null && detailUrl != null) {
                doc = crawlerGET(detailUrl);
            }

            if (doc == null && detailUrl != null && times < 4) {
                run(++times);
                return;
            }

            try {
                Element title = doc.select("body > div.container > div.content > div:nth-child(2) > div.bookinfo").get(0);

                novelInfo.setName(title.select("h1").text());
                novelInfo.setAuthor(title.select("p.booktag > a").text());
                novelInfo.setComplete(title.select("p.booktag > span.red").text().contains("连载") ? 0 : 1);
                novelInfo.setIntroduce(title.select("p.bookintro").text());
                novelInfo.setUrl(doc.baseUri());
                novelInfo.setSource(Sourcembtxt.this.getClass().getName());
                novelInfo.setChapter(title.select("p:nth-child(4) > a").get(0).text());
                imgUrl = doc.select("body > div.container > div.content > div:nth-child(2) > div.bookcover.hidden-xs > img").attr("src");

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
