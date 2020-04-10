package flandre.cn.novel.activity;

import android.database.Cursor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.adapter.MenuAdapter;
import flandre.cn.novel.serializable.SelectList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节选择界面(应该做一个fragment的, 懒得改了)
 * 2019.12.8
 */
public class MenuActivity extends BaseActivity implements AdapterView.OnItemClickListener {
    private List<Map<String, String>> list;
    private String table;
    private int chapter;
    private SelectList selectList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        setupMusicService();

        LinearLayout linearLayout = findViewById(R.id.layout);
        linearLayout.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundColor());

        loadData(savedInstanceState);

        int show = 10;
        int all = list.size();
        // 显示的位置
        int position = chapter - show / 2;
        position = position > -1 ? position : 0;
        position = position < all - show ? position : all - show;

        MenuAdapter adapter = new MenuAdapter(this, list);
        ListView listView = findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setSelection(position);
        // 点击时返回小说文本界面
        listView.setOnItemClickListener(this);
    }

    private void loadData(Bundle savedInstanceState){
        SQLiteNovel sqLiteNovel = SQLiteNovel.getSqLiteNovel(getApplicationContext());
        Bundle bundle;
        if (savedInstanceState == null)
            bundle = getIntent().getExtras();
        else bundle = savedInstanceState;
        table = (String) bundle.get("table");
        chapter = bundle.getInt("chapter");
        // 小说是否已经收藏了, 决定了是从数据库拿数据, 还是上一个页面拿
        if (table == null) {
            list = new ArrayList<>();
            selectList = (SelectList) bundle.get("list");
            List<String> maps = selectList.getList();
            for (int i = 0; i < maps.size(); i++) {
                Map<String, String> map = new HashMap<>();
                map.put("id", String.valueOf(i + 1));
                map.put("chapter", maps.get(i));
                list.add(map);
            }
        } else {
            list = new ArrayList<>();
            Cursor cursor = sqLiteNovel.getReadableDatabase().query(table, new String[]{"id", "chapter"},
                    null, null, null, null, null);

            cursor.moveToNext();
            do {
                Map<String, String> map = new HashMap<>();
                map.put("id", String.valueOf(cursor.getInt(0)));
                map.put("chapter", cursor.getString(1));
                list.add(map);
            } while (cursor.moveToNext());

            cursor.close();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("table", table);
        outState.putInt("chapter", chapter);
        outState.putSerializable("list", selectList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Map<String, String> map = (Map<String, String>) parent.getItemAtPosition(position);
        setResult(Integer.parseInt(map.get("id")));
        finish();
    }
}
