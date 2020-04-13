package com.revolgenx.anilib.viewmodel

import com.revolgenx.anilib.service.MediaListEntryService
import com.revolgenx.anilib.service.list.MediaListService

class CompletedViewModel(mediaListService: MediaListService, entryService: MediaListEntryService) :
    MediaListViewModel(mediaListService, entryService) {

}