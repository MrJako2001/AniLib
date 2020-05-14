package com.revolgenx.anilib.service.review

import androidx.lifecycle.MutableLiveData
import com.apollographql.apollo.exception.ApolloHttpException
import com.revolgenx.anilib.DeleteReviewMutation
import com.revolgenx.anilib.ReviewQuery
import com.revolgenx.anilib.SaveReviewMutation
import com.revolgenx.anilib.field.reivew.ReviewField
import com.revolgenx.anilib.model.BasicUserModel
import com.revolgenx.anilib.model.CommonMediaModel
import com.revolgenx.anilib.model.UserAvatarImageModel
import com.revolgenx.anilib.model.review.ReviewModel
import com.revolgenx.anilib.repository.network.BaseGraphRepository
import com.revolgenx.anilib.repository.network.converter.toModel
import com.revolgenx.anilib.repository.util.ERROR
import com.revolgenx.anilib.repository.util.Resource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber
import java.net.HttpURLConnection

class ReviewServiceImpl(private val graphRepository: BaseGraphRepository) : ReviewService {

    override val reviewLiveData: MutableLiveData<Resource<ReviewModel>> = MutableLiveData()

    override fun getReview(
        field: ReviewField,
        compositeDisposable: CompositeDisposable
    ) {
        val disposable = graphRepository.request(field.toQueryOrMutation() as ReviewQuery).map {
            it.data()?.Review()?.let {
                ReviewModel().also { model ->
                    model.reviewId = it.id()
                    model.rating = it.rating()
                    model.ratingAmount = it.ratingAmount()
                    model.userRating = it.userRating()?.ordinal
                    model.summary = it.summary()
                    model.body = it.body()
                    model.score = it.score()
                    model.private = it.private_()
                    model.userModel = it.user()?.let {
                        BasicUserModel().also { user ->
                            user.userId = it.id()
                            user.userName = it.name()
                            user.avatar = it.avatar()?.let {
                                UserAvatarImageModel().also { img ->
                                    img.large = it.large()
                                    img.medium = it.medium()
                                }
                            }
                        }
                    }
                    model.mediaModel = it.media()?.let {
                        CommonMediaModel().also { media ->
                            media.mediaId = it.id()
                            media.title = it.title()?.fragments()?.mediaTitle()?.toModel()
                            media.coverImage =
                                it.coverImage()?.fragments()?.mediaCoverImage()?.toModel()
                        }
                    }
                }
            }
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                reviewLiveData.value = Resource.success(it)
            }, {
                if (it is ApolloHttpException) {
                    if (it.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                        reviewLiveData.value = Resource.success(null)
                    } else {
                        Timber.e(it)
                        reviewLiveData.value = Resource.error(it.message ?: ERROR, null, it)
                    }
                } else {
                    Timber.e(it)
                    reviewLiveData.value = Resource.error(it.message ?: ERROR, null, it)
                }
            })
        compositeDisposable.add(disposable)
    }


    override fun saveReview(
        field: ReviewField,
        compositeDisposable: CompositeDisposable,
        callback: (Resource<Boolean>) -> Unit
    ) {
        val disposable = graphRepository.request(field.toQueryOrMutation() as SaveReviewMutation)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                callback.invoke(Resource.success(true))
            }, {
                Timber.e(it)
                callback.invoke(Resource.error(it.message ?: ERROR, false, it))
            })

        compositeDisposable.add(disposable)
    }

    override fun deleteReview(
        field: ReviewField,
        compositeDisposable: CompositeDisposable,
        callback: (Resource<Boolean>) -> Unit
    ) {
        val disposable = graphRepository.request(field.toQueryOrMutation() as DeleteReviewMutation)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                callback.invoke(Resource.success(true))
            }, {
                Timber.e(it)
                callback.invoke(Resource.error(it.message ?: ERROR, false, it))
            })
        compositeDisposable.add(disposable)
    }

}