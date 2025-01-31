package com.revolgenx.anilib.social.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.otaliastudios.elements.Presenter
import com.otaliastudios.elements.Source
import com.pranavpandey.android.dynamic.support.model.DynamicMenu
import com.revolgenx.anilib.R
import com.revolgenx.anilib.common.ui.fragment.BasePresenterFragment
import com.revolgenx.anilib.constant.AlActivityType
import com.revolgenx.anilib.databinding.ActivityUnionFragmentLayoutBinding
import com.revolgenx.anilib.common.event.OpenActivityTextComposer
import com.revolgenx.anilib.social.data.model.ActivityUnionModel
import com.revolgenx.anilib.social.ui.presenter.ActivityUnionPresenter
import com.revolgenx.anilib.social.ui.viewmodel.ActivityInfoViewModel
import com.revolgenx.anilib.social.ui.viewmodel.ActivityUnionViewModel
import com.revolgenx.anilib.social.ui.viewmodel.activity_composer.ActivityMessageComposerViewModel
import com.revolgenx.anilib.social.ui.viewmodel.activity_composer.ActivityTextComposerViewModel
import com.revolgenx.anilib.type.ActivityType
import com.revolgenx.anilib.ui.view.makeArrayPopupMenu
import com.revolgenx.anilib.ui.view.makeSpinnerAdapter
import com.revolgenx.anilib.util.onItemSelected
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class ActivityUnionFragment : BasePresenterFragment<ActivityUnionModel>() {
    override val applyInset: Boolean = false
    override val basePresenter: Presenter<ActivityUnionModel>
        get() = ActivityUnionPresenter(
            requireContext(),
            viewModel,
            activityInfoViewModel,
            textComposerViewModel,
            messageComposerViewModel
        )
    override val baseSource: Source<ActivityUnionModel>
        get() = viewModel.source ?: createSource()


    private val viewModel by viewModel<ActivityUnionViewModel>()
    private val textComposerViewModel by sharedViewModel<ActivityTextComposerViewModel>()
    private val messageComposerViewModel by sharedViewModel<ActivityMessageComposerViewModel>()
    private val activityInfoViewModel by sharedViewModel<ActivityInfoViewModel>()


    private var _uBinding: ActivityUnionFragmentLayoutBinding? = null
    private val uBinding get() = _uBinding!!

    private val activityAdapterItems by lazy {
        resources.getStringArray(R.array.activity_following_entries).map {
            DynamicMenu(
                null, it
            )
        }
    }

    override fun createSource(): Source<ActivityUnionModel> {
        return viewModel.createSource()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = super.onCreateView(inflater, container, savedInstanceState)

        _uBinding = ActivityUnionFragmentLayoutBinding.inflate(inflater, container, false)
        uBinding.activityUnionContainer.addView(v)

        baseRecyclerView.recycledViewPool.setMaxRecycledViews(ActivityType.TEXT.ordinal, 10)
        return uBinding.root
    }

    private val onTabSelectListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab?) {
        }

        override fun onTabUnselected(tab: TabLayout.Tab?) {
        }

        override fun onTabReselected(tab: TabLayout.Tab?) {
            baseRecyclerView.smoothScrollToPosition(0)
        }
    }

    override fun getBaseLayoutManager() = LinearLayoutManager(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uBinding.activityUnionTabLayout.addOnTabSelectedListener(onTabSelectListener)

        uBinding.activityTypePopupMenu.setOnClickListener {
            makeArrayPopupMenu(
                it,
                resources.getStringArray(R.array.activity_type_entries),
                selectedPosition = viewModel.field.type.ordinal
            ) { _, _: View, position: Int, _: Long ->
                viewModel.field.type = AlActivityType.values()[position]
                viewModel.storeField()
                createSource()
                invalidateAdapter()
            }
        }

        val adapter = makeSpinnerAdapter(requireContext(), activityAdapterItems)
        uBinding.activityGlobalSpinner.adapter = adapter
        uBinding.activityGlobalSpinner.onItemSelectedListener = null
        uBinding.activityGlobalSpinner.setSelection(if (viewModel.field.isFollowing) 0 else 1)

        uBinding.activityGlobalSpinner.onItemSelected {
            if (!visibleToUser) return@onItemSelected
            viewModel.field.isFollowing = it == 0
            viewModel.storeField()
            createSource()
            invalidateAdapter()
        }

        uBinding.activityCreateFab.setOnClickListener {
            OpenActivityTextComposer().postEvent
        }
    }

    override fun onDestroyView() {
        uBinding.activityUnionTabLayout.removeOnTabSelectedListener(onTabSelectListener)
        super.onDestroyView()
        _uBinding = null
    }
}