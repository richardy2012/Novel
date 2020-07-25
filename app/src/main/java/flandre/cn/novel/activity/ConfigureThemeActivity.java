package flandre.cn.novel.activity;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v4.widget.NestedScrollView;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import flandre.cn.novel.R;
import flandre.cn.novel.Tools.NovelConfigure;
import flandre.cn.novel.Tools.NovelConfigureManager;
import flandre.cn.novel.Tools.Decoration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 开放给用户的页面主题等数据的配置页面
 * 2019.12.7
 */
public class ConfigureThemeActivity extends BaseActivity {
    private NovelConfigure configure;

    private String[] data = new String[]{"文字颜色：", "文字大小：", "背景颜色：", "章节颜色：", "标题背景：", "背景颜色：", "书名颜色：", "作者颜色：", "介绍颜色："};
    private String[] saveData;  // 保存的数据, 保存时把这的数据放入配置文件
    private int current;  // 当前选择的页面的索引, 保存时根据此设置配置文件
    private boolean alwaysNext;  // 是否全屏点击下一页
    private boolean alarmForce;  // 小说闹钟是否强制
    private boolean constantAlarm;  // 是否循环闹钟

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configure);
        setupMusicService();
        configure = NovelConfigureManager.getConfigure(this.getApplicationContext());

        saveData = new String[]{
                configure.getBaseTextColor(),
                String.valueOf(configure.getTextSize()),
                configure.getBaseBackgroundColor(),
                configure.getBaseChapterColor(),
                configure.getBaseMainTheme(),
                configure.getBaseBackgroundTheme(),
                configure.getBaseNameTheme(),
                configure.getBaseAuthorTheme(),
                configure.getBaseIntroduceTheme()
        };

        alwaysNext = configure.isAlwaysNext();
        alarmForce = configure.isAlarmForce();
        constantAlarm = configure.isConstantAlarm();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(configure.getMainTheme()));

        NestedScrollView total = findViewById(R.id.total);
        total.setBackgroundColor(configure.getBackgroundTheme());
        LinearLayout backSet = findViewById(R.id.backSet);
        backSet.setBackgroundColor((~configure.getBackgroundTheme()) & 0x11FFFFFF | 0x11000000);

        ((TextView) findViewById(R.id.intr_1)).setTextColor(configure.getIntroduceTheme());
        ((TextView) findViewById(R.id.intr_2)).setTextColor(configure.getIntroduceTheme());
        ((TextView) findViewById(R.id.page)).setTextColor(configure.getIntroduceTheme());
        ((TextView) findViewById(R.id.other)).setTextColor(configure.getIntroduceTheme());

        setupRecycleView();
    }

    private void setupRecycleView() {
        LinearLayoutManager[] managers = new LinearLayoutManager[4];
        for (int i = 0; i < managers.length; i++) {
            managers[i] = new LinearLayoutManager(this);
            managers[i].setOrientation(LinearLayoutManager.VERTICAL);
        }

        Decoration decoration = new Decoration(this);

        Adapter readAdapter = new Adapter(0, 4);
        Adapter themeAdapter = new Adapter(4, 5);
        PageAdapter pageAdapter = new PageAdapter();
        OtherAdapter otherAdapter = new OtherAdapter();

        RecyclerView readSetting = findViewById(R.id.readSetting);
        RecyclerView themeSetting = findViewById(R.id.themeSetting);
        RecyclerView pageSetting = findViewById(R.id.pageSetting);
        RecyclerView otherSetting = findViewById(R.id.otherSetting);

        readSetting.setLayoutManager(managers[0]);
        themeSetting.setLayoutManager(managers[1]);
        pageSetting.setLayoutManager(managers[2]);
        otherSetting.setLayoutManager(managers[3]);

        readSetting.setAdapter(readAdapter);
        themeSetting.setAdapter(themeAdapter);
        pageSetting.setAdapter(pageAdapter);
        otherSetting.setAdapter(otherAdapter);

        readSetting.addItemDecoration(decoration);
        themeSetting.addItemDecoration(decoration);
        pageSetting.addItemDecoration(decoration);
        otherSetting.addItemDecoration(decoration);

        readSetting.setNestedScrollingEnabled(false);
        readSetting.setFocusable(false);
        themeSetting.setNestedScrollingEnabled(false);
        themeSetting.setFocusable(false);
        pageSetting.setNestedScrollingEnabled(false);
        pageSetting.setFocusable(false);
        otherSetting.setNestedScrollingEnabled(false);
        otherSetting.setFocusable(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 在ActionBar上添加菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.configure_theme_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.save:
                doSave();
                break;
        }
        return true;
    }

    private void doSave() {
        try {
            // 把数据保存到配置类里, 然后把类写入文件
            configure.setTextColor(saveData[0]);
            configure.setTextSize(Integer.parseInt(saveData[1]));
            configure.setBackgroundColor(saveData[2]);
            configure.setChapterColor(saveData[3]);
            configure.setMainTheme(saveData[4]);
            configure.setBackgroundTheme(saveData[5]);
            configure.setNameTheme(saveData[6]);
            configure.setAuthorTheme(saveData[7]);
            configure.setIntroduceTheme(saveData[8]);
            configure.setNowPageView(NovelConfigureManager.getPageView().get(current).get("source"));
            configure.setAlwaysNext(alwaysNext);
            configure.setAlarmForce(alarmForce);
            configure.setConstantAlarm(constantAlarm);

            NovelConfigureManager.saveConfigure(this.configure, this);
            Intent intent = new Intent();
            intent.setAction(IndexActivity.CHANGE_THEME);
            sendBroadcast(intent);
            setResult(0);
            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "你写的数据有问题", Toast.LENGTH_SHORT).show();
        }
    }

    class Adapter extends RecyclerView.Adapter<Adapter.Holder> {
        private int offset;
        private int size;

        Adapter(int offset, int size) {
            this.offset = offset;
            this.size = size;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.theme_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            int pos = i + offset;
            Watcher watcher = new Watcher(pos);
//            if (holder.editText.getTag() == null) {
//                holder.editText.setTag(watcher);
//            } else {
//                holder.editText.removeTextChangedListener((TextWatcher) holder.editText.getTag());
//            }
            if (holder.editText.getTag() != null)
                holder.editText.removeTextChangedListener((TextWatcher) holder.editText.getTag());
            holder.editText.setTag(watcher);

            holder.textView.setText(data[pos]);
            holder.textView.setTextColor(configure.getNameTheme());
            holder.editText.setText(saveData[pos]);
            holder.editText.setTextColor(configure.getNameTheme());
            holder.editText.addTextChangedListener(watcher);
        }

        @Override
        public int getItemCount() {
            return size;
        }

        class Watcher implements TextWatcher {
            private int pos;

            Watcher(int pos) {
                this.pos = pos;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                // 修改数据时, 暂时把数据保存到saveData
                saveData[pos] = s.toString();
            }
        }

        class Holder extends RecyclerView.ViewHolder {
            TextView textView;
            EditText editText;

            Holder(View itemView) {
                super(itemView);
                itemView.setBackgroundColor(configure.getBackgroundTheme());
                textView = itemView.findViewById(R.id.introduce);
                editText = itemView.findViewById(R.id.input);
            }
        }
    }

    class PageAdapter extends RecyclerView.Adapter<Holder> implements View.OnClickListener {
        private List<Map<String, String>> list;
        private Holder[] holders = new Holder[NovelConfigureManager.getPageView().size()];

        PageAdapter() {
            this.list = NovelConfigureManager.getPageView();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.source_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            holder.itemView.setTag(i);
            holder.itemView.setOnClickListener(this);
            holder.name.setText(list.get(i).get("description"));
            holder.name.setTextColor(configure.getNameTheme());
            holder.choice.setBackground(getResources().getDrawable(
                    configure.getMode() == NovelConfigure.DAY ? R.drawable.choice_day : R.drawable.choice_night));
            if (list.get(i).get("source").equals(configure.getNowPageView())) {
                current = i;
                holder.choice.setVisibility(View.VISIBLE);
            }
            holders[i] = holder;
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onClick(View v) {
            int pos = (int) v.getTag();
            holders[current].choice.setVisibility(View.GONE);
            holders[pos].choice.setVisibility(View.VISIBLE);
            current = pos;
        }
    }

    class OtherAdapter extends RecyclerView.Adapter<Holder> implements View.OnClickListener {
        List<String> list = new ArrayList<String>() {{
            add("全屏点击翻下页");
            add("小说闹钟强制休息");
            add("循环闹钟");
        }};

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.source_list, viewGroup, false);
            return new Holder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int i) {
            holder.itemView.setTag(i);
            holder.itemView.setOnClickListener(this);
            holder.name.setTextColor(configure.getNameTheme());
            holder.name.setText(list.get(i));
            holder.choice.setBackground(getResources().getDrawable(configure.getMode() ==
                    NovelConfigure.DAY ? R.drawable.choice_day : R.drawable.choice_night));
            switch (i) {
                case 0:
                    holder.choice.setVisibility(alwaysNext ? View.VISIBLE : View.GONE);
                    break;
                case 1:
                    holder.choice.setVisibility(alarmForce ? View.VISIBLE : View.GONE);
                    break;
                case 2:
                    holder.choice.setVisibility(constantAlarm ? View.VISIBLE : View.GONE);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        public void onClick(View v) {
            switch ((int) v.getTag()) {
                case 0:
                    v.findViewById(R.id.choice).setVisibility((alwaysNext = !alwaysNext) ? View.VISIBLE : View.GONE);
                    break;
                case 1:
                    v.findViewById(R.id.choice).setVisibility((alarmForce = !alarmForce) ? View.VISIBLE : View.GONE);
                    break;
                case 2:
                    v.findViewById(R.id.choice).setVisibility((constantAlarm = !constantAlarm) ? View.VISIBLE : View.GONE);
                    break;
            }
        }
    }

    class Holder extends RecyclerView.ViewHolder {
        TextView name;
        ImageView choice;

        Holder(View itemView) {
            super(itemView);
            itemView.setBackgroundColor(configure.getBackgroundTheme());
            name = itemView.findViewById(R.id.name);
            choice = itemView.findViewById(R.id.choice);
        }
    }
}
