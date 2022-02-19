package com.revolgenx.anilib.home.discover.presenter

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.otaliastudios.elements.Presenter
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.revolgenx.anilib.R
import com.revolgenx.anilib.entry.data.meta.EntryEditorMeta
import com.revolgenx.anilib.media.data.meta.MediaInfoMeta
import com.revolgenx.anilib.common.preference.loggedIn
import com.revolgenx.anilib.common.preference.disableCardStyleInHomeScreen
import com.revolgenx.anilib.databinding.MediaPresenterLayoutBinding
import com.revolgenx.anilib.infrastructure.event.OpenMediaInfoEvent
import com.revolgenx.anilib.infrastructure.event.OpenMediaListEditorEvent
import com.revolgenx.anilib.infrastructure.event.OpenSearchEvent
import com.revolgenx.anilib.common.presenter.Constant
import com.revolgenx.anilib.media.data.model.MediaModel
import com.revolgenx.anilib.search.data.model.filter.SearchFilterModel
import com.revolgenx.anilib.ui.view.makeToast
import com.revolgenx.anilib.util.loginContinue
import com.revolgenx.anilib.util.naText

class MediaPresenter(
    context: Context,
    private val onMediaClickedListener: (media: MediaModel) -> Unit
) :
    Presenter<MediaModel>(context) {
    override val elementTypes: Collection<Int>
        get() = listOf(0)

    companion object {
        const val SELECTABLE_MEDIA_MODEL_KEY = "SELECTABLE_MEDIA_MODEL_KEY"
    }

    override fun onCreate(parent: ViewGroup, elementType: Int): Holder {
        return MediaPresenterLayoutBinding.inflate(getLayoutInflater(), parent, false)
            .let { binding ->
                binding.mediaMetaBackground.setBackgroundColor(
                    ColorUtils.setAlphaComponent(
                        DynamicTheme.getInstance().get().backgroundColor,
                        220
                    )
                )
                Holder(binding.root).also { it[Constant.PRESENTER_BINDING_KEY] = binding }
            }
    }

    private val mediaFormats by lazy {
        context.resources.getStringArray(R.array.media_format)
    }

    private val disableCardInHomeScreen: Boolean by lazy {
        disableCardStyleInHomeScreen()
    }

    private val dynamicTheme get() = DynamicTheme.getInstance().get()
    private val accentColor = dynamicTheme.accentColor


    override fun onBind(page: Page, holder: Holder, element: Element<MediaModel>) {
        super.onBind(page, holder, element)
        val item = element.data ?: return
        val binding: MediaPresenterLayoutBinding = holder[Constant.PRESENTER_BINDING_KEY] ?: return

        holder[SELECTABLE_MEDIA_MODEL_KEY] = item
        binding.apply {
            mediaSimpleDrawee.setImageURI(item.coverImage?.image(context))
            mediaRatingTv.text = item.averageScore
            mediaTitleTv.text = item.title?.title(context)
            mediaFormatTv.text = context.getString(R.string.format_episode_s).format(
                item.format?.let { mediaFormats[it] }.naText(),
                item.episodes.naText()
            )
            mediaFormatTv.status = item.mediaListEntry?.status

            mediaGenreLayout.addGenre(item.genres?.take(3)) { genre ->
                OpenSearchEvent(SearchFilterModel(genre = genre)).postEvent
            }

            mediaPresenterCardView.strokeColor =
                if (item.isSelected && !disableCardInHomeScreen) accentColor else Color.TRANSPARENT

            if (!disableCardInHomeScreen) {
                item.onClickListener = { selected ->
                    mediaPresenterCardView.strokeColor =
                        if (selected) accentColor else Color.TRANSPARENT
                    mediaPresenterCardView.invalidate()
                }

                if (item.isSelected) {
                    onMediaClickedListener.invoke(item.also { it.isSelected = true })
                }
            }


            holder.itemView.setOnClickListener {
                if (item.isSelected || disableCardInHomeScreen) {
                    OpenMediaInfoEvent(
                        MediaInfoMeta(
                            item.id,
                            item.type!!,
                            item.title!!.romaji!!,
                            item.coverImage!!.image(context),
                            item.coverImage!!.largeImage,
                            item.bannerImage
                        )
                    ).postEvent
                } else {
                    item.isSelected = true
                    mediaPresenterCardView.strokeColor = accentColor
                    onMediaClickedListener.invoke(item)
                }
            }

            holder.itemView.setOnLongClickListener {
                context.loginContinue {
                    OpenMediaListEditorEvent(item.id).postEvent
                }
                true
            }
        }
    }

    override fun onUnbind(holder: Holder) {
        super.onUnbind(holder)
        val model: MediaModel = holder[SELECTABLE_MEDIA_MODEL_KEY] ?: return
        model.onClickListener = null
    }


}