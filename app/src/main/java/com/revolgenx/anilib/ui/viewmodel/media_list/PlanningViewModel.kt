package com.revolgenx.anilib.ui.viewmodel.media_list

import com.revolgenx.anilib.infrastructure.service.list.MediaListService
import com.revolgenx.anilib.infrastructure.service.media.MediaListEntryService

class PlanningViewModel(
    mediaListService: MediaListService,
    entryService: MediaListEntryService
) :
    MediaListCollectionViewModel(mediaListService, entryService) {

}
