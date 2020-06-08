package com.revolgenx.anilib.fragment.home

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.lifecycle.observe
import androidx.recyclerview.widget.*
import com.obsez.android.lib.filechooser.ChooserDialog
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.revolgenx.anilib.R
import com.revolgenx.anilib.activity.MainActivity
import com.revolgenx.anilib.activity.ViewPagerContainerActivity
import com.revolgenx.anilib.adapter.SelectableAdapter
import com.revolgenx.anilib.dialog.ConfirmationDialog
import com.revolgenx.anilib.dialog.InputDialog
import com.revolgenx.anilib.dialog.TorrentSortDialog
import com.revolgenx.anilib.dialog.openFileChooser
import com.revolgenx.anilib.event.*
import com.revolgenx.anilib.exception.TorrentPauseException
import com.revolgenx.anilib.exception.TorrentResumeException
import com.revolgenx.anilib.fragment.base.BaseLayoutFragment
import com.revolgenx.anilib.meta.ViewPagerContainerMeta
import com.revolgenx.anilib.meta.ViewPagerContainerType
import com.revolgenx.anilib.preference.TorrentPreference
import com.revolgenx.anilib.preference.torrentSort
import com.revolgenx.anilib.repository.util.Status
import com.revolgenx.anilib.torrent.core.Torrent
import com.revolgenx.anilib.torrent.core.TorrentEngine
import com.revolgenx.anilib.torrent.core.TorrentProgressListener
import com.revolgenx.anilib.torrent.sort.makeTorrentSortingComparator
import com.revolgenx.anilib.torrent.state.TorrentActiveState
import com.revolgenx.anilib.torrent.state.TorrentState
import com.revolgenx.anilib.util.*
import com.revolgenx.anilib.util.ThreadUtil.runOnUiThread
import com.revolgenx.anilib.viewmodel.TorrentViewModel
import kotlinx.android.synthetic.main.torrent_fragment_layout.*
import kotlinx.android.synthetic.main.torrent_recycler_adapter_layout.view.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import timber.log.Timber

class TorrentFragment : BaseLayoutFragment() {

    companion object {
        private const val recyclerStateKey = "recycler_state_key"
    }

    private val viewModel by viewModel<TorrentViewModel>()
    private val torrentEngine by inject<TorrentEngine>()
    private val torrentActiveState by inject<TorrentActiveState>()
    private val torrentPreference by inject<TorrentPreference>()
    private lateinit var adapter: TorrentRecyclerAdapter

    private var rotating = false
    private var forceShutdown = false

    private val accentColor: Int
        get() = DynamicTheme.getInstance().get().accentColor

    private val tintAccentColor: Int
        get() = DynamicTheme.getInstance().get().tintAccentColor

    private val translucentSurfaceColor: Int by lazy {
        ColorUtils.setAlphaComponent(
            DynamicTheme.getInstance().get().tintSurfaceColor, 80
        )
    }

    override val layoutRes: Int = R.layout.torrent_fragment_layout
    override var titleRes: Int? = R.string.torrent
    override var setHomeAsUp: Boolean = false

    private var actionMode: ActionMode? = null
    private var inActionMode = false
        set(value) {
            field = value

            actionMode = if (value) {
                (activity as? AppCompatActivity)?.startSupportActionMode(actionModeCallback)
            } else {
                actionMode?.finish()
                null
            }
        }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            return when (item?.itemId) {
                R.id.torrentDeleteItem -> {
                    ConfirmationDialog.Companion.Builder().apply {
                        titleRes(R.string.delete)
                        messageTextRes(R.string.are_you_sure)
                        neutralTextRes(R.string.with_files)
                        onButtonClicked { _, which ->
                            deleteTorrent(which)
                        }
                    }.build().show(childFragmentManager, ConfirmationDialog.TAG)
                    true
                }

                R.id.recheckTorrentItem -> {
                    TorrentRecheckEvent(adapter.getSelectedHashes()).postEvent
                    adapter.clearSelection()
                    inActionMode = false
                    true
                }

                R.id.torrentSelectAllItem -> {
                    adapter.selectAll()
                    true
                }
                android.R.id.home -> {
                    false
                }
                else -> false
            }
        }

        @SuppressLint("RestrictedApi")
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            mode?.menuInflater?.inflate(R.menu.torrent_action_menu, menu)
            if (menu is MenuBuilder) {
                menu.setOptionalIconsVisible(true)
                val primaryColorDark = DynamicTheme.getInstance().get().primaryColorDark
                menu.findItem(R.id.torrentSelectAllItem).icon?.setTint(primaryColorDark)
                menu.findItem(R.id.torrentDeleteItem).icon?.setTint(primaryColorDark)
                menu.findItem(R.id.recheckTorrentItem).icon?.setTint(primaryColorDark)
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode?) {
            inActionMode = false
            adapter.clearSelection()
        }
    }

    private fun deleteTorrent(which: Int) {
        when (which) {
            AlertDialog.BUTTON_POSITIVE -> {
                TorrentRemovedEvent(
                    adapter.getSelectedHashes(),
                    false
                ).postEvent
            }
            AlertDialog.BUTTON_NEUTRAL -> {
                TorrentRemovedEvent(
                    adapter.getSelectedHashes(),
                    true
                ).postEvent
            }
        }
        inActionMode = false
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TorrentRecyclerAdapter()

        torrentRecyclerview.layoutManager =
            GridLayoutManager(
                this.context,
                if (requireContext().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 2 else 1
            )
        torrentRecyclerview.adapter = adapter
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.torrent_fragment_menu, menu)
        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.addTorrentMenu -> {
                PopupMenu(
                    requireContext(),
                    requireActivity().findViewById(R.id.addTorrentMenu)
                ).let {
                    it.inflate(R.menu.torrent_add_menu)
                    it.setOnMenuItemClickListener {
                        when (it.itemId) {
                            R.id.addTorrentFileMenu -> {
                                if ((activity as? MainActivity)?.checkPermission() == true) {
                                    openTorrentFileChooser()
                                }
                                true
                            }
                            R.id.addTorrentMagnetMenu -> {
                                if ((activity as? MainActivity)?.checkPermission() == true) {
                                    openInputDialog()
                                }
                                true
                            }
                            else -> false
                        }
                    }
                    it.show()
                }
                true
            }
            R.id.resumeAllMenu -> {
                resumeAll()
                true
            }
            R.id.pauseAllMenu -> {
                pauseAll()
                true
            }
            R.id.sortTorrentMenu -> {
                makeTorrentSortDialog()
                true
            }
            R.id.exitMenu -> {
                ShutdownEvent().postEvent
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun makeTorrentSortDialog() {
        TorrentSortDialog.newInstance(torrentSort(requireContext())).also {
            it.addListener()
            it.show(childFragmentManager, TorrentSortDialog.tag)
        }
    }

    private fun TorrentSortDialog.addListener() {
        onButtonClickedListener = { _, which ->
            if (which == AlertDialog.BUTTON_POSITIVE) {
                makeTorrentSort()
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        makeTorrentSort()
        registerForEvent()
        viewModel.torrentLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    progressText.showProgress(R.string.loading, false)
                    progressText.visibility = View.GONE
                    adapter.submitList(resource.data)
                }

                Status.ERROR -> {
                    progressText.showProgress(R.string.loading, false)
                    progressText.visibility = View.GONE
                    makeToast(R.string.unable_to_load_torrent)
                }

                Status.LOADING -> {
                    progressText.visibility = View.VISIBLE
                    progressText.showProgress(R.string.loading, true)
                }
            }
        }

        if (savedInstanceState == null) {
            torrentEngine.start()
        }


        savedInstanceState?.let {
            it.getParcelable<Parcelable>(recyclerStateKey)?.let { parcel ->
                torrentRecyclerview.layoutManager?.onRestoreInstanceState(parcel)
            }
            (childFragmentManager.findFragmentByTag(InputDialog.tag) as? InputDialog)?.addListener()
            (childFragmentManager.findFragmentByTag(TorrentSortDialog.tag) as? TorrentSortDialog)?.addListener()
        }
    }

    private fun makeTorrentSort() {
        viewModel.torrentSort = makeTorrentSortingComparator(torrentSort(requireContext()))
    }

    override fun onResume() {
        super.onResume()
        invalidateOptionMenu()
        setHasOptionsMenu(true)
    }


    override fun onSaveInstanceState(outState: Bundle) {
        rotating = true
        outState.putParcelable(
            recyclerStateKey,
            torrentRecyclerview.layoutManager?.onSaveInstanceState()
        )
        super.onSaveInstanceState(outState)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEngineEvent(event: TorrentEngineEvent) {
        when (event.engineEventTypes) {
            TorrentEngineEventTypes.ENGINE_STARTING -> {
                progressText.showProgress(R.string.starting_engine, true)
            }
            TorrentEngineEventTypes.ENGINE_STARTED -> {
                progressText.visibility = View.GONE
                viewModel.getAllTorrents()
            }
            TorrentEngineEventTypes.ENGINE_STOPPING -> {
                progressText.visibility = View.VISIBLE
                progressText.showProgress(R.string.engine_stopping, false)
                viewModel.removeAllTorrentEngineListener()
            }
            TorrentEngineEventTypes.ENGINE_FAULT -> {
                progressText.showProgress(R.string.unable_to_start_engine)
                makeToast(R.string.unable_to_start_engine, icon = R.drawable.ic_error)
            }
            TorrentEngineEventTypes.ENGINE_STOPPED -> {
                progressText.visibility = View.VISIBLE
                progressText.showProgress(R.string.engine_stopped, false)
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShutdownEvent(event: ShutdownEvent) {
        forceShutdown = true
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSessionEvent(event: SessionEvent) {
        rotating = true
    }

    override fun onDestroy() {
        adapter.currentList.forEach { it.removeAllListener() }
        if ((!rotating && !torrentActiveState.serviceActive) || forceShutdown) {
            torrentEngine.stop()
        }
        unRegisterForEvent()
        super.onDestroy()
    }


    private fun openInputDialog() {
        InputDialog.newInstance(R.string.magnet_link).also {
            it.addListener()
            it.show(childFragmentManager, InputDialog.tag)
        }
    }

    private fun InputDialog.addListener() {
        onInputDoneListener = {
            openAddTorrentActivity(it.trim().toUri())
        }
    }

    private fun openAddTorrentActivity(uri: Uri) {
        AddTorrentEvent(uri).postEvent
    }

    private fun openTorrentFileChooser() {
        openFileChooser(
            requireContext(),
            torrentPreference.storagePath
        ) withChosenListener@{ _, file ->
            if (file.extension != "torrent") {
                makeToast(R.string.not_a_torrent_file)
                return@withChosenListener
            }
            openAddTorrentActivity(file.toUri())
        }
    }

    private fun resumeAll() {
        viewModel.resumeAll()
    }

    private fun pauseAll() {
        viewModel.pauseAll()
    }


    inner class TorrentRecyclerAdapter :
        SelectableAdapter<TorrentRecyclerAdapter.TorrentViewHolder, Torrent>(object :
            DiffUtil.ItemCallback<Torrent>() {
            override fun areItemsTheSame(oldItem: Torrent, newItem: Torrent): Boolean =
                oldItem == newItem

            override fun areContentsTheSame(oldItem: Torrent, newItem: Torrent): Boolean =
                oldItem == newItem
        }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TorrentViewHolder =
            TorrentViewHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.torrent_recycler_adapter_layout,
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: TorrentViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun performFiltering(constraint: CharSequence?) {
            if (constraint?.length == 0) {
                if (searchTempList.isNotEmpty()) {
                    submitList(mutableListOf<Torrent>().apply { addAll(searchTempList) })
                    searchTempList.clear()
                }
            } else {
                if (searchTempList.isEmpty()) {
                    searchTempList.addAll(currentList)
                }
                submitList(emptyList())
                constraint?.toString()?.toLowerCase()?.trim()?.let { pattern ->
                    searchTempList.filter { it.name.toLowerCase().contains(pattern) }
                        .takeIf { it.isNotEmpty() }?.let {
                            submitList(it)
                        }
                }
            }
        }


        override fun onViewRecycled(holder: TorrentViewHolder) {
            holder.unbind()
            super.onViewRecycled(holder)
        }

        fun getSelectedHashes() = getSelectedItems().map { currentList[it].hash }

        inner class TorrentViewHolder(private val v: View) : RecyclerView.ViewHolder(v),
            TorrentProgressListener {
            private var torrent: Torrent? = null
            private var currentState: TorrentState = TorrentState.UNKNOWN

            fun bind(item: Torrent) {
                torrent = item
                torrent!!.addListener(this)
                v.apply {
                    this.torrentAdapterConstraintLayout.setBackgroundColor(
                        if (isSelected(adapterPosition)) {
                            translucentSurfaceColor
                        } else {
                            DynamicTheme.getInstance().get().surfaceColor
                        }
                    )
                    pausePlayIv.setOnClickListener {
                        if (torrent!!.isPausedWithState()) {
                            try {
                                torrent!!.resume()
                            } catch (e: TorrentResumeException) {
                                makeToast(msg = e.message)
                            }
                        } else {
                            try {
                                torrent!!.pause()
                            } catch (e: TorrentPauseException) {
                                makeToast(msg = e.message)
                            }
                        }
                    }

                    setOnClickListener {
                        if (selectedItemCount > 0) {
                            toggleSelection(adapterPosition)
                            return@setOnClickListener
                        }

                        if (selectedItemCount <= 0) {
                            if (inActionMode) {
                                inActionMode = false
                                return@setOnClickListener
                            }
                        }

                        ViewPagerContainerActivity.openActivity(
                            requireContext(),
                            ViewPagerContainerMeta(
                                ViewPagerContainerType.TORRENT_META,
                                torrent
                            )
                        )
                    }

                    setOnLongClickListener {
                        toggleSelection(adapterPosition)

                        if (!inActionMode) {
                            inActionMode = true
                        }

                        if (selectedItemCount <= 0) {
                            if (inActionMode) inActionMode = false
                        }

                        true
                    }
                }
                updateView()
            }

            @SuppressLint("SetTextI18n")
            private fun updateView() {
                runOnUiThread {
                    v.apply {
                        if (context == null) return@runOnUiThread

                        torrentNameTv.text = torrent!!.name
                        val progress = torrent!!.progress
                        torrentProgressBar.progress = progress.toInt()

//                    if (torrent!!.hasError) {
//                        indicatorView.setBackgroundColor(context.color(R.color.errorColor))
//                    }

                        val state = torrent!!.state
                        torrentFirstTv.text =
                            "${torrent!!.state.name} · S:${torrent!!.connectedSeeders()} · L:${torrent!!.connectedLeechers()}${
                            if (state == TorrentState.DOWNLOADING) {
                                " · ET: ${torrent!!.eta().formatRemainingTime()}"
                            } else ""}"

                        torrentSecondTv.text =
                            if (state == TorrentState.COMPLETED || state == TorrentState.SEEDING) {
                                "${torrent!!.totalCompleted.formatSize()}/${torrent!!.totalSize.formatSize()} · " +
                                        "↑ ${torrent!!.uploadSpeed.formatSpeed()}"
                            } else
                                "${torrent!!.totalCompleted.formatSize()}/${torrent!!.totalSize.formatSize()} · " +
                                        "↓ ${torrent!!.downloadSpeed.formatSpeed()} · ↑ ${torrent!!.uploadSpeed.formatSpeed()}"

                        if (currentState == state) return@runOnUiThread

                        currentState = state

//                    indicatorView.setBackgroundColor(
//                        when (currentState) {
//                            TorrentState.PAUSED -> {
//                                context.color(R.color.pausedColor)
//                            }
//                            TorrentState.UNKNOWN -> {
//                                context.color(R.color.red)
//                            }
//
//                            TorrentState.DOWNLOADING
//                                , TorrentState.CHECKING
//                                , TorrentState.QUEUE
//                                , TorrentState.CHECKING_FILES
//                                , TorrentState.DOWNLOADING_METADATA
//                                , TorrentState.ALLOCATING
//                                , TorrentState.CHECKING_RESUME_DATA -> {
//                                context.color(R.color.downloadingColor)
//                            }
//
//                            TorrentState.SEEDING -> {
//                                context.color(R.color.seedingColor)
//                            }
//                            TorrentState.COMPLETED -> {
//                                context.color(R.color.completedColor)
//                            }
//                        }
//                    )

                        pausePlayIv.setImageResource(
                            if (torrent!!.isPausedWithState()) {
                                R.drawable.ic_play
                            } else {
                                R.drawable.ic_pause
                            }
                        )

                    }
                }
            }


            override fun invoke() {
                updateView()
            }


            fun unbind() {
                torrent!!.removeListener(this)
                torrent = null
            }
        }
    }


}
