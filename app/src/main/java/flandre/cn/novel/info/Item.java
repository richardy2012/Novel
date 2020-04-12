package flandre.cn.novel.info;


public class Item {
    private String text;
    private int imageId;

    public String getText() {
        return text;
    }

    public int getImageId() {
        return imageId;
    }

    public Item(String text, int imageId) {
        this.text = text;
        this.imageId = imageId;
    }
}
