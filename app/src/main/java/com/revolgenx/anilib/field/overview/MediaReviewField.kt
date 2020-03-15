package com.revolgenx.anilib.field.overview

import com.revolgenx.anilib.MediaReviewQuery
import com.revolgenx.anilib.field.BaseField
import com.revolgenx.anilib.field.BaseField.Companion.PER_PAGE

class MediaReviewField :BaseField<MediaReviewQuery>{
    var mediaId = -1
    var page = 1
    var perPage = PER_PAGE

    override fun toQueryOrMutation(): MediaReviewQuery {
        return MediaReviewQuery.builder()
            .page(page)
            .perPage(perPage)
            .mediaId(mediaId)
            .build()
    }

}