package flandre.cn.novel.Tools;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.Toast;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.crawler.BaseCrawler;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;

public class GetNovelInfoAsync extends AsyncTask<String, Void, Integer> {
    private WeakReference<Context> mContext;
    private int start, length;
    private byte[] data;

    public GetNovelInfoAsync(Context context) {
        mContext = new WeakReference<>(context);
    }

    private String getString() {
        String s = new String(data, start, length, Charset.forName("UTF8"));
        start += length;
        length = data[start];
        start++;
        return s;
    }

    @Override
    public Integer doInBackground(String... strings) {
        NovelInfo novelInfo = new NovelInfo();
        try {
            FileInputStream inputStream = new FileInputStream(strings[0]);
            data = new byte[1024];
            int read = inputStream.read(data);
            start = 1;
            length = data[0];

            String source = getString();
            String name = getString();
            String author = getString();
            String address = new String(data, start, length, Charset.forName("UTF8"));

            if (SQLTools.getNovelId(SQLiteNovel.getSqLiteNovel(), name, author) == -1) {
                BaseCrawler crawler = (BaseCrawler) Class.forName(source).getConstructor(Activity.class,
                        Handler.class).newInstance((Activity) mContext.get(), null);
                crawler.getNovelInfo(address, novelInfo).run().run();
                SQLTools.saveInSQLite(novelInfo, SQLiteNovel.getSqLiteNovel(), mContext.get());
                return 1;
            } else {
                return -1;
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (java.lang.InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public void onPostExecute(Integer integer) {
        switch (integer) {
            case -1:
                Toast.makeText(mContext.get(), "该小说已经存在了！", Toast.LENGTH_SHORT).show();
                break;
            case 0:
                Toast.makeText(mContext.get(), "加载失败！", Toast.LENGTH_SHORT).show();
                break;
            case 1:
                Toast.makeText(mContext.get(), "加载成功！", Toast.LENGTH_SHORT).show();
                ((IndexActivity)mContext.get()).getBookFragment().loadData();
                break;
        }
        ((IndexActivity)mContext.get()).getBookFragment().getRefresh().setRefreshing(false);
    }
}
