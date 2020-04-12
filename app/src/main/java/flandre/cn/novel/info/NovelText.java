package flandre.cn.novel.info;

import java.io.Serializable;

/**
 * 小说文本信息
 */
public class NovelText implements Serializable {
    private String text;
    private String chapter;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }
}
