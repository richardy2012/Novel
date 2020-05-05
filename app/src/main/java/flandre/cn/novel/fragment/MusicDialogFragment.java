package flandre.cn.novel.fragment;

import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.activity.LocalMusicActivity;
import flandre.cn.novel.info.MusicInfo;
import flandre.cn.novel.service.PlayMusicService;
import flandre.cn.novel.Tools.Decoration;

import java.util.List;

/**
 * 音乐播放列表弹窗
 * 2020.4.2
 */
public class MusicDialogFragment extends AttachDialogFragment {
    private List<MusicInfo> infos;
    private TextView playList;
    private TextView clear;
    private TextView status;
    private Adapter mAdapter;

    public void setInfos(List<MusicInfo> infos) {
        if (infos == null) return;
        this.infos = infos;
        if (mAdapter != null) {
            mAdapter.updateData(infos);
            playList.setText("播放列表 ( " + infos.size() + " ) ");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.CustomDatePickerDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //设置无标题
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getDialog().getWindow();
        // 设置停留再底部
        WindowManager.LayoutParams params = window.getAttributes();
        window.requestFeature(Window.FEATURE_NO_TITLE);
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        window.setAttributes(params);

        View view = inflater.inflate(R.layout.music_dialog_fragment, container, false);
        setupView(view);
        setData();
        setListener();
        return view;
    }

    private void setupView(View view) {
        view.findViewById(R.id.top).setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        playList = view.findViewById(R.id.play_list);
        clear = view.findViewById(R.id.clear);
        status = view.findViewById(R.id.status);

        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        Decoration decoration = new Decoration(mContext);
        mAdapter = new Adapter(infos);

        RecyclerView recyclerView = view.findViewById(R.id.data);
        recyclerView.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(decoration);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setHasFixedSize(false);
    }

    public void adapterUpdate() {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    private void setData() {
        playList.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        status.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        clear.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
        clear.setText("清空");
        if (infos != null) playList.setText("播放列表 ( " + infos.size() + " ) ");
        try {
            int order = ((LocalMusicActivity) mContext).getMusicService().getPlayOrder();
            status.setTag(order);
            status.setText(PlayMusicService.STATUS[order]);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void setListener() {
        // 修改播放顺序
        status.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int order = (int) status.getTag();
                switch (order) {
                    case PlayMusicService.STATUS_ONE_LOOPING:
                        order = PlayMusicService.STATUS_ALL_LOOPING;
                        break;
                    case PlayMusicService.STATUS_ALL_LOOPING:
                        order = PlayMusicService.STATUS_ALL_RANDOM;
                        break;
                    case PlayMusicService.STATUS_ALL_RANDOM:
                        order = PlayMusicService.STATUS_ONE_LOOPING;
                        break;
                }
                try {
                    ((LocalMusicActivity) mContext).getMusicService().setPlayOrder(order);
                    status.setTag(order);
                    status.setText(PlayMusicService.STATUS[order]);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });

        // 清空播放列表
        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ((LocalMusicActivity) mContext).getMusicService().deleteAllPlayQueue();
                    mAdapter.list.clear();
                    mAdapter.notifyDataSetChanged();
                    dismiss();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        int dialogHeight = (int) (mContext.getResources().getDisplayMetrics().heightPixels * 0.6);
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, dialogHeight);
        getDialog().setCanceledOnTouchOutside(true);
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private List<MusicInfo> list;

        public Adapter(List<MusicInfo> list) {
            this.list = list;
        }

        void updateData(List<MusicInfo> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
            return new Holder(inflater.inflate(R.layout.music_list, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull final Holder holder, int i) {
            MusicInfo musicInfo = list.get(i);
            holder.name.setText(musicInfo.getName());
            holder.singer.setText(musicInfo.getSinger());
            holder.control.setImageResource(NovelConfigureManager.getConfigure().getMode() == NovelConfigure.DAY ?
                    R.drawable.remove_day : R.drawable.remove_night);
            holder.name.setTextColor(NovelConfigureManager.getConfigure().getNameTheme());
            holder.singer.setTextColor(NovelConfigureManager.getConfigure().getIntroduceTheme());
            holder.itemView.findViewById(R.id.isPlaying).setVisibility(musicInfo.isPlaying() ? View.VISIBLE : View.GONE);
            holder.itemView.findViewById(R.id.isPlaying).setBackgroundColor(NovelConfigureManager.getConfigure().getMainTheme());

            // 播放点击的歌曲
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (pos < 0 || pos >= list.size()) return;
                    try {
                        ((LocalMusicActivity) mContext).getMusicService().playTarget(list.get(pos).getSongId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });

            // 从播放列表中移除一首歌曲
            holder.control.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (pos < 0 || pos >= list.size()) return;
                    try {
                        ((LocalMusicActivity) mContext).getMusicService().deletePlayQueue(list.get(pos).getSongId());
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    list.remove(pos);
                    playList.setText("播放列表 ( " + infos.size() + " ) ");
                    notifyItemRemoved(pos);
                }
            });
        }

        @Override
        public int getItemCount() {
            if (list == null) return 0;
            return list.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView name;
            TextView singer;
            ImageView control;

            public Holder(@NonNull View itemView) {
                super(itemView);
                singer = itemView.findViewById(R.id.singer);
                name = itemView.findViewById(R.id.name);
                control = itemView.findViewById(R.id.control);
            }
        }
    }
}
