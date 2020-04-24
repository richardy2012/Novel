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

import static java.lang.StrictMath.min;

public class Sourcelinovelib extends BaseCrawler {
    private List<NovelInfo> list;

    public Sourcelinovelib(Activity activity, Handler handler) {
        super(activity, handler);
        DOMAIN = "https://www.linovelib.com/";
        CHARSET = "UTF8";
        THREAD_COUNT = MIDDLE_THREAD_COUNT;
    }

    @Override
    List<NovelInfo> run_search(String s) {
        list = new ArrayList<>();
        Document document = crawlerPOST(DOMAIN + "s/", "searchkey=" + s + "&searchtype=all");
        if (document.select("div.book-img").size() > 0) {
            MapThread mapThread = new MapThread(document, null, new NovelInfo());
            mapThread.run(1);
        } else {
            Elements elements = document.select("body > div.wrap > div > div.search-main.fl > div.search-tab > div.search-result-list.clearfix");
            orderBy(elements, "div.fl.se-result-infos > h2 > a", s);
            List<Integer> integers = new ArrayList<>();
            for (int i = elements.size() - 1; i >= 0; i--)
                if (elements.get(i).select("div.fl.se-result-infos > h2 > a").text().endsWith("（漫画）"))
                    integers.add(i);
            for (int i : integers) {
                elements.remove(i);
            }
            ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
            for (int i = 0; i < min(elements.size(), SEARCH_COUNT); i++) {
                NovelInfo novelInfo = new NovelInfo();
                String detailUrl = DOMAIN + elements.get(i).select("div.fl.se-result-infos > h2 > a").attr("href").substring(1);
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
        Elements elements = document.select("body > div.wrap > div.container > div:nth-child(2) > div.volume-list > div > ul > li.col-4");
        for (Element element : elements) {
            NovelTextItem textItem = new NovelTextItem();
            textItem.setUrl(DOMAIN + element.select("a").attr("href").substring(1));
            textItem.setChapter(element.select("a").text());
            list.add(textItem);
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
        novelText.setChapter(document.select("#readerFt > div > div.title > h1").text());
        String text = withBr(document, "#readerFt > div > div.content", " ", "");
        if (text.length() == 5) {
            text = "假装有图片";
        } else if (text.endsWith("（本章未完）" + BR_REPLACEMENT)) {
            text = text.substring(0, text.length() - 6 - BR_REPLACEMENT.length() - 1);
            text += run_text(DOMAIN + document.select("#linkNext").attr("href").substring(1), 1).getText();
        } else
            text = text.substring(0, text.length() - BR_REPLACEMENT.length());
        novelText.setText(text);
        return novelText;
    }

    @Override
    public List<NovelInfo> run_rank(int type) {
        list = new ArrayList<>();
        for (int i = 0; i < 8; i++) list.add(null);
        Document document = null;
        switch (type) {
            case BaseCrawler.DAY_RANK:
                document = crawlerGET(DOMAIN + "top/weekvisit/1.html");
                break;
            case BaseCrawler.MONTH_RANK:
                document = crawlerGET(DOMAIN + "top/monthvisit/1.html");
                break;
            case BaseCrawler.TOTAL_RANK:
                document = crawlerGET(DOMAIN + "top/allvisit/1.html");
                break;
        }
        Elements elements = document.select("body > div.wrap > div.rank_wrap.clearfix.pdt40 > div.rank_main > div.rankpage_box > div");
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < Math.min(elements.size(), RANK_COUNT); i++) {
            NovelInfo novelInfo = new NovelInfo();
            String detailUrl = DOMAIN + elements.get(i).select("div.rank_d_book_intro.fl > div.rank_d_b_name > a").attr("href").substring(1);
            fixedThreadPool.execute(new MapThread(novelInfo, null, detailUrl).setRank(i).run());
        }
        checkTimeOut(fixedThreadPool, RANK_TIMEOUT);
        return list;
    }

    @Override
    public List<NovelRemind> run_remind() {
        List<NovelRemind> list = new ArrayList<>();
        Document document = crawlerGET(DOMAIN);
        Elements elements = document.select("body > div.wrap > div.new_chapter > div.tabpanel.tabC_wap > div:nth-child(1) > div > ul > li");
        for (Element element : elements) {
            NovelRemind novelRemind = new NovelRemind();
            novelRemind.setName(element.select("span.bookname > a").text());
            novelRemind.setChapter(element.select("span.chap > a").text());
            list.add(novelRemind);
        }
        return list;
    }

    class MapThread extends BaseThread {

        MapThread(Document doc, String imgUrl, NovelInfo novelInfo) {
            super(doc, imgUrl, novelInfo);
        }

        MapThread(NovelInfo novelInfo, String imgUrl, String detailUrl) {
            super(novelInfo, imgUrl, detailUrl);
        }

        private void run(int times) {
            if (doc == null) {
                doc = crawlerGET(detailUrl);
            }

            if (doc == null && times < 4) {
                run(++times);
                return;
            }

            try {
                Elements elements = doc.select("body > div.wrap > div.book-html-box.clearfix > div:nth-child(1) > div.book-detail.clearfix");
                imgUrl = elements.select("div.book-img.fl > img").attr("src");

                novelInfo.setName(elements.select("div.book-info > h1").text());
                novelInfo.setAuthor(doc.select("div.book-author > div.au-name > a").text());
                novelInfo.setChapter(doc.select("div.book-new-chapter > div.tit.fl > a").get(0).text());
                novelInfo.setIntroduce(withBr(elements, "div.book-info > div.book-dec.Jbook-dec > p", " ", "\n"));
                novelInfo.setUrl(DOMAIN + elements.select("div.book-info > div.btn-group > a.btn.read-btn").attr("href").substring(1));
                novelInfo.setComplete(elements.select("div.book-info > div.book-label > a.state").text().contains("连载") ? 0 : 1);
                novelInfo.setSource(Sourcelinovelib.class.getName());

                getImage();

                if (rank != -1) list.set(rank, novelInfo);
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
