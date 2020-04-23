package flandre.cn.novel.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.*;
import flandre.cn.novel.R;

/**
 * 小说闹钟时间到时的弹窗
 * 2020.4.7
 */
public class AlarmTriggerDialogFragment extends AttachDialogFragment implements View.OnTouchListener {
    private Handler handler;
    private View view;
    private float y;
    private boolean isTouch = false;
    private boolean force = false;

    public static AlarmTriggerDialogFragment newInstance(boolean force){
        AlarmTriggerDialogFragment alarmTiggerDialogFragment = new AlarmTriggerDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean("force", false);
        alarmTiggerDialogFragment.setArguments(bundle);
        return alarmTiggerDialogFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(mContext.getMainLooper());
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AlarmTriggerDialog);
        if (getArguments() != null)
            force = getArguments().getBoolean("force");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alarm_tigger_dialog_fragment, container, false);
        this.view = view.findViewById(R.id.move);
        view.setPadding(0, 0, 0, 0);
        if (!force) view.setOnTouchListener(this);
        else handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                dismiss();
            }
        }, 1000 * 120);
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        getDialog().setCanceledOnTouchOutside(false);
    }

    private void paddingResume() {
        if (y > 0 && !isTouch) {
            y = y - 0x10;
            view.setPadding(0, 0, 0, (int) y);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    paddingResume();
                }
            }, 30);
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                y = event.getY();
                isTouch = true;
                break;
            case MotionEvent.ACTION_MOVE:
                int y = (int) (this.y - event.getY());
                view.setPadding(0, 0, 0, y > 0 ? y : 0);
                break;
            case MotionEvent.ACTION_UP:
                isTouch = false;
                if (this.y - event.getY() > 100) {
                    dismiss();
                } else {
                    this.y = this.y - event.getY();
                    paddingResume();
                }
                break;
        }
        return true;
    }
}
