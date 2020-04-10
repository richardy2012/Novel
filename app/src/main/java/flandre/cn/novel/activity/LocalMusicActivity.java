package flandre.cn.novel.activity;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import flandre.cn.novel.MusicAidlInterface;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.PermissionManager;
import flandre.cn.novel.database.SharedTools;
import flandre.cn.novel.fragment.MusicControlerFragment;
import flandre.cn.novel.fragment.MusicDialogFragment;
import flandre.cn.novel.info.MusicInfo;
import flandre.cn.novel.Tools.Decoration;
import flandre.cn.novel.service.PlayMusicService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static flandre.cn.novel.Tools.PermissionManager.*;
import static flandre.cn.novel.service.PlayMusicService.music_pos;

/**
 * 本地音乐播放
 * 2020.4.1
 */
public class LocalMusicActivity extends BaseActivity {
    private Adapter mAdapter;
    private MusicDialogFragment mDialogFragment;
    private Map<Long, MusicInfo> musicData;  // 所有的歌曲
    private MusicControlerFragment musicControlerFragment;  // 歌曲的控制组件
    private long nowSongId = -1;  // 当前播放的歌曲ID
    private RelativeLayout relativeLayout;
    private TextView permission;

    public MusicAidlInterface getMusicService() {
        return musicService;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * 检查是否有播放音乐的权限
     */
    private void checkPermission() {
        // 当版本达到23时, 需要向用户申请权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE_CODE);
        } else {
            SharedTools sharedTools = new SharedTools(this);
            sharedTools.setMusicEnable(true);
            setupMusicService();
            loadData();
        }
    }

    private void checkPermission(final String permission, final int code) {
        final PermissionManager manager = new PermissionManager(this);
        // 如果已经具有权限直接跑业务逻辑
        if (manager.checkPermission(permission)) {
            this.permission.setVisibility(View.GONE);
            setupMusicService();
            loadData();
        } else if (manager.shouldShowRequestPermissionRationale(permission)) {
            this.permission.setVisibility(View.VISIBLE);
            this.permission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 如果是第二次申请权限, 向用户表名权限的用途
                    Snackbar snackbar = Snackbar.make(relativeLayout, CODE_INFO.get(code),
                            Snackbar.LENGTH_INDEFINITE)
                            .setAction(android.R.string.ok, new View.OnClickListener() {  // 点击确定时询问
                                @Override
                                public void onClick(View view) {
                                    manager.askPermission(permission, code);
                                }
                            });
                    View view = snackbar.getView();
                    NovelConfigure configure = NovelConfigureManager.getConfigure(getApplicationContext());
                    ((TextView) view.findViewById(R.id.snackbar_text)).setTextColor(configure.getAuthorTheme());
                    view.setBackgroundColor(configure.getMainTheme());
                    ((TextView) view.findViewById(R.id.snackbar_action)).setTextColor(configure.getAuthorTheme());
                    snackbar.show();
                }
            });
        } else {
            // 第一次可以直接申请权限
            this.permission.setVisibility(View.VISIBLE);
            this.permission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    manager.askPermission(permission, code);
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_music);
        NovelConfigureManager.getConfigure(getApplicationContext());

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("本地音乐播放");
        actionBar.setBackgroundDrawable(new ColorDrawable(NovelConfigureManager.getConfigure().getMainTheme()));
        actionBar.setDisplayHomeAsUpEnabled(true);
        relativeLayout = findViewById(R.id.total);
        relativeLayout.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        permission = findViewById(R.id.permission);
        findViewById(R.id.control).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Intent intent = new Intent();
                intent.setAction(PlayMusicService.NOTIFICATION_CHANGE);
                sendBroadcast(intent);
                return true;
            }
        });
        if (savedInstanceState == null) {
            musicControlerFragment = new MusicControlerFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.control, musicControlerFragment, "ControlFragment").commit();
        }else musicControlerFragment = (MusicControlerFragment) getSupportFragmentManager().findFragmentByTag("ControlFragment");
        mDialogFragment = new MusicDialogFragment();

        setupRecycle();
        addMusicListener(this);
        checkPermission();
    }

    private void setupRecycle() {
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        RecyclerView recyclerView = findViewById(R.id.music);
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new Decoration(this));
        recyclerView.setHasFixedSize(false);
        mAdapter = new Adapter(null);
        recyclerView.setAdapter(mAdapter);
    }

    /**
     * 加载音乐数据
     */
    private void loadData() {
        musicData = new HashMap<>();
        List<MusicInfo> list = new ArrayList<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(uri, music_pos, "title != '' and _size > 1048576 and duration > 60000",
                null, "title_key");
        while (cursor.moveToNext()) {
            if (!cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)).endsWith("mp3")) continue;
            MusicInfo musicInfo = new MusicInfo();
            musicInfo.setSongId(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            musicInfo.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
            musicInfo.setName(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            musicInfo.setSinger(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            musicInfo.setData(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
            list.add(musicInfo);
            musicData.put(musicInfo.getSongId(), musicInfo);
        }
        cursor.close();
        mAdapter.updateData(list);
    }

    @Override
    void onServiceConnected(int service) {
        if (service == BaseActivity.MUSIC_SERVICE_CONNECTED) {
            // 当服务连接时, 为音乐控制组件设置数据
            musicControlerFragment.setData();
            if (!musicControlerFragment.isProgressChanging()) {
                musicControlerFragment.setProgressChanging(true);
                musicControlerFragment.updateProgress();
            }
            try {
                MusicInfo musicInfo = musicService.getPlayInfo();
                if (musicInfo != null) {
                    nowSongId = musicInfo.getSongId();
                    musicData.get(musicInfo.getSongId()).setPlaying(true);
                    mAdapter.notifyDataSetChanged();
                    mDialogFragment.adapterUpdate();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.music_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.list:
                if (musicService != null) {
                    try {
                        long[] queue = musicService.getPlayQueue();
                        List<MusicInfo> musicInfos = new ArrayList<>();
                        for (long l : queue) {
                            MusicInfo musicInfo = musicData.get(l);
                            if (musicInfo != null)
                                musicInfos.add(musicData.get(l));
                        }
                        mDialogFragment.setInfos(musicInfos);
                        mDialogFragment.show(getSupportFragmentManager(), "MusicDialog");
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
//        return super.onOptionsItemSelected(item);
        return true;
    }

    @Override
    public void onNextSong() {
        if (nowSongId != -1 && musicService != null) {
            try {
                MusicInfo musicInfo = musicService.getPlayInfo();
                if (musicInfo != null) {
                    musicData.get(nowSongId).setPlaying(false);
                    musicData.get(musicInfo.getSongId()).setPlaying(true);
                    nowSongId = musicInfo.getSongId();
                    mAdapter.notifyDataSetChanged();
                    mDialogFragment.adapterUpdate();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClearPlayList() {
        if (nowSongId != -1) {
            musicData.get(nowSongId).setPlaying(false);
            mAdapter.notifyDataSetChanged();
            mDialogFragment.adapterUpdate();
            nowSongId = -1;
        }
    }

    @Override
    public void onPlayMusic() {
        if (nowSongId != -1)
            musicData.get(nowSongId).setPlaying(false);
        if (musicService != null) {
            try {
                MusicInfo musicInfo = musicService.getPlayInfo();
                if (musicInfo != null) {
                    nowSongId = musicInfo.getSongId();
                    musicData.get(musicInfo.getSongId()).setPlaying(true);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mAdapter.notifyDataSetChanged();
        mDialogFragment.adapterUpdate();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
//        Intent intent = getParentActivityIntent();
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case READ_EXTERNAL_STORAGE_CODE:
                // 如果没有权限, 则关闭该功能
                if (!(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    SharedTools sharedTools = new SharedTools(this);
                    sharedTools.setMusicEnable(false);
                    Toast.makeText(this, "我们需要读取权限才可以使用此功能", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    SharedTools sharedTools = new SharedTools(this);
                    sharedTools.setMusicEnable(true);
                    this.permission.setVisibility(View.GONE);
                    setupMusicService();
                    loadData();
                }
                break;
            case ACCESS_NOTIFICATION_POLICY_CODE:
                break;
        }
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        List<MusicInfo> data;

        public Adapter(List<MusicInfo> data) {
            this.data = data;
        }

        public void updateData(List<MusicInfo> data) {
            this.data = data;
            this.notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.music_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            MusicInfo musicInfo = data.get(i);
            holder.itemView.setTag(holder);
            holder.control.setTag(holder);
            holder.name.setText(musicInfo.getName());
            holder.singer.setText(musicInfo.getSinger());
            holder.control.setImageResource(NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY ?
                    R.drawable.more_day : R.drawable.more_night);
            holder.name.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
            holder.singer.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
            holder.itemView.findViewById(R.id.isPlaying).setBackgroundColor(NovelConfigureManager.getConfigure().getMainTheme());
            holder.itemView.findViewById(R.id.isPlaying).setVisibility(musicInfo.isPlaying() ? View.VISIBLE : View.GONE);
            setListener(holder);
        }

        private void setListener(Holder holder) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = ((Holder) v.getTag()).getAdapterPosition();
                    if (position < 0 || position >= data.size()) return;
                    long[] queue = new long[data.size()];
                    for (int i = 0; i < data.size(); i++) {
                        queue[i] = data.get(i).getSongId();
                    }
                    try {
                        long[] leak = musicService.setPlayQueue(queue);
                        if (leak != null) {
                            for (long l : leak)
                                musicService.addPlayInfo(l, musicData.get(l));
                        }
                        musicService.playTarget(data.get(position).getSongId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

            holder.control.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = ((Holder) v.getTag()).getAdapterPosition();
                    if (position < 0 || position >= data.size()) return;
                    AlertDialog.Builder musicDialog = new AlertDialog.Builder(LocalMusicActivity.this);
                    final MusicInfo musicInfo = data.get(position);
                    musicDialog.setTitle(musicInfo.getName());
                    String[] items = new String[]{"下一首播放", "删除(包括本地)"};
                    musicDialog.setItems(items, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case 0:
                                    try {
                                        if (!musicService.addPlayQueue(musicInfo.getSongId())){
                                            musicService.addPlayInfo(musicInfo.getSongId(), musicInfo);
                                        }
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case 1:
                                    try {
                                        musicService.deletePlayQueue(musicInfo.getSongId());
                                        Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicInfo.getSongId());
                                        getContentResolver().delete(uri, null, null);
                                    } catch (RemoteException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                            }
                        }
                    });
                    musicDialog.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            if (data == null) return 0;
            return data.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView name;
            TextView singer;
            ImageView control;

            public Holder(@NonNull View itemView) {
                super(itemView);
                name = itemView.findViewById(R.id.name);
                singer = itemView.findViewById(R.id.singer);
                control = itemView.findViewById(R.id.control);
            }
        }
    }
}
