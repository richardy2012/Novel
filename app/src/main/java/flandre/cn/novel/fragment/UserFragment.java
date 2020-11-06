package flandre.cn.novel.fragment;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.*;
import flandre.cn.novel.activity.DownloadManagerActivity;
import flandre.cn.novel.activity.LocalMusicActivity;
import flandre.cn.novel.database.SharedTools;
import flandre.cn.novel.parse.FileParse;
import flandre.cn.novel.parse.OnFinishParse;
import flandre.cn.novel.parse.PathParse;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.adapter.UserAdapter;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.Item;
import flandre.cn.novel.parse.ShareFile;
import flandre.cn.novel.view.CircleArcView;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户个人中心
 * 2019.??
 */
public class UserFragment extends AttachFragment implements UserAdapter.OnItemClick, OnFinishParse, View.OnTouchListener {
    public static final String TAG = "UserFragment";
    private FrameLayout top;
    private TextView todayRead;
    private TextView alarmRest;
    private TextView todayIntro;
    private TextView alarmIntro;
        private TextView angle;
    private RecyclerView bottom;
    private ImageView imageView;
    private View sep;
    private UserAdapter adapter;
    private List<Item> items;
    private String code;
    private CircleArcView arcView;
    private Animator animator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.user_fragment_layout, container, false);
        imageView = view.findViewById(R.id.image);
        setupImage();
        arcView = view.findViewById(R.id.circle);
        top = view.findViewById(R.id.image_wrap);
        sep = view.findViewById(R.id.sep);
        bottom = view.findViewById(R.id.bottom);
        todayRead = view.findViewById(R.id.todayRead);
        alarmRest = view.findViewById(R.id.alarmRest);
        todayIntro = view.findViewById(R.id.todayIntro);
        alarmIntro = view.findViewById(R.id.alarmIntro);
        angle = view.findViewById(R.id.angle);
        setupRecycle();
        changeTheme();
        return view;
    }

    private void setupImage() {
        // 设置头部的圆形图片
        RequestOptions options = RequestOptions.circleCropTransform()
                .diskCacheStrategy(DiskCacheStrategy.NONE)//不做磁盘缓存
                .skipMemoryCache(true);//不做内存缓存
        String path;
        File file;
        if ((path = SharedTools.getHeadImagePath(mContext)) != null && (file = new File(path)).exists()) {
            Glide.with(this).load(file).apply(options).into(imageView);
        } else {
            Glide.with(this).load(R.drawable.flandre).apply(options).into(imageView);
        }
        imageView.setOnTouchListener(this);
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
        sharedTools.setTodayRead(0);
        long alarm = sharedTools.getAlarm();
        alarmRest.setText(NovelTools.resolver(alarm == AlarmDialogFragment.NO_ALARM_STATE ? 0 : alarm));
        alarmRest.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        todayRead.setText(NovelTools.resolver(sharedTools.getTodayRead()));
        todayRead.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
        sep.setBackgroundColor((~NovelConfigureManager.getConfigure().getBackgroundTheme()) & 0x11FFFFFF | 0x11000000);
        arcView.setColor(NovelConfigureManager.getConfigure().getMainTheme());
        angle.setTextColor(0x88ffffff & NovelConfigureManager.getConfigure().getTextColor());
    }

    @Override
    public void onClick(int position) {
        Intent intent;
        switch (position) {
            case 0:
                openSystemFile(0x2, new Operation() {
                    @Override
                    public void operation(Intent intent) {
                        //intent.setType(“image/*”);//选择图片
                        //intent.setType(“audio/*”); //选择音频
                        //intent.setType(“video/*”); //选择视频 （mp4 3gp 是android支持的视频格式）
                        //intent.setType(“video/*;image/*”);//同时选择视频和图片
                        intent.setType("*/*");//无类型限制
                    }
                });
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
    private void openSystemFile(int result, Operation operation) {
        SharedTools sharedTools = new SharedTools(mContext);
        if (!sharedTools.getMusicEnable()) {
            Toast.makeText(mContext, "我们需要权限才能加载本地小说", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        operation.operation(intent);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        ((Activity) mContext).startActivityForResult(intent, result);
    }

    /**
     * 解析本地文本
     */
    public void handleFile(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            PathParse pathParse = new PathParse(mContext);
            pathParse.parse(data);
            String path = pathParse.getPath();
//            String p = data.getData().getPath();
//            AlertDialog.Builder b = new AlertDialog.Builder(mContext);
//            b.setTitle(data.getData().getScheme());
//            b.setMessage(p + " " + path + " " + (new File(path).exists() ? "true" : "false"));
//            b.setNegativeButton("确定", null);
//            b.setPositiveButton("取消", null);
//            b.create();//创建
//            b.show();
//            return;
            if (requestCode == 0x2) {  // 处理本地txt导入
                new ShareFile(path, mContext).setOnfinishParse(this).parseFile(((IndexActivity) mContext).getBookFragment().getRefresh());
            } else if (requestCode == 0x3) {  // 处理头像设置
                File file = new File(path);
                if (!(file.exists() && file.canRead())) {  // 选中的图片不存在或不能读时
                    Toast.makeText(mContext, "图片不可用！", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 把选中的头像复制一份到app内部路劲
                try {
                    File out = new File(mContext.getExternalFilesDir(null), "img");
                    if (!file.exists()) file.mkdir();
                    out = new File(out, file.getName());
                    byte[] bytes = new byte[1024];
                    int length;
                    FileInputStream fileInputStream = new FileInputStream(file);
                    FileOutputStream fileOutputStream = new FileOutputStream(out);
                    while ((length = fileInputStream.read(bytes)) != -1) {
                        fileOutputStream.write(bytes, 0, length);
                    }
                    SharedTools.setHeadImagePath(mContext, out.getAbsolutePath());
                    setupImage();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mContext, "图片读取错误！", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onFinishParse(int mode) {
        ((IndexActivity) mContext).getBookFragment().getRefresh().setRefreshing(false);
        if (mode == OnFinishParse.OK)
            ((IndexActivity) mContext).getBookFragment().loadData();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (animator != null) animator.cancel();
                animator = ObjectAnimator.ofFloat(arcView, "angle", arcView.getAngle(), 360);
                animator.setDuration((int) ((360 - arcView.getAngle()) / 360 * 1000));
                animator.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        angle.setText("" + arcView.getAngle());
                        if (arcView.getAngle() != 360) return;
                        angle.setText("");
                        openSystemFile(0x3, new Operation() {
                            @Override
                            public void operation(Intent intent) {
                                intent.setType("image/*");//选择图片
                            }
                        });
                        arcView.setAngle(0);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) {

                    }

                    @Override
                    public void onAnimationRepeat(Animator animation) {

                    }
                });
                animator.start();
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP:
                if (animator != null) animator.cancel();
                if (arcView.getAngle() == 0) break;
                animator = ObjectAnimator.ofFloat(arcView, "angle", arcView.getAngle(), 0);
                animator.setDuration((int) (arcView.getAngle() / 360 * 500));
                animator.start();
                break;
        }
        return true;
    }

    interface Operation {
        void operation(Intent intent);
    }
}
