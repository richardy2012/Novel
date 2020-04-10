package flandre.cn.novel.info;

import java.io.Serializable;

public class NovelTextItem implements Serializable {
    private String chapter;
    private String url;

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
