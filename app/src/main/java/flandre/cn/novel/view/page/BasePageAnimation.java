package flandre.cn.novel.view.page;

public abstract class BasePageAnimation implements PageAnimation {
    int width;
    int height;
    PageView mPageView;

    public BasePageAnimation(PageView view){
        mPageView = view;
    }

    @Override
    public void onLoad(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void onCycle() {

    }
}
