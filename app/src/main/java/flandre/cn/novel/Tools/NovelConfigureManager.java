package flandre.cn.novel.Tools;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import flandre.cn.novel.crawler.*;
import flandre.cn.novel.view.NormalPageView;
import flandre.cn.novel.view.PageView;
import flandre.cn.novel.view.SmoothPageView;

import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NovelConfigureManager {
    private static NovelConfigure novelConfigure;
    private static WeakReference<Context> context;
    private static Constructor<?> constructor;

    // 所有源, 添加源时要修改ConfigureSourceActivity.Adapter.holders大小
    private final static List<Map<String, String>> source = new ArrayList<Map<String, String>>() {{
        add(new HashMap<String, String>(){{
            put("name", "望书阁 www.wangshugu.com");
            put("source", Sourcewangshugu.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "衍墨轩 www.ymoxuan.com");
            put("source", Sourceymoxuan.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "手机小说 www.aixiatxt.com");
            put("source", Sourceaixiatxt.class.getName());
        }});
        add(new HashMap<String, String>(){{
            put("name", "凤凰小说 www.fhxiaoshuo.org");
            put("source", Sourcefhxiaoshuo.class.getName());
        }});
        add(new HashMap<String, String>(){{
            put("name", "妙笔文学 www.mbtxt.cc");
            put("source", Sourcembtxt.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "铅笔小说 www.x23qb.com(备用)");
            put("source", Sourcex23qb.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "笔趣塔 www.biquta.la(废弃)");
            put("source", SourceBiquta.class.getName());
        }});
    }};

    public static Map<String, String> novelSource = new HashMap<String, String>() {{
        put(Sourcex23qb.class.getName(), "铅笔小说(www.x23qb.com)");
        put(Sourceymoxuan.class.getName(), "衍墨轩(www.ymoxuan.com)");
        put(SourceBiquta.class.getName(), "笔趣塔(www.biquta.la)");
        put(Sourcewangshugu.class.getName(), "望书阁(www.wangshugu.com)");
        put(Sourcefhxiaoshuo.class.getName(), "凤凰小说(www.fhxiaoshuo.org)");
        put(Sourceaixiatxt.class.getName(), "手机小说(www.aixiatxt.com)");
        put(Sourceaixiatxt.class.getName(), "妙笔文学(www.mbtxt.cc)");
    }};

    private final static List<Map<String, String>> pageView = new ArrayList<Map<String, String>>() {{
        add(new HashMap<String, String>() {{
            put("description", "普通翻页(无动画)");
            put("source", NormalPageView.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("description", "仿真翻页(平移动画)");
            put("source", SmoothPageView.class.getName());
        }});
    }};

    public static List<Map<String, String>> getPageView() {
        return pageView;
    }

    public static List<Map<String, String>> getSource() {
        return source;
    }

    public static Constructor<?> getConstructor() {
        return constructor;
    }

    public static void setConstructor(Constructor<?> constructor) {
        NovelConfigureManager.constructor = constructor;
    }

    public static NovelConfigure getConfigure(Context context) {
        if (NovelConfigureManager.context == null) NovelConfigureManager.context = new WeakReference<>(context);
        return getConfigure();
    }

    public static NovelConfigure getConfigure() {
        if (novelConfigure == null)
            loadConfigure(context.get());
        return novelConfigure;
    }

    public static void changeConfigure() {
        NovelConfigureManager.novelConfigure = NovelConfigureManager.novelConfigure.getNovelConfigure();
    }

    public static void setContext(Context context) {
        NovelConfigureManager.context = new WeakReference<>(context);
    }

    public static void saveConfigure(NovelConfigure configure, Context context) throws IOException {
        File file = context.getFilesDir();
        File con = new File(file, NovelTools.md5("Novel") + ".cfg");

        OutputStream stream = new FileOutputStream(con);
        ObjectOutputStream outputStream = new ObjectOutputStream(stream);

        outputStream.writeObject(configure);
        outputStream.flush();
        outputStream.close();
    }

    /**
     * 加载配置文件
     */
    private static void loadConfigure(Context context) {
        File file = context.getFilesDir();

        File configure = new File(file, NovelTools.md5("Novel") + ".cfg");

        // 不存在使用默认的并创造文件,创造使用自己的
        if (!configure.exists()) {
            novelConfigure = new NovelConfigure();

            try {
                boolean isOk = configure.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            try {
                OutputStream stream = new FileOutputStream(configure);
                ObjectOutputStream outputStream = new ObjectOutputStream(stream);
                outputStream.writeObject(novelConfigure);
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                boolean isOk = file.delete();
            }
        } else {
            try {
                InputStream stream = new FileInputStream(configure);
                ObjectInputStream inputStream = new ObjectInputStream(stream);
                novelConfigure = (NovelConfigure) inputStream.readObject();
                inputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
                NovelConfigureManager.novelConfigure = new NovelConfigure();
            }
        }
        try {
            constructor = Class.forName(novelConfigure.getNowSourceValue()).getConstructor(Activity.class, Handler.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static PageView getPageView(Context context) {
        try {
            Constructor constructor = Class.forName(NovelConfigureManager.novelConfigure.getNowPageView()).getConstructor(Context.class);
            return (PageView) constructor.newInstance(context);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}