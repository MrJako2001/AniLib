package com.revolgenx.anilib.infrastructure.source.friend

import com.otaliastudios.elements.Element
import com.otaliastudios.elements.Page
import com.revolgenx.anilib.common.infrastruture.source.BaseRecyclerSource
import com.revolgenx.anilib.data.field.friend.UserFriendField
import com.revolgenx.anilib.data.model.user.UserModel
import com.revolgenx.anilib.infrastructure.service.user.UserService
import io.reactivex.disposables.CompositeDisposable

class UserFriendSource(
    field: UserFriendField,
    private val userService: UserService,
    private val compositeDisposable: CompositeDisposable
) :
    BaseRecyclerSource<UserModel, UserFriendField>(field) {
    override fun areItemsTheSame(first: UserModel, second: UserModel): Boolean {
        return first.id == second.id
    }

    override fun onPageOpened(page: Page, dependencies: List<Element<*>>) {
        super.onPageOpened(page, dependencies)
        field.page = pageNo
        userService.getUserFriend(field, compositeDisposable) {
            postResult(page, it)
        }
    }
}