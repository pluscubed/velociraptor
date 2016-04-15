package com.pluscubed.velociraptor.appselection;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.pluscubed.velociraptor.R;
import com.pluscubed.velociraptor.utils.PrefUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Observable;
import rx.Single;
import rx.SingleSubscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class AppSelectionActivity extends AppCompatActivity {

    private AppAdapter mAdapter;

    /**
     * Returns sorted list of AppInfos.
     */
    public static Single<List<AppInfo>> getInstalledApps(final Context context) {
        return Single.create(new Single.OnSubscribe<List<ApplicationInfo>>() {
            @Override
            public void call(SingleSubscriber<? super List<ApplicationInfo>> singleSubscriber) {
                singleSubscriber.onSuccess(context.getPackageManager().getInstalledApplications(0));
            }
        }).subscribeOn(Schedulers.io())
                .flatMapObservable(new Func1<List<ApplicationInfo>, Observable<ApplicationInfo>>() {
                    @Override
                    public Observable<ApplicationInfo> call(List<ApplicationInfo> appInfos) {
                        return Observable.from(appInfos);
                    }
                })
                .map(new Func1<ApplicationInfo, AppInfo>() {
                    @Override
                    public AppInfo call(ApplicationInfo applicationInfo) {
                        AppInfo appInfo = new AppInfo();
                        appInfo.packageName = applicationInfo.packageName;
                        appInfo.name = applicationInfo.loadLabel(context.getPackageManager()).toString();
                        return appInfo;
                    }
                })
                .toSortedList().toSingle()
                .map(new Func1<List<AppInfo>, List<AppInfo>>() {
                    @Override
                    public List<AppInfo> call(List<AppInfo> appInfos) {
                        List<String> enabledApps = new ArrayList<>(Arrays.asList(PrefUtils.getEnabledApps(context)));
                        for (String enabled : enabledApps) {
                            for (AppInfo info : appInfos) {
                                if (info.packageName.equals(enabled)) {
                                    info.enabled = true;
                                    break;
                                }
                            }
                        }

                        return appInfos;
                    }
                });
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RecyclerView view = new RecyclerView(this);
        setContentView(view);

        mAdapter = new AppAdapter(savedInstanceState);
        view.setAdapter(mAdapter);
        view.setLayoutManager(new LinearLayoutManager(this));

        getInstalledApps(this)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleSubscriber<List<AppInfo>>() {
                    @Override
                    public void onSuccess(List<AppInfo> list) {
                        mAdapter.setAppInfos(list);
                    }

                    @Override
                    public void onError(Throwable error) {

                    }
                });
    }

    private void onItemClick(int index, AppInfo appInfo) {

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

        public static final String STATE_APPS = "state_apps";
        private List<AppInfo> mAppInfos;

        public AppAdapter(Bundle savedInstanceState) {
            super();
            setHasStableIds(true);
            if (savedInstanceState != null) {
                mAppInfos = savedInstanceState.getParcelableArrayList(STATE_APPS);
            } else {
                mAppInfos = new ArrayList<>();
            }
        }

        public void setAppInfos(List<AppInfo> list) {
            mAppInfos = list;
            notifyDataSetChanged();
        }

        public Bundle onSaveInstanceState(Bundle outState) {
            outState.putParcelableArrayList(STATE_APPS, (ArrayList<AppInfo>) mAppInfos);
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
                        int adapterPosition = getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            onItemClick(adapterPosition, mAppInfos.get(adapterPosition));
                        }
                    }
                });
            }
        }
    }
}
