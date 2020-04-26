package com.revolgenx.anilib.repository.network.converter

import com.revolgenx.anilib.BasicUserQuery
import com.revolgenx.anilib.MediaOverViewQuery
import com.revolgenx.anilib.MediaWatchQuery
import com.revolgenx.anilib.fragment.MediaListContent
import com.revolgenx.anilib.fragment.NarrowMediaContent
import com.revolgenx.anilib.model.*
import com.revolgenx.anilib.model.entry.MediaEntryListModel
import com.revolgenx.anilib.model.season.SeasonMediaModel
import com.revolgenx.anilib.util.pmap
import kotlinx.coroutines.runBlocking

fun NarrowMediaContent.getCommonMedia() =
    SeasonMediaModel().also {
        it.mediaId = id()
        it.title = TitleModel().also { title ->
            title.english = title()!!.english() ?: title()!!.romaji()
            title.romaji = title()!!.romaji()
            title.native = title()!!.native_() ?: title()!!.romaji()
            title.userPreferred = title()!!.userPreferred()
        }
        it.format = format()!!.ordinal
        it.type = type()!!.ordinal
        it.episodes = episodes()?.toString()
        it.duration = duration()?.toString()
        it.chapters = chapters()?.toString()
        it.volumes = volumes()?.toString()
        it.status = status()!!.ordinal
        it.coverImage = CoverImageModel().also { image ->
            image.medium = coverImage()!!.medium()
            image.large = coverImage()!!.large()
            image.extraLarge = coverImage()!!.extraLarge()
        }
        it.genres = genres()
        it.averageScore = averageScore()
        startDate()?.let { date ->
            it.startDate = DateModel().also { dateModel ->
                dateModel.year = date.year()
                dateModel.month = date.month()
                dateModel.day = date.day()
                dateModel.date = dateModel.toString()
            }
        }
        endDate()?.let { date ->
            it.endDate = DateModel().also { dateModel ->
                dateModel.year = date.year()
                dateModel.month = date.month()
                dateModel.day = date.day()
                dateModel.date = dateModel.toString()
            }
        }
        it.bannerImage = bannerImage() ?: it.coverImage!!.extraLarge
        it.mediaEntryListModel = MediaEntryListModel(mediaListEntry()?.let { it.progress() ?: 0 })
    }

fun BasicUserQuery.User.toBasicUserModel() = BasicUserModel().also {
    it.userId = id()
    it.userName = name()
    it.scoreFormat = mediaListOptions()!!.scoreFormat()!!.ordinal
    it.avatar = UserAvatarImageModel().also { img ->
        img.large = avatar()!!.large()
        img.medium = avatar()!!.medium()
    }
    it.bannerImage = bannerImage() ?: ""
}


fun MediaListContent.toListEditorMediaModel() = EntryListEditorMediaModel().also {
    it.mediaId = mediaId()
    it.userId = userId()
    it.listId = id()
    it.status = status()!!.ordinal
    it.notes = notes() ?: ""
    it.progress = progress()!!
    it.progressVolumes = progressVolumes() ?: 0
    it.private = private_()!!
    it.repeat = repeat()!!
    it.startDate = startedAt()!!.let {
        DateModel().also { date ->
            date.day = it.day()
            date.month = it.month()
            date.year = it.year()
        }
    }
    it.endDate = completedAt()!!.let {
        DateModel().also { date ->
            date.day = it.day()
            date.month = it.month()
            date.year = it.year()
        }
    }

}


fun MediaOverViewQuery.Media.toMediaOverviewModel() = MediaOverviewModel().also {
    runBlocking {

        it.mediaId = id()
        it.title = title()?.fragments()?.mediaTitle()?.let {
            TitleModel().also { title ->
                title.english = it.english()
                title.romaji = it.romaji()
                title.native = it.native_()
                title.userPreferred = it.userPreferred()
            }
        }
        it.startDate = startDate()?.fragments()?.fuzzyDate()?.let {
            DateModel(it.year(), it.month(), it.day())
        }
        it.endDate = endDate()?.fragments()?.fuzzyDate()?.let {
            DateModel(it.year(), it.month(), it.day())
        }
        it.genres = genres()
        it.episodes = episodes()?.toString()
        it.chapters = chapters()?.toString()
        it.volumes = volumes()?.toString()
        it.averageScore = averageScore()
        it.meanScore = meanScore()
        it.duration = duration()?.toString()
        it.status = status()?.ordinal
        it.format = format()?.ordinal
        it.type = type()?.ordinal
        it.isAdult = isAdult ?: false
        it.popularity = popularity()
        it.favourites = favourites()
        it.season = season()?.ordinal
        it.seasonYear = seasonYear()
        it.description = description()
        it.source = source()?.ordinal
        it.hashTag = hashtag()
        it.airingTimeModel = nextAiringEpisode()?.let {
            AiringTimeModel().also { model ->
                model.episode = it.episode()
                model.airingTime = AiringTime().also { ti -> ti.time = it.timeUntilAiring() }
            }
        }
        it.relationship = relations()?.edges()?.pmap {
            MediaRelationshipModel().also { rel ->
                rel.relationshipType = it.relationType()?.ordinal
                it.node()?.let { node ->
                    rel.mediaId = node.id()
                    rel.title = node.title()?.fragments()?.mediaTitle()?.let {
                        TitleModel().also { titleModel ->
                            titleModel.english = it.english()
                            titleModel.romaji = it.romaji()
                            titleModel.native = it.native_()
                            titleModel.userPreferred = it.userPreferred()
                        }
                    }
                    rel.format = node.format()?.ordinal
                    rel.type = node.type()?.ordinal
                    rel.status = node.status()?.ordinal
                    rel.averageScore = node.averageScore()
                    rel.seasonYear = node.seasonYear()
                    rel.coverImage = node.coverImage()?.fragments()?.mediaCoverImage()?.let {
                        CoverImageModel().also { model ->
                            model.large = it.large()
                            model.extraLarge = it.extraLarge()
                            model.medium = it.medium()
                        }
                    }
                    rel.bannerImage = node.bannerImage() ?: rel.coverImage?.largeImage
                }
            }
        }
        it.externalLink = externalLinks()?.pmap {
            MediaExternalLinkModel().also { link ->
                link.linkId = it.id()
                link.site = it.site()
                link.url = it.url()
            }
        }
        it.tags = tags()?.pmap {
            MediaTagsModel().also { tag ->
                tag.name = it.name()
                tag.isSpoilerTag = it.isMediaSpoiler ?: false
            }
        }
        it.trailer = trailer()?.let {
            MediaTrailerModel().also { trailer ->
                trailer.id = it.id()
                trailer.site = it.site()
                trailer.thumbnail = it.thumbnail()
            }
        }
        it.studios = studios()?.edges()?.pmap {
            MediaStudioModel().also { studio ->
                it.node()?.let { node ->
                    studio.studioName = node.name()
                    studio.isAnimationStudio = node.isAnimationStudio
                    studio.studioId = node.id()
                }
            }
        }
    }
}

fun MediaWatchQuery.Media.toModel() = streamingEpisodes()?.map {
    MediaWatchModel().also { model ->
        model.site = it.site()
        model.thumbnail = it.thumbnail()
        model.title = it.title()
        model.url = it.url()
    }
}
