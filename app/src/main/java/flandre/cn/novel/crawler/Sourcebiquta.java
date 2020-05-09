package flandre.cn.novel.crawler;

import android.app.Activity;
import android.os.Handler;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.info.NovelRemind;
import flandre.cn.novel.info.NovelText;
import flandre.cn.novel.info.NovelTextItem;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.min;

public class Sourcebiquta extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourcebiquta(Activity activity, Handler handler) {
        super(activity, handler);
        this.CHARSET = "UTF8";
        this.DOMAIN = "https://www.biquta.la/";
        this.THREAD_COUNT = MIDDLE_THREAD_COUNT;
    }

    @Override
    List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN + "searchbook.php?keyword=" + s);
        document.select("#main > div.novelslist2 > ul > li:nth-child(1)").remove();
        Elements elements = document.select("#main > div.novelslist2 > ul > li");
        elements = orderBy(elements, "span.s2 > a", s);
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = DOMAIN.substring(0, DOMAIN.length() - 1) + document.select("span.s2 > a").get(i).attr("href");
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).run());
        }
        checkTimeOut(fixedThreadPool, SEARCH_TIMEOUT);
        return list;
    }

    @Override
    List<NovelTextItem> run_list(String URL) {
        List<NovelTextItem> list = new ArrayList<>();
        Document document = crawlerGET(URL);
        document.select(".list > dl > dt").remove();
        Elements last = document.select(".list").last().select("dd > a");

        List<NovelTextItem> lastList = new ArrayList<>();
        for (Element element : last) {
            NovelTextItem textItem = new NovelTextItem();
            textItem.setChapter(element.text());
            textItem.setUrl(DOMAIN + element.attr("href").substring(1));
            lastList.add(textItem);
        }
        document.select(".list").last().remove();

        Elements elements = document.select("dd > a");
        for (Element e : elements) {
            NovelTextItem textItem = new NovelTextItem();
            textItem.setUrl(DOMAIN + e.attr("href").substring(1));
            textItem.setChapter(e.text());
            list.add(textItem);
        }

        try {
            String href = document.select("#showMore > div > dt > a").get(0).attr("href");
            href = href.substring(href.indexOf("(") + 1, href.indexOf(","));
            String json = crawlerPOST("https://www.biquta.la/action.php", "action=clist&bookid=" + href).body().text();
            String JSONBaseURL = document.select("dd > a").first().attr("href").substring(1);
            JSONBaseURL = JSONBaseURL.substring(0, JSONBaseURL.lastIndexOf("/"));

            List<NovelTextItem> jsonList = new ArrayList<>();
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = (JSONArray) jsonObject.get("columnlist");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = (JSONObject) jsonArray.get(i);
                JSONArray array = (JSONArray) object.get("chapterlist");
                for (int j = 0; j < array.length(); j++) {
                    Map<String, String> map = new HashMap<>();
                    NovelTextItem textItem = new NovelTextItem();
                    JSONObject data = (JSONObject) array.get(j);
                    textItem.setUrl(DOMAIN + JSONBaseURL + "/" + data.get("chapterid") + ".html");
                    textItem.setChapter((String) data.get("chaptername"));
                    jsonList.add(textItem);
                }
            }
            list.addAll(jsonList);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        list.addAll(lastList);
        return list;
    }

    @Override
    NovelText run_text(String URL) {
        return run_text(URL, 1);
    }

    @Override
    public List<NovelInfo> run_rank(int type) {
        list = new ArrayList<>();
        for (int i = 0; i < 8; i++) list.add(null);
        Document document = crawlerGET(DOMAIN + "biqutaph.html");
        Elements elements = null;
        switch (type) {
            case BaseCrawler.DAY_RANK:
                elements = document.select("#con_hng_1 > ul > li > a");
                break;
            case BaseCrawler.MONTH_RANK:
                elements = document.select("#con_hng_2 > ul > li > a");
                break;
            case BaseCrawler.TOTAL_RANK:
                elements = document.select("#con_hng_3 > ul > li > a");
                break;
        }
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < Math.min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = DOMAIN + elements.get(i).attr("href").substring(1);
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    private NovelText run_text(String URL, int times) {
        Document document = crawlerGET(URL);
        if (document == null && times < 4) {
            return run_text(URL, ++times);
        }
        NovelText novelText = new NovelText();
        novelText.setChapter(document.select("#wrapper > div.content_read > div.box_con > div.bookname > h1").get(0).text());
        document.select("#content > div").remove();
        document.select("#content > script").remove();
        novelText.setText(document.select("#content").html().replace("<br>", "\n").replace("\n\n\n\n", "\n"));

        return novelText;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        Elements elements = document.select("#newscontent > div.l > ul > li");
        for (Element element : elements) {
            NovelRemind novelRemind = new NovelRemind();
            novelRemind.setName(element.select("a").get(0).text());
            novelRemind.setChapter(element.select("a").get(1).text());
            list.add(novelRemind);
        }
        return list;
    }

    @Override
    public BaseThread getNovelInfo(String addr, NovelInfo novelInfo) {
        return new MapThread(novelInfo, null, addr);
    }

    public class MapThread extends BaseThread {

        MapThread(NovelInfo map, String imgUrl, String detailUrl) {
            super(map, imgUrl, detailUrl);
        }

        private void run(int times) {
            doc = crawlerGET(detailUrl);
            if (doc == null && detailUrl != null && times < 4) {
                run(++times);
            }

            try {
                imgUrl = DOMAIN.substring(0, DOMAIN.length() - 1) + doc.select("#fmimg > img").attr("src");
                Element data = doc.select("#info").get(0);
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String time = data.select("p").get(2).text().substring(5);
                long ts = simpleDateFormat.parse(time).getTime();
                novelInfo.setName(data.select("h1").text());
                novelInfo.setAuthor(data.select("p").get(0).text().substring(5));
                novelInfo.setComplete(new Date().getTime() - ts > 14 * 60 * 60 * 1000 * 24 ? 1 : 0);
                novelInfo.setIntroduce(doc.select("#intro").text());
                novelInfo.setSource(Sourcebiquta.this.getClass().getName());
                novelInfo.setUrl(doc.baseUri());
                novelInfo.setChapter(data.select("p").get(3).select("a").text());

                getImage();
                if (novelInfo.getName().equals("")) return;
                if (list == null) return;
                if (rank != -1) list.set(rank, novelInfo);
                else list.add(novelInfo);
            } catch (NullPointerException e) {
                e.printStackTrace();
            } catch (ParseException e) {
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

