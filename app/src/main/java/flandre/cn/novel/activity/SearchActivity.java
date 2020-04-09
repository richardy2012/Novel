package flandre.cn.novel.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.support.v7.widget.SearchView;
import android.widget.*;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.crawler.BaseCrawler;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.info.NovelRemind;
import flandre.cn.novel.Tools.Decoration;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * 搜索小说
 * 2019.12.8
 */
public class SearchActivity extends BaseActivity {
    private SearchView searchView;  // 顶部的搜索框
    private FrameLayout frameLayout;  // 一个容器
    private RecordAdapter recordAdapter;
    private RemindAdapter remindAdapter;
    private ResultAdapter resultAdapter;
    private SwipeRefreshLayout refreshLayout;  // 下拉的旋转组件
    private View search_result;  // 搜索结果界面
    private View search_remind;  // 搜索提示界面
    private SQLiteNovel sqLiteNovel;
    private NovelConfigure configure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        configure = NovelConfigureManager.getConfigure(getApplicationContext());
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(getApplicationContext());

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setBackgroundDrawable(new ColorDrawable(configure.getMainTheme()));

        frameLayout = findViewById(R.id.data);
        refreshLayout = findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 下拉时如果搜索框里有文字, 进行重新搜索, 否则什么都不做
                if (!searchView.getQuery().toString().equals("")) {
                    searchView.setSubmitButtonEnabled(false);
                    new SearchTask(SearchActivity.this).execute(searchView.getQuery().toString());
                } else
                    refreshLayout.setRefreshing(false);
            }
        });
        findViewById(R.id.total).setBackgroundColor(configure.getBackgroundTheme());
        findViewById(R.id.theme).setBackgroundColor((~configure.getBackgroundTheme()) & 0x11FFFFFF | 0x11000000);
        setupRemind();
        setupResult();
        loadData();
    }

    /**
     * 设置搜索的提示界面
     */
    private void setupRemind() {
        search_remind = LayoutInflater.from(this).inflate(R.layout.search_remind, frameLayout, false);
        search_remind.setTag("remind");

        ((TextView) search_remind.findViewById(R.id.remind_txt)).setTextColor(configure.getIntroduceTheme());
        ((TextView) search_remind.findViewById(R.id.record_txt)).setTextColor(configure.getIntroduceTheme());

        LinearLayoutManager recordManager = new LinearLayoutManager(this);
        recordManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recordAdapter = new RecordAdapter(null);
        RecyclerView record = search_remind.findViewById(R.id.record);
        record.setLayoutManager(recordManager);
        record.setAdapter(recordAdapter);

        LinearLayoutManager remindManager = new LinearLayoutManager(this);
        remindManager.setOrientation(LinearLayoutManager.VERTICAL);
        List<NovelRemind> list = new ArrayList<>();
        NovelRemind novelRemind = new NovelRemind();
        novelRemind.setName("正在搜索");
        novelRemind.setChapter("......");
        list.add(novelRemind);
        remindAdapter = new RemindAdapter(list);
        RecyclerView remind = search_remind.findViewById(R.id.remind);
        remind.setLayoutManager(remindManager);
        remind.setAdapter(remindAdapter);
        remind.addItemDecoration(new Decoration(this));

        record.setBackgroundColor(configure.getBackgroundTheme());
        remind.setBackgroundColor(configure.getBackgroundTheme());
        remind.setNestedScrollingEnabled(false);
        remind.setFocusable(false);

        frameLayout.addView(search_remind);
    }

    /**
     * 设置搜索的结果界面, 等搜索完成才把界面放入
     */
    private void setupResult() {
        search_result = LayoutInflater.from(this).inflate(R.layout.search_result, frameLayout, false);
        search_result.setTag("result");
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        resultAdapter = new ResultAdapter(null);
        RecyclerView result = search_result.findViewById(R.id.result);
        result.setLayoutManager(manager);
        result.setAdapter(resultAdapter);
        result.addItemDecoration(new Decoration(this));
        search_result.findViewById(R.id.background).setBackgroundColor(configure.getBackgroundTheme());
    }

    /**
     * 给提示页面加载数据
     */
    private void loadData() {
        List<String> list = new ArrayList<>();
        Cursor cursor = sqLiteNovel.getReadableDatabase().query("search", new String[]{"name"}, null,
                null, null, null, "-time", "8");
        if (cursor.moveToNext()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        recordAdapter.update(list);
        new RemindTask(this).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        final MenuItem item = menu.findItem(R.id.search);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            // 点击时搜索内容并return给handler
            @Override
            public boolean onQueryTextSubmit(String s) {
                if (!refreshLayout.isRefreshing()) {
                    refreshLayout.setRefreshing(true);
                    searchView.setSubmitButtonEnabled(false);
                    new SearchTask(SearchActivity.this).execute(s);
                } else {
                    Toast.makeText(SearchActivity.this, "搜索紧啊，扑街！", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                if (s.equals("") && frameLayout.findViewWithTag("result") != null) {
                    frameLayout.removeAllViews();
                    frameLayout.addView(search_remind);
                }
                return true;
            }
        });
        // 设置搜索为打开状态
        searchView.setIconified(false);
        searchView.setSubmitButtonEnabled(true);
        return true;
    }

    static class SearchTask extends AsyncTask<String, Void, List<NovelInfo>> {
        private WeakReference<SearchActivity> mContext;

        public SearchTask(SearchActivity mContext) {
            this.mContext = new WeakReference<>(mContext);
        }

        /**
         * 搜索小说
         */
        @Override
        protected List<NovelInfo> doInBackground(String... s) {
            try {
                SQLiteNovel sqLiteNovel = mContext.get().sqLiteNovel;
                Cursor cursor = sqLiteNovel.getReadableDatabase().query("search", null,
                        "name=?", s, null, null, null);
                if (cursor.moveToNext()) {
                    ContentValues values = new ContentValues();
                    values.put("time", new Date().getTime());
                    sqLiteNovel.getReadableDatabase().update("search", values, "name=?", s);
                } else {
                    ContentValues values = new ContentValues();
                    values.put("name", s[0]);
                    values.put("time", new Date().getTime());
                    sqLiteNovel.getReadableDatabase().insert("search", null, values);
                }
                cursor.close();
                BaseCrawler crawler = (BaseCrawler) NovelConfigureManager.getConstructor().newInstance(mContext.get(), null);
                return crawler.search(s[0]);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return null;
        }

        /**
         * 搜索完时把数据放入界面
         */
        @Override
        protected void onPostExecute(List<NovelInfo> list) {
            FrameLayout frameLayout = mContext.get().frameLayout;
            if (list != null) {
                if (frameLayout.findViewWithTag("remind") != null) {
                    frameLayout.removeAllViews();
                    frameLayout.addView(mContext.get().search_result);
                }
                // 显示到界面
                mContext.get().resultAdapter.update(list);
                mContext.get().findViewById(R.id.theme).setBackgroundColor(Color.parseColor("#FFFFFF"));
            } else {
                if (frameLayout.findViewWithTag("remind") == null) {
                    frameLayout.removeAllViews();
                    frameLayout.addView(mContext.get().search_remind);
                }
                Toast.makeText(mContext.get(), "搜索失败，网络异常", Toast.LENGTH_SHORT).show();
            }
            mContext.get().searchView.setSubmitButtonEnabled(true);
            mContext.get().refreshLayout.setRefreshing(false);
        }
    }

    static class RemindTask extends AsyncTask<Void, Void, List<NovelRemind>> {
        private WeakReference<SearchActivity> mContext;

        public RemindTask(SearchActivity mContext) {
            this.mContext = new WeakReference<>(mContext);
        }

        @Override
        protected List<NovelRemind> doInBackground(Void... voids) {
            try {
                BaseCrawler crawler = (BaseCrawler) NovelConfigureManager.getConstructor().newInstance(mContext.get(), null);
                return crawler.remind();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<NovelRemind> list) {
            if (list == null) {
                list = new ArrayList<>();
                NovelRemind novelRemind = new NovelRemind();
                novelRemind.setName("网络异常");
                novelRemind.setChapter("点击重试");
                list.add(novelRemind);
            }
            mContext.get().remindAdapter.update(list);
        }
    }

    class RemindAdapter extends RecyclerView.Adapter<RemindAdapter.Holder> implements View.OnClickListener {
        List<NovelRemind> list;

        RemindAdapter(List<NovelRemind> list) {
            this.list = list;
        }

        public void update(List<NovelRemind> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.remind_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(final Holder holder, int i) {
            NovelRemind novelRemind = list.get(i);
            holder.itemView.setTag(i);
            holder.name.setText(novelRemind.getName());
            holder.chapter.setText(novelRemind.getChapter());
            holder.name.setTextColor(configure.getNameTheme());
            holder.chapter.setTextColor(configure.getAuthorTheme());
            if (list.size() == 1) {
                if (list.get(0).getName().equals("网络异常"))
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new RemindTask(SearchActivity.this).execute();
                            v.setOnClickListener(null);
                            ((TextView) v.findViewById(R.id.name)).setText("正在重试");
                            ((TextView) v.findViewById(R.id.chapter)).setText("......");
                        }
                    });
            } else
                holder.itemView.setOnClickListener(this);
        }

        @Override
        public int getItemCount() {
            if (list == null) return 0;
            return list.size();
        }

        @Override
        public void onClick(View v) {
            int pos = (int) v.getTag();
            if (!refreshLayout.isRefreshing())
                searchView.setQuery(list.get(pos).getName(), searchView.isSubmitButtonEnabled());
            else
                Toast.makeText(SearchActivity.this, "搜索紧啊，扑街！", Toast.LENGTH_SHORT).show();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView name, chapter;

            Holder(View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                chapter = itemView.findViewById(R.id.chapter);
            }
        }
    }

    class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.Holder> implements View.OnClickListener {
        List<String> list;

        RecordAdapter(List<String> list) {
            this.list = list;
        }

        public void update(List<String> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.record_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder holder, int i) {
            holder.itemView.setTag(i);
            holder.text.setText(list.get(i));
            holder.text.setTextColor(configure.getNameTheme());
//            holder.delete.setTextColor(configure.getNameTheme());
            holder.itemView.setOnClickListener(this);
        }

        @Override
        public int getItemCount() {
            if (list == null) return 0;
            return list.size();
        }

        @Override
        public void onClick(View v) {
            int pos = (int) v.getTag();
            searchView.setQuery(list.get(pos), searchView.isSubmitButtonEnabled());
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView text, delete;

            Holder(View itemView) {
                super(itemView);
                text = itemView.findViewById(R.id.text);
//                delete = itemView.findViewById(R.id.delete);
            }
        }
    }

    class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.Holder> implements View.OnClickListener {
        List<NovelInfo> list;

        ResultAdapter(List<NovelInfo> list) {
            this.list = list;
        }

        public void update(List<NovelInfo> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(Holder viewHolder, int i) {
            NovelInfo novelInfo = list.get(i);
            viewHolder.itemView.setTag(i);
            viewHolder.itemView.setOnClickListener(this);
            viewHolder.image.setImageBitmap(novelInfo.getBitmap());
            viewHolder.name.setText(novelInfo.getName());
            viewHolder.author.setText(novelInfo.getAuthor());
            viewHolder.chapter.setText(novelInfo.getChapter());
            viewHolder.name.setTextColor(configure.getNameTheme());
            viewHolder.author.setTextColor(configure.getAuthorTheme());
            viewHolder.chapter.setTextColor(configure.getIntroduceTheme());
        }

        @Override
        public int getItemCount() {
            if (list == null) return 0;
            return list.size();
        }

        @Override
        public void onClick(View v) {
            NovelInfo novelInfo = list.get((Integer) v.getTag()).copy();
            Bitmap bitmap = novelInfo.getBitmap();
            Intent intent = new Intent(SearchActivity.this, NovelDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("NovelInfo", novelInfo);
            novelInfo.setBitmap(bitmap);
            intent.putExtras(bundle);
            startActivity(intent);
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView name, author, chapter;

            Holder(View itemView) {
                super(itemView);
                author = itemView.findViewById(R.id.author);
                image = itemView.findViewById(R.id.image);
                chapter = itemView.findViewById(R.id.chapter);
                name = itemView.findViewById(R.id.name);
            }
        }
    }
}
