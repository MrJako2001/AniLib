package com.revolgenx.anilib.appwidget.service

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.net.toUri
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSources
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.image.CloseableStaticBitmap
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.revolgenx.anilib.R
import com.revolgenx.anilib.appwidget.ui.widget.AiringScheduleWidget
import com.revolgenx.anilib.common.preference.AiringWidgetPreference
import com.revolgenx.anilib.common.preference.getAiringScheduleFieldForWidget
import com.revolgenx.anilib.common.preference.loggedIn
import com.revolgenx.anilib.common.preference.userId
import com.revolgenx.anilib.data.field.home.AiringMediaField
import com.revolgenx.anilib.data.model.airing.AiringMediaModel
import com.revolgenx.anilib.infrastructure.event.ListEditorResultEvent
import com.revolgenx.anilib.infrastructure.repository.util.Resource
import com.revolgenx.anilib.infrastructure.repository.util.Status
import com.revolgenx.anilib.infrastructure.service.airing.AiringMediaService
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.WeekFields
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class AiringScheduleRemoteViewsService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AiringScheduleWidgetFactory(applicationContext, intent)
    }


    class AiringScheduleWidgetFactory(
        private val context: Context, intent: Intent
    ) : RemoteViewsFactory, KoinComponent{

        private val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        private val items = mutableListOf<AiringMediaModel>()

        private val airingMediaService: AiringMediaService by inject()
        private val compositeDisposable = CompositeDisposable()
        private var field: AiringMediaField = AiringMediaField()

        private val weekFields by lazy { WeekFields.of(Locale.getDefault()) }

        private val startDateTime
            get() = if (field.isWeeklyTypeDate)
                ZonedDateTime.now().with(weekFields.dayOfWeek(), 1)
                    .with(LocalTime.MIN) else ZonedDateTime.now().with(LocalTime.MIN)

        private val endDateTime
            get() = if (field.isWeeklyTypeDate)
                ZonedDateTime.now().with(weekFields.dayOfWeek(), 7)
                    .with(LocalTime.MAX) else ZonedDateTime.now().with(LocalTime.MAX)

        private val appWidgetManager = AppWidgetManager.getInstance(context)

        override fun onCreate() {
            if (context.loggedIn()) {
                field.userId = context.userId()
            }
            updateField()
        }

        @Subscribe(sticky =  true, threadMode = ThreadMode.MAIN)
        fun onEVent(event: ListEditorResultEvent){
            field.isNewField = true
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.airing_widget_list_view)
            EventBus.getDefault().removeStickyEvent(event)
        }

        private fun updateField() {
            getAiringScheduleFieldForWidget(context, field)
            field.airingGreaterThan = startDateTime.toEpochSecond().toInt()
            field.airingLessThan = endDateTime.toEpochSecond().toInt()
            field.perPage = 8
            field.page = AiringWidgetPreference.getPage(context, appWidgetId)
        }

        override fun onDataSetChanged() {
            runBlocking {
                showLoader(true)
                updateField()
                val suspendedQuery =
                    suspendCoroutine<Resource<List<AiringMediaModel>>> { continuation ->
                        airingMediaService.getAiringMedia(field, compositeDisposable) {
                            continuation.resume(it)
                        }
                    }
                if (suspendedQuery.status == Status.SUCCESS && suspendedQuery.data != null) {
                    val airingModels = suspendedQuery.data
                    items.clear()
                    items.addAll(airingModels)
                } else {
                    Timber.e(suspendedQuery.exception, suspendedQuery.message)
                }
                showLoader(false)
            }
        }

        override fun getCount(): Int {
            return items.size
        }

        override fun getViewAt(position: Int): RemoteViews? {

            val item = items[position]

            val airingTimeModel = item.airingTimeModel!!
            val airingAtTime = airingTimeModel.airingAt!!.airingTime
            val airingAtDay = airingTimeModel.airingAt!!.airingDayMedium
            val airingEpisode = airingTimeModel.episode
            val timeUntilAiring = airingTimeModel.timeUntilAiring!!
            val coverImage = item.coverImage?.sImage

            val remoteViews =
                RemoteViews(context.packageName, R.layout.airing_schedule_widget_item_layout)

            val fillIntent = Intent()
            fillIntent.putExtra(AiringScheduleWidget.WIDGET_MEDIA_ITEM, item.mediaId)
            remoteViews.setOnClickFillInIntent(R.id.wg_airing_container, fillIntent)

            remoteViews.setTextViewText(
                R.id.wg_media_name_tv,
                items[position].title!!.title(context)
            )

            remoteViews.setTextViewText(
                R.id.wg_airing_at_tv,
                if (field.isWeeklyTypeDate) {
                    context.getString(
                        R.string.widget_airing_at_weekly_format,
                        airingEpisode,
                        timeUntilAiring.day,
                        timeUntilAiring.hour,
                        timeUntilAiring.min
                    )
                } else {
                    context.getString(
                        R.string.widget_airing_at_format,
                        airingEpisode,
                        timeUntilAiring.hour,
                        timeUntilAiring.min
                    )
                }
            )

            remoteViews.setTextViewText(
                R.id.wg_airing_at_time_tv,
                if (field.isWeeklyTypeDate) {
                    "$airingAtDay, $airingAtTime"
                }else{
                    airingAtTime
                }
            )

            val dataSource = Fresco.getImagePipeline().fetchDecodedImage(
                ImageRequestBuilder.newBuilderWithSource(coverImage!!.toUri())
                    .setImageDecodeOptions(
                        ImageDecodeOptions.newBuilder()
                            .setForceStaticImage(true).build()
                    )
                    .setLocalThumbnailPreviewsEnabled(true)
                    .build(),
                context
            )
            val result =
                DataSources.waitForFinalResult<CloseableReference<CloseableImage>>(
                    dataSource
                )
            if (result != null) {
                (result.get() as CloseableStaticBitmap).underlyingBitmap.copy(
                    (result.get() as CloseableStaticBitmap).underlyingBitmap.config,
                    true
                ).let {
                    remoteViews.setImageViewBitmap(R.id.wg_airing_media_iv, it);
                }
            }
            return remoteViews
        }

        override fun getLoadingView(): RemoteViews? {
            return null
        }

        override fun getViewTypeCount(): Int {
            return 1
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun hasStableIds(): Boolean {
            return true
        }


        private fun showLoader(b: Boolean) {
            val widgetRemoteView =
                RemoteViews(context.packageName, R.layout.airing_schedule_widget_layout)
            if (b) {
                widgetRemoteView.setViewVisibility(R.id.wg_loading_bar, View.VISIBLE)
            } else {
                widgetRemoteView.setViewVisibility(R.id.wg_loading_bar, View.GONE)
            }
            appWidgetManager.partiallyUpdateAppWidget(appWidgetId, widgetRemoteView)
        }

        override fun onDestroy() {}
    }
}