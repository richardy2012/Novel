package flandre.cn.novel.info;

import java.io.Serializable;

public class NovelChapter implements Serializable {
    private String chapter;

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }
}
