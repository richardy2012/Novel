package flandre.cn.novel.interfaces;

public interface MusicListener {
    public void onPlayMusic();

    public void onPauseMusic();

    public void onNextSong();

    public void onLastSong();

    public void onProgressChange();

    public void onClearPlayList();
}
