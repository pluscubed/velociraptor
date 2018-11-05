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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import com.pluscubed.velociraptor.BuildConfig
import com.pluscubed.velociraptor.R
import com.pluscubed.velociraptor.detection.AppDetectionService
import com.pluscubed.velociraptor.utils.PrefUtils
import kotlinx.coroutines.*
import java.util.*
import kotlin.coroutines.CoroutineContext

class AppSelectionActivity : AppCompatActivity(), CoroutineScope {

    private var mAdapter: AppAdapter? = null
    private var mScroller: RecyclerFastScroller? = null
    private var mSwipeRefreshLayout: SwipeRefreshLayout? = null

    private var mSelectedApps: MutableSet<String>? = null
    private var mAppList: List<AppInfo>? = null
    private var mMapApps: List<AppInfo>? = null

    private var mMapsOnly: Boolean = false

    private var mLoadingAppList: Boolean = false
    private var mLoadingMapApps: Boolean = false

    protected lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_appselection)

        val toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        job = Job()

        val recyclerView = findViewById<View>(R.id.recyclerview) as RecyclerView
        mAdapter = AppAdapter()
        recyclerView.adapter = mAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        mScroller = findViewById<View>(R.id.fastscroller) as RecyclerFastScroller
        mScroller!!.attachRecyclerView(recyclerView)

        mSwipeRefreshLayout = findViewById<View>(R.id.swiperefresh) as SwipeRefreshLayout
        mSwipeRefreshLayout!!.setOnRefreshListener {
            if (mMapsOnly) {
                reloadMapApps()
            } else {
                reloadInstalledApps()
            }
            mAdapter!!.setAppInfos(ArrayList())
        }
        mSwipeRefreshLayout!!.setColorSchemeColors(ContextCompat.getColor(this, R.color.colorAccent))

        if (savedInstanceState == null) {
            mMapsOnly = true
        } else {
            mAppList = savedInstanceState.getParcelableArrayList(STATE_APPS)
            mMapApps = savedInstanceState.getParcelableArrayList(STATE_MAP_APPS)
            mMapsOnly = savedInstanceState.getBoolean(STATE_MAPS_ONLY)
            mSelectedApps = HashSet(savedInstanceState.getStringArrayList(STATE_SELECTED_APPS))
        }

        if (mMapApps == null) {
            reloadMapApps()
        } else if (mMapsOnly) {
            mAdapter!!.setAppInfos(mMapApps)
        }

        if (mAppList == null) {
            reloadInstalledApps()
        } else if (!mMapsOnly) {
            mAdapter!!.setAppInfos(mAppList)
        }

        setTitle(R.string.select_apps)
    }

    override fun onPostResume() {
        super.onPostResume()
        if (mLoadingAppList || mLoadingMapApps) {
            mSwipeRefreshLayout!!.isRefreshing = true
        }
    }

    private fun reloadInstalledApps() = launch {
        mLoadingAppList = true
        mSwipeRefreshLayout!!.isRefreshing = true
        mSelectedApps = HashSet(PrefUtils.getApps(this@AppSelectionActivity))

        try {
            val installedApps = withContext(Dispatchers.IO) {
                SelectedAppDatabase.getInstalledApps(this@AppSelectionActivity)
            }

            if (!mMapsOnly) {
                mAdapter!!.setAppInfos(installedApps)
                mSwipeRefreshLayout!!.isRefreshing = false
            }
            mAppList = installedApps

            mLoadingAppList = false
        } catch (e: Exception) {
            e.printStackTrace()
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(e)
            }
        }
    }

    private fun reloadMapApps() = launch {
        mLoadingMapApps = true
        mSwipeRefreshLayout!!.isRefreshing = true
        mSelectedApps = PrefUtils.getApps(this@AppSelectionActivity)

        try {
            val mapApps = withContext(Dispatchers.IO) {
                SelectedAppDatabase.getMapApps(this@AppSelectionActivity)
            }

            if (mMapsOnly) {
                mAdapter!!.setAppInfos(mapApps)
                mSwipeRefreshLayout!!.isRefreshing = false
            }
            mMapApps = mapApps

            mLoadingMapApps = false
        } catch (e: Exception) {
            e.printStackTrace()
            if (!BuildConfig.DEBUG) {
                Crashlytics.logException(e)
            }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_selection, menu)
        val item = menu.findItem(R.id.menu_app_selection_maps)
        var drawable = AppCompatResources.getDrawable(this, R.drawable.ic_map_white_24dp)!!.mutate()
        drawable = DrawableCompat.wrap(drawable)
        if (mMapsOnly) {
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
                mMapsOnly = !mMapsOnly
                invalidateOptionsMenu()
                mAdapter!!.setAppInfos(if (mMapsOnly) mMapApps else mAppList)
                mSwipeRefreshLayout!!.isRefreshing = mMapsOnly && mLoadingMapApps || !mMapsOnly && mLoadingAppList
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onItemClick(appInfo: AppInfo, checked: Boolean) {
        if (appInfo.packageName != null && !appInfo.packageName.isEmpty()) {
            if (checked) {
                mSelectedApps!!.add(appInfo.packageName)
            } else {
                mSelectedApps!!.remove(appInfo.packageName)
            }

            PrefUtils.setApps(this, mSelectedApps)
            if (AppDetectionService.get() != null) {
                AppDetectionService.get().updateSelectedApps()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelableArrayList(STATE_APPS, mAppList as ArrayList<AppInfo>?)
        outState.putParcelableArrayList(STATE_MAP_APPS, mMapApps as ArrayList<AppInfo>?)
        outState.putBoolean(STATE_MAPS_ONLY, mMapsOnly)
        outState.putStringArrayList(STATE_SELECTED_APPS, ArrayList(mSelectedApps!!))
        super.onSaveInstanceState(outState)
    }

    private inner class AppAdapter : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
        private var mAppInfos: List<AppInfo>? = null

        init {
            setHasStableIds(true)
            mAppInfos = ArrayList()
        }

        fun setAppInfos(list: List<AppInfo>?) {
            if (list == null) {
                mAppInfos = ArrayList()
            } else {
                mAppInfos = list
            }
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.list_item_app, parent, false)
            return ViewHolder(view)
        }


        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val app = mAppInfos!![position]

            Glide.with(this@AppSelectionActivity)
                    .load(app)
                    .crossFade()
                    .into(holder.icon)

            holder.title.text = app.name
            holder.desc.text = app.packageName
            holder.checkbox.isChecked = mSelectedApps!!.contains(app.packageName)

        }

        override fun getItemCount(): Int {
            return mAppInfos!!.size
        }

        override fun getItemId(position: Int): Long {
            return mAppInfos!![position].packageName.hashCode().toLong()
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var icon: ImageView
            var title: TextView
            var desc: TextView
            var checkbox: CheckBox

            init {

                icon = itemView.findViewById<View>(R.id.image_app) as ImageView
                title = itemView.findViewById<View>(R.id.text_name) as TextView
                desc = itemView.findViewById<View>(R.id.text_desc) as TextView
                checkbox = itemView.findViewById<View>(R.id.checkbox) as CheckBox

                itemView.setOnClickListener {
                    checkbox.toggle()

                    val adapterPosition = adapterPosition
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        onItemClick(mAppInfos!![adapterPosition], checkbox.isChecked)
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
