package flandre.cn.novel.parse;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.support.v4.content.FileProvider;
import android.util.Base64;
import android.widget.Toast;
import flandre.cn.novel.BuildConfig;
import flandre.cn.novel.Tools.AES;
import flandre.cn.novel.Tools.ByteBuilder;
import flandre.cn.novel.Tools.GetNovelInfoAsync;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.info.NovelInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ShareFile {
    private String mPath;
    private OnFinishParse mOnfinishParse;
    private Context mContext;
    private NovelInfo mNovelInfo;

    public ShareFile(String path, Context context) {
        mPath = path;
        mContext = context;
    }

    public ShareFile(NovelInfo novelInfo, Context context){
        mNovelInfo = novelInfo;
        mContext = context;
    }

    public ShareFile(Context context){
        mContext = context;
    }

    public ShareFile setOnfinishParse(OnFinishParse onfinishParse) {
        this.mOnfinishParse = onfinishParse;
        return this;
    }

    public void parseFile() {
        if (mPath.endsWith(".fh.txt")) {
            new GetNovelInfoAsync(mContext).setOnFinishParse(mOnfinishParse).execute(mPath);
        } else {
            FileParse fileParse = new FileParse(mPath, SQLiteNovel.getSqLiteNovel(), mContext);
            if (mOnfinishParse != null)
                fileParse.setOnfinishParse(mOnfinishParse);
            try {
                fileParse.parseFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void shareFile() {
        try {
            File file;
            if (mNovelInfo.getSource() != null) {
                // 如果是网上小说, 生成一个fh文件分享过去
                String name = mNovelInfo.getName();
                File dir = new File(mContext.getExternalFilesDir(null), "tmp");
                if (!dir.exists()) dir.mkdir();
                file = new File(dir, name + ".fh.txt");
                ByteBuilder byteBuilder = new ByteBuilder(1024);
                FileOutputStream outputStream = new FileOutputStream(file);
                byteBuilder.writeInt(mNovelInfo.getSource().getBytes().length);
                byteBuilder.writeString(mNovelInfo.getSource());
                byteBuilder.writeInt(name.getBytes().length);
                byteBuilder.writeString(name);
                byteBuilder.writeInt(mNovelInfo.getAuthor().getBytes().length);
                byteBuilder.writeString(mNovelInfo.getAuthor());
                byteBuilder.writeInt(mNovelInfo.getUrl().getBytes().length);
                byteBuilder.writeString(mNovelInfo.getUrl());
                try {
                    outputStream.write(Base64.encode(AES.encrypt(byteBuilder.getBytes()), Base64.DEFAULT));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                outputStream.flush();
                outputStream.close();
            } else if (mNovelInfo.getUrl() != null) {
                // 如果是本地小说, 把本地小说分享过去
                file = new File(mNovelInfo.getUrl());
                if (!file.exists()) {
                    Toast.makeText(mContext, "要分享的小说的本地文件已被删除！", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                Toast.makeText(mContext, "分享不了该小说！", Toast.LENGTH_SHORT).show();
                return;
            }
            share(file, getMimeType(file.getAbsolutePath()), "分享小说");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void share(File file, String mineType, String title){
        Intent share = new Intent(Intent.ACTION_SEND);
        Uri contentUri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            contentUri = FileProvider.getUriForFile(mContext, BuildConfig.APPLICATION_ID + ".fileProvider", file);
        } else {
            contentUri = Uri.fromFile(file);
        }
        share.putExtra(Intent.EXTRA_STREAM, contentUri);
        share.setType(mineType);//此处可发送多种文件
        share.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        mContext.startActivity(Intent.createChooser(share, title));
    }

    public String getMimeType(String filePath) {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String mime = "*/*";
        if (filePath != null) {
            try {
                mmr.setDataSource(filePath);
                mime = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            } catch (RuntimeException e) {
                return mime;
            }
        }
        return mime;
    }
}
