package flandre.cn.novel.info;

import java.io.Serializable;

/**
 * 小说文本信息
 */
public class NovelText extends NovelChapter implements Serializable {
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
