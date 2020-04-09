package flandre.cn.novel.info;


import java.io.Serializable;

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

    public static class WrapperNovelText {
        private String table;
        private NovelText novelText;

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public NovelText getNovelText() {
            return novelText;
        }

        public void setNovelText(NovelText novelText) {
            this.novelText = novelText;
        }
    }
}
