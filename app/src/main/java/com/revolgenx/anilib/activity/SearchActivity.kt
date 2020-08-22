package com.revolgenx.anilib.activity

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.app.SharedElementCallback
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.observe
import com.facebook.drawee.view.SimpleDraweeView
import com.otaliastudios.elements.Adapter
import com.paulrybitskyi.persistentsearchview.adapters.model.SuggestionItem
import com.paulrybitskyi.persistentsearchview.listeners.OnSuggestionChangeListener
import com.paulrybitskyi.persistentsearchview.utils.SuggestionCreationUtil
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.revolgenx.anilib.R
import com.revolgenx.anilib.dialog.TagChooserDialogFragment
import com.revolgenx.anilib.event.BrowseFilterAppliedEvent
import com.revolgenx.anilib.event.TagEvent
import com.revolgenx.anilib.event.TagOperationType
import com.revolgenx.anilib.field.TagChooserField
import com.revolgenx.anilib.field.TagField
import com.revolgenx.anilib.fragment.search.SearchFragment
import com.revolgenx.anilib.fragment.base.BaseFragment
import com.revolgenx.anilib.model.search.filter.SearchFilterModel
import com.revolgenx.anilib.repository.util.Status
import com.revolgenx.anilib.util.DataProvider
import com.revolgenx.anilib.util.registerForEvent
import com.revolgenx.anilib.util.unRegisterForEvent
import com.revolgenx.anilib.view.navigation.BrowseFilterNavigationView
import com.revolgenx.anilib.viewmodel.BrowseActivityViewModel
import kotlinx.android.synthetic.main.browse_activity_layout.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchActivity : BaseDynamicActivity(),
    BrowseFilterNavigationView.AdvanceBrowseNavigationCallbackListener,
    TagChooserDialogFragment.TagChooserDialogCallback {

    companion object {
        const val GENRE_CHOOSER_DIALOG_TAG = "genre_chooser_tag"
        const val TAG_CHOOSER_DIALOG_TAG = "tag_chooser_tag"
        const val ADVANCE_SEARCH_FRAGMENT_TAG = "advance_search_fragment_tag"
        const val STREAM_CHOOSER_DIALOG_TAG = "stream_chooser_tag"
        const val ADVANCE_SEARCH_INTENT_KEY = "advance_search_intent_key"


        fun openActivity(context: Context, searchFilterModel: SearchFilterModel? = null) {
            context.startActivity(Intent(context, SearchActivity::class.java).also { intent ->
                searchFilterModel?.let {
                    intent.putExtra(ADVANCE_SEARCH_INTENT_KEY, it)
                }
            })
        }
    }

    private val tagAdapter: Adapter.Builder
        get() {
            return Adapter.builder(this)
        }

    private val backDrawable: Drawable
        get() = ContextCompat.getDrawable(context, R.drawable.ic_arrow_back)!!.also {
            it.setTint(DynamicTheme.getInstance().get().tintPrimaryColor)
        }

    private val filterDrawable: Drawable
        get() = ContextCompat.getDrawable(context, R.drawable.ic_button_setting)!!.also {
            it.setTint(DynamicTheme.getInstance().get().tintPrimaryColor)
        }

    private val dataProvider by lazy {
        DataProvider(this)
    }

    private val viewModel by viewModel<BrowseActivityViewModel>()


    override fun onStart() {
        super.onStart()
        registerForEvent()
    }

    override fun onStop() {
        super.onStop()
        unRegisterForEvent()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setUpTheme()
        setUpPersistentSearchView()
        setUpListener()
        setUpViews()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(
                R.id.browseFragmentContainer,
                BaseFragment.newInstance(SearchFragment::class.java),
                ADVANCE_SEARCH_FRAGMENT_TAG
            ).commitNow()

            if (intent.hasExtra(ADVANCE_SEARCH_INTENT_KEY)) {
                intent.getParcelableExtra<SearchFilterModel>(ADVANCE_SEARCH_INTENT_KEY)?.let {
                    browseFilterNavView.setFilter(it)
                }
            }
        }
    }

    private fun getAdvanceSearchFragment() = supportFragmentManager.findFragmentByTag(
        ADVANCE_SEARCH_FRAGMENT_TAG
    ) as? SearchFragment

    private fun setUpViews() {
        viewModel.tagTagFields.takeIf { it.isNotEmpty() }?.let {
            invalidateTagFilter(it)
        }
        viewModel.genreTagFields.takeIf { it.isNotEmpty() }?.let {
            invalidateGenreFilter(it)
        }
        viewModel.streamTagFields.takeIf { it.isNotEmpty() }?.let {
            invalidateStreamFilter(it)
        }
    }

    private fun setUpPersistentSearchView() {
        with(persistentSearchView) {
            setLeftButtonDrawable(backDrawable)
            setRightButtonDrawable(filterDrawable)
            showRightButton()

            setOnSearchConfirmedListener { _, query ->
                if (query.isEmpty()) {
                    return@setOnSearchConfirmedListener
                }
                dataProvider.addToSearchHistory(query)
                viewModel.searchQuery = query
                viewModel.searchNow()
                persistentSearchView.collapse(false)
            }

            setOnSuggestionChangeListener(object : OnSuggestionChangeListener {
                override fun onSuggestionRemoved(suggestion: SuggestionItem) {
                    dataProvider.removeFromSearchHistory(suggestion.itemModel.text)
                }

                override fun onSuggestionPicked(suggestion: SuggestionItem) {
                    viewModel.searchQuery = suggestion.itemModel.text
                    dataProvider.addToSearchHistory(viewModel.searchQuery)
                    viewModel.searchNow()
                }
            })
            dataProvider.getAllHistory().takeIf { it.isNotEmpty() }?.let {
                setSuggestions(
                    SuggestionCreationUtil.asRecentSearchSuggestions(it),
                    false
                )
            }
        }
    }

    private fun setUpTheme() {
        with(persistentSearchView) {
            DynamicTheme.getInstance().get().primaryColor.let {
                setCardBackgroundColor(it)
            }

            ResourcesCompat.getFont(this@SearchActivity, R.font.open_sans_regular)?.let {
                setQueryTextTypeface(it)
                setSuggestionTextTypeface(it)
            }

            DynamicTheme.getInstance().get().tintPrimaryColor.let {
                setSuggestionSelectedTextColor(it)
                setQueryInputHintColor(it)
                setSuggestionTextColor(it)
                setSuggestionIconColor(it)
                setSearchSuggestionIconColor(it)
            }

            DynamicTheme.getInstance().get().tintPrimaryColor.let {
                setQueryInputTextColor(it)
                setQueryInputCursorColor(it)
                setRecentSearchIconColor(it)
            }

        }


        DynamicTheme.getInstance().get().backgroundColor.let {
            rootDrawerLayout.setBackgroundColor(it)
            advanceSearchCoordinatorLayout.setBackgroundColor(it)
        }
        statusBarColor = statusBarColor

    }


    private fun setUpListener() {
        checkDialogs()
        with(persistentSearchView) {
            setOnLeftBtnClickListener {
                if (isExpanded) {
                    collapse(true)
                } else
                    finish()
            }
            setOnRightBtnClickListener {
                rootDrawerLayout.openDrawer(GravityCompat.END)
            }
        }
        with(browseFilterNavView) {
            setNavigationCallbackListener(this@SearchActivity)
        }
        rootDrawerLayout.addDrawerListener(browseFilterNavView.drawerListener)

        dataProvider.onDataChanged {
            persistentSearchView.setSuggestions(
                SuggestionCreationUtil.asRecentSearchSuggestions(dataProvider.getAllHistory()),
                false
            )
        }


        viewModel.searchLiveData.observe(this) { resource ->
            when (resource.status) {
                Status.SUCCESS -> {
                    resource.data?.let {
                        if (it.searchNow) {
                            it.searchNow = false
                            search()
                        }
                    }
                }
                else -> {}
            }
        }

        /**problem with transition
         * {@link https://github.com/facebook/fresco/issues/1445}*/
        ActivityCompat.setExitSharedElementCallback(this, object : SharedElementCallback() {
            override fun onSharedElementEnd(
                sharedElementNames: List<String?>?,
                sharedElements: List<View>,
                sharedElementSnapshots: List<View?>?
            ) {
                super.onSharedElementEnd(sharedElementNames, sharedElements, sharedElementSnapshots)
                if (sharedElements.isEmpty()) {
                    return
                }
                for (view in sharedElements) {
                    if (view is SimpleDraweeView) {
                        view.drawable.setVisible(true, true)
                    }
                }
            }
        })
    }


    private fun checkDialogs() {
        val dialog = when {
            supportFragmentManager.findFragmentByTag(TAG_CHOOSER_DIALOG_TAG) != null -> {
                supportFragmentManager.findFragmentByTag(TAG_CHOOSER_DIALOG_TAG)
            }
            supportFragmentManager.findFragmentByTag(GENRE_CHOOSER_DIALOG_TAG) != null -> {
                supportFragmentManager.findFragmentByTag(GENRE_CHOOSER_DIALOG_TAG)
            }
            supportFragmentManager.findFragmentByTag(STREAM_CHOOSER_DIALOG_TAG) != null -> {
                supportFragmentManager.findFragmentByTag(STREAM_CHOOSER_DIALOG_TAG)
            }
            else -> null
        }

        if (dialog != null) {
            (dialog as TagChooserDialogFragment).onDoneListener(this)
        }
    }


    override fun onTagChooserDone(fragmentTag: String?, list: List<TagField>) {
        when (fragmentTag) {
            TAG_CHOOSER_DIALOG_TAG -> {
                invalidateTagFilter(list)
            }
            GENRE_CHOOSER_DIALOG_TAG -> {
                invalidateGenreFilter(list)
            }
            STREAM_CHOOSER_DIALOG_TAG -> {
                invalidateStreamFilter(list)
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTagEvent(event: TagEvent) {
        val fragmentTag = event.tag
        val tagFields = event.tagFields;
        when (event.tag) {
            TAG_CHOOSER_DIALOG_TAG -> {
                when (event.operationType) {
                    TagOperationType.ADD_TAG -> {
                        viewModel.tagTagFields.addAll(tagFields)
                        addTagToNavView(fragmentTag, tagFields)
                    }
                    TagOperationType.DELETE_TAG -> {
                        viewModel.tagTagFields.removeAll { r ->
                            tagFields.any { it.tag == r.tag }
                        }
                        removeTagToNavView(fragmentTag, tagFields)

                    }
                    else -> {

                    }
                }
            }
            GENRE_CHOOSER_DIALOG_TAG -> {
                when (event.operationType) {
                    TagOperationType.ADD_GENRE -> {
                        viewModel.genreTagFields.addAll(tagFields)
                        addTagToNavView(fragmentTag, tagFields)
                    }
                    TagOperationType.DELETE_GENRE -> {
                        viewModel.genreTagFields.removeAll { r ->
                            tagFields.any { it.tag == r.tag }
                        }
                        removeTagToNavView(fragmentTag, tagFields)

                    }
                    else -> {

                    }
                }
            }
            STREAM_CHOOSER_DIALOG_TAG -> {
                when (event.operationType) {
                    TagOperationType.ADD_STREAM -> {
                        viewModel.streamTagFields.addAll(tagFields)
                        addTagToNavView(fragmentTag, tagFields)
                    }
                    TagOperationType.DELETE_STREAM -> {
                        viewModel.streamTagFields.removeAll { r ->
                            tagFields.any { it.tag == r.tag }
                        }
                        removeTagToNavView(fragmentTag, tagFields)
                    }
                    else -> {

                    }
                }
            }
            else -> {

            }
        }
    }

    private fun addTagToNavView(fragmentTag: String?, tags: List<TagField>) {
        when (fragmentTag) {
            TAG_CHOOSER_DIALOG_TAG -> {
                browseFilterNavView.addTagField(tags)
            }
            GENRE_CHOOSER_DIALOG_TAG -> {
                browseFilterNavView.addGenreField(tags)
            }
            STREAM_CHOOSER_DIALOG_TAG -> {
                browseFilterNavView.addStreamField(tags)
            }
            else -> {

            }
        }
    }


    private fun removeTagToNavView(fragmentTag: String?, tags: List<TagField>) {
        when (fragmentTag) {
            TAG_CHOOSER_DIALOG_TAG -> {
                browseFilterNavView.removeTagField(tags)
            }
            GENRE_CHOOSER_DIALOG_TAG -> {
                browseFilterNavView.removeGenreField(tags)

            }
            STREAM_CHOOSER_DIALOG_TAG -> {
                browseFilterNavView.removeStreamField(tags)
            }
            else -> {

            }
        }
    }

    private fun invalidateStreamFilter(list: List<TagField>) {
        viewModel.streamTagFields = list.toMutableList()
        browseFilterNavView.buildStreamAdapter(
            tagAdapter,
            list
        )
    }

    private fun invalidateGenreFilter(list: List<TagField>) {
        viewModel.genreTagFields = list.toMutableList()
        browseFilterNavView.buildGenreAdapter(
            tagAdapter,
            list
        )
    }

    private fun invalidateTagFilter(list: List<TagField>) {
        viewModel.tagTagFields = list.toMutableList()
        browseFilterNavView.buildTagAdapter(
            tagAdapter,
            list
        )
    }


    override fun onBackPressed() {
        if (rootDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            rootDrawerLayout.closeDrawer(GravityCompat.END)
        } else
            super.onBackPressed()
    }


    override val layoutRes: Int = R.layout.browse_activity_layout


    /**
     * Called by advance search filter nav view
     * */
    override fun onGenreChoose(tags: List<TagField>) {
        openTagChooserDialog(tags, GENRE_CHOOSER_DIALOG_TAG, getString(R.string.genre))
    }

    /**
     * Called by advance search filter nav view
     * */
    override fun onStreamChoose(tags: List<TagField>) {
        openTagChooserDialog(tags, STREAM_CHOOSER_DIALOG_TAG, getString(R.string.streaming_on))
    }

    /**
     * Called by advance search filter nav view
     * */
    override fun onTagChoose(tags: List<TagField>) {
        openTagChooserDialog(tags, TAG_CHOOSER_DIALOG_TAG, getString(R.string.tags))
    }


    override fun onGenreAdd(tags: List<TagField>) {
        viewModel.genreTagFields = tags.toMutableList()
    }

    override fun onTagAdd(tags: List<TagField>) {
        viewModel.tagTagFields = tags.toMutableList()
    }

    override fun onStreamAdd(tags: List<TagField>) {
        viewModel.streamTagFields = tags.toMutableList()
    }

    override fun onTagRemoved(tag: String) {
        viewModel.tagTagFields.removeAll { it.tag == tag }
    }

    override fun onGenreRemoved(tag: String) {
        viewModel.genreTagFields.removeAll { it.tag == tag }
    }

    override fun onStreamRemoved(tag: String) {
        viewModel.streamTagFields.removeAll { it.tag == tag }
    }

    /**
     * Called by advance search filter nav view to fill search box
     * */
    override fun getQuery(): String {
        return viewModel.searchQuery
    }

    override fun updateGenre() {
        browseFilterNavView.invalidateGenreAdapter(tagAdapter)
    }

    override fun updateStream() {
        browseFilterNavView.invalidateStreamAdapter(tagAdapter)
    }

    override fun updateTags() {
        browseFilterNavView.invalidateTagAdapter(tagAdapter)
    }

    override fun applyFilter() {
        if (rootDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            rootDrawerLayout.closeDrawer(GravityCompat.END)
        }
        browseFilterNavView.getFilter()?.let {
            viewModel.searchQuery = it.query!!
            persistentSearchView.inputQuery = viewModel.searchQuery
            BrowseFilterAppliedEvent(it).postSticky
        }
    }

    private fun search() {
        browseFilterNavView.getFilter()?.let {
            it.query = viewModel.searchQuery
            BrowseFilterAppliedEvent(it).postSticky
        }
    }


    private fun openTagChooserDialog(
        tags: List<TagField>,
        dialogTag: String,
        tagHeader: String
    ) {
        TagChooserDialogFragment.newInstance(
            TagChooserField(
                tagHeader,
                tags
            )
        ).apply {
            onDoneListener(this@SearchActivity)
            show(supportFragmentManager, dialogTag)
        }
    }

}