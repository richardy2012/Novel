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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.StrictMath.min;

public class Sourceymoxuan extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourceymoxuan(Activity activity, Handler handler) {
        super(activity, handler);
        CHARSET = "UTF-8";
        DOMAIN = "https://www.ymoxuan.com/";
        THREAD_COUNT = MIN_THREAD_COUNT;
    }

    @Override
    List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();

        Document document = crawlerGET(DOMAIN + "search.htm?keyword=" + s);
        Elements elements = document.select("section.lastest > ul > li");
        elements.remove(0);
        elements.remove(elements.size() - 1);
        elements = orderBy(elements, "span.n2 > a", s);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = "https:" + elements.get(i).select("span.n2 > a").get(0).attr("href");
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).run());
        }
        checkTimeOut(fixedThreadPool, SEARCH_TIMEOUT);
        return list;
    }

    @Override
    List<NovelTextItem> run_list(String URL) {
        List<NovelTextItem> list = new ArrayList<>();
        Document document = crawlerGET(URL);

        Element data = document.select("body > section > article").get(0);

        Element last = data.select(".col1.volumn").get(0);
        Element next;

        do {
            next = last.nextElementSibling();
            last.remove();
            last = next;
        } while (!next.attr("class").equals("col1 volumn"));

        data.select(".col1.volumn").remove();

        Elements info = data.select("ul > li > a");

        for (Element ele : info) {
            NovelTextItem textItem = new NovelTextItem();

            textItem.setChapter(ele.text());
            textItem.setUrl("https:" + ele.attr("href"));

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

        Element text = document.select("#content").get(0);
        text.select("script").remove();

        String data = text.html().replace("<br>", "\n").replace("\n\n", "\n");

        NovelText novelText = new NovelText();
        novelText.setText(data);
        novelText.setChapter(document.select("#a3 > header > h1").get(0).text());

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
        Document document = crawlerGET(DOMAIN);
        Elements elements = null;
        switch (type) {
            case BaseCrawler.DAY_RANK:
                elements = document.select("body > section.container > div.left > section > ul > li > span.n > a");
                break;
            case BaseCrawler.MONTH_RANK:
                elements = document.select("body > section.container > div.left > article.author.clearfix > div > dl > dt > a");
                break;
            case BaseCrawler.TOTAL_RANK:
                elements = document.select("body > section.container > div.right > section:nth-child(1) > div > ul > li > a");
                break;
        }
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < Math.min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = "https:" + elements.get(i).attr("href");
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        Elements elements = document.select("body > section.container > div.right > section:nth-child(1) > div > ul > li");
        for (Element element : elements) {
            NovelRemind novelRemind = new NovelRemind();
            novelRemind.setName(element.select("a").get(1).text());
            novelRemind.setChapter(element.select("a").get(0).text());
            list.add(novelRemind);
        }
        return list;
    }

    class MapThread extends BaseThread {
        MapThread(NovelInfo map, String imgUrl, String detailUrl) {
            super(map, imgUrl, detailUrl);
        }

        private void run(int times) {
            doc = crawlerGET(detailUrl);

            if (doc == null && times < 4) {
                run(++times);
                return;
            }

            try {
                Elements elements = doc.select("body > section > div.left > article.info");
                imgUrl = elements.select("div.cover > img").attr("src");

                novelInfo.setName(elements.select("header > h1").get(0).text());
                novelInfo.setAuthor(elements.select("p.detail.pt20 > i:nth-child(1) > a").text());
                novelInfo.setComplete(elements.select("p.detail.pt20 > i:nth-child(3)").text().contains("完本") ? 1 : 0);
                novelInfo.setChapter(elements.select("p").get(2).select("i > a").select("i > a").text());
                novelInfo.setIntroduce(elements.select("p.desc").text());
                novelInfo.setSource(Sourceymoxuan.class.getName());
                novelInfo.setUrl("https:" + elements.select("footer > a").get(0).attr("href"));

                getImage();

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
