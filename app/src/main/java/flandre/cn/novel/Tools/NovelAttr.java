package flandre.cn.novel.Tools;

import android.annotation.SuppressLint;
import flandre.cn.novel.database.SQLiteNovel;

import java.lang.reflect.Constructor;

/**
 * 全局的参数属性
 */
@SuppressLint("StaticFieldLeak")
public class NovelAttr {
//    public static SQLiteNovel sqLiteNovel;  // 全局的数据库
//    public static Constructor<?> constructor;  // 搜索小说的使用源
//    public static NovelConfigure configure;  // 配置文件
    public static boolean changeThemeEnable;  // 是否可以更改主题
    public static boolean loadDataEnable;  // IndexActivity是否可以loadData
//    public static boolean downloadEnable = true;  // 是否可以下载
}
