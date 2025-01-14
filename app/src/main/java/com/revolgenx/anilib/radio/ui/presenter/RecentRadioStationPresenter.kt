package com.revolgenx.anilib.radio.ui.presenter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.revolgenx.anilib.common.presenter.BasePresenter.Companion.PRESENTER_BINDING_KEY
import com.revolgenx.anilib.databinding.RecentRadioStationPresenterBinding
import com.revolgenx.anilib.radio.repository.room.RadioStation
import com.revolgenx.anilib.radio.ui.util.RadioPlayerHelper

class RecentRadioStationPresenter(context: Context) : Presenter<RadioStation>(context) {
    override val elementTypes: Collection<Int> = listOf(0)

    private val accentColor = DynamicTheme.getInstance().get().accentColor

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return RecentRadioStationPresenterBinding.inflate(
            LayoutInflater.from(context),
            parent,
            false
        ).let { binding ->
            Holder(binding.root).also { it[PRESENTER_BINDING_KEY] = binding }
        }
    }

    override fun onBind(page: Page, holder: Holder, element: Element<RadioStation>) {
        super.onBind(page, holder, element)
        val station = element.data ?: return
        val binding = holder.get<RecentRadioStationPresenterBinding>(PRESENTER_BINDING_KEY)

        binding.radioStationNameTv.text = station.name
        binding.radioStationIv.setImageURI(station.logo)

        binding.root.setOnClickListener {
            RadioPlayerHelper.pausePlay(context, station.id)
        }
    }

}