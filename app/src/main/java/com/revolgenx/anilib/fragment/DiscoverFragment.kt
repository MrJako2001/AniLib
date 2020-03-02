package com.revolgenx.anilib.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.revolgenx.anilib.R
import com.revolgenx.anilib.fragment.base.BasePagerFragment

class DiscoverFragment : BasePagerFragment() {

    companion object {
        fun newInstance() = DiscoverFragment()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.discover_fragment_layout, container, false)
    }

    override fun title(context: Context): String? {
        return context.getString(R.string.discover)
    }
}
