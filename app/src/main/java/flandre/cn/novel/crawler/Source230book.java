package flandre.cn.novel.crawler;

import android.content.Context;
import android.os.Handler;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.info.NovelRemind;
import flandre.cn.novel.info.NovelText;
import flandre.cn.novel.info.NovelTextItem;
import org.jsoup.nodes.Document;

import java.util.List;

public class Source230book extends BaseCrawler {
    public Source230book(Context context, Handler handler) {
        super(context, handler);
        DOMAIN = "https://www.230book.com/";
        THREAD_COUNT = MIDDLE_THREAD_COUNT;
    }

    @Override
    public List<NovelInfo> run_search(String s) {
        return null;
    }

    @Override
    public List<NovelTextItem> run_list(String URL) {
        return null;
    }

    @Override
    public NovelText run_text(String URL) {
        return null;
    }

    @Override
    public List<NovelInfo> run_rank(int type) {
        return null;
    }

    @Override
    public List<NovelRemind> run_remind() {
        return null;
    }

    @Override
    public BaseThread getNovelInfo(String addr, NovelInfo novelInfo) {
        return null;
    }

    class MapThread extends BaseCrawler.BaseThread{

        MapThread(NovelInfo novelInfo, String imgUrl, String detailUrl) {
            super(novelInfo, imgUrl, detailUrl);
        }

        MapThread(Document doc, String imgUrl, NovelInfo novelInfo) {
            super(doc, imgUrl, novelInfo);
        }

        @Override
        public Runnable run() {
            return new Runnable() {
                @Override
                public void run() {

                }
            };
        }
    }
}
