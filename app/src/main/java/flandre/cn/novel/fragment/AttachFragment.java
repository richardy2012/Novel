package flandre.cn.novel.fragment;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import flandre.cn.novel.interfaces.DownloadListener;
import flandre.cn.novel.interfaces.MusicListener;

public abstract class AttachFragment extends Fragment implements DownloadListener, MusicListener {
    Context mContext;

    public String getFragmentTag(){
        return null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
    }

    @Override
    public void onDownloadFinish(int downloadFinish, int downloadCount, long downloadId) {

    }

    @Override
    public void onDownloadFail(long id) {

    }

    @Override
    public void onPlayMusic() {

    }

    @Override
    public void onPauseMusic() {

    }

    @Override
    public void onNextSong() {

    }

    @Override
    public void onLastSong() {

    }

    @Override
    public void onProgressChange() {

    }

    @Override
    public void onClearPlayList() {

    }
}
