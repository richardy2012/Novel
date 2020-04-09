package flandre.cn.novel.fragment;

import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.info.NovelDownloadInfo;
import flandre.cn.novel.info.NovelTextItem;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelAttr;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.activity.ConfigureThemeActivity;
import flandre.cn.novel.activity.MenuActivity;
import flandre.cn.novel.activity.TextActivity;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.serializable.SelectList;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 点击TextActivity中间时的弹出框
 * 2019.??
 */
public class TextPopupFragment extends AttachFragment implements DownloadDialogFragment.onDownloadListener{
    private TextView textView;
    private TextView download;
    private ImageView imageView;
    private SQLiteNovel sqLiteNovel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(mContext.getApplicationContext());
        ((TextActivity)mContext).addDownloadFinishListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.popup_fragment, container, false);
        download = view.findViewById(R.id.download_progress);
        setupTool(view);
        setupSeekBar(view);
        setupButton(view);
        return view;
    }

    private void setupTool(View view) {
        ImageView back = view.findViewById(R.id.back);
        ImageView list = view.findViewById(R.id.list);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((TextActivity)mContext).finish();
            }
        });
        list.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, MenuActivity.class);
                Bundle bundle = new Bundle();
                if (((TextActivity)mContext).getTable() == null) {
                    List<String> list = new ArrayList<>();
                    for (NovelTextItem textItem : ((TextActivity)mContext).list) {
                        list.add(textItem.getChapter());
                    }
                    SelectList<String> selectList = new SelectList<>();
                    selectList.setList(list);
                    bundle.putSerializable("list", selectList);
                }
                bundle.putString("table", ((TextActivity)mContext).getTable());
                bundle.putInt("chapter", ((TextActivity)mContext).getChapter());
                intent.putExtras(bundle);
                ((TextActivity)mContext).startActivityForResult(intent, ((TextActivity)mContext).MENU_ACTIVITY_RETURN);
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
                Window window = ((TextActivity)mContext).getWindow();
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
                    NovelAttr.changeThemeEnable = true;
                    ((TextActivity)mContext).changeTheme();
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
                fragment.show(getFragmentManager(), "dialog");
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
        if (((TextActivity)mContext).getTable() != null)
            if (((TextActivity)mContext).getService().download(String.valueOf(((TextActivity)mContext).getNovelInfo().getId()), type))
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
        if (((TextActivity)mContext).getService() != null) {
            String text = "正在下载 " + ((TextActivity)mContext).getService().getDownloadTable() + "(" + downloadFinish + "/" + downloadCount + ")";
            download.setText(text);
            download.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((TextActivity)mContext).removeDownloadFinishListener(this);
    }
}
