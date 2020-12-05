package com.pluscubed.velociraptor.settings.appselection

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import butterknife.BindView
import butterknife.ButterKnife
import com.bumptech.glide.Glide
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.detection.AppDetectionService
import com.pluscubed.velociraptor.utils.PrefUtils
import kotlinx.coroutines.*
import java.util.*

class AppSelectionActivity : AppCompatActivity() {

    private lateinit var adapter: AppAdapter

    @BindView(R.id.fastscroller)
    lateinit var scroller: RecyclerFastScroller

    @BindView(R.id.swiperefresh)
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    @BindView(R.id.recyclerview)
    lateinit var recyclerView: RecyclerView

    @BindView(R.id.toolbar)
    lateinit var toolbar: Toolbar

    private var selectedPackageNames: MutableSet<String>? = null
    private var allApps: ArrayList<AppInfo>? = null
    private var mapApps: ArrayList<AppInfo>? = null

    private var isMapsOnly: Boolean = false

    private var isLoadingAllApps: Boolean = false
    private var isLoadingMapApps: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appselection)
        ButterKnife.bind(this)

        setSupportActionBar(toolbar)

        adapter = AppAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        scroller = findViewById(R.id.fastscroller)
        scroller.attachRecyclerView(recyclerView)

        swipeRefreshLayout = findViewById(R.id.swiperefresh)
        swipeRefreshLayout.setOnRefreshListener {
            if (isMapsOnly) {
                reloadMapApps()
            } else {
                reloadInstalledApps()
            }
            adapter.setAppInfos(ArrayList())
        }
        swipeRefreshLayout.setColorSchemeColors(
                ContextCompat.getColor(
                        this,
                        R.color.colorAccent
                )
        )

        if (savedInstanceState == null) {
            isMapsOnly = true
        } else {
            allApps = savedInstanceState.getParcelableArrayList(STATE_APPS)
            mapApps = savedInstanceState.getParcelableArrayList(STATE_MAP_APPS)
            isMapsOnly = savedInstanceState.getBoolean(STATE_MAPS_ONLY)
            selectedPackageNames =
                    HashSet(savedInstanceState.getStringArrayList(STATE_SELECTED_APPS))
        }

        if (mapApps == null) {
            reloadMapApps()
        } else if (isMapsOnly) {
            adapter.setAppInfos(mapApps)
        }

        if (allApps == null) {
            reloadInstalledApps()
        } else if (!isMapsOnly) {
            adapter.setAppInfos(allApps)
        }

        setTitle(R.string.select_apps)
    }

    override fun onPostResume() {
        super.onPostResume()
        if (isLoadingAllApps || isLoadingMapApps) {
            swipeRefreshLayout.isRefreshing = true
        }
    }

    private fun reloadInstalledApps() = lifecycleScope.launch {
        isLoadingAllApps = true
        if (!isMapsOnly)
            swipeRefreshLayout.isRefreshing = true
        selectedPackageNames = HashSet(PrefUtils.getApps(this@AppSelectionActivity))

        try {
            val installedApps = withContext(Dispatchers.IO) {
                SelectedAppDatabase.getInstalledApps(this@AppSelectionActivity)
            }

            if (!isMapsOnly) {
                adapter.setAppInfos(installedApps)
                swipeRefreshLayout.isRefreshing = false
            }
            allApps = ArrayList(installedApps)

            isLoadingAllApps = false
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    private fun reloadMapApps() = lifecycleScope.launch {
        isLoadingMapApps = true
        if (isMapsOnly)
            swipeRefreshLayout.isRefreshing = true
        selectedPackageNames = PrefUtils.getApps(this@AppSelectionActivity)

        try {
            val mapApps = withContext(Dispatchers.IO) {
                SelectedAppDatabase.getMapApps(this@AppSelectionActivity)
            }

            if (isMapsOnly) {
                adapter.setAppInfos(mapApps)
                swipeRefreshLayout.isRefreshing = false
            }
            this@AppSelectionActivity.mapApps = ArrayList(mapApps)

            isLoadingMapApps = false
        } catch (e: Exception) {
            e.printStackTrace()
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_selection, menu)
        val item = menu.findItem(R.id.menu_app_selection_maps)
        var drawable = AppCompatResources.getDrawable(this, R.drawable.ic_map_white_24dp)!!.mutate()
        drawable = DrawableCompat.wrap(drawable)
        if (isMapsOnly) {
            DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.colorAccent))
        }
        item.icon = drawable
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_app_selection_done -> {
                finish()
                return true
            }
            R.id.menu_app_selection_maps -> {
                isMapsOnly = !isMapsOnly
                invalidateOptionsMenu()
                adapter.setAppInfos(if (isMapsOnly) mapApps else allApps)
                swipeRefreshLayout.isRefreshing = isMapsOnly && isLoadingMapApps ||
                        !isMapsOnly && isLoadingAllApps
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onItemClick(appInfo: AppInfo, checked: Boolean) {
        if (appInfo.packageName != null && !appInfo.packageName.isEmpty()) {
            if (checked) {
                selectedPackageNames?.add(appInfo.packageName)
            } else {
                selectedPackageNames?.remove(appInfo.packageName)
            }

            PrefUtils.setApps(this, selectedPackageNames)
            if (AppDetectionService.get() != null) {
                AppDetectionService.get().updateSelectedApps()
            }
        }

        val allMapApps = mapApps?.let { mapApps ->
            (selectedPackageNames?.containsAll(mapApps.map { it.packageName }) == true)
        } ?: false
        PrefUtils.setAllMapApps(this@AppSelectionActivity, allMapApps);
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_APPS, allApps)
        outState.putParcelableArrayList(STATE_MAP_APPS, mapApps)
        outState.putBoolean(STATE_MAPS_ONLY, isMapsOnly)
        outState.putStringArrayList(STATE_SELECTED_APPS, ArrayList(selectedPackageNames!!))
        super.onSaveInstanceState(outState)
    }

    private inner class AppAdapter : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        private var appInfos: List<AppInfo>? = null

        init {
            setHasStableIds(true)
            appInfos = ArrayList()
        }

        fun setAppInfos(list: List<AppInfo>?) {
            if (list == null) {
                appInfos = ArrayList()
            } else {
                appInfos = list
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.list_item_app, parent, false)
            return ViewHolder(view)
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = appInfos!![position]

            Glide.with(this@AppSelectionActivity)
                    .load(app)
                    .crossFade()
                    .into(holder.icon)

            holder.title.text = app.name
            holder.desc.text = app.packageName
            holder.checkbox.isChecked = selectedPackageNames!!.contains(app.packageName)

        }

        override fun getItemCount(): Int {
            return appInfos!!.size
        }

        override fun getItemId(position: Int): Long {
            return appInfos!![position].packageName.hashCode().toLong()
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var icon: ImageView
            var title: TextView
            var desc: TextView
            var checkbox: CheckBox

            init {
                icon = itemView.findViewById(R.id.image_app)
                title = itemView.findViewById(R.id.text_name)
                desc = itemView.findViewById(R.id.text_desc)
                checkbox = itemView.findViewById(R.id.checkbox)

                itemView.setOnClickListener {
                    checkbox.toggle()

                    val adapterPosition = adapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onItemClick(appInfos!![adapterPosition], checkbox.isChecked)
                    }
                }
            }
        }
    }

    companion object {
        const val STATE_SELECTED_APPS = "state_selected_apps"
        const val STATE_APPS = "state_apps"
        const val STATE_MAP_APPS = "state_map_apps"
        const val STATE_MAPS_ONLY = "state_maps_only"
    }
}
