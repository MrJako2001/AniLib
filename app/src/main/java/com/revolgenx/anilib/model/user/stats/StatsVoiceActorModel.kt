package com.revolgenx.anilib.model.user.stats

class StatsVoiceActorModel : BaseStatsModel() {
    var voiceActorId: Int? = null
        set(value) {
            field = value
            baseId = value
        }
    var name: String? = null
    var image: String? = null
}