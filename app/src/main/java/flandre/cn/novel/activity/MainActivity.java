package flandre.cn.novel.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import android.widget.TextView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.PermissionManager;
import flandre.cn.novel.database.SharedTools;
import flandre.cn.novel.parse.PathParse;

import static flandre.cn.novel.Tools.PermissionManager.CODE_INFO;
import static flandre.cn.novel.Tools.PermissionManager.INTERNET_CODE;

/**
 * 开始的广告界面
 * 2019
 */
public class MainActivity extends AppCompatActivity {
    private RelativeLayout layoutSplash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 显示3秒的背景后,跳转到导航页面
//        startActivity(new Intent(this, IndexActivity.class));
        layoutSplash = findViewById(R.id.activity_splash);
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.1f, 1.0f);
        alphaAnimation.setDuration(500);
        layoutSplash.startAnimation(alphaAnimation);
        alphaAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            //动画结束
            @Override
            public void onAnimationEnd(Animation animation) {
                // 权限检测
                checkPermission();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final PermissionManager manager = new PermissionManager(this);
            // 先设置是否有权限播放音乐
            SharedTools sharedTools = new SharedTools(this);
            if (manager.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
                sharedTools.setMusicEnable(true);
            else sharedTools.setMusicEnable(false);

            // 含有权限直接可以加载数据
            if (manager.checkPermission(Manifest.permission.INTERNET)) startActivity();
                // 没有权限时, 如果是第二次描述权限的作用再申请, 第一次直接申请
                // 第二次被拒绝时就不能再申请权限了
            else if (manager.shouldShowRequestPermissionRationale(Manifest.permission.INTERNET)) {
                Snackbar snackbar = Snackbar.make(layoutSplash, CODE_INFO.get(INTERNET_CODE),
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction(android.R.string.ok, new View.OnClickListener() {  // 点击确定时询问
                            @Override
                            public void onClick(View view) {
                                manager.askPermission(Manifest.permission.INTERNET, INTERNET_CODE);
                            }
                        });
                View view = snackbar.getView();
                NovelConfigure configure = NovelConfigureManager.getConfigure(getApplicationContext());
                view.setBackgroundColor(configure.getMainTheme());
                ((TextView) view.findViewById(R.id.snackbar_text)).setTextColor(configure.getAuthorTheme());
                ((TextView) view.findViewById(R.id.snackbar_action)).setTextColor(configure.getAuthorTheme());
                snackbar.show();
            } else manager.askPermission(Manifest.permission.INTERNET, INTERNET_CODE);
        } else startActivity();
    }

    private void startActivity() {
        Intent intent = new Intent(MainActivity.this, IndexActivity.class);
        receiveOut(intent);
        startActivity(intent);
    }

    private void receiveOut(Intent out) {
        Intent intent = getIntent();
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Bundle bundle = new Bundle();
            String s = new PathParse(this).parse(intent).getPath();
            bundle.putString("path", s);
            out.putExtras(bundle);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == INTERNET_CODE) {
            startActivity();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 跳转页面后把本页面关闭
        finish();
    }
}
