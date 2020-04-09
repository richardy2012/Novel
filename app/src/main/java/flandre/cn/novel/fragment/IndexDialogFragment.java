package flandre.cn.novel.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import flandre.cn.novel.info.NovelDownloadInfo;
import flandre.cn.novel.info.NovelInfo;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.adapter.PopUpAdapter;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.R;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.activity.NovelDetailActivity;
import flandre.cn.novel.activity.TextActivity;
import flandre.cn.novel.Tools.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * 底部弹出框的界面
 * 2019.??
 */
public class IndexDialogFragment extends AttachDialogFragment implements PopUpAdapter.OnPopUpClickListener {
    private NovelInfo novelInfo;
    private int height;  // 本对话框的高度
    private List<Item> items;
    private SQLiteNovel sqLiteNovel;
    private int position;

    private void addItem(String string, int id) {
        items.add(new Item(string, id));
    }

    /**
     * 初始化方法
     *
     * @param novelInfo 传给Fragment的参数
     * @return IndexDialogFragment对象
     */
    static IndexDialogFragment newInstance(NovelInfo novelInfo, int position) {
        // 把map传输到本对话框上
        IndexDialogFragment fragment = new IndexDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable("NovelInfo", novelInfo);
        bundle.putInt("position", position);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 设置本对话框的Style, 就是这里设置底部弹出
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomDatePickerDialog);
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(mContext.getApplicationContext());
        ((IndexActivity)mContext).addDownloadFinishListener(this);
        unpack();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void unpack() {
        // 拿出传参
        Bundle bundle = getArguments();
        novelInfo = (NovelInfo) bundle.getSerializable("NovelInfo");
        position = bundle.getInt("position");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //设置无标题
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getDialog().getWindow();
        // 设置停留再底部
        WindowManager.LayoutParams params = window.getAttributes();
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        window.requestFeature(Window.FEATURE_NO_TITLE);
        window.setAttributes(params);
        // 设置界面
        View view = inflater.inflate(R.layout.dialog_layout, container, false);

        TextView name = view.findViewById(R.id.name);
        TextView author = view.findViewById(R.id.author);
        name.setText("书名：" + novelInfo.getName());
        author.setText("作者：" + novelInfo.getAuthor());
        name.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
        author.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());

        RecyclerView recyclerView = view.findViewById(R.id.pop_list);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayout.VERTICAL);
        recyclerView.setLayoutManager(manager);
        addItem();
        PopUpAdapter adapter = new PopUpAdapter(items);
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new Decoration(mContext));
        // 拿到弹出框的高度, 一定要在RecycleView的数据填充后才用, 不然RecycleView就不会显示
        height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(0, height);
        height = view.getMeasuredHeight();

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
    }

    private void addItem() {
        items = new ArrayList<>();
        if (NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY) {
            addItem("开始阅读", R.drawable.read_day);
            addItem("更新小说", R.drawable.update_day);
            addItem("缓存全本", R.drawable.download_day);
            addItem("查看详细", R.drawable.read_detail_day);
            addItem("删除小说", R.drawable.delete_day);
        } else {
            addItem("开始阅读", R.drawable.read_night);
            addItem("更新小说", R.drawable.update_night);
            addItem("缓存全本", R.drawable.download_night);
            addItem("查看详细", R.drawable.read_detail_night);
            addItem("删除小说", R.drawable.delete_night);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // 设置本对话框的大小
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, height);
        getDialog().setCanceledOnTouchOutside(true);
    }

    private void startRead() {
        Bundle bundle = new Bundle();
        bundle.putString("author", novelInfo.getAuthor());
        bundle.putString("name", novelInfo.getName());
        Intent intent = new Intent(mContext, TextActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void update() {
        ((IndexActivity)mContext).getService().update(novelInfo.getId(), ((IndexActivity)mContext).getBookFragment());
    }

    private void download() {
        if (((IndexActivity)mContext).getService().download(String.valueOf(novelInfo.getId())))
            Toast.makeText(mContext, "开始下载", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(mContext, "加入下载队列", Toast.LENGTH_SHORT).show();
    }

    private void lookDetail() {
        Bundle bundle = new Bundle();
        NovelInfo novelInfo = this.novelInfo.copy();
        Cursor cursor = sqLiteNovel.getReadableDatabase().query("nc", new String[]{"name"},
                "novel_id=?", new String[]{String.valueOf(novelInfo.getId())}, null, null, null, null);
        cursor.moveToNext();
        novelInfo.setUrl(cursor.getString(0));
        cursor.close();

        novelInfo.setBitmap(BitmapFactory.decodeFile(novelInfo.getImagePath()));

        bundle.putSerializable("NovelInfo", novelInfo);

        Intent intent = new Intent(mContext, NovelDetailActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private void deleteBook() {
        SQLTools.delete(sqLiteNovel, String.valueOf(novelInfo.getId()), ((IndexActivity)mContext).getService(), mContext);
        ((IndexActivity)mContext).getHandler().obtainMessage(0x103, position).sendToTarget();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((IndexActivity)mContext).removeDownloadFinishListener(this);
    }

    @Override
    public void popUpClickListener(View view, int pos) {
        switch (pos) {
            case 0:
                startRead();
                break;
            case 1:
                update();
                break;
            case 2:
                download();
                break;
            case 3:
                lookDetail();
                break;
            case 4:
                deleteBook();
                break;
        }
        dismiss();
    }

    @Override
    public void onDownloadFinish(int downloadFinish, int downloadCount, long downloadId) {
        if (downloadFinish == downloadCount)
            Toast.makeText(mContext, "下载完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadFail(long id) {
        NovelDownloadInfo downloadInfo = SQLTools.getDownloadInfo(sqLiteNovel, "id = ?", new String[]{String.valueOf(id)}, null).get(0);
        Toast.makeText(mContext, downloadInfo.getTable() + " 下载失败，请重试", Toast.LENGTH_SHORT).show();
    }
}
