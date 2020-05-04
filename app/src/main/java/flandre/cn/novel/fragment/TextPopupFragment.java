package flandre.cn.novel.fragment;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.*;
import flandre.cn.novel.Tools.DisplayUtil;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.info.NovelDownloadInfo;
import flandre.cn.novel.info.NovelTextItem;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.activity.ConfigureThemeActivity;
import flandre.cn.novel.activity.MenuActivity;
import flandre.cn.novel.activity.TextActivity;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.serializable.SelectList;
import flandre.cn.novel.view.CircleView;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 点击TextActivity中间时的弹出框
 * 2019.??
 */
public class TextPopupFragment extends AttachFragment implements DownloadDialogFragment.onDownloadListener, View.OnClickListener {
    private TextView textView;
    private TextView download;
    private ImageView imageView;
    private SQLiteNovel sqLiteNovel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(mContext.getApplicationContext());
        ((TextActivity) mContext).addDownloadFinishListener(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.popup_fragment, container, false);
        // 消化好点击事件
        view.findViewById(R.id.top).setOnClickListener(this);
        view.findViewById(R.id.bottom).setOnClickListener(this);
        download = view.findViewById(R.id.download_progress);
        setupTool(view);
        setupSeekBar(view);
        setupButton(view);
        setupRecycleView(view);
        return view;
    }

    private void setupRecycleView(View view) {
        RecyclerView recyclerView = view.findViewById(R.id.themeChoice);
        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(manager);
        NovelConfigure configure = NovelConfigureManager.getConfigure();
        Adapter adapter = new Adapter(configure.getNovelThemes(), configure.getNovelThemePosition());
        recyclerView.setAdapter(adapter);
    }

    private void setupTool(View view) {
        ImageView back = view.findViewById(R.id.back);
        ImageView list = view.findViewById(R.id.list);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextActivity) mContext).finish();
            }
        });
        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MenuActivity.class);
                Bundle bundle = new Bundle();
                if (((TextActivity) mContext).getTable() == null) {
                    List<String> list = new ArrayList<>();
                    for (NovelTextItem textItem : ((TextActivity) mContext).list) {
                        list.add(textItem.getChapter());
                    }
                    SelectList<String> selectList = new SelectList<>();
                    selectList.setList(list);
                    bundle.putSerializable("list", selectList);
                }
                bundle.putString("table", ((TextActivity) mContext).getTable());
                bundle.putInt("chapter", ((TextActivity) mContext).getChapter());
                intent.putExtras(bundle);
                ((TextActivity) mContext).startActivityForResult(intent, ((TextActivity) mContext).MENU_ACTIVITY_RETURN);
            }
        });
    }

    private void setupSeekBar(View view) {
        SeekBar seekBar = view.findViewById(R.id.light);

        Drawable drawable = mContext.getResources().getDrawable(R.drawable.seek_bar_icon);
//        Drawable drawableCompat = DrawableCompat.wrap(drawable);
//        DrawableCompat.setTintList(drawableCompat, ColorStateList.valueOf(NovelConfigureManager.getConfigure().getMainTheme()));
        seekBar.setThumb(drawable);

        ContentResolver contentResolver = mContext.getContentResolver();
        int pro = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 125);
        seekBar.setProgress(pro);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Window window = ((TextActivity) mContext).getWindow();
                WindowManager.LayoutParams lp = window.getAttributes();
                lp.screenBrightness = progress / 255.0f;
                window.setAttributes(lp);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void setupButton(View view) {
        LinearLayout night = view.findViewById(R.id.night);
        LinearLayout buffer = view.findViewById(R.id.buffer);
        LinearLayout setting = view.findViewById(R.id.setting);

        imageView = view.findViewById(R.id.night_img);
        textView = view.findViewById(R.id.night_txt);

        night.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    NovelConfigureManager.changeConfigure();
                    NovelConfigureManager.saveConfigure(NovelConfigureManager.getConfigure(), mContext);
                    if (NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY) {
                        imageView.setBackground(mContext.getResources().getDrawable(R.drawable.sleep));
                        textView.setText("夜间");
                    } else {
                        imageView.setBackground(mContext.getResources().getDrawable(R.drawable.day));
                        textView.setText("日间");
                    }
                    Intent intent = new Intent();
                    intent.setAction(IndexActivity.CHANGE_THEME);
                    mContext.sendBroadcast(intent);
                    ((TextActivity) mContext).changeTheme();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        buffer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadDialogFragment fragment = new DownloadDialogFragment();
                fragment.setListener(TextPopupFragment.this);
                fragment.show(getChildFragmentManager(), "dialog");
            }
        });

        setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, ConfigureThemeActivity.class));
            }
        });
    }

    @Override
    public void onDownload(View v, int type) {
        if (((TextActivity) mContext).getTable() != null)
            if (((TextActivity) mContext).getService().download(String.valueOf(((TextActivity) mContext).getNovelInfo().getId()), type))
                Toast.makeText(mContext, "开始下载", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(mContext, "加入下载队列", Toast.LENGTH_SHORT).show();
        else Toast.makeText(mContext, "请先收藏！", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadFinish(int downloadFinish, int downloadCount, long downloadId) {
        downloadUI(downloadFinish, downloadCount);
        if (downloadFinish == downloadCount)
            Toast.makeText(mContext, "下载完成", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDownloadFail(long id) {
        NovelDownloadInfo downloadInfo = SQLTools.getDownloadInfo(sqLiteNovel, "id = ?", new String[]{String.valueOf(id)}, null).get(0);
        Toast.makeText(mContext, downloadInfo.getTable() + " 下载失败，请重试", Toast.LENGTH_SHORT).show();
    }

    public void downloadUI(int downloadFinish, int downloadCount) {
        if (((TextActivity) mContext).getService() != null) {
            String text = "正在下载 " + ((TextActivity) mContext).getService().getDownloadTable() + "(" + downloadFinish + "/" + downloadCount + ")";
            download.setText(text);
            download.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((TextActivity) mContext).removeDownloadFinishListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private NovelConfigure.NovelTheme[] novelThemes;
        private int nowChoice;

        public Adapter(NovelConfigure.NovelTheme[] novelThemes, int nowChoice) {
            this.novelThemes = novelThemes;
            this.nowChoice = nowChoice;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            CircleView view = new CircleView(mContext);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            view.setLength(DisplayUtil.dip2px(mContext, 35));
            view.setPadding(DisplayUtil.dip2px(mContext, 10));
            view.setLayoutParams(params);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            NovelConfigure.NovelTheme theme = novelThemes[i];
            CircleView circleView = (CircleView) holder.itemView;
            circleView.setTag(i);
            circleView.setColor(Color.parseColor(theme.backgroundColor));
            circleView.setSelected(i == nowChoice);
            circleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    NovelConfigureManager.getConfigure().setNovelThemePosition((Integer) v.getTag());
                    ((TextActivity) mContext).changeTheme();
                    nowChoice = (int) v.getTag();
                    try {
                        NovelConfigureManager.saveConfigure(mContext);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    notifyDataSetChanged();
                }
            });
        }

        @Override
        public int getItemCount() {
            return novelThemes.length;
        }

        class Holder extends RecyclerView.ViewHolder {

            public Holder(@NonNull View itemView) {
                super(itemView);
            }
        }
    }
}
