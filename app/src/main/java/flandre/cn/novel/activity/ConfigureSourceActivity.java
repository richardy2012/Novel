package flandre.cn.novel.activity;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.*;
import android.widget.ImageView;
import android.widget.TextView;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.Decoration;

import java.io.*;
import java.util.Map;

/**
 * 配置源的Activity
 * 2019.12.7
 */
public class ConfigureSourceActivity extends BaseActivity {
    private int current;  // 当前选择的源的索引, 保存时根据此设置配置文件
    private NovelConfigure configure;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure_source);
        setupMusicService();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle("设置来源");

        RecyclerView recyclerView = findViewById(R.id.rec);
        LinearLayoutManager manager = new LinearLayoutManager(this);
        manager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(manager);
        Adapter adapter = new Adapter();
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new Decoration(this));

        configure = NovelConfigureManager.getConfigure(getApplicationContext());

        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(configure.getMainTheme()));
        recyclerView.setBackgroundColor(configure.getBackgroundTheme());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.set_sourcec_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
            case R.id.save:
                try {
                    // 设置当前使用源为选择源
                    Map<String, String> map = NovelConfigureManager.getSource().get(current);
                    configure.setNowSourceValue(map.get("source"));
                    configure.setNowSourceKey(map.get("name"));
                    try {
                        NovelConfigureManager.setConstructor(Class.forName(map.get("source")).getConstructor(Activity.class, Handler.class));
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    // 保存配置文件并结束界面
                    NovelConfigureManager.saveConfigure(configure, this);
                    finish();
                }catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
        return true;
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> implements View.OnClickListener {
        Holder[] holders = new Holder[NovelConfigureManager.getSource().size()];

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.source_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            String name = NovelConfigureManager.getSource().get(i).get("name");
            holder.textView.setText(name);
            holder.textView.setTextColor(configure.getNameTheme());
            holder.imageView.setBackground(getResources().getDrawable(
                    configure.getMode() == NovelConfigure.DAY ? R.drawable.choice_day : R.drawable.choice_night));
            holder.itemView.setTag(i);
            holder.itemView.setOnClickListener(this);
            if (name.equals(configure.getNowSourceKey())){
                current = i;
                holder.imageView.setVisibility(View.VISIBLE);
            }
            holders[i] = holder;
        }

        @Override
        public int getItemCount() {
            return NovelConfigureManager.getSource().size();
        }

        @Override
        public void onClick(View v) {
            int pos = (int) v.getTag();
            holders[current].imageView.setVisibility(View.GONE);
            holders[pos].imageView.setVisibility(View.VISIBLE);
            current = pos;
        }

        class Holder extends RecyclerView.ViewHolder{
            ImageView imageView;
            TextView textView;

            Holder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.choice);
                textView = itemView.findViewById(R.id.name);
            }
        }
    }
}
