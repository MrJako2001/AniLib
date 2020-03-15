package com.revolgenx.anilib.field

import android.content.Context
import com.revolgenx.anilib.SeasonListQuery
import com.revolgenx.anilib.preference.getSeasonField
import com.revolgenx.anilib.type.MediaFormat
import com.revolgenx.anilib.type.MediaSeason
import com.revolgenx.anilib.type.MediaStatus


data class SeasonField(
    var format: Int,
    var year: Int,
    var season: Int,
    var status: Int,
    var sort: Int,
    var genres: List<String>,
    var order: Int
) : BaseField<SeasonListQuery> {
    companion object {
        fun create(context: Context) = getSeasonField(context)

        enum class SeasonOrder {
            ASC, DESC
        }
    }

    var page = 0
    var perPage = 10

//    val sorted:String
//        get() {
//            return if (order == "ASC") sort else sort + "_" + order
//        }


    fun changeSeason(i: Int) {
        season += i
        if (season > 3) {
            year += 1
            season = 0
        } else if (season < 0) {
            year -= 1
            season = 3
        }
    }

    override fun toQueryOrMutation(): SeasonListQuery {
        return SeasonListQuery.builder()
            .page(page)
            .perPage(perPage)
            .also {
                if (format != MediaFormat.`$UNKNOWN`.ordinal) {
                    it.format(MediaFormat.values()[format])
                }
            }
            .seasonYear(year)
            .season(MediaSeason.values()[season]).apply {
                if (status != MediaStatus.`$UNKNOWN`.ordinal) {
                    this.status(MediaStatus.values()[status])
                }
            }
            .build()
    }

//    fun update(context: Context) {
//        setSeasonField(context, this)
//    }

}