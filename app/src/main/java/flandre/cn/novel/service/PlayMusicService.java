package flandre.cn.novel.service;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.support.v4.app.NotificationCompat;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.widget.RemoteViews;
import flandre.cn.novel.MusicAidlInterface;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelTools;
import flandre.cn.novel.database.SharedTools;
import flandre.cn.novel.info.MusicInfo;
import flandre.cn.novel.info.MusicSaveData;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;

public class PlayMusicService extends Service {
    // 音乐控件被点击
    public static final String PROGRESS_CHANGE = "flandre.cn.novel.progresschange";
    public static final String MUSIC_NEXT = "flandre.cn.novel.musicnext";
    public static final String MUSIC_LAST = "flandre.cn.novel.musiclast";
    public static final String MUSIC_PLAY = "flandre.cn.novel.musicplay";
    public static final String MUSIC_PAUSE = "flandre.cn.novel.musicpause";
    public static final String CLEAR_PLAY_LIST = "flandre.cn.novel.clearplaylist";

    // 通知栏被点击
    private static final String NOTIFICATION_NEXT = "flandre.cn.novel.notificationnext";
    private static final String NOTIFICATION_LAST = "flandre.cn.novel.notificationlast";
    private static final String NOTIFICATION_PLAY_PAUSE = "flandre.cn.novel.notificationplaypause";
    private static final String NOTIFICATION_CLOSE = "flandre.cn.novel.notificationclose";
    public static final String NOTIFICATION_CHANGE = "flandre.cn.novel.notificationchange";

    public static final int STATUS_ONE_LOOPING = 0;  // 单曲循环
    public static final int STATUS_ALL_LOOPING = 1;  // 循环播放
    public static final int STATUS_ALL_RANDOM = 2;  // 随机播放

    private static final int NOTIFICATION_WAIT = -1;  // 通知栏为未创建
    private static final int NOTIFICATION_CREATE = 0;  // 创建通知栏
    private static final int NOTIFICATION_UPDATE = 1;  // 更新通知栏
    private static final int NOTIFICATION_CANCEL = 2;  // 清除通知栏

    private static final int NOTIFICATION_ID = 0x495;  // 通知栏ID

    public static final String[] STATUS = new String[]{"单曲循环", "列表循环", "随机播放"};

    public static String[] music_pos = new String[]{
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION};

    private IBinder mBinder;
    private List<Long> saveList;  // 用于保存的歌曲ID
    private List<Long> playList;  // 用于播放的歌曲ID
    private Map<Long, MusicInfo> playSongs;  // 播放的歌曲
    private int playPosition = 0;  // 当前播放的位置
    private int playStatus = PlayMusicService.STATUS_ALL_LOOPING;  // 当前播放的状态
    private MusicPlay musicPlay;  // 音乐播放者
    private Notification mNotification;  // 通知栏
    private NotificationManager mNotificationManager;  // 通知栏管理
    private boolean isShowNotification = false;  // 是否显示了通知栏
    private int mode = -1;  // 通知栏的当前状态
    private Receiver receiver = null;  // 广播的接收者
    private boolean isContinuePlay = false;  // 电话结束后是否继续播放音乐
    private PhoneListener phoneListener;

    @Override
    public void onCreate() {
        super.onCreate();
        // 设置音乐的数据
        mBinder = new ServiceStub(this);
        playList = new ArrayList<>();
        saveList = new ArrayList<>();
        musicPlay = new MusicPlay(this);
        loadData();
        // 设置好通知栏和广播
        receiver = new Receiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(PlayMusicService.NOTIFICATION_NEXT);
        filter.addAction(PlayMusicService.NOTIFICATION_LAST);
        filter.addAction(PlayMusicService.NOTIFICATION_PLAY_PAUSE);
        filter.addAction(PlayMusicService.NOTIFICATION_CLOSE);
        filter.addAction(PlayMusicService.NOTIFICATION_CHANGE);
        registerReceiver(receiver, filter);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (isShowNotification) setNotification();
        // 设置好电话接收器
        phoneListener = new PhoneListener();
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    /**
     * 设置通知栏
     */
    private void setNotification() {
        switch (mode) {
            case PlayMusicService.NOTIFICATION_WAIT:
            case PlayMusicService.NOTIFICATION_CANCEL:
                changeNotification(PlayMusicService.NOTIFICATION_CREATE);
                break;
            case PlayMusicService.NOTIFICATION_CREATE:
            case PlayMusicService.NOTIFICATION_UPDATE:
                changeNotification(PlayMusicService.NOTIFICATION_UPDATE);
                break;
        }
        isShowNotification = true;
    }

    /**
     * 改变通知栏的状态
     * @param mode
     */
    private void changeNotification(int mode) {
        switch (mode) {
            case PlayMusicService.NOTIFICATION_CREATE:  // 创建通知栏
                startForeground(NOTIFICATION_ID, getNotification());
//                mNotificationManager.notify(NOTIFICATION_ID, getNotification());
                break;
            case PlayMusicService.NOTIFICATION_UPDATE:  // 更新通知栏
                mNotificationManager.notify(NOTIFICATION_ID, getNotification());
                break;
            case PlayMusicService.NOTIFICATION_CANCEL:  // 删除通知栏
//                mNotificationManager.cancel(NOTIFICATION_ID);
                stopForeground(true);
                isShowNotification = false;
                break;
        }
        this.mode = mode;
    }

    /**
     * 拿到通知栏对象
     */
    private Notification getNotification() {
        if (mNotification == null) {
            // 当版本大于26时要设置channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                CharSequence name = "Novel Music";
                String description = "Looking Book And Listen This Music ：)";
                int importance = NotificationManager.IMPORTANCE_MIN;
                NotificationChannel mChannel = new NotificationChannel(String.valueOf(NOTIFICATION_ID), name, importance);
                mChannel.setDescription(description);
                mChannel.setLightColor(Color.RED);
                mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
                mNotificationManager.createNotificationChannel(mChannel);
            }
            MusicInfo musicInfo = playSongs.get(playList.get(playPosition));
            RemoteViews remoteViews = new RemoteViews(this.getPackageName(), R.layout.natification_layout);
            remoteViews.setTextViewText(R.id.name, musicInfo.getName());
            remoteViews.setTextViewText(R.id.singer, musicInfo.getSinger());

            Intent playPauseIntent = new Intent(PlayMusicService.NOTIFICATION_PLAY_PAUSE);
            PendingIntent pausePIntent = PendingIntent.getBroadcast(this, 0, playPauseIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.control, pausePIntent);

            Intent nextIntent = new Intent(PlayMusicService.NOTIFICATION_NEXT);
            PendingIntent nextPIntent = PendingIntent.getBroadcast(this, 0, nextIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.next, nextPIntent);

            Intent lastIntent = new Intent(PlayMusicService.NOTIFICATION_LAST);
            PendingIntent lastPIntent = PendingIntent.getBroadcast(this, 0, lastIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.last, lastPIntent);

            Intent closeIntent = new Intent(PlayMusicService.NOTIFICATION_CLOSE);
            PendingIntent closePIntent = PendingIntent.getBroadcast(this, 0, closeIntent, 0);
            remoteViews.setOnClickPendingIntent(R.id.close, closePIntent);
            // 通知栏是否为黑色背景, 根据这个设置主题
            boolean isDarkTheme = NovelTools.isDarkNotificationTheme(this, remoteViews.getLayoutId());
            if (isDarkTheme) {
                remoteViews.setTextColor(R.id.name, 0xAAFFFFFF);
                remoteViews.setTextColor(R.id.singer, 0xAAFFFFFF);
                remoteViews.setImageViewResource(R.id.control, isPlaying() ? R.drawable.pause_night : R.drawable.play_night);
                remoteViews.setImageViewResource(R.id.next, R.drawable.next_music_night);
                remoteViews.setImageViewResource(R.id.last, R.drawable.last_music_night);
                remoteViews.setImageViewResource(R.id.close, R.drawable.remove_night);
            }else {
                remoteViews.setTextColor(R.id.name, 0xAA000000);
                remoteViews.setTextColor(R.id.singer, 0xAA000000);
                remoteViews.setImageViewResource(R.id.control, isPlaying() ? R.drawable.pause_day : R.drawable.play_day);
                remoteViews.setImageViewResource(R.id.next, R.drawable.next_music_day);
                remoteViews.setImageViewResource(R.id.last, R.drawable.last_music_day);
                remoteViews.setImageViewResource(R.id.close, R.drawable.remove_day);
            }

            final Intent nowPlayingIntent = new Intent();
            nowPlayingIntent.setComponent(new ComponentName("flandre.cn.novel", "flandre.cn.novel.activity.LocalMusicActivity"));
            nowPlayingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            PendingIntent clickIntent = PendingIntent.getBroadcast(this, 0, nowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            PendingIntent click = PendingIntent.getActivity(this, 0, nowPlayingIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, String.valueOf(NOTIFICATION_ID))
                    .setContent(remoteViews)
                    .setSmallIcon(R.mipmap.main)
                    .setOngoing(false)
                    .setVisibility(Notification.VISIBILITY_PUBLIC)
                    .setDefaults(Notification.DEFAULT_LIGHTS)
                    .setContentIntent(click)
                    .setWhen(System.currentTimeMillis());
            mNotification = builder.build();
            mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        } else {
            boolean isDarkTheme = NovelTools.isDarkNotificationTheme(this, mNotification.contentView.getLayoutId());
            MusicInfo musicInfo = playSongs.get(playList.get(playPosition));
            mNotification.contentView.setTextViewText(R.id.name, musicInfo.getName());
            mNotification.contentView.setTextViewText(R.id.singer, musicInfo.getSinger());
            if (isDarkTheme) {
                mNotification.contentView.setTextColor(R.id.name, 0xAAFFFFFF);
                mNotification.contentView.setTextColor(R.id.singer, 0xAAFFFFFF);
                mNotification.contentView.setImageViewResource(R.id.control, isPlaying() ? R.drawable.pause_night : R.drawable.play_night);
                mNotification.contentView.setImageViewResource(R.id.next, R.drawable.next_music_night);
                mNotification.contentView.setImageViewResource(R.id.last, R.drawable.last_music_night);
                mNotification.contentView.setImageViewResource(R.id.close, R.drawable.remove_night);
            }else {
                mNotification.contentView.setTextColor(R.id.name, 0xAA000000);
                mNotification.contentView.setTextColor(R.id.singer, 0xAA000000);
                mNotification.contentView.setImageViewResource(R.id.control, isPlaying() ? R.drawable.pause_day : R.drawable.play_day);
                mNotification.contentView.setImageViewResource(R.id.next, R.drawable.next_music_day);
                mNotification.contentView.setImageViewResource(R.id.last, R.drawable.last_music_day);
                mNotification.contentView.setImageViewResource(R.id.close, R.drawable.remove_day);
            }
        }
        return mNotification;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * 保存当前的音乐数据
     */
    private void saveData() {
        String saveList = null;
        // 把数组转换成字符串
        // [12314, 43548, 787952] => 12314 43548 787952
        if (this.saveList.size() > 0) {
            StringBuilder builder = new StringBuilder();
            for (long l : this.saveList) {
                builder.append(l).append(" ");
            }
            builder.substring(0, builder.length() - 2);
            saveList = builder.toString();
        }
        // 保存当前信息
        MusicSaveData musicSaveData = new MusicSaveData(saveList, playList.get(playPosition), playStatus, musicPlay.getCurrentPosition(), isShowNotification);
        SharedTools sharedTools = new SharedTools(this);
        sharedTools.saveMusic(musicSaveData);
    }

    /**
     * 加载音乐数据
     */
    private void loadData() {
        // 加载音乐的歌曲信息
        playSongs = new HashMap<>();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(uri, music_pos, "title != '' and _size > 1048576 and duration > 60000",
                null, "title_key");
        while (cursor.moveToNext()) {
            if (!cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)).endsWith("mp3")) continue;
            MusicInfo musicInfo = new MusicInfo();
            musicInfo.setDuration(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)));
            musicInfo.setSongId(cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media._ID)));
            musicInfo.setSinger(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
            musicInfo.setName(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
            musicInfo.setData(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)));
            playSongs.put(musicInfo.getSongId(), musicInfo);
        }
        cursor.close();
        // 加载音乐的播放信息
        SharedTools sharedTools = new SharedTools(this);
        MusicSaveData musicSaveData = sharedTools.getMusic();
        if (musicSaveData.getSaveList() != null) {
            String[] saveList = musicSaveData.getSaveList().split(" ");
            if (saveList.length > 0) {
                for (String list : saveList) {
                    this.saveList.add(Long.valueOf(list));
                }
                upsetOrder();
                playPosition = (musicSaveData.getSongId() != -1 ? playList.indexOf(musicSaveData.getSongId()) : 0);
                playStatus = musicSaveData.getPlayStatus();
                musicPlay.preparePlayer();
                musicPlay.setCurrentPosition(musicSaveData.getCurrent());
                isShowNotification = musicSaveData.isShowNotification();
            }
        }
    }

    /**
     * 播放列表是否为空
     */
    private boolean playQueueIsEmpty() {
        return playList == null || playList.size() == 0;
    }

    /**
     * @return 当前播放歌曲的信息
     */
    private MusicInfo getPlayInfo() {
        if (playList.size() > 0) {
            while (playList.size() > 0) {
                playPosition = playPosition % playList.size();
                MusicInfo musicInfo = playSongs.get(playList.get(playPosition));
                if (musicInfo == null || !new File(musicInfo.getData()).exists()) {
                    long id = playList.get(playPosition);
                    playSongs.remove(id);
                    playList.remove(id);
                    saveList.remove(id);
                } else {
                    return musicInfo;
                }
            }
        }
        return null;
    }

    /**
     * 设置歌曲信息
     */
    private void setPlayInfo(Map<Long, MusicInfo> infos) {
        this.playSongs = infos;
    }

    /**
     * 添加歌曲信息
     */
    private void addPlayInfo(long id, MusicInfo info) {
        if (playSongs.get(id) == null) {
            playSongs.put(id, info);
        }
    }

    /**
     * 设置播放列表
     */
    private long[] setPlayQueue(long[] queue) {
        saveList.clear();
        List<Long> longs = new ArrayList<>();
        for (long l : queue) {
            saveList.add(l);
            if (playSongs.get(l) == null) longs.add(l);
        }
        if (saveList.size() > 0)
            upsetOrder();
        if (longs.size() == 0) return null;
        return toLongArray(longs);
    }

    /**
     * 把Long类型的List转换成Array
     */
    private long[] toLongArray(List<Long> longs) {
        long[] lack = new long[longs.size()];
        for (int i = 0; i < longs.size(); i++) {
            lack[i] = longs.get(i);
        }
        return lack;
    }

    /**
     * 添加一首歌到播放列表
     */
    private boolean addPlayQueue(long id) {
        if (!playList.contains(id)) {
            if (playList.size() == 0) {
                saveList.add(id);
                playList.add(id);
                playTarget(id);
            } else {
                long songId = playList.get(playPosition);
                saveList.add(saveList.indexOf(songId) + 1, id);
                playList.add(playPosition + 1, id);
            }
            return playSongs.containsKey(id);
        }
        return true;
    }

    /**
     * 从播放列表删除一首歌
     */
    private void deletePlayQueue(long id) {
        if (id == playList.get(playPosition)) {
            if (saveList.size() == 1) {
                musicPlay.pause();
                changeNotification(PlayMusicService.NOTIFICATION_CANCEL);
                Intent intent = new Intent();
                intent.setAction(PlayMusicService.CLEAR_PLAY_LIST);
                sendBroadcast(intent);
            } else {
                int index = (saveList.indexOf(id) + 1) % saveList.size();
                playPosition = playList.indexOf(saveList.get(index)) - 1;
                if (playPosition < 0) playPosition = playList.size() - 1;
                nextMusic();
            }
        }
        saveList.remove(id);
        playList.remove(id);
    }

    /**
     * 清空播放列表
     */
    private void deleteAllPlayQueue() {
        saveList.clear();
        playList.clear();
        musicPlay.pause();
        changeNotification(PlayMusicService.NOTIFICATION_CANCEL);
        Intent intent = new Intent();
        intent.setAction(PlayMusicService.CLEAR_PLAY_LIST);
        sendBroadcast(intent);
    }

    /**
     * 返回播放列表
     */
    private long[] getPlayQueue() {
        return toLongArray(saveList);
    }

    /**
     * 按照当前的播放模式重组playList
     */
    private void upsetOrder() {
        switch (playStatus) {
            case PlayMusicService.STATUS_ONE_LOOPING:
            case PlayMusicService.STATUS_ALL_LOOPING:
                playList.clear();
                playList.addAll(saveList);
                break;
            case PlayMusicService.STATUS_ALL_RANDOM:
                playList.clear();
                playList.addAll(saveList);
                Collections.shuffle(playList);
                break;
        }
    }

    /**
     * 设置播放模式
     */
    private void setPlayOrder(int status) {
        playStatus = status;
        upsetOrder();
    }

    /**
     * 返回播放模式
     */
    private int getPlayOrder() {
        return playStatus;
    }

    /**
     * 是否正在播放
     */
    private boolean isPlaying() {
        if (saveList.size() == 0) return false;
        try {
            return musicPlay.isPlaying();
        } catch (RuntimeException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 播放目标歌曲
     */
    private void playTarget(long id) {
        playPosition = playList.indexOf(id);
        if (musicPlay.preparePlayer()) {
            musicPlay.start();
            setNotification();
            Intent intent = new Intent();
            intent.setAction(PlayMusicService.MUSIC_PLAY);
            sendBroadcast(intent);
        }
    }

    /**
     * 播放当前歌曲
     */
    private void play() {
        if (saveList.size() == 0) return;
        musicPlay.start();
        setNotification();
        Intent intent = new Intent();
        intent.setAction(PlayMusicService.MUSIC_PLAY);
        sendBroadcast(intent);
    }

    /**
     * 粘贴当前歌曲
     */
    private void pause() {
        musicPlay.pause();
        setNotification();
        Intent intent = new Intent();
        intent.setAction(PlayMusicService.MUSIC_PAUSE);
        sendBroadcast(intent);
    }

    /**
     * 返回当前歌曲的进度
     */
    private int getCurrentPosition() {
        if (saveList.size() == 0) return 0;
        return musicPlay.getCurrentPosition();
    }

    /**
     * 设置当前歌曲的进度
     */
    private void setCurrentPosition(int pos) {
        if (saveList.size() == 0) return;
        musicPlay.setCurrentPosition(pos);
        Intent intent = new Intent();
        intent.setAction(PlayMusicService.PROGRESS_CHANGE);
        sendBroadcast(intent);
    }

    /**
     * 返回当前歌曲的长度
     */
    private int getPlayDuration() {
        if (saveList.size() == 0) return 0;
        return musicPlay.getDuration();
    }

    /**
     * 播放下一首歌曲
     */
    private void nextMusic() {
        if (saveList.size() == 0) return;
        musicPlay.next();
        setNotification();
        Intent intent = new Intent();
        intent.setAction(PlayMusicService.MUSIC_NEXT);
        sendBroadcast(intent);
    }

    /**
     * 播放上一首歌曲
     */
    private void lastMusic() {
        if (saveList.size() == 0) return;
        musicPlay.last();
        setNotification();
        Intent intent = new Intent();
        intent.setAction(PlayMusicService.MUSIC_LAST);
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        TelephonyManager tmgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tmgr.listen(phoneListener, 0);
        super.onDestroy();
        if (receiver != null)
            unregisterReceiver(receiver);
        if (mNotification != null)
            mNotificationManager.cancel(PlayMusicService.NOTIFICATION_ID);
        saveData();
        musicPlay.pause();
        musicPlay.release();
        musicPlay.currentMediaPlayer = null;
        changeNotification(PlayMusicService.NOTIFICATION_CREATE);
    }

    class PhoneListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String phoneNumber) {
            switch (state){
                case TelephonyManager.CALL_STATE_RINGING:
                    // 响铃时
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    // 摘机时
                    if (isPlaying()){
                        isContinuePlay = true;
                        pause();
                    }
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    // 电话结束时
                    if (isContinuePlay){
                        play();
                        isContinuePlay = false;
                    }
                    break;
            }
        }
    }

    private class Receiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            switch (action) {
                case PlayMusicService.NOTIFICATION_PLAY_PAUSE:
                    if (isPlaying()) pause();
                    else play();
                    break;
                case PlayMusicService.NOTIFICATION_NEXT:
                    nextMusic();
                    break;
                case PlayMusicService.NOTIFICATION_LAST:
                    lastMusic();
                    break;
                case PlayMusicService.NOTIFICATION_CLOSE:
                    pause();
                    PlayMusicService.this.changeNotification(PlayMusicService.NOTIFICATION_CANCEL);
                    break;
                case PlayMusicService.NOTIFICATION_CHANGE:
                    SharedTools sharedTools = new SharedTools(PlayMusicService.this);
                    sharedTools.changeNotificationDarkTheme();
                    PlayMusicService.this.changeNotification(PlayMusicService.NOTIFICATION_UPDATE);
                    break;
            }
        }
    }

    /**
     * 音乐播放的实现
     */
    static class MusicPlay implements MediaPlayer.OnCompletionListener {
        WeakReference<PlayMusicService> mService;
        private MediaPlayer currentMediaPlayer;  // 音乐的播放者

        public MusicPlay(PlayMusicService mService) {
            this.mService = new WeakReference<>(mService);
            currentMediaPlayer = new MediaPlayer();
            currentMediaPlayer.setOnCompletionListener(this);
        }

        private boolean preparePlayer() {
            return prepare(currentMediaPlayer);
        }

        private void release(){
            currentMediaPlayer.release();
        }

        /**
         * 准备音乐
         */
        private boolean prepare(MediaPlayer player) {
            PlayMusicService service = mService.get();
            // 当前播放列表起码要有歌才能播放
            if (service.playList.size() > 0) {
                while (service.playList.size() > 0) {
                    service.playPosition = service.playPosition % service.playList.size();
                    MusicInfo musicInfo = service.playSongs.get(service.playList.get(service.playPosition));
                    // 当歌曲不存在时, 从播放列表里面删除
                    if (musicInfo == null || !new File(musicInfo.getData()).exists()) {
                        long id = service.playList.get(service.playPosition);
                        service.playSongs.remove(id);
                        service.playList.remove(id);
                        service.saveList.remove(id);
                    } else {
                        try {
                            // 准备音乐
                            player.reset();
                            player.setDataSource(musicInfo.getData());
                            player.prepare();
                            return true;
                        } catch (IOException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                }
            }
            return false;
        }

        private boolean isPlaying() {
            return currentMediaPlayer.isPlaying();
        }

        private int getDuration() {
            return currentMediaPlayer.getDuration();
        }

        private int getCurrentPosition() {
            return currentMediaPlayer.getCurrentPosition();
        }

        private void setCurrentPosition(long pos) {
            currentMediaPlayer.seekTo((int) pos);
        }

        private void start() {
            currentMediaPlayer.start();
        }

        private void pause() {
            currentMediaPlayer.pause();
        }

        private void next() {
            mService.get().playPosition++;
            if (prepare(currentMediaPlayer))
                currentMediaPlayer.start();
        }

        private void last() {
            mService.get().playPosition--;
            if (mService.get().playPosition < 0) mService.get().playPosition = mService.get().playList.size() - 1;
            if (mService.get().playPosition < 0) mService.get().playPosition = 0;
            if (prepare(currentMediaPlayer))
                currentMediaPlayer.start();
        }

        @Override
        public void onCompletion(MediaPlayer mp) {
            // 如果不是单曲循环播放下一首
            if (mService.get().playStatus != PlayMusicService.STATUS_ONE_LOOPING)
                mService.get().playPosition++;
            // 播放目标歌曲
            if (prepare(currentMediaPlayer)) {
                currentMediaPlayer.start();
                mService.get().setNotification();
                Intent intent = new Intent();
                intent.setAction(PlayMusicService.MUSIC_NEXT);
                mService.get().sendBroadcast(intent);
            }
        }
    }

    private static final class ServiceStub extends MusicAidlInterface.Stub {
        private final WeakReference<PlayMusicService> mService;

        public ServiceStub(PlayMusicService mService) {
            this.mService = new WeakReference<>(mService);
        }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {

        }

        @Override
        public boolean playQueueIsEmpty() throws RemoteException {
            return mService.get().playQueueIsEmpty();
        }

        @Override
        public MusicInfo getPlayInfo() throws RemoteException {
            return mService.get().getPlayInfo();
        }

        @Override
        public void setPlayInfo(Map infos) throws RemoteException {
            mService.get().setPlayInfo(infos);
        }

        @Override
        public void addPlayInfo(long id, MusicInfo info) throws RemoteException {
            mService.get().addPlayInfo(id, info);
        }

        @Override
        public long[] setPlayQueue(long[] queue) throws RemoteException {
            return mService.get().setPlayQueue(queue);
        }

        @Override
        public boolean addPlayQueue(long id) throws RemoteException {
            return mService.get().addPlayQueue(id);
        }

        @Override
        public void deletePlayQueue(long id) throws RemoteException {
            mService.get().deletePlayQueue(id);
        }

        @Override
        public void deleteAllPlayQueue() throws RemoteException {
            mService.get().deleteAllPlayQueue();
        }

        @Override
        public long[] getPlayQueue() throws RemoteException {
            return mService.get().getPlayQueue();
        }

        @Override
        public void setPlayOrder(int status) throws RemoteException {
            mService.get().setPlayOrder(status);
        }

        @Override
        public int getPlayOrder() throws RemoteException {
            return mService.get().getPlayOrder();
        }

        @Override
        public boolean isPlaying() throws RemoteException {
            return mService.get().isPlaying();
        }

        @Override
        public void playTarget(long id) throws RemoteException {
            mService.get().playTarget(id);
        }

        @Override
        public void play() throws RemoteException {
            mService.get().play();
        }

        @Override
        public void pause() throws RemoteException {
            mService.get().pause();
        }

        @Override
        public void setCurrentPosition(int pos) throws RemoteException {
            mService.get().setCurrentPosition(pos);
        }

        @Override
        public int getCurrentPosition() throws RemoteException {
            return mService.get().getCurrentPosition();
        }

        @Override
        public int getPlayDuration() throws RemoteException {
            return mService.get().getPlayDuration();
        }

        @Override
        public void nextMusic() throws RemoteException {
            mService.get().nextMusic();
        }

        @Override
        public void lastMusic() throws RemoteException {
            mService.get().lastMusic();
        }

        @Override
        public void saveData() throws RemoteException {
            mService.get().saveData();
        }
    }
}
