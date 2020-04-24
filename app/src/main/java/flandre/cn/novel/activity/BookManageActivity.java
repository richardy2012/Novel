package flandre.cn.novel.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.*;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.adapter.BookManagerAdapter;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.Tools.Decoration;

import java.util.*;

/**
 * 书籍管理
 * 2019.12.9
 */
public class BookManageActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    private boolean manage = true;  // 决定菜单的显示, 如果是true显示管理, 否则显示取消
    private boolean select_all = false;  // 是否选择全部
    private Adapter adapter;
    private TextView textView;
    private SQLiteNovel sqLiteNovel;
    private List<String> position = new ArrayList<>();  // 设置选中的位置
    private LinearLayout linearLayout;
    private int mode;  // 进行的操作(0 删除选中)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_manage);
        setupMusicService();
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(getApplicationContext());
        NovelConfigureManager.getConfigure(getApplicationContext());

        linearLayout = findViewById(R.id.bottom);
        Spinner spinner = findViewById(R.id.select);
        textView = findViewById(R.id.select_all);
        textView.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        linearLayout.setBackgroundColor((~NovelConfigureManager.getConfigure().getBackgroundTheme()) & 0x11ffffff | 0x11000000);
        textView.setOnClickListener(this);
        spinner.setAdapter(new BookManagerAdapter(new ArrayList<String>() {{
            add("删除");
            add("设为连载");
            add("设为完结");
        }}, this));
        spinner.setPopupBackgroundDrawable(new ColorDrawable(NovelConfigureManager.getConfigure().getBackgroundTheme()));

        spinner.setOnItemSelectedListener(this);

        setupActionBar();
        setupData();
        loadData();
    }

    private void loadData() {
        List<NovelInfo> list = SQLTools.getNovelData(sqLiteNovel);
        adapter.update(list);
    }

    private void setupData() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        RecyclerView recyclerView = findViewById(R.id.data);
        recyclerView.setLayoutManager(manager);
        adapter = new Adapter(null);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new Decoration(this));
        recyclerView.setHasFixedSize(false);
        recyclerView.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 设置菜单
        if (manage) {
            menu.findItem(R.id.cancel).setVisible(false);
            menu.findItem(R.id.manager).setVisible(true);
        } else {
            menu.findItem(R.id.cancel).setVisible(true);
            menu.findItem(R.id.manager).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 在ActionBar上添加菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.manager_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.cancel:
                manage = true;
                select_all = false;
                textView.setText("全选");
                // 使菜单无效重新生成
                invalidateOptionsMenu();
                adapter.notifyDataSetChanged();
                if (linearLayout != null) linearLayout.setVisibility(View.GONE);
                break;
            case R.id.manager:
                manage = false;
                invalidateOptionsMenu();
                adapter.notifyDataSetChanged();
                if (linearLayout != null) linearLayout.setVisibility(View.VISIBLE);
                break;
            case R.id.save:
                doSave();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    private void doSave() {
        switch (mode) {
            case 0:
                // 删除选中的小说
                for (String i : position) {
                    SQLTools.delete(sqLiteNovel, i, mService, this);
                }
                position.clear();
                loadData();
                break;
            case 1:
            case 2:
                ContentValues values = new ContentValues();
                values.put("complete", mode >> 1);
                for (String i : position)
                    sqLiteNovel.getReadableDatabase().update("novel", values, "id = ?", new String[]{i});
                loadData();
                break;
        }
        Intent intent = new Intent();
        intent.setAction(IndexActivity.LOAD_DATA);
        sendBroadcast(intent);
        // 还原界面
        manage = true;
        select_all = false;
        linearLayout.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("书本管理");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(NovelConfigureManager.getConfigure().getMainTheme()));
    }

    @Override
    public void onClick(View v) {
        if (select_all) {
            select_all = false;
            ((TextView) v).setText("全选");
        } else {
            ((TextView) v).setText("取消");
            select_all = true;
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        mode = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> implements View.OnClickListener {
        List<NovelInfo> data;

        Adapter(List<NovelInfo> data) {
            this.data = data;
        }

        public void update(List<NovelInfo> list) {
            this.data = list;
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.manage_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int i) {
            NovelInfo novelInfo = data.get(i);
            holder.name.setText(novelInfo.getName());
            holder.author.setText(novelInfo.getAuthor());
            holder.image.setImageBitmap(BitmapFactory.decodeFile(novelInfo.getImagePath()));
            holder.checkBox.setTag(i);
            holder.name.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
            holder.author.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
            holder.checkIcon.setBackground(getResources().getDrawable(NovelConfigureManager.getConfigure().getMode()
                    == NovelConfigure.DAY ? R.drawable.choice_day : R.drawable.choice_night));
            if (manage) {
                holder.checkBox.setVisibility(View.GONE);
            } else {
                holder.checkBox.setVisibility(View.VISIBLE);
                if (select_all) {
                    holder.checkIcon.setTag(true);
                    holder.checkIcon.setVisibility(View.VISIBLE);
                } else {
                    holder.checkIcon.setTag(false);
                    holder.checkIcon.setVisibility(View.GONE);
                }
            }
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(10);
            drawable.setStroke(5, NovelConfigureManager.getConfigure().getTextColor());
            holder.checkBox.setBackground(drawable);
            holder.checkBox.setOnClickListener(this);
        }

        @Override
        public int getItemCount() {
            if (data == null) return 0;
            return data.size();
        }

        private void onCheckedChanged(View buttonView, boolean isChecked) {
            int pos = (int) buttonView.getTag();
            if (isChecked) {
                position.add(String.valueOf(data.get(pos).getId()));
            } else {
                position.remove(String.valueOf(data.get(pos).getId()));
            }
        }

        @Override
        public void onClick(View v) {
            ImageView checkIcon = v.findViewById(R.id.check_icon);
            boolean isShow = !(boolean) checkIcon.getTag();
            checkIcon.setTag(isShow);
            checkIcon.setVisibility(isShow ? View.VISIBLE : View.GONE);
            onCheckedChanged(v, (Boolean) v.findViewById(R.id.check_icon).getTag());
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView name, author;
            RelativeLayout checkBox;
            ImageView checkIcon;

            Holder(View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.image);
                name = itemView.findViewById(R.id.title);
                author = itemView.findViewById(R.id.author);
                checkBox = itemView.findViewById(R.id.check);
                checkIcon = itemView.findViewById(R.id.check_icon);
            }
        }
    }
}
