package flandre.cn.novel.fragment;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.activity.ConfigureSourceActivity;
import flandre.cn.novel.activity.NovelDetailActivity;
import flandre.cn.novel.activity.SearchActivity;
import flandre.cn.novel.crawler.BaseCrawler;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelInfo;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.List;

public class SearchResultFragment extends AttachFragment implements View.OnClickListener {
    private ResultAdapter resultAdapter;
    private View search_result;
    private TextView changeSource;

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        search_result.setVisibility(isVisibleToUser?View.VISIBLE:View.GONE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        search_result = inflater.inflate(R.layout.search_result, container, false);
        search_result.setTag("result");
        changeSource = search_result.findViewById(R.id.changeSource);
        changeSource.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        changeSource.setOnClickListener(this);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        resultAdapter = new ResultAdapter(null);
        RecyclerView result = search_result.findViewById(R.id.result);
        result.setLayoutManager(manager);
        result.setAdapter(resultAdapter);
        result.addItemDecoration(new Decoration(mContext));
        search_result.findViewById(R.id.background).setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        return search_result;
    }

    public void runSearch(String text) {
        new SearchTask(this).execute(text);
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent(mContext, ConfigureSourceActivity.class);
        startActivity(intent);
    }

    static class SearchTask extends AsyncTask<String, Void, List<NovelInfo>> {
        private WeakReference<SearchResultFragment> mFragment;

        SearchTask(SearchResultFragment mFragment) {
            this.mFragment = new WeakReference<>(mFragment);
        }

        /**
         * 搜索小说
         */
        @Override
        protected List<NovelInfo> doInBackground(String... s) {
            try {
                SearchActivity mContext = (SearchActivity) mFragment.get().mContext;
                SQLiteNovel sqLiteNovel = mContext.getSqLiteNovel();
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
                BaseCrawler crawler = (BaseCrawler) NovelConfigureManager.getConstructor().newInstance(mFragment.get().mContext, null);
                return crawler.search(s[0]);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (java.lang.InstantiationException e) {
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
            SearchActivity mContext = (SearchActivity) mFragment.get().mContext;
            // 如果搜索成功显示数据, 如果网络存在问题显示提示页面
            if (list != null) {
                mFragment.get().changeSource.setVisibility(View.GONE);
                if (mContext.getRemindFragment().getUserVisibleHint()) {
                    mContext.getRemindFragment().setUserVisibleHint(false);
                    mContext.getResultFragment().setUserVisibleHint(true);
                }
                if (list.size() == 0){
                    mFragment.get().changeSource.setVisibility(View.VISIBLE);
                }
                // 显示到界面
                mFragment.get().resultAdapter.update(list);
                mContext.findViewById(R.id.theme).setBackgroundColor(Color.parseColor("#FFFFFF"));
            } else {
                if (!mContext.getRemindFragment().getUserVisibleHint()) {
                    mContext.getRemindFragment().setUserVisibleHint(true);
                    mContext.getResultFragment().setUserVisibleHint(false);
                }
                Toast.makeText(mContext, "搜索失败，网络异常", Toast.LENGTH_SHORT).show();
            }
            mContext.getSearchView().setSubmitButtonEnabled(true);
            mContext.getRefreshLayout().setRefreshing(false);
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

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.search_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder viewHolder, int i) {
            NovelInfo novelInfo = list.get(i);
            viewHolder.itemView.setTag(i);
            viewHolder.itemView.setOnClickListener(this);
            viewHolder.image.setImageBitmap(novelInfo.getBitmap());
            viewHolder.name.setText(novelInfo.getName());
            viewHolder.author.setText(novelInfo.getAuthor());
            viewHolder.chapter.setText(novelInfo.getChapter());
            viewHolder.name.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
            viewHolder.author.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
            viewHolder.chapter.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        }

        @Override
        public int getItemCount() {
            if (list == null) return 0;
            return list.size();
        }

        @Override
        public void onClick(View v) {
            NovelInfo novelInfo = list.get((Integer) v.getTag()).copy();
//            Bitmap bitmap = novelInfo.getBitmap();
            Intent intent = new Intent(mContext, NovelDetailActivity.class);
            Bundle bundle = new Bundle();
            bundle.putSerializable("NovelInfo", novelInfo);
//            novelInfo.setBitmap(bitmap);
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
