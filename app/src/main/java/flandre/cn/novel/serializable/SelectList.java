package flandre.cn.novel.serializable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SelectList<T> implements Serializable {
    private List<T> list;

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
