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
import java.util.concurrent.TimeUnit;

import static java.lang.Math.min;

public class Sourcefhxiaoshuo extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourcefhxiaoshuo(Activity activity, Handler handler) {
        super(activity, handler);
        CHARSET = "GBK";
        DOMAIN = "https://www.fhxiaoshuo.org/";
        THREAD_COUNT = MIDDLE_THREAD_COUNT;
    }

    @Override
    List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();
        Document document = crawlerPOST(DOMAIN + "modules/article/search.php", "searchkey=" + s);

        if (document.select("#fmimg").size() != 0) {
            NovelInfo novelInfo = new NovelInfo();
            new MapThread(document, null, novelInfo).run(1);
        } else {
            Elements elements = document.select("table.grid > tbody > tr");
            elements.get(0).remove();
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(MAX_THREAD_COUNT);
            for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
                NovelInfo novelInfo = new NovelInfo();
                String detailUrl = elements.get(i).select("td.odd > a").attr("href");
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
        Elements elements = document.select("#list > dl > dd > a");

        int i;
        for (i = 0; i < elements.size(); i++) {
            if (i % 3 == 0 && i != 0) {
                for (int j = i - 1; j >= i - 3; j--) {
                    NovelTextItem textItem = new NovelTextItem();
                    Element element = elements.get(j);
                    textItem.setChapter(element.text());
                    textItem.setUrl(element.attr("href"));
                    list.add(textItem);
                }
            }
        }
        int leave = i - (i % 3 != 0 ? i % 3 : 3);
        for (int k = i - 1; k >= leave; k--) {
            NovelTextItem textItem = new NovelTextItem();
            Element element = elements.get(k);
            textItem.setUrl(element.attr("href"));
            textItem.setChapter(element.text());
            list.add(textItem);
        }

        return list;
    }

    private NovelText run_text(String URL, int times) {
        Document document = crawlerGET(URL);

        if (document == null && times < 4) {
            return run_text(URL, ++times);
        }

        document.select("#TXT > div").remove();
        document.select("#TXT > font").remove();
        String text = document.select("#TXT").get(0).html().substring(9).replace("<br>",
                "\n").replace("\n\n\n\n", "\n    ");
        NovelText novelText = new NovelText();
        novelText.setText(text);
        novelText.setChapter(document.select(".zhangjieming > h1").get(0).text());

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
                document = crawlerGET(DOMAIN + "weekvisit/1/");
                break;
            case BaseCrawler.MONTH_RANK:
                document = crawlerGET(DOMAIN + "monthvisit/1/");
                break;
            case BaseCrawler.TOTAL_RANK:
                document = crawlerGET(DOMAIN + "allvisit/1/");
                break;
        }
        Elements elements = document.select("#alist > div");
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < Math.min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = elements.get(i).select("div.info > div.title > h2 > a").attr("href");
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        Elements elements = document.select("#newscontent > div.l > ul > li");
        for (Element element:elements){
            NovelRemind novelRemind = new NovelRemind();
            novelRemind.setChapter(element.select("span.s3 > a").text());
            novelRemind.setName(element.select("span.s2 > a").text());
            list.add(novelRemind);
        }
        return list;
    }

    class MapThread extends BaseThread {
        MapThread(NovelInfo map, String imgUrl, String detailUrl) {
            super(map, imgUrl, detailUrl);
        }

        MapThread(Document doc, String imgUrl, NovelInfo map) {
            super(doc, imgUrl, map);
        }

        private void run(int times) {
            if (doc == null) {
                doc = crawlerGET(detailUrl);
            }

            if (doc == null && detailUrl != null && times < 4) {
                run(++times);
                return;
            }

            try {
                imgUrl = doc.select("#fmimg > img").get(0).attr("src");

                novelInfo.setName(doc.select("#info > h1").get(0).text());
                novelInfo.setAuthor(doc.select("#info > p").get(0).text().substring(7));
                novelInfo.setChapter(doc.select("#info > font > p > a").get(0).text());
                novelInfo.setIntroduce(doc.select("#intro > .introtxt").get(0).text().substring(6));
                novelInfo.setUrl(doc.baseUri());
                novelInfo.setComplete(0);
                novelInfo.setSource(Sourcefhxiaoshuo.class.getName());

                getImage();

                list.add(novelInfo);
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
