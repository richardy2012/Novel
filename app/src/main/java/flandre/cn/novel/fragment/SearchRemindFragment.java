package flandre.cn.novel.fragment;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.activity.SearchActivity;
import flandre.cn.novel.crawler.BaseCrawler;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelRemind;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SearchRemindFragment extends AttachFragment {

    private RemindAdapter remindAdapter;
    private RecordAdapter recordAdapter;
    private View search_remind;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        search_remind.setVisibility(isVisibleToUser ? View.VISIBLE : View.GONE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        search_remind = inflater.inflate(R.layout.search_remind, container, false);
        search_remind.setTag("remind");

        ((TextView) search_remind.findViewById(R.id.remind_txt)).setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        ((TextView) search_remind.findViewById(R.id.record_txt)).setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());

        LinearLayoutManager recordManager = new LinearLayoutManager(mContext);
        recordManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recordAdapter = new RecordAdapter(null);
        RecyclerView record = search_remind.findViewById(R.id.record);
        record.setLayoutManager(recordManager);
        record.setAdapter(recordAdapter);

        LinearLayoutManager remindManager = new LinearLayoutManager(mContext);
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
        remind.addItemDecoration(new Decoration(mContext));

        record.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        remind.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        remind.setNestedScrollingEnabled(false);
        remind.setFocusable(false);
        loadData();
        return search_remind;
    }

    /**
     * 给提示页面加载数据
     */
    private void loadData() {
        List<String> list = new ArrayList<>();
        Cursor cursor = SQLiteNovel.getSqLiteNovel(mContext.getApplicationContext()).getReadableDatabase().query("search",
                new String[]{"name"}, null, null, null, null, "-time", "8");
        if (cursor.moveToNext()) {
            do {
                list.add(cursor.getString(0));
            } while (cursor.moveToNext());
        }
        cursor.close();
        recordAdapter.update(list);
        new RemindTask(this).execute();
    }

    static class RemindTask extends AsyncTask<Void, Void, List<NovelRemind>> {
        private WeakReference<SearchRemindFragment> mFragment;

        RemindTask(SearchRemindFragment mFragment) {
            this.mFragment = new WeakReference<>(mFragment);
        }

        @Override
        protected List<NovelRemind> doInBackground(Void... voids) {
            BaseCrawler crawler = NovelConfigureManager.getCrawler(mFragment.get().mContext, null);
            if (crawler == null) return null;
            return crawler.remind();
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
            mFragment.get().remindAdapter.update(list);
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

        @NonNull
        @Override
        public RemindAdapter.Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.remind_list, viewGroup, false);
            return new RemindAdapter.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final RemindAdapter.Holder holder, int i) {
            NovelRemind novelRemind = list.get(i);
            holder.itemView.setTag(i);
            holder.name.setText(novelRemind.getName());
            holder.chapter.setText(novelRemind.getChapter());
            holder.name.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
            holder.chapter.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
            if (list.size() == 1) {
                if (list.get(0).getName().equals("网络异常"))
                    holder.itemView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            new RemindTask(SearchRemindFragment.this).execute();
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
            if (!((SearchActivity) mContext).getRefreshLayout().isRefreshing())
                ((SearchActivity) mContext).getSearchView().setQuery(list.get(pos).getName(),
                        ((SearchActivity) mContext).getSearchView().isSubmitButtonEnabled());
            else
                Toast.makeText(mContext, "搜索紧啊，扑街！", Toast.LENGTH_SHORT).show();
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

        @NonNull
        @Override
        public RecordAdapter.Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.record_list, viewGroup, false);
            return new RecordAdapter.Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecordAdapter.Holder holder, int i) {
            holder.itemView.setTag(i);
            holder.text.setText(list.get(i));
            holder.text.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
//            holder.delete.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
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
            ((SearchActivity) mContext).getSearchView().setQuery(list.get(pos), ((SearchActivity) mContext).getSearchView().isSubmitButtonEnabled());
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
}
