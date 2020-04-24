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

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.min;

public class Sourcex23qb extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourcex23qb(Activity activity, Handler handler) {
        super(activity, handler);
        this.CHARSET = "GBK";
        this.DOMAIN = "https://www.x23qb.com/";
        this.THREAD_COUNT = MIN_THREAD_COUNT;
    }

    @Override
    List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();
        Document document = crawlerPOST(DOMAIN + "search.php", "searchkey=" + s);
        document.baseUri();

        if (document.select("#newlist").size() > 0) {
            MapThread mapThread = new MapThread(document, document.select("#bookimg > img").get(0).attr("src"), new NovelInfo());
            mapThread.run(1);
        } else {
            Elements elements = document.select("#sitebox > dl");
            elements = orderBy(elements, "h3 > a", s);
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
            for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
                NovelInfo novelInfo = new NovelInfo();
                Element element = elements.get(i).select("dt > a").get(0);
                String imageUrl = element.select("img").attr("_src");
                String detailUrl = element.attr("href");
                fixedThreadPool.execute(new MapThread(novelInfo, imageUrl, detailUrl).run());
            }
            checkTimeOut(fixedThreadPool, SEARCH_TIMEOUT);
        }
        return list;
    }

    @Override
    List<NovelTextItem> run_list(String URL) {
        List<NovelTextItem> list = new ArrayList<>();
        Document document = crawlerGET(URL);
        Elements elements = document.select("#chapterList > li > a");
        for (Element element : elements) {
            NovelTextItem textItem = new NovelTextItem();
            textItem.setUrl(DOMAIN + element.attr("href").substring(1));
            textItem.setChapter(element.text());
            list.add(textItem);
        }
        return list;
    }

    private NovelText run_text(String URL, int times) {
        try {
            Thread.sleep((long) (Math.random() * 2000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Document document = crawlerGET(URL);
        if (document == null && times < 4) {
            return run_text(URL, ++times);
        }

        Element next = document.select(".mlfy_page > a").get(4);
        Elements isNext = document.select("#TextContent > center");

        NovelText novelText = new NovelText();
        novelText.setChapter(document.select("#mlfy_main_text > h1").get(0).text());

        document.select("#mlfy_main_text > h1").remove();
        document.select("#TextContent > div").remove();
        document.select("#TextContent > dt").remove();
        document.select("#TextContent > script").remove();
        document.select("#TextContent > center").remove();
        document.select("#TextContent > a").remove();
        String text = document.select("#TextContent").text().replace("     ", "\n     ");


        if (isNext.size() != 0) {
//            text = text.concat(run_text(DOMAIN + next.attr("href").substring(1)).get("text")).replace(" ＞＞\n\n\n\n", "");
            text = text.concat(run_text(DOMAIN + next.attr("href").substring(1)).getText()).replace(" ＞＞", "");
        }

        novelText.setText(text);

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
        Document document = crawlerGET(DOMAIN + "paihang.html");
        Elements elements = null;
        switch (type) {
            case BaseCrawler.DAY_RANK:
                elements = document.select("#tabData_9 > div > ul > li");
                break;
            case BaseCrawler.MONTH_RANK:
                elements = document.select("#tabData_10 > div > ul > li");
                break;
            case BaseCrawler.TOTAL_RANK:
                elements = document.select("#main > div:nth-child(11) > div.topbook > ul > li");
                break;
        }
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = elements.get(i).select("a").get(0).attr("href");
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> strings = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        Elements elements = document.select("#index_last > div.list_center.w_770.left > div.update_list > ul > li");
        for (Element element : elements) {
            NovelRemind novelRemind = new NovelRemind();
            novelRemind.setName(element.select("span.r_spanone > a").get(0).text());
            novelRemind.setChapter(element.select("span.r_spantwo > a").get(0).text());
            strings.add(novelRemind);
        }
        return strings;
    }

    class MapThread extends BaseThread {

        MapThread(NovelInfo map, String imgUrl, String detailUrl) {
            super(map, imgUrl, detailUrl);
        }

        MapThread(Document doc, String imgUrl, NovelInfo map) {
            super(doc, imgUrl, map);
        }

        private void run(int times) {
            try {
                Thread.sleep((long) (Math.random() * 2000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (doc == null && detailUrl != null) {
                doc = crawlerGET(detailUrl);
            }

            if (doc == null && detailUrl != null && times < 4) {
                run(++times);
                return;
            }

            try {
                Element data = doc.select("#bookinfo .bookright").get(0);
                Element title = data.select(".d_title").get(0);

                novelInfo.setName(title.select("h1").get(0).text());
                novelInfo.setAuthor(title.select(".p_author a").get(0).text());
                novelInfo.setComplete(data.select("#count ul li:nth-child(3) span").get(0).text().contains("连载") ? 0 : 1);
                novelInfo.setIntroduce(data.select("#bookintro > p:nth-child(1)").get(0).text());
                novelInfo.setUrl(doc.baseUri());
                novelInfo.setSource(Sourcex23qb.this.getClass().getName());
                novelInfo.setChapter(doc.select(".chaw > li:nth-child(1)").get(0).text());
                if (imgUrl == null) {
                    imgUrl = doc.select("#bookimg > img").attr("src");
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
