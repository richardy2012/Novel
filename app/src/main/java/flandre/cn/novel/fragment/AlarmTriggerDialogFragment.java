package flandre.cn.novel.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.*;
import android.widget.TextView;
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
    private int restTime = 120;
    private TextView bottom;
    private TextView countdown;

    public void setForce(boolean force) {
        this.force = force;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(mContext.getMainLooper());
        setStyle(DialogFragment.STYLE_NO_FRAME, R.style.AlarmTriggerDialog);
        setCancelable(false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.alarm_tigger_dialog_fragment, container, false);
        this.view = view.findViewById(R.id.move);
        bottom = view.findViewById(R.id.bottom);
        countdown = view.findViewById(R.id.countdown);
        view.setPadding(0, 0, 0, 0);
        view.setOnTouchListener(this);
        if (force) countdown();
        return view;
    }

    private void countdown() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (--restTime == 0) {
                    force = false;
                    restTime = 120;
                    countdown.setText("↑↑↑↑");
                    bottom.setText("上滑关闭");
                } else {
                    countdown.setText("剩下 " + restTime + " 秒");
                    countdown();
                }
            }
        }, 1000);
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
        if (force) return true;
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
