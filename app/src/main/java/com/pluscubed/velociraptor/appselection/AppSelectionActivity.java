package com.pluscubed.velociraptor.appselection;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller;
import com.pluscubed.velociraptor.App;
import com.pluscubed.velociraptor.AppDetectionService;
import com.pluscubed.velociraptor.R;

import java.util.ArrayList;
import java.util.List;

import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;

public class AppSelectionActivity extends AppCompatActivity {

    private AppAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_appselection);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recyclerview);
        mAdapter = new AppAdapter(savedInstanceState);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        RecyclerFastScroller scroller = (RecyclerFastScroller) findViewById(R.id.fastscroller);
        scroller.attachRecyclerView(recyclerView);

        SelectedAppDatabase.getInstalledApps(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<AppInfoEntity>>() {
                    @Override
                    public void onSuccess(List<AppInfoEntity> list) {
                        mAdapter.setAppInfos(list);
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });

        setTitle(R.string.select_apps);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_app_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_app_selection_done:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onItemClick(AppInfoEntity appInfo, boolean checked) {
        SingleSubscriber<Object> subscriber = new SingleSubscriber<Object>() {
            @Override
            public void onSuccess(Object value) {
                if (AppDetectionService.get() != null) {
                    AppDetectionService.get().updateSelectedApps();
                }
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        };
        if (checked) {
            App.getData(this).insert(appInfo).subscribe(subscriber);
        } else {
            App.getData(this).delete(appInfo).subscribe(subscriber);
        }
        appInfo.enabled = checked;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mAdapter.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

        public static final String STATE_APPS = "state_apps";
        private List<AppInfoEntity> mAppInfos;

        public AppAdapter(Bundle savedInstanceState) {
            super();
            setHasStableIds(true);
            if (savedInstanceState != null) {
                mAppInfos = savedInstanceState.getParcelableArrayList(STATE_APPS);
            } else {
                mAppInfos = new ArrayList<>();
            }
        }

        public void setAppInfos(List<AppInfoEntity> list) {
            mAppInfos = list;
            notifyDataSetChanged();
        }

        public Bundle onSaveInstanceState(Bundle outState) {
            outState.putParcelableArrayList(STATE_APPS, (ArrayList<AppInfoEntity>) mAppInfos);
            return outState;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_app, parent, false);
            return new ViewHolder(view);
        }


        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            final AppInfo app = mAppInfos.get(position);

            Glide.with(AppSelectionActivity.this)
                    .load(app)
                    .crossFade()
                    .into(holder.icon);

            holder.title.setText(app.name);
            holder.desc.setText(app.packageName);
            holder.checkbox.setChecked(app.enabled);

        }

        @Override
        public int getItemCount() {
            return mAppInfos.size();
        }

        @Override
        public long getItemId(int position) {
            return mAppInfos.get(position).packageName.hashCode();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView icon;
            TextView title;
            TextView desc;
            CheckBox checkbox;

            public ViewHolder(View itemView) {
                super(itemView);

                icon = (ImageView) itemView.findViewById(R.id.image_app);
                title = (TextView) itemView.findViewById(R.id.text_name);
                desc = (TextView) itemView.findViewById(R.id.text_desc);
                checkbox = (CheckBox) itemView.findViewById(R.id.checkbox);

                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkbox.toggle();

                        int adapterPosition = getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            onItemClick(mAppInfos.get(adapterPosition), checkbox.isChecked());
                        }
                    }
                });
            }
        }
    }
}
