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
    public static final int REST_TIME = 120;

    private Handler handler;
    private View view;
    private float y;
    private boolean isTouch = false;
    private boolean force = false;
    private int restTime = REST_TIME;
    private TextView bottom;
    private TextView countdown;
    private boolean mIsPaddingResume = false;

    private Runnable mTimeCount = new Runnable() {
        @Override
        public void run() {
            if (--restTime == 0) {
                force = false;
                restTime = REST_TIME;
                countdown.setText("↑↑↑↑");
                bottom.setText("上滑关闭");
            } else {
                countdown.setText("剩下 " + restTime + " 秒");
                countdown();
            }
        }
    };

    private Runnable mPaddingResume = new Runnable() {
        @Override
        public void run() {
            if (y > 0 && !isTouch) {
                y = y - 0x10;
                view.setPadding(0, 0, 0, (int) y);
                handler.postDelayed(mPaddingResume, 30);
            }
        }
    };

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
        if (!force){
            countdown.setText("↑↑↑↑");
            bottom.setText("上滑关闭");
        }
        return view;
    }

    private void countdown() {
        handler.postDelayed(mTimeCount, 1000);
    }

    @Override
    public void onStart() {
        super.onStart();
        getDialog().getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        getDialog().setCanceledOnTouchOutside(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (force) countdown();
        if (mIsPaddingResume) paddingResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (force) handler.removeCallbacks(mTimeCount);
        if (mIsPaddingResume) handler.removeCallbacks(mPaddingResume);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIsPaddingResume) handler.removeCallbacks(mPaddingResume);
    }

    private void paddingResume() {
        mIsPaddingResume = true;
        handler.post(mPaddingResume);
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
