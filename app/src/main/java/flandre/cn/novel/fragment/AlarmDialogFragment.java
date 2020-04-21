package flandre.cn.novel.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.NovelTools;
import flandre.cn.novel.activity.IndexActivity;
import flandre.cn.novel.database.SharedTools;
import flandre.cn.novel.Tools.Decoration;

/**
 * 小说闹钟弹窗
 * 2020.4.7
 */
public class AlarmDialogFragment extends AttachDialogFragment {
    public static final int NO_ALARM_STATE = -0x1000000;

    private static final String[] CHOICE = new String[]{"不开启", "10分钟后", "20分钟后", "30分钟后", "40分钟后", "50分钟后", "60分钟后", "自定义"};
    private int height;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AlarmDialog);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alarm_dialog_fragment, container, false);
        view.findViewById(R.id.top).setBackgroundColor(NovelConfigureManager.getConfigure().getMainTheme());

        LinearLayoutManager manager = new LinearLayoutManager(mContext);
        manager.setOrientation(LinearLayoutManager.VERTICAL);

        RecyclerView recyclerView = view.findViewById(R.id.choice);
        recyclerView.setBackgroundColor(NovelConfigureManager.getConfigure().getBackgroundTheme());
        recyclerView.setLayoutManager(manager);
        Adapter adapter = new Adapter();
        recyclerView.addItemDecoration(new Decoration(mContext));
        recyclerView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        height = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        view.measure(0, height);
        height = view.getMeasuredHeight();
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.WRAP_CONTENT, height);
        getDialog().setCanceledOnTouchOutside(true);
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> implements View.OnClickListener {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
            return new Holder(layoutInflater.inflate(R.layout.alarm_list, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            holder.textView.setTag(i);
            holder.textView.setText(CHOICE[i]);
            holder.textView.setTextColor(NovelConfigureManager.getConfigure().getAuthorTheme());
            holder.textView.setOnClickListener(this);
        }

        @Override
        public int getItemCount() {
            return CHOICE.length;
        }

        @Override
        public void onClick(View v) {
            int pos = (int) v.getTag();
            SharedTools sharedTools;
            switch (pos) {
                case 0:
                    sharedTools = new SharedTools(mContext);
                    sharedTools.setAlarm(AlarmDialogFragment.NO_ALARM_STATE);
                    break;
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                    sharedTools = new SharedTools(mContext);
                    sharedTools.setAlarm(600 * pos * 1000);
                    Toast.makeText(mContext, "闹钟将在" + NovelTools.resolver(600 * pos * 1000) + "后提示", Toast.LENGTH_SHORT).show();
                    break;
                case 7:
                    Toast.makeText(mContext, "开发者认为你不需要这个功能", Toast.LENGTH_SHORT).show();
                    break;
            }
            ((IndexActivity)mContext).getUserFragment().changeTheme();
            dismiss();
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView textView;

            public Holder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.item);
            }
        }
    }
}
