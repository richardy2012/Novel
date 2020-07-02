package flandre.cn.novel.Tools;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Base64;
import android.widget.Toast;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.crawler.BaseCrawler;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.parse.OnFinishParse;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;

public class GetNovelInfoAsync extends AsyncTask<String, Void, Integer> {
    private WeakReference<Context> mContext;
    private OnFinishParse onFinishParse;

    public GetNovelInfoAsync(Context context) {
        mContext = new WeakReference<>(context);
    }

    public GetNovelInfoAsync setOnFinishParse(OnFinishParse onFinishParse) {
        this.onFinishParse = onFinishParse;
        return this;
    }

    @Override
    public Integer doInBackground(String... strings) {
        NovelInfo novelInfo = new NovelInfo();
        try {
            byte[] data = new byte[1024];
            FileInputStream inputStream = new FileInputStream(strings[0]);
            int read = inputStream.read(data);
            ByteBuilder byteBuilder = new ByteBuilder(AES.decrypt(Base64.decode(data, 0, read, Base64.DEFAULT)));
            String source = byteBuilder.readString(byteBuilder.readInt());
            String name = byteBuilder.readString(byteBuilder.readInt());
            String author = byteBuilder.readString(byteBuilder.readInt());
            String address = byteBuilder.readString(byteBuilder.readInt());

            if (SQLTools.getNovelId(SQLiteNovel.getSqLiteNovel(), name, author) == -1) {
                BaseCrawler crawler = (BaseCrawler) Class.forName(source).getConstructor(Activity.class,
                        Handler.class).newInstance((Activity) mContext.get(), null);
                crawler.getNovelInfo(address, novelInfo).run().run();
                SQLTools.saveInSQLite(novelInfo, SQLiteNovel.getSqLiteNovel(), mContext.get());
                return OnFinishParse.OK;
            } else {
                return OnFinishParse.ALWAYS;
            }
        } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException | IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return OnFinishParse.ERROR;
    }

    @Override
    public void onPostExecute(Integer integer) {
        switch (integer) {
            case OnFinishParse.ALWAYS:
                Toast.makeText(mContext.get(), "该小说已经存在了！", Toast.LENGTH_SHORT).show();
                break;
            case OnFinishParse.ERROR:
                Toast.makeText(mContext.get(), "加载失败！", Toast.LENGTH_SHORT).show();
                break;
            case OnFinishParse.OK:
                Toast.makeText(mContext.get(), "加载成功！", Toast.LENGTH_SHORT).show();
                ((IndexActivity) mContext.get()).getBookFragment().loadData();
                break;
        }
//        ((IndexActivity) mContext.get()).getBookFragment().getRefresh().setRefreshing(false);
        if (onFinishParse != null) onFinishParse.onFinishParse(integer);
    }
}
