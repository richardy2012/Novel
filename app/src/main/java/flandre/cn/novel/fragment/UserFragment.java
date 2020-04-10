package flandre.cn.novel.fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.NovelTools;
import flandre.cn.novel.activity.DownloadManagerActivity;
import flandre.cn.novel.activity.LocalMusicActivity;
import flandre.cn.novel.database.SharedTools;
import flandre.cn.novel.parse.FileParse;
import flandre.cn.novel.parse.PathParse;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.adapter.UserAdapter;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.Tools.Item;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户个人中心
 * 2019.??
 */
public class UserFragment extends AttachFragment implements UserAdapter.OnItemClick, FileParse.OnfinishParse {
    public static final String TAG = "UserFragment";
    private LinearLayout top;
    private TextView todayRead;
    private TextView alarmRest;
    private TextView todayIntro;
    private TextView alarmIntro;
    private RecyclerView bottom;
    private View sep;
    private UserAdapter adapter;
    private List<Item> items;
    private String code;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_fragment_layout, container, false);
        // 设置头部的圆形图片
        ImageView imageView = view.findViewById(R.id.image);
        RequestOptions options = RequestOptions.circleCropTransform()
                .diskCacheStrategy(DiskCacheStrategy.NONE)//不做磁盘缓存
                .skipMemoryCache(true);//不做内存缓存
        Glide.with(this).load(R.drawable.flandre).apply(options).into(imageView);

        top = view.findViewById(R.id.image_wrap);
        sep = view.findViewById(R.id.sep);
        bottom = view.findViewById(R.id.bottom);
        todayRead = view.findViewById(R.id.todayRead);
        alarmRest = view.findViewById(R.id.alarmRest);
        todayIntro = view.findViewById(R.id.todayIntro);
        alarmIntro = view.findViewById(R.id.alarmIntro);
        setupRecycle();
        changeTheme();
        return view;
    }

    private void setupRecycle() {
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        bottom.setLayoutManager(manager);
        bottom.addItemDecoration(new Decoration(mContext));
        adapter = new UserAdapter(null);
        bottom.setAdapter(adapter);
        adapter.setOnItemClick(this);
        bottom.setNestedScrollingEnabled(false);
        bottom.setFocusable(false);
    }

    private void addItem() {
        items = new ArrayList<>();
        if (NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY) {
            addItem("加载本地文本", R.drawable.load_text_day);
            addItem("书本下载管理", R.drawable.download_manager_day);
            addItem("本地音乐播放", R.drawable.music_day);
        } else {
            addItem("加载本地文本", R.drawable.load_text_night);
            addItem("书本下载管理", R.drawable.download_manager_night);
            addItem("本地音乐播放", R.drawable.music_night);
        }
        adapter.updateList(items);
    }

    private void addItem(String string, int id) {
        items.add(new Item(string, id));
    }

    public void changeTheme() {
        if (adapter == null) return;
        addItem();
        if (NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY) {
            top.setBackground(mContext.getResources().getDrawable(R.drawable.user_top_day));
        } else {
            top.setBackground(mContext.getResources().getDrawable(R.drawable.user_top_night));
        }
        todayIntro.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        alarmIntro.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        SharedTools sharedTools = new SharedTools(mContext);
        long alarm = sharedTools.getAlarm();
        alarmRest.setText(NovelTools.resolver(alarm == AlarmDialogFragment.NO_ALARM_STATE ? 0 : alarm));
        alarmRest.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        todayRead.setText(NovelTools.resolver(sharedTools.getTodayRead()));
        todayRead.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        sep.setBackgroundColor((~NovelConfigureManager.getConfigure().getBackgroundTheme()) & 0x11FFFFFF | 0x11000000);
    }

    @Override
    public void onClick(int position) {
        Intent intent;
        switch (position) {
            case 0:
                openSystemFile();
                break;
            case 1:
                intent = new Intent(mContext, DownloadManagerActivity.class);
                mContext.startActivity(intent);
                break;
            case 2:
                intent = new Intent(mContext, LocalMusicActivity.class);
                mContext.startActivity(intent);
                break;
        }
    }

    /**
     * 加载本地文件
     */
    private void openSystemFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        //intent.setType(“image/*”);//选择图片
        //intent.setType(“audio/*”); //选择音频
        //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
        //intent.setType(“video/*;image/*”);//同时选择视频和图片
        intent.setType("*/*");//无类型限制
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        ((Activity) mContext).startActivityForResult(intent, 0x2);
    }

    /**
     * 解析本地文本
     */
    public void handleFile(int requestCode, int resultCode, Intent data) {
        PathParse pathParse = new PathParse(mContext);
        if (resultCode == Activity.RESULT_OK) {
            pathParse.parse(data);
            FileParse fileParse = new FileParse(pathParse.getPath(), SQLiteNovel.getSqLiteNovel(mContext.getApplicationContext()), mContext);
            fileParse.setOnfinishParse(this);
            try {
                fileParse.parseFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFinishParse(int mode) {
        if (mode == FileParse.OK)
            ((IndexActivity) mContext).getBookFragment().loadData();
    }
}