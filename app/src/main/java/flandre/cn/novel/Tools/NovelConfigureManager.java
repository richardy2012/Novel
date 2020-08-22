package flandre.cn.novel.Tools;

import android.content.Context;
import android.os.Handler;
import flandre.cn.novel.crawler.*;
import flandre.cn.novel.view.page.*;

import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NovelConfigureManager {
    private static NovelConfigure novelConfigure;  // 配置类
    private static WeakReference<Context> context;  // ApplicationContext
    private static Constructor<?> constructor;  // 网络连接的构造函数对象

    /**
     * 所有的小说源
     */
    private final static List<Map<String, String>> source = new ArrayList<Map<String, String>>() {{
        add(new HashMap<String, String>() {{
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
        add(new HashMap<String, String>() {{
            put("name", "妙笔文学 www.mbtxt.cc");
            put("source", Sourcembtxt.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "轻小说 www.linovelib.com");
            put("source", Sourcelinovelib.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "凤凰小说 www.fhxsz.com");
            put("source", Sourcefhxiaoshuo.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "铅笔小说 www.x23qb.com(备用)");
            put("source", Sourcex23qb.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("name", "笔趣塔 www.biquta.la(废弃)");
            put("source", Sourcebiquta.class.getName());
        }});
    }};

    /**
     * 源类名与小说网站名的映射
     */
    public static Map<String, String> novelSource = new HashMap<String, String>() {{
        put(Sourcex23qb.class.getName(), "铅笔小说(www.x23qb.com)");
        put(Sourceymoxuan.class.getName(), "衍墨轩(www.ymoxuan.com)");
        put(Sourcebiquta.class.getName(), "笔趣塔(www.biquta.la)");
        put(Sourcewangshugu.class.getName(), "望书阁(www.wangshugu.com)");
        put(Sourcefhxiaoshuo.class.getName(), "凤凰小说(www.fhxiaoshuo.org)");
        put(Sourceaixiatxt.class.getName(), "手机小说(www.aixiatxt.com)");
        put(Sourcembtxt.class.getName(), "妙笔文学(www.mbtxt.cc)");
        put(Sourcelinovelib.class.getName(), "轻小说(www.linovelib.com)");
    }};

    /**
     * 所有的翻页动画
     */
    private final static List<Map<String, String>> pageView = new ArrayList<Map<String, String>>() {{
        add(new HashMap<String, String>() {{
            put("description", "普通翻页(无动画)");
            put("source", NormalPageAnimation.class.getName());
        }});
        add(new HashMap<String, String>() {{
            put("description", "仿真翻页(左右层叠)");
            put("source", CascadeSmoothPageAnimation.class.getName());
        }});
        add(new HashMap<String, String>(){{
            put("description", "仿真翻页(左右平移)");
            put("source", TranslationSmoothPageAnimation.class.getName());
        }});
        add(new HashMap<String, String>(){{
            put("description", "仿真翻页(上下滚动)");
            put("source", ScrollPageAnimation.class.getName());
        }});
    }};

    public static List<Map<String, String>> getPageView() {
        return pageView;
    }

    public static List<Map<String, String>> getSource() {
        return source;
    }

    public static BaseCrawler getCrawler(Context context, Handler handler){
        try {
            return (BaseCrawler) constructor.newInstance(context, handler);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void setConstructor(String source){
        try {
            NovelConfigureManager.constructor = Class.forName(source).getConstructor(Context.class, Handler.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
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

    /**
     * 改变当前的主题
     */
    public static void changeConfigure() {
        int mode = NovelConfigureManager.novelConfigure.getMode();
        if (mode == NovelConfigure.DAY) {
            NovelConfigureManager.novelConfigure.setMainThemePosition(NovelConfigure.NIGHT);
            NovelConfigureManager.novelConfigure.setNovelThemePosition(NovelConfigure.BLACK);
        } else {
            NovelConfigureManager.novelConfigure.setMainThemePosition(NovelConfigure.DAY);
        }
    }

    public static void setContext(Context context) {
        NovelConfigureManager.context = new WeakReference<>(context);
    }

    /**
     * 保存当前的配置类
     */
    public static void saveConfigure(NovelConfigure configure, Context context) throws IOException {
        File file = context.getFilesDir();
        File con = new File(file, NovelTools.md5("Novel") + ".cfg");
        writeObject(con, configure);
    }

    public static void saveConfigure(Context context) throws IOException {
        saveConfigure(NovelConfigureManager.novelConfigure, context);
    }

    private static void writeObject(File file, Serializable o) throws IOException {
        OutputStream stream = new FileOutputStream(file);
        ObjectOutputStream outputStream = new ObjectOutputStream(stream);

        outputStream.writeObject(o);
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
                writeObject(configure, novelConfigure);
            } catch (Exception e) {
                e.printStackTrace();
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
            constructor = Class.forName(novelConfigure.getNowSourceValue()).getConstructor(Context.class, Handler.class);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public static BaseCrawler getCrawler(String source, Context context, Handler handler){
        try {
            return (BaseCrawler) Class.forName(source).
                getConstructor(Context.class, Handler.class).newInstance(context, handler);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 拿到当前的页面View
     */
    public static PageAnimation getPageView(PageView pageView) {
        try {
            Constructor constructor = Class.forName(NovelConfigureManager.novelConfigure.getNowPageView()).getConstructor(PageView.class);
            return (PageAnimation) constructor.newInstance(pageView);
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
