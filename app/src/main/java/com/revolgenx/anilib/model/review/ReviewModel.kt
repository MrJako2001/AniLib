package com.revolgenx.anilib.model.review

import com.revolgenx.anilib.model.BaseModel
import com.revolgenx.anilib.model.BasicUserModel
import com.revolgenx.anilib.model.CommonMediaModel

open class ReviewModel : BaseModel() {
    var reviewId: Int? = null
    var summary: String? = null
    var body: String? = null
    var userRating: Int? = null
    var rating: Int? = null
    var ratingAmount: Int? = null
    var score: Int? = null
    var private: Boolean? = null
    var userModel: BasicUserModel? = null
    var mediaModel: CommonMediaModel? = null
}


/*    id
        userId
        mediaId
        summary
        body(asHtml:true)
        rating
        ratingAmount
        score
        private
        userRating
        user{
            id
            name
            avatar{
                large
                medium
            }
        }
        media{
            id
            title{
                ... mediaTitle
            }
            coverImage{
                ... mediaCoverImage
            }
        }
    }*/