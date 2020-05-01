package com.revolgenx.anilib.fragment.base

import android.os.Bundle
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
data class NavViewPagerParcelableFragments(
    val clzzes: List<String>,
    val bundles: List<Bundle>
): Parcelable