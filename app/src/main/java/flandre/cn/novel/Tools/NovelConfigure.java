package flandre.cn.novel.Tools;

import android.graphics.Color;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.crawler.*;
import flandre.cn.novel.view.NormalPageView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 刚安装软件时的参数
 */
public class NovelConfigure implements Serializable {
    public static final int DAY = 0;  // 日间模式
    public static final int NIGHT = 1;  // 夜间模式

    private String textColor = "#000000";  // 文字颜色
    private String backgroundColor = "#FFF4E8CD";  // 背景颜色
    private String chapterColor = "#AA000000";  // 章节颜色
    private int textSize = 20;  // 文字大小
    private String mainTheme = "#4376AC";  // actionBar颜色
    private String backgroundTheme = "#FFFFFF";  // 背景颜色
    private String nameTheme = "#000000";  // 书名颜色
    private String authorTheme = "#AA000000";  // 作者颜色
    private String introduceTheme = "#66000000";  // 介绍颜色

    private String nowSourceKey = "望书阁 www.wangshugu.com";  // 当前使用源名
    private String nowSourceValue = Sourcewangshugu.class.getName();  // 当前使用源类

    private int mode = NovelConfigure.DAY;  // 当前的模式
    private NovelConfigure novelConfigure;  // 另一套配置

    private static boolean first = true;  // 是否第一次加载对象

    private String nowPageView = NormalPageView.class.getName();  // 当前使用的翻页
    private boolean isAlwaysNext = false;  // 是否全屏点击下一页

    public NovelConfigure(){
        // 加载夜间模式
        if (first) {
            first = false;
            novelConfigure = new NovelConfigure();
            novelConfigure.setTextColor("#AAFFFFFF");
            novelConfigure.setBackgroundColor("#000000");
            novelConfigure.setChapterColor("#88FFFFFF");
            novelConfigure.setMainTheme("#000000");
            novelConfigure.setBackgroundTheme("#DD000000");
            novelConfigure.setNameTheme("#AAFFFFFF");
            novelConfigure.setAuthorTheme("#88FFFFFF");
            novelConfigure.setIntroduceTheme("#77FFFFFF");
            novelConfigure.setNovelConfigure(this);
            novelConfigure.setMode(NovelConfigure.NIGHT);
        }
    }

    public NovelConfigure getNovelConfigure() {
        return novelConfigure;
    }

    private void setNovelConfigure(NovelConfigure novelConfigure) {
        this.novelConfigure = novelConfigure;
    }

    public int getMode() {
        return mode;
    }

    private void setMode(int mode) {
        this.mode = mode;
    }

    public int getMainTheme() {
        return Color.parseColor(mainTheme);
    }

    public int getNameTheme() {
        return Color.parseColor(nameTheme);
    }

    public void setNameTheme(String nameTheme) {
        Color.parseColor(nameTheme);
        this.nameTheme = nameTheme;
    }

    public int getAuthorTheme() {
        return Color.parseColor(authorTheme);
    }

    public void setAuthorTheme(String authorTheme) {
        Color.parseColor(authorTheme);
        this.authorTheme = authorTheme;
    }

    public int getIntroduceTheme() {
        return Color.parseColor(introduceTheme);
    }

    public void setIntroduceTheme(String introduceTheme) {
        Color.parseColor(introduceTheme);
        this.introduceTheme = introduceTheme;
    }

    public void setMainTheme(String mainTheme) {
        Color.parseColor(mainTheme);
        this.mainTheme = mainTheme;
    }

    public int getBackgroundTheme() {
        return Color.parseColor(backgroundTheme);
    }

    public void setBackgroundTheme(String backgroundTheme) {
        Color.parseColor(backgroundTheme);
        this.backgroundTheme = backgroundTheme;
    }

    public String getBaseNameTheme() {
        return nameTheme;
    }

    public String getBaseAuthorTheme() {
        return authorTheme;
    }

    public String getBaseIntroduceTheme() {
        return introduceTheme;
    }

    public String getBaseTextColor() {
        return textColor;
    }

    public String getBaseMainTheme() {
        return mainTheme;
    }

    public String getBaseBackgroundTheme() {
        return backgroundTheme;
    }

    public String getBaseBackgroundColor() {
        return backgroundColor;
    }

    public String getBaseChapterColor() {
        return chapterColor;
    }

    public int getTextColor() {
        return Color.parseColor(textColor);
    }

    public void setTextColor(String textColor) {
        Color.parseColor(textColor);
        this.textColor = textColor;
    }

    public int getBackgroundColor() {
        return Color.parseColor(backgroundColor);
    }

    public void setBackgroundColor(String backgroundColor) {
        Color.parseColor(backgroundColor);
        this.backgroundColor = backgroundColor;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
        novelConfigure.textSize = textSize;
    }

    public String getNowSourceKey() {
        return nowSourceKey;
    }

    public void setNowSourceKey(String nowSourceKey) {
        this.nowSourceKey = nowSourceKey;
    }

    public String getNowSourceValue() {
        return nowSourceValue;
    }

    public void setNowSourceValue(String nowSourceValue) {
        this.nowSourceValue = nowSourceValue;
    }

    public int getChapterColor() {
        return Color.parseColor(chapterColor);
    }

    public void setChapterColor(String chapterColor) {
        Color.parseColor(chapterColor);
        this.chapterColor = chapterColor;
    }

    public String getNowPageView() {
        return nowPageView;
    }

    public void setNowPageView(String nowPageView) {
        this.nowPageView = nowPageView;
        novelConfigure.nowPageView = nowPageView;
    }

    public boolean isAlwaysNext() {
        return isAlwaysNext;
    }

    public void setAlwaysNext(boolean alwaysNext) {
        isAlwaysNext = alwaysNext;
        novelConfigure.isAlwaysNext = alwaysNext;
    }
}
