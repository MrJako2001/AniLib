package com.revolgenx.anilib.social.data.field

import com.revolgenx.anilib.DeleteActivityReplyMutation
import com.revolgenx.anilib.data.field.BaseField

class DeleteActivityReplyField : BaseField<DeleteActivityReplyMutation>() {
    var id: Int? = null
    override fun toQueryOrMutation(): DeleteActivityReplyMutation {
        return DeleteActivityReplyMutation.builder().id(id).build()
    }
}