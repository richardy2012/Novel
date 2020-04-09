package flandre.cn.novel.Tools;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.database.SharedTools;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class NovelTools {
    /**
     * md5算法
     */
    public static String md5(String plainText) {
        //定义一个字节数组
        byte[] secretBytes = null;
        try {
            // 生成一个MD5加密计算摘要
            MessageDigest md = MessageDigest.getInstance("MD5");
            //对字符串进行加密
            md.update(plainText.getBytes());
            //获得加密后的数据
            secretBytes = md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("没有md5这个算法！");
        }
        //将加密后的数据转换为16进制数字
        String md5code = new BigInteger(1, secretBytes).toString(16);// 16进制数字
        // 如果生成数字未满32位，需要前面补0
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code = "0" + md5code;
        }
        return md5code;
    }

    /**
     * 把时间戳转换为字符串
     *
     * @param time 时间戳
     * @return 字符串
     */
    static public String resolver(long time) {
        String s;
        if (time < 60 * 1000) {
            BigDecimal num1 = new BigDecimal((double) time / 1000).setScale(2, BigDecimal.ROUND_HALF_UP);
            s = num1 + " 秒";
        } else if (time > 60 * 1000 && time < 60 * 1000 * 60) {
            BigDecimal num1 = new BigDecimal((double) time / 60000).setScale(2, BigDecimal.ROUND_HALF_UP);
            s = num1 + " 分";
        } else {
            BigDecimal num1 = new BigDecimal((double) time / 3600000).setScale(2, BigDecimal.ROUND_HALF_UP);
            s = num1 + " 时";
        }
        return s;
    }

    /**
     * 当前通知栏主题是否是黑色的
     */
    public static boolean isDarkNotificationTheme(Context context, int layoutID) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SharedTools sharedTools = new SharedTools(context);
            return sharedTools.isNotificationDarkTheme();
        }else
            return !isSimilarColor(Color.BLACK, getNotificationColor(context, layoutID));
    }

    /**
     * 获取通知栏颜色
     */
    public static int getNotificationColor(Context context, int layoutID) {
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(context).inflate(layoutID, null, false);
        if (viewGroup.findViewById(android.R.id.title) != null) {
            return ((TextView) viewGroup.findViewById(android.R.id.title)).getCurrentTextColor();
        }
        return findColor(viewGroup);
    }

    private static boolean isSimilarColor(int baseColor, int color) {
        int simpleBaseColor = baseColor | 0xff000000;
        int simpleColor = color | 0xff000000;
        int baseRed = Color.red(simpleBaseColor) - Color.red(simpleColor);
        int baseGreen = Color.green(simpleBaseColor) - Color.green(simpleColor);
        int baseBlue = Color.blue(simpleBaseColor) - Color.blue(simpleColor);
        double value = Math.sqrt(baseRed * baseRed + baseGreen * baseGreen + baseBlue * baseBlue);
        if (value < 180.0) {
            return true;
        }
        return false;
    }

    private static int findColor(ViewGroup viewGroupSource) {
        int color = Color.TRANSPARENT;
        LinkedList<ViewGroup> viewGroups = new LinkedList<>();
        viewGroups.add(viewGroupSource);
        while (viewGroups.size() > 0) {
            ViewGroup viewGroup1 = viewGroups.getFirst();
            for (int i = 0; i < viewGroup1.getChildCount(); i++) {
                if (viewGroup1.getChildAt(i) instanceof ViewGroup) {
                    viewGroups.add((ViewGroup) viewGroup1.getChildAt(i));
                } else if (viewGroup1.getChildAt(i) instanceof TextView) {
                    if (((TextView) viewGroup1.getChildAt(i)).getCurrentTextColor() != -1) {
                        color = ((TextView) viewGroup1.getChildAt(i)).getCurrentTextColor();
                    }
                }
            }
            viewGroups.remove(viewGroup1);
        }
        return color;
    }
}
