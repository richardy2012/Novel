package flandre.cn.novel.info;

/**
 * 音乐保存信息
 */
public class MusicSaveData {
    private String saveList;
    private long songId;
    private int playStatus;
    private int current;
    private boolean isShowNotification;

    public MusicSaveData(String saveList, long songId, int playStatus, int current, boolean isShowNotification) {
        this.saveList = saveList;
        this.songId = songId;
        this.playStatus = playStatus;
        this.current = current;
        this.isShowNotification = isShowNotification;
    }

    public boolean isShowNotification() {
        return isShowNotification;
    }

    public int getCurrent(){
        return current;
    }

    public String getSaveList() {
        return saveList;
    }

    public long getSongId() {
        return songId;
    }

    public int getPlayStatus() {
        return playStatus;
    }
}
