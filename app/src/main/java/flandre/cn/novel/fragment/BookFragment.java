package flandre.cn.novel.fragment;

import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.service.NovelService;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.R;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.activity.TextActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 小说书架
 * 2019.??
 */
public class BookFragment extends AttachFragment implements SwipeRefreshLayout.OnRefreshListener, NovelService.UpdateNovel {
    public static final String TAG = "BookFragment";
    private BookAdapter bookAdapter;
    private SwipeRefreshLayout refresh;
    private SQLiteNovel sqLiteNovel;
    private List<Integer> hasDelete;

    private TextView empty;

    public SwipeRefreshLayout getRefresh() {
        return refresh;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }

    public void loadData() {
        // 拿到收藏的小说, 填充主界面, 若没有小说, 显示空空如也
        List<NovelInfo> list = SQLTools.getNovelData(sqLiteNovel);
        hasDelete = null;
        if (list.size() > 0) {
            empty.setVisibility(View.GONE);
            bookAdapter.updateAdapter(list);
        } else {
            bookAdapter.updateAdapter(null);
            empty.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(mContext.getApplicationContext());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.book_fragment_layout, container, false);
        empty = view.findViewById(R.id.empty);
        empty.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        refresh = view.findViewById(R.id.refresh);
        refresh.setColorSchemeResources(R.color.blue_dark);
        refresh.setOnRefreshListener(this);
        RecyclerView recyclerView = view.findViewById(R.id.book_main);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayout.VERTICAL);
        recyclerView.setLayoutManager(manager);
        bookAdapter = new BookAdapter(null);
        recyclerView.addItemDecoration(new Decoration(mContext));
        recyclerView.setAdapter(bookAdapter);
        recyclerView.setHasFixedSize(false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onRefresh() {
        ((IndexActivity) mContext).getService().update(NovelService.UPDATE_ALL, this);
    }

    @Override
    public void onUpdateStart() {
        refresh.setRefreshing(true);
        Toast.makeText(mContext, "更新小说中", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpdateFail() {
        Toast.makeText(mContext, "更新紧啊，扑街！", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpdateFinish(int updateFinish, int updateCount, int id) {
        if (updateFinish == updateCount) {
            refresh.setRefreshing(false);
            Toast.makeText(mContext, "更新完毕", Toast.LENGTH_SHORT).show();
            if (updateFinish != -1)
                loadData();
        }
    }

    /**
     * 删除小说后更新界面
     *
     * @param message obj 数据在列表中的位置
     */
    public void deleteBook(Message message) {
        int position, base;
        if (hasDelete == null) hasDelete = new ArrayList<>();
        base = position = (int) message.obj;
        hasDelete.add(position);
        for (Integer p : hasDelete) if (p < base) position--;
        bookAdapter.data.remove(position);
        bookAdapter.notifyItemRemoved(position);
        if (bookAdapter.data.size() == 0) empty.setVisibility(View.VISIBLE);
    }

    public void changeTheme() {
        if (empty == null) return;
        empty.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        bookAdapter.notifyDataSetChanged();
    }

    class BookAdapter extends RecyclerView.Adapter<BookAdapter.Holder> implements View.OnLongClickListener, View.OnClickListener {
        List<NovelInfo> data;

        BookAdapter(List<NovelInfo> data) {
            this.data = data;
        }

        void updateAdapter(List<NovelInfo> list) {
            this.data = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.index_list, viewGroup, false);
            view.setOnClickListener(this);
            view.setOnLongClickListener(this);
            return new Holder(view);
        }

        @Override
        public boolean onLongClick(View v) {
            // 长按底部弹出菜单
            int position, base;
            position = base = (Integer) v.getTag();
            if (hasDelete != null) for (Integer p : hasDelete) if (p < base) position--;
            final NovelInfo novelInfo = data.get(position);
            IndexDialogFragment fragment = IndexDialogFragment.newInstance(novelInfo, base);
            fragment.show(getChildFragmentManager(), "dialog");
            return true;
        }

        @Override
        public void onClick(View v) {
            // 点击开始阅读
            NovelInfo novelInfo = data.get((Integer) v.getTag());
            Intent intent = new Intent(mContext, TextActivity.class);
            Bundle bundle = new Bundle();
            bundle.putString("name", novelInfo.getName());
            bundle.putString("author", novelInfo.getAuthor());
            intent.putExtras(bundle);
            startActivity(intent);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder viewHolder, int i) {
            NovelInfo novelInfo = data.get(i);
            viewHolder.itemView.setTag(i);
            viewHolder.image.setImageBitmap(NovelInfo.getBitmap(novelInfo.getImagePath(), mContext));
            viewHolder.title.setText(novelInfo.getName());
            viewHolder.chapter.setText(novelInfo.getChapter());
            viewHolder.title.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
            viewHolder.chapter.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        }

        @Override
        public int getItemCount() {
            if (data == null) return 0;
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            ImageView image;
            TextView title, chapter;

            Holder(final View itemView) {
                super(itemView);
                image = itemView.findViewById(R.id.image);
                title = itemView.findViewById(R.id.title);
                chapter = itemView.findViewById(R.id.chapter);
                // 点击时进入看小说界面
            }
        }
    }
}
