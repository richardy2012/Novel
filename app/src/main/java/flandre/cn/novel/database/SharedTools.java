package flandre.cn.novel.database;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import flandre.cn.novel.fragment.AlarmDialogFragment;
import flandre.cn.novel.info.MusicSaveData;
import flandre.cn.novel.service.PlayMusicService;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class SharedTools {
    private Context mContext;

    public SharedTools(Context context) {
        mContext = context;
    }

    public static void setSongSort(Context context, boolean sort){
        SharedPreferences sharedPreferences = context.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("sort", sort);
        editor.apply();
    }

    public static boolean getSongSort(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getBoolean("sort", true);
    }

    public static void setHeadImagePath(Context context, String path){
        SharedPreferences sharedPreferences = context.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("head", path);
        editor.apply();
    }

    public static String getHeadImagePath(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getString("head", null);
    }

    /**
     * 设置总阅读时间
     *
     * @param addTime 阅读时间的增量
     */
    public void setReadTime(long addTime) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        long time = sharedPreferences.getLong("readTime", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("readTime", time + addTime);
        editor.apply();
    }

    /**
     * @return 总阅读时间
     */
    public long getReadTime() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getLong("readTime", 0);
    }

    /**
     * 已看完的书本++
     */
    public void increaseFinish() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        int finish = sharedPreferences.getInt("finish", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("finish", ++finish);
        editor.apply();
    }

    /**
     * @return 已看完书本的大小
     */
    public int getFinish() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getInt("finish", 0);
    }

    /**
     * 看完书本--
     */
    public void decreaseFinish(){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        int finish = sharedPreferences.getInt("finish", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("finish", --finish);
        editor.apply();
    }

    /**
     * 观看书本++
     */
    public void increaseStart() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        int start = sharedPreferences.getInt("start", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("start", ++start);
        editor.apply();
    }

    /**
     * @return 观看书本的大小
     */
    public int getStart() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getInt("start", 0);
    }

    /**
     * 观看书本--
     */
    public void decreaseStart(){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        int start = sharedPreferences.getInt("start", 0);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("start", --start);
        editor.apply();
    }

    /**
     * 保存当前播放位置
     */
    public static void saveMusicProgress(Context context, int progress){
        SharedPreferences sharedPreferences = context.getSharedPreferences("music", Activity.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("Current", progress);
        editor.apply();
    }

    /**
     * 保存当前的播放信息
     *
     * @param musicSaveData 要保存的信息
     */
    public void saveMusic(MusicSaveData musicSaveData) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("music", Activity.MODE_MULTI_PROCESS);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("SaveList", musicSaveData.getSaveList());
        editor.putLong("SongId", musicSaveData.getSongId());
        editor.putInt("PlayStatus", musicSaveData.getPlayStatus());
//        editor.putInt("Current", musicSaveData.getCurrent());
        editor.putBoolean("IsShowNotification", musicSaveData.isShowNotification());
        editor.apply();
    }

    /**
     * 拿到之前保存的播放信息
     */
    public MusicSaveData getMusic() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("music", Activity.MODE_MULTI_PROCESS);
        String saveList = sharedPreferences.getString("SaveList", null);
        long songId = sharedPreferences.getLong("SongId", -1);
        int playStatus = sharedPreferences.getInt("PlayStatus", PlayMusicService.STATUS_ALL_LOOPING);
        int current = sharedPreferences.getInt("Current", 0);
        boolean isShowNotification = sharedPreferences.getBoolean("IsShowNotification", false);
        return new MusicSaveData(saveList, songId, playStatus, current, isShowNotification);
    }

    /**
     * 记录用户设置闹钟的时间, 用于循环闹钟
     */
    public void setAlarmTime(long time){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("AlarmTime", time);
        editor.apply();
    }

    /**
     * 拿到用户设置的闹钟时间
     */
    public long getAlarmTime(){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getLong("AlarmTime", AlarmDialogFragment.NO_ALARM_STATE);
    }

    /**
     * 设置当前的闹钟时间
     */
    public void setAlarm(long time) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong("Alarm", time);
        editor.apply();
    }

    /**
     * 拿到当前的闹钟时间
     */
    public long getAlarm() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getLong("Alarm", AlarmDialogFragment.NO_ALARM_STATE);
    }

    /**
     * 设置今天的观看时间
     * 看上去没有问题实际上却有个bug, 有时明明不是新的一天却会重置todayRead
     *
     * @param addTime 今天观看时间的增加时长
     */
    public void setTodayRead(long addTime) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        long todayRead = sharedPreferences.getLong("TodayRead", 0) + addTime;
        int saveDay = sharedPreferences.getInt("Today", -1);
        // 设置北京时区
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        int today = Integer.parseInt(simpleDateFormat.format(new Date().getTime()));
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (today - saveDay != 0) {
            editor.putInt("Today", today);
            editor.putLong("TodayRead", addTime);
        } else {
            editor.putLong("TodayRead", todayRead);
        }
        editor.apply();
    }

    /**
     * 获取今天的观看时间
     */
    public long getTodayRead() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getLong("TodayRead", 0);
    }

    /**
     * 返回是否有权限播放音乐
     */
    public boolean getMusicEnable() {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getBoolean("MusicEnable", false);
    }

    /**
     * 设置是否有权限播放音乐
     */
    public void setMusicEnable(boolean musicEnable) {
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("MusicEnable", musicEnable);
        editor.apply();
    }

    /**
     * 返回通知栏是否以黑色为背景
     */
    public boolean isNotificationDarkTheme(){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        return sharedPreferences.getBoolean("NotificationDarkTheme", true);
    }

    /**
     * 设置通知栏是否以黑色为背景
     */
    public void changeNotificationDarkTheme(){
        SharedPreferences sharedPreferences = mContext.getSharedPreferences("novel", Activity.MODE_PRIVATE);
        boolean darkTheme = !sharedPreferences.getBoolean("NotificationDarkTheme", true);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("NotificationDarkTheme", darkTheme);
        editor.apply();
    }
}
