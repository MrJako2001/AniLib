package com.revolgenx.anilib.view.navigation

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.flexbox.FlexboxLayoutManager
import com.otaliastudios.elements.Adapter
import com.otaliastudios.elements.Source
import com.pranavpandey.android.dynamic.support.adapter.DynamicSpinnerImageAdapter
import com.pranavpandey.android.dynamic.support.model.DynamicSpinnerItem
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.pranavpandey.android.dynamic.support.widget.DynamicNavigationView
import com.revolgenx.anilib.R
import com.revolgenx.anilib.constant.SearchTypes
import com.revolgenx.anilib.controller.ThemeController
import com.revolgenx.anilib.field.TagField
import com.revolgenx.anilib.model.search.filter.*
import com.revolgenx.anilib.presenter.TagPresenter
import com.revolgenx.anilib.type.MediaSort
import com.revolgenx.anilib.util.hideKeyboard
import com.revolgenx.anilib.util.onItemSelected
import kotlinx.android.synthetic.main.browse_filter_navigation_view.view.*
import java.util.*
import kotlin.math.ceil


class BrowseFilterNavigationView(context: Context, attributeSet: AttributeSet?, style: Int) :
    DynamicNavigationView(context, attributeSet, style) {

    private var mListener: AdvanceBrowseNavigationCallbackListener? = null
    private val rView by lazy {
        LayoutInflater.from(context).inflate(
            R.layout.browse_filter_navigation_view,
            null,
            false
        )
    }

    private val rippleDrawable: RippleDrawable
        get() = RippleDrawable(ColorStateList.valueOf(accentColor), null, null)

    private val accentColor: Int
        get() = DynamicTheme.getInstance().get().accentColor

    private lateinit var streamAdapter: Adapter
    private lateinit var genreAdapter: Adapter
    private lateinit var tagAdapter: Adapter
    private val streamPresenter by lazy {
        TagPresenter(context).also {
            it.tagRemoved {
                streamingTagMap!![it]?.isTagged = false
                mListener?.onStreamRemoved(it)
            }
        }
    }
    private val genrePresenter by lazy {
        TagPresenter(context).also {
            it.tagRemoved {
                genreTagMap!![it]?.isTagged = false
                mListener?.onGenreRemoved(it)
            }
        }
    }
    private val tagPresenter by lazy {
        TagPresenter(context).also {
            it.tagRemoved {
                tagTagMap!![it]?.isTagged = false
                mListener?.onTagRemoved(it)
            }
        }
    }

    private val streamingOnList
        get() =
            context.resources.getStringArray(R.array.streaming_on)

    private val tagList
        get() =
            context.resources.getStringArray(R.array.media_tags)

    private val genreList
        get() =
            context.resources.getStringArray(R.array.media_genre)

    private var genreTagMap: MutableMap<String, TagField>? = null
        get() {
            field = field ?: mutableMapOf<String, TagField>().also { map ->
                genreList.map { map[it] = TagField(it, false) }
            }
            return field
        }

    private var tagTagMap: MutableMap<String, TagField>? = null
        get() {
            field = field ?: mutableMapOf<String, TagField>().also { map ->
                tagList.map { map[it] = TagField(it, false) }
            }
            return field
        }

    private var streamingTagMap: MutableMap<String, TagField>? = null
        get() {
            field = field ?: mutableMapOf<String, TagField>().also { map ->
                streamingOnList.map { map[it] = TagField(it, false) }
            }
            return field
        }

    val drawerListener = object : DrawerLayout.DrawerListener {
        override fun onDrawerStateChanged(newState: Int) {

        }

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {

        }

        override fun onDrawerClosed(drawerView: View) {

        }

        override fun onDrawerOpened(drawerView: View) {
            mListener?.getQuery()?.let {
                browseSearchEt.setText(it)
            }
        }
    }


    var filter: BaseSearchFilterModel? = null
        get() {
            return when (browseTypeSpinner.selectedItemPosition) {
                SearchTypes.ANIME.ordinal, SearchTypes.MANGA.ordinal -> {
                    MediaSearchFilterModel().apply {
                        query = browseSearchEt?.text?.toString()
                        season = browseSeasonSpinner?.selectedItemPosition?.minus(1)
                            ?.takeIf { it >= 0 }

                        type = browseTypeSpinner.selectedItemPosition
                        yearEnabled = enableYearCheckBox.isChecked
                        if (yearEnabled) {
                            minYear = browseYearSeekBar?.leftSeekBar?.progress?.let {
                                ceil(it).toInt()
                            }
                            maxYear = browseYearSeekBar?.rightSeekBar?.progress?.let {
                                ceil(it).toInt()
                            }
                        }

                        sort = browseSortSpinner?.selectedItemPosition?.minus(1)?.takeIf {
                            it >= 0
                        }?.let {
                            (browseSortSpinner.selectedItem as? DynamicSpinnerItem)?.text?.toString()
                                ?.replace(" ", "_")
                                ?.toUpperCase()?.let {
                                    MediaSort.valueOf(it).ordinal
                                }
                        }

                        format =
                            browseFormatSpinner?.selectedItemPosition?.minus(1)?.takeIf {
                                it >= 0
                            }

                        status =
                            browseStatusSpinner?.selectedItemPosition?.minus(1)?.takeIf {
                                it >= 0
                            }

                        streamingOn = streamingTagMap!!.values.filter { it.isTagged }.map { it.tag }
                        countryOfOrigin =
                            browseCountrySpinner?.selectedItemPosition?.minus(1)?.takeIf {
                                it >= 0
                            }
                        source =
                            browseSourceSpinner?.selectedItemPosition?.minus(1)?.takeIf {
                                it >= 0
                            }
                        genre = genreTagMap!!.values.filter { it.isTagged }.map { it.tag }
                        tags = tagTagMap!!.values.filter { it.isTagged }.map { it.tag }
                    }
                }
                SearchTypes.CHARACTER.ordinal -> {
                    CharacterSearchFilterModel().apply {
                        query = browseSearchEt?.text?.toString()
                    }
                }
                SearchTypes.STAFF.ordinal -> {
                    StaffSearchFilterModel().apply {
                        query = browseSearchEt?.text?.toString()
                    }
                }
                SearchTypes.STUDIO.ordinal -> {
                    StudioSearchFilterModel().apply {
                        query = browseSearchEt?.text?.toString()
                    }
                }
                else -> {
                    null
                }
            }
        }
        set(value) {
            field = value
            when (value) {
                is MediaSearchFilterModel -> {
                    value.let {
                        browseSearchEt?.setText(value.query ?: "")
                        it.type?.let {
                            browseTypeSpinner?.setSelection(it)
                        }
                        it.season?.let {
                            browseSeasonSpinner?.setSelection(it + 1)
                        }
                        if (it.yearEnabled) {
                            browseYearSeekBar?.setProgress(
                                it.minYear!!.toFloat(),
                                it.maxYear!!.toFloat()
                            )
                        }
                        it.sort?.let {
                            browseSortSpinner?.setSelection(it + 1)
                        }
                        it.format?.let {
                            browseFormatSpinner?.setSelection(it + 1)
                        }
                        it.status?.let {
                            browseStatusSpinner?.setSelection(it + 1)
                        }
                        it.streamingOn?.forEach {
                            streamingTagMap!![it]?.isTagged = true
                        }

                        it.countryOfOrigin?.let {
                            browseCountrySpinner.setSelection(it + 1)
                        }
                        it.source?.let {
                            browseSortSpinner.setSelection(it + 1)
                        }
                        it.genre?.forEach {
                            genreTagMap!![it]?.isTagged = true
                        }
                        it.tags?.forEach {
                            tagTagMap!![it]?.isTagged = true
                        }

                        mListener?.updateTags()
                        mListener?.updateGenre()
                        mListener?.updateStream()

                    }
                }
                is CharacterSearchFilterModel -> {
                    browseSearchEt?.setText(value.query ?: "")
                }
                is StaffSearchFilterModel -> {
                    browseSearchEt?.setText(value.query ?: "")
                }
                is StudioSearchFilterModel -> {
                    browseSearchEt?.setText(value.query ?: "")
                }
            }
        }


//    private var seekBarMode = RangeSeekBar.SEEKBAR_MODE_RANGE
//        set(value) {
//            field = value
//            searchYearIndicator.seekBarMode = value
//            searchYearIndicator.invalidate()
//            yearRangeToggleButton.checked = value == RangeSeekBar.SEEKBAR_MODE_RANGE
//        }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0) {
        addView(rView)
        rView.browseSearchInputLayout.apply {
            this.setEndIconTintList(ColorStateList.valueOf(DynamicTheme.getInstance().get().tintAccentColor))
            this.setStartIconTintList(ColorStateList.valueOf(DynamicTheme.getInstance().get().accentColorDark))
        }
        updateTheme(rView)
        updateView(rView)
        updateRecyclerView(rView)
        updateListener(rView)
    }

    private fun updateRecyclerView(rView: View) {
        rView.apply {
            streamingOnRecyclerView.layoutManager = FlexboxLayoutManager(context)
            genreRecyclerView.layoutManager = FlexboxLayoutManager(context)
            tagRecyclerView.layoutManager = FlexboxLayoutManager(context)
        }
    }


    fun buildStreamAdapter(builder: Adapter.Builder, list: List<TagField>) {
        list.forEach {
            streamingTagMap!![it.tag]?.isTagged = it.isTagged
        }
        invalidateStreamAdapter(builder)
    }

    fun buildGenreAdapter(builder: Adapter.Builder, list: List<TagField>) {
        list.forEach {
            genreTagMap!![it.tag]?.isTagged = it.isTagged
        }
        invalidateGenreAdapter(builder)
    }


    fun buildTagAdapter(builder: Adapter.Builder, list: List<TagField>) {
        list.forEach {
            tagTagMap!![it.tag]?.isTagged = it.isTagged
        }
        invalidateTagAdapter(builder)
    }


    fun invalidateGenreAdapter(builder: Adapter.Builder) {
        genreAdapter = builder
            .addSource(
                Source.fromList(genreTagMap!!.values.filter { it.isTagged }.map { it.tag })
            )
            .addPresenter(genrePresenter)
            .into(genreRecyclerView)
    }


    fun invalidateTagAdapter(builder: Adapter.Builder) {
        tagAdapter = builder
            .addSource(
                Source.fromList(tagTagMap!!.values.filter { it.isTagged }.map { it.tag })
            )
            .addPresenter(tagPresenter)
            .into(tagRecyclerView)
    }

    fun invalidateStreamAdapter(builder: Adapter.Builder) {
        streamAdapter = builder
            .addSource(
                Source.fromList(streamingTagMap!!.values.filter { it.isTagged }.map { it.tag })
            )
            .addPresenter(streamPresenter)
            .into(streamingOnRecyclerView)
    }


    fun updateView(rView: View) {
        val searchTypeItems: List<DynamicSpinnerItem>
        val searchSeasonItems: List<DynamicSpinnerItem>
        val searchSortItems: List<DynamicSpinnerItem>
        val searchFormatItems: List<DynamicSpinnerItem>
        val searchStatusItems: List<DynamicSpinnerItem>
        val searchSourceItems: List<DynamicSpinnerItem>
        val searchCountryItems: List<DynamicSpinnerItem>

        rView.apply {
            searchTypeItems = context.resources.getStringArray(R.array.advance_search_type).map {
                DynamicSpinnerItem(
                    null, it
                )
            }
            searchSeasonItems =
                context.resources.getStringArray(R.array.advance_search_season).map {
                    DynamicSpinnerItem(
                        null, it
                    )
                }
            searchSortItems = context.resources.getStringArray(R.array.advance_search_sort).map {
                DynamicSpinnerItem(
                    null, it
                )
            }

            searchFormatItems =
                context.resources.getStringArray(R.array.advance_search_format).map {
                    DynamicSpinnerItem(
                        null, it
                    )
                }

            searchStatusItems =
                context.resources.getStringArray(R.array.advance_search_status).map {
                    DynamicSpinnerItem(
                        null, it
                    )
                }

            searchSourceItems =
                context.resources.getStringArray(R.array.advance_search_source).map {
                    DynamicSpinnerItem(
                        null, it
                    )
                }
            searchCountryItems =
                context.resources.getStringArray(R.array.advance_search_country).map {
                    DynamicSpinnerItem(
                        null, it
                    )
                }

            browseTypeSpinner.adapter = makeSpinnerAdapter(searchTypeItems)
            browseSeasonSpinner.adapter = makeSpinnerAdapter(searchSeasonItems)
            browseSortSpinner.adapter = makeSpinnerAdapter(searchSortItems)
            browseFormatSpinner.adapter = makeSpinnerAdapter(searchFormatItems)
            browseStatusSpinner.adapter = makeSpinnerAdapter(searchStatusItems)
            browseSourceSpinner.adapter = makeSpinnerAdapter(searchSourceItems)
            browseCountrySpinner.adapter = makeSpinnerAdapter(searchCountryItems)

            browseYearSeekBar.isEnabled = enableYearCheckBox.isChecked
            val currentYear = Calendar.getInstance().get(Calendar.YEAR) + 1f
            browseYearSeekBar.setRange(1950f, currentYear)
            browseYearSeekBar.setProgress(1950f, currentYear)
            yearTv.text =
                if (enableYearCheckBox.isChecked)
                    context.getString(R.string.year)
                else
                    context.getString(R.string.year_disabled)
            browseYearSeekBar.progressLeft = 1950

        }

    }

    private fun View.makeSpinnerAdapter(items: List<DynamicSpinnerItem>) =
        DynamicSpinnerImageAdapter(
            context,
            R.layout.ads_layout_spinner_item,
            R.id.ads_spinner_item_icon,
            R.id.ads_spinner_item_text, items
        )

    private fun updateTheme(rView: View) {
        rView.apply {
            ThemeController.lightSurfaceColor().let {
                searchTypeFrameLayout.setBackgroundColor(it)
                browseSeasonFrameLayout.setBackgroundColor(it)
                browseSortFrameLayout.setBackgroundColor(it)
                browseFormatFrameLayout.setBackgroundColor(it)
                browseStatusFrameLayout.setBackgroundColor(it)
                browseSourceFrameLayout.setBackgroundColor(it)
                browseCountryFrameLayout.setBackgroundColor(it)
                browseStreamingFrameLayout.setBackgroundColor(it)
                genreFrameLayout.setBackgroundColor(it)
                tagFrameLayout.setBackgroundColor(it)
                tagAddIv.background = rippleDrawable
                genreAddIv.background = rippleDrawable
                streamAddIv.background = rippleDrawable

                browseYearSeekBar.setIndicatorTextDecimalFormat("0")
                browseYearSeekBar.setTypeface(
                    ResourcesCompat.getFont(
                        context,
                        R.font.open_sans_light
                    )
                )

            }

            DynamicTheme.getInstance().get().accentColor.let {
                browseYearSeekBar.progressColor = it
                browseYearSeekBar.leftSeekBar?.indicatorBackgroundColor = it
                browseYearSeekBar.rightSeekBar?.indicatorBackgroundColor = it
            }
        }
    }


    private fun updateListener(rView: View) {
        rView.apply {
            enableYearCheckBox.setOnCheckedChangeListener { _, isChecked ->
                browseYearSeekBar.isEnabled = isChecked
                yearTv.text =
                    if (isChecked)
                        context.getString(R.string.year)
                    else
                        context.getString(R.string.year_disabled)
            }

//            yearRangeToggleButton.setToggleListener {
//                seekBarMode =
//                    if (it) RangeSeekBar.SEEKBAR_MODE_RANGE else RangeSeekBar.SEEKBAR_MODE_SINGLE
//            }

            browseTypeSpinner.onItemSelected {
                if (it == 0 || it == 1) {
                    browseMediaFilterContainer.visibility = View.VISIBLE

                } else {
                    browseMediaFilterContainer.visibility = View.GONE
                }
            }

            streamAddIv.setOnClickListener {
                mListener?.onStreamAdd(streamingTagMap!!.values.toList())
            }

            genreAddIv.setOnClickListener {
                mListener?.onGenreAdd(genreTagMap!!.values.toList())
            }

            tagAddIv.setOnClickListener {
                mListener?.onTagAdd(tagTagMap!!.values.toList())
            }



            applyFilterCardView.setOnClickListener {
                context.hideKeyboard(browseSearchEt)
                applyFilter()
            }
        }
    }

    private fun View.applyFilter() {
        mListener?.applyFilter()
    }


    fun setNavigationCallbackListener(listener: AdvanceBrowseNavigationCallbackListener) {
        mListener = listener
    }

    interface AdvanceBrowseNavigationCallbackListener {
        fun onGenreAdd(tags: List<TagField>)
        fun onStreamAdd(tags: List<TagField>)
        fun onTagAdd(tags: List<TagField>)
        fun onTagRemoved(tag: String)
        fun onGenreRemoved(tag: String)
        fun onStreamRemoved(tag: String)
        fun updateGenre()
        fun updateTags()
        fun updateStream()
        fun getQuery(): String
        fun applyFilter()
    }


}