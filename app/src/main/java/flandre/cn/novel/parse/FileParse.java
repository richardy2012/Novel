package flandre.cn.novel.parse;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.Toast;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelTools;
import flandre.cn.novel.database.SQLiteNovel;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解析小说文本, 放入数据库
 */
public class FileParse {
    public static final int OK = 1;
    private static final int ALWAYS = 0;
    private static final int ERROR = 2;
    private  static final String[] CODE = new String[]{"utf8", "gbk", "utf16", "utf32", "ansi", "gb2312", "big5", "gb18030"};
    private final static String allChineseNum = "零一二三四五六七八九十百千万亿";
    private static final String compile = "第[0-9零一二三四五六七八九十百千万亿]*?[章节]";

    private Charset code;  // 编码格式
    private SQLiteNovel sqLiteNovel;
    private String path;  // 路径
    private StringBuilder stringBuffer;  // 文本缓冲

    private String result = null;  // 章节名
    private String name = null;  // 小说名
    private String author = null;  // 作者名
    private String introduce = null;  // 介绍
    private String table;  // 表名
    private Context mContext;
    private long novel_id;  // novel的id
    private OnfinishParse onfinishParse = null;  // 接口

    private BufferedReader reader;

    public void setOnfinishParse(OnfinishParse onfinishParse) {
        this.onfinishParse = onfinishParse;
    }

    public FileParse(String path, SQLiteNovel sqLiteNovel, Context context) {
        this.sqLiteNovel = sqLiteNovel;
        this.path = path;
        this.mContext = context;
        stringBuffer = new StringBuilder();
    }

    /**
     * 解析小说
     */
    public void parseFile() throws IOException {
        final File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        Toast.makeText(mContext, "加载本地小说中...", Toast.LENGTH_SHORT).show();
        // 开一个新的线程实现小说的解析
        new ParseTask(this, file).execute();
    }

    /**
     * 解析文本, 放入数据库
     */
    private void parseData() throws IOException {
        sqLiteNovel.freeStatus = false;
        int read, lastLength,  start = result.length();
        int now = parseInt(result.substring(1, result.length() - 1));  // 当前章节是第几章
        int next;  // 下一章节, 是第几章
        char[] buffer = new char[4092];
        String chapter;
        String text;

        SQLiteDatabase database = sqLiteNovel.getReadableDatabase();
        database.beginTransaction();
        out:
        while (true) {
            while (true) {
                // 匹配下一个章节, 没找到再读4092个字符, 找到时放入数据库, 读完时跳出大循环
                result = regexp(compile, stringBuffer, start, 0);
                if (!result.equals("")) {  // 读到新章节
                    next = parseInt(result.substring(1, result.length() - 1));
                    // 新章节与原来的章节差在3以内, 确认为新章节的头, 跳出循环
                    if (Math.abs(next - now) >= 1 && Math.abs(next - now) <= 3)
                        break;
                    // 否则重新定位
                    start = stringBuffer.indexOf(result, start) + result.length();
                    continue;
                }
                start = stringBuffer.length();
                read = reader.read(buffer);
                if (read == -1) break out;
                stringBuffer.append(buffer, 0, read);
            }
            // 第一行是章节名, Linux是\n, 但我不信有人拿Linux来码字
            lastLength = stringBuffer.indexOf("\r\n");
            chapter = stringBuffer.substring(0, lastLength);
            start = stringBuffer.indexOf(result, start);
            // 文本是第二行开始到新章节标题
            text = strip(stringBuffer.substring(lastLength, start), "\r\n", "\r\n= -").replace("\r\n\r\n", "\r\n");
            database.execSQL("insert into " + table +
                    " (chapter, text) values (?, ?)", new String[]{chapter, text});
            stringBuffer.delete(0, start);
            start = result.length();
            // 设置当前章节
            now = next;
        }
        lastLength = stringBuffer.indexOf("\r\n");
        chapter = stringBuffer.substring(0, lastLength);
        text = strip(stringBuffer.substring(lastLength), "\r\n", "\r\n= -").replace("\r\n\r\n", "\r\n");
        database.execSQL("insert into " + table +
                " (chapter, text) values (?, ?)", new String[]{chapter, text});
        database.setTransactionSuccessful();
        database.endTransaction();
        // 拿到最新的章节名, 放入novel的最新章节
        Cursor cursor = database.query(table, new String[]{"chapter"}, null, null, null, null, "-id", "1");
        cursor.moveToNext();
        String newChapter = cursor.getString(0);
        cursor.close();
        ContentValues values = new ContentValues();
        values.put("newChapter", newChapter);
        database.update("novel", values, "id=?", new String[]{String.valueOf(novel_id)});
        sqLiteNovel.freeStatus = true;
    }

    /**
     * @return 该小说是否已经解析过
     */
    private boolean checkInfo() {
        Cursor cursor = sqLiteNovel.getReadableDatabase().query("novel", null, "name=? and author=?",
                new String[]{name, author != null ? author : "Somebody"}, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        return count == 0;
    }

    /**
     * 保存刚才解析的信息, 并创建文本表
     */
    private void saveInfo() {
        // 保存图片
        String imageName = "FL" + NovelTools.md5(name) + ".jpg";
        File file = new File(mContext.getExternalFilesDir(null), "img");
        if (!file.exists()) file.mkdir();
        File image = new File(file, imageName);
        try {
            OutputStream stream = new FileOutputStream(image);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Bitmap bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.not_found);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            bitmap.recycle();
            byte[] data = outputStream.toByteArray();
            stream.write(data);
            stream.flush();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("author", author != null ? author : "Somebody");
        values.put("introduce", introduce);
        values.put("complete", 1);
        values.put("time", new Date().getTime());
        values.put("image", image.getAbsolutePath());
        values.put("watch", "1:1");
        novel_id = sqLiteNovel.getReadableDatabase().insert("novel", null, values);
        // 在nc里面记录表名
        values = new ContentValues();
        table = "FL" + NovelTools.md5(name + (author != null ? author : "Somebody"));
        values.put("novel_id", novel_id);
        values.put("md5", table);
        long NC_id = sqLiteNovel.getReadableDatabase().insert("nc", null, values);
        // 创建存文本的表
        sqLiteNovel.getReadableDatabase().execSQL(
                "create table " + table + "(" +
                        "id INTEGER primary key AUTOINCREMENT," +
                        "chapter varchar(255)," +
                        "url varcahr(255)," +
                        "text text)"
        );
    }

    /**
     * 解析小说基本信息
     */
    private void parseInfo() throws IOException {
        char[] buffer = new char[1024];
        int read, count = 0;
        read = reader.read(buffer);
        count += read;
        StringBuilder stringBuffer = new StringBuilder();
        stringBuffer.append(new String(buffer, 0, read));
        // 《》里面包着的是小说名
        int start = 0, end = 0;
        start = stringBuffer.indexOf("《");
        if (start > 0) {
            end = stringBuffer.indexOf("》", start);
            if (end > 0)
                name = stringBuffer.substring(start + 1, end);
            end++;
        }
        // 如果文本没有小说名, 那么小说名使用文件名
        if (name == null) {
            name = new File(path).getName();
            end = name.indexOf(".");
            if (end > 0) {
                name = name.substring(0, end);
                end = 0;
            }
        }
        // 匹配作者名, 没有作者取名 somebody
        start = stringBuffer.indexOf("作者：");
        if (start > 0) {
            start += 3;
            end = stringBuffer.indexOf("\r\n", start);
            author = stringBuffer.substring(start, end);
            end += 2;
        }
        if (end > 0) start = end;
        if (start < 0) start = 0;
        // 如果读取了10kb的数据仍然没有找到章节, 报解析错误
        while (count < 1024 * 10 && read != -1) {
            read = reader.read(buffer);
            count += read;
            stringBuffer = stringBuffer.append(new String(buffer, 0, read));
            result = regexp(compile, stringBuffer, start, 0);
            if (!result.equals("")) break;
        }
        if (result.equals("")) {
            throw new ParseException("can not parse novel");
        }
        // 介绍拿作者结尾到章节开头
        end = stringBuffer.indexOf(result, start);
        if (start > 0 && end > 0) {
            introduce = strip(stringBuffer.substring(start, end), "\r\n -", "\r\n= -");
        }else {
            introduce = "没有简介";
        }
        // 去除以解析部分
        stringBuffer.delete(0, end);
        this.stringBuffer.append(stringBuffer);
    }

    /**
     * 获取小说的编码
     */
    private void getCode(File file) throws IOException {
        byte[] bytes = new byte[2048];
        int read;
        InputStream stream = new FileInputStream(file);
        read = stream.read(bytes);
        for (String s1 : CODE) {
            try {
                String s = new String(bytes, 0, read, Charset.forName(s1));
                if (!s.substring(0, s.length() - 1).contains("�")) {
                    code = Charset.forName(s1);
                    stream.close();
                    return;
                }
            } catch (UnsupportedCharsetException e) {
                e.printStackTrace();
            }
        }
        throw new ParseException("can not parse novel");
    }

    private String strip(String src, String front, String back) {
        int start = 0, end = src.length();
        try {

            while (front.indexOf(src.charAt(start)) >= 0) {
                start++;
            }

            while (back.indexOf(src.charAt(end - 1)) >= 0) {
                end--;
            }
        } catch (StringIndexOutOfBoundsException e) {
            return "空章节或解析错误";
        }

        if (start == 0 && end == src.length()) return src;
        return src.substring(start, end);
    }

    private String regexp(String compile, StringBuilder input, int start, int group) throws IOException {
        Pattern pattern = Pattern.compile(compile, Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find(start)) {
            return matcher.group(group);
        }
        return "";
    }

    /**
     * 把字符串的数字(阿拉伯以及中国)转换成数字
     */
    private int parseInt(String number) {
        try {
            return Integer.parseInt(number);
        } catch (Exception ignored) {
        }
        int result = 0;
        int result2 = 0;
        boolean have = false;
        int temp = 1;
        for (int i = 0; i < number.length(); i++) {
            char c = number.charAt(i);
            int p = allChineseNum.indexOf(c);
            if (p >= 10) have = true;
            if (!have) {
                if (i != 0) result2 *= 10;
                result2 += p;
            }
            if (p < 10) {
                if (i != 0) result += temp;
                temp = p;
                continue;
            }
            switch (p - 10) {
                case 0:
                    temp *= 10;
                    break;
                case 1:
                    temp *= 100;
                    break;
                case 2:
                    temp *= 1000;
                    break;
                case 3:
                    temp *= 10000;
                    break;
                case 4:
                    temp *= 100000000;
                    break;
                default:
                    break;
            }
        }
        result += temp;
        return have ? result : result2;
    }

    static class ParseTask extends AsyncTask<Void, Void, Integer>{
        private FileParse mParse;
        private File file;

        ParseTask(FileParse mParse, File file) {
            this.mParse = mParse;
            this.file = file;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                mParse.getCode(file);
                mParse.reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), mParse.code));
                mParse.parseInfo();
                if (mParse.checkInfo()) {
                    mParse.saveInfo();
                    mParse.parseData();
                    return OK;
                } else {
                    return ALWAYS;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ParseException e) {
                e.printStackTrace();
                return ERROR;
            }
            return 3;
        }

        @Override
        protected void onPostExecute(Integer mode) {
            switch (mode) {
                case ALWAYS:
                    Toast.makeText(mParse.mContext, "小说已经加载了！", Toast.LENGTH_SHORT).show();
                    break;
                case OK:
                    Toast.makeText(mParse.mContext, "小说加载完成！", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    Toast.makeText(mParse.mContext, "解析不了这本小说...", Toast.LENGTH_SHORT).show();
                    break;
            }
            if (mParse.onfinishParse != null) mParse.onfinishParse.onFinishParse(mode);
        }
    }

    public interface OnfinishParse {
        /**
         * 当解析完成时调用
         * @param mode 解析的情况
         */
        public void onFinishParse(int mode);
    }
}
