package flandre.cn.novel.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.database.SQLTools;
import flandre.cn.novel.database.SQLiteNovel;
import flandre.cn.novel.Tools.NovelTools;
import flandre.cn.novel.info.NovelInfo;

import java.io.*;
import java.util.Date;

/**
 * 查看详细界面
 * 2019.12.6
 */
public class NovelDetailActivity extends BaseActivity implements View.OnClickListener {
    private NovelInfo novelInfo;
    private SQLiteNovel sqLiteNovel;
    private ImageView image;
    private Button star;
    private Button read;
    private TextView name;
    private TextView author;
    private TextView status;
    private TextView introduce;
    private NovelConfigure configure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_detail);
        setupNovelService();
        configure = NovelConfigureManager.getConfigure(getApplicationContext());
        getValue();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        unpack(savedInstanceState);
        // 判断数据库里是否存在数据
        sqLiteNovel = SQLiteNovel.getSqLiteNovel(getApplicationContext());
        Cursor cursor = sqLiteNovel.getReadableDatabase().query("novel", new String[]{"name", "image", "newChapter"},
                "author=? and name=?", new String[]{novelInfo.getAuthor(), novelInfo.getName()},
                null, null, null);
        if (cursor.moveToNext()) {
            star.setText(getResources().getText(R.string.cancel));
            read.setText(getResources().getText(R.string.ctn));
        } else {
            star.setText(getResources().getText(R.string.star));
            read.setText(getResources().getText(R.string.read));
        }
        NestedScrollView scrollView = findViewById(R.id.wrap);
        name.setText(novelInfo.getName());
        author.setText("作者: " + novelInfo.getAuthor());
        status.setText("状态: " + (novelInfo.getComplete() == 0 ? "连载" : "完本"));
        introduce.setText(novelInfo.getIntroduce());
        image.setImageBitmap(novelInfo.getBitmap());

        star.setOnClickListener(starListener);

        read.setOnClickListener(this);
        cursor.close();
    }

    private void unpack(Bundle savedInstanceState) {
        Bundle bundle;
        // 拿到传参
        if (savedInstanceState == null)
            bundle = getIntent().getExtras();
        else bundle = savedInstanceState;

        novelInfo = (NovelInfo) bundle.getSerializable("NovelInfo");
    }

    private void getValue() {
        // 拿到标签
        name = findViewById(R.id.name);
        author = findViewById(R.id.author);
        status = findViewById(R.id.status);
        introduce = findViewById(R.id.introduce);
        star = findViewById(R.id.star);
        read = findViewById(R.id.read);
        image = findViewById(R.id.image);
        LinearLayout linearLayout = findViewById(R.id.total);
        // 设置主题
        linearLayout.setBackgroundColor(configure.getBackgroundTheme());
        star.setBackgroundColor(configure.getMainTheme());
        read.setBackgroundColor(configure.getMainTheme());
        name.setTextColor(configure.getNameTheme());
        author.setTextColor(configure.getAuthorTheme());
        status.setTextColor(configure.getAuthorTheme());
        introduce.setTextColor(configure.getIntroduceTheme());
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(configure.getMainTheme()));
    }

    private void starBook(TextView readView, TextView textView) {
        if (novelInfo.getSource() == null){
            Toast.makeText(this, "请再次使用本地导入", Toast.LENGTH_SHORT).show();
            return;
        }
        // 把数据保存起来
        Bitmap bitmap = novelInfo.getBitmap();
        File imagePath = getImagePath();
        // 保存图片到本地
        try {
            OutputStream stream = new FileOutputStream(imagePath);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            byte[] data = outputStream.toByteArray();
            stream.write(data);
            stream.flush();
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 在novel里面添加收藏的记录
        novelInfo.setTime(new Date().getTime());
        novelInfo.setWatch("1:1");
        novelInfo.setImagePath(imagePath.getAbsolutePath());
        long novel_id = SQLTools.insertNovel(sqLiteNovel, novelInfo);
        novelInfo.setId((int) novel_id);
        // 在nc里面记录表名
        ContentValues values = new ContentValues();
        String table = "FL" + NovelTools.md5(novelInfo.getName() + novelInfo.getAuthor());
        values.put("novel_id", novel_id);
        values.put("name", novelInfo.getUrl());
        values.put("md5", table);
        novelInfo.setTable(table);
        long NC_id = sqLiteNovel.getReadableDatabase().insert("nc", null, values);
        // 创建存文本的表
        sqLiteNovel.getReadableDatabase().execSQL(
                "create table " + table + "(" +
                        "id INTEGER primary key AUTOINCREMENT," +
                        "chapter varchar(255)," +
                        "url varcahr(255)," +
                        "text text)"
        );
        textView.setText(getResources().getText(R.string.cancel));
        readView.setText(getResources().getText(R.string.ctn));
    }

    private void cancelStar(TextView readView, TextView textView) {
        // 删除图片与相关的表与数据
        int id = SQLTools.getNovelId(sqLiteNovel, novelInfo.getName(), novelInfo.getAuthor());
        SQLTools.delete(sqLiteNovel, String.valueOf(id), mService, this);
        novelInfo.setTable(null);
        textView.setText(getResources().getText(R.string.star));
        readView.setText(getResources().getText(R.string.read));
    }

    private File getImagePath() {
        String image = new File(novelInfo.getImagePath()).getName();

        File file = new File(getExternalFilesDir(null), "img");
        if (!file.exists()) file.mkdir();
        return new File(file, image);
    }

    private View.OnClickListener starListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            TextView textView = (TextView) view;
            TextView readView = findViewById(R.id.read);
            if (textView.getText() == getResources().getText(R.string.star)) {
                starBook(readView, textView);
            } else {
                cancelStar(readView, textView);
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putSerializable("NovelInfo", novelInfo);
        super.onSaveInstanceState(outState);
    }

    /**
     * 进入阅读界面
     */
    @Override
    public void onClick(View v) {
        Intent intent = new Intent(this, TextActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("name", novelInfo.getName());
        bundle.putString("author", novelInfo.getAuthor());
        if (star.getText() == getResources().getText(R.string.star)) {
            bundle.putString("url", novelInfo.getUrl());
            bundle.putString("source", novelInfo.getSource());
        }
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
