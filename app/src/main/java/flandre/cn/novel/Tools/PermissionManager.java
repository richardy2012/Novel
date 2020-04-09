package flandre.cn.novel.Tools;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;

public class PermissionManager {
    public static final int INTERNET_CODE = 0x10;
    public static final int READ_EXTERNAL_STORAGE_CODE = 0x20;
    public static final int ACCESS_NOTIFICATION_POLICY_CODE = 0x30;

    public static final Map<Integer, String> CODE_INFO = new HashMap<Integer, String>(){{
        put(INTERNET_CODE, "我们需要网络去搜索小说");
        put(READ_EXTERNAL_STORAGE_CODE, "我们需要权限去读取本地音乐");
        put(ACCESS_NOTIFICATION_POLICY_CODE, "我们需要权限再通知栏设置音乐控件");
    }};

    private Activity mContext;

    public PermissionManager(Context mContext) {
        this.mContext = (Activity) mContext;
    }

    public void askPermission(String permission, int code){
        askPermission(new String[]{permission}, code);
    }

    public void askPermission(String[] permissions, int code){
        ActivityCompat.requestPermissions(mContext, permissions, code);
    }

    public boolean hasPermission(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(mContext, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public boolean checkPermission(String permissionName) {
        return PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(mContext, permissionName);
    }

    public boolean shouldShowRequestPermissionRationale(String permissions) {
        return ActivityCompat.shouldShowRequestPermissionRationale(mContext, permissions);
    }
}
