package com.revolgenx.anilib.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.pranavpandey.android.dynamic.support.activity.DynamicSystemActivity
import com.pranavpandey.android.dynamic.support.theme.DynamicTheme
import com.revolgenx.anilib.R
import com.revolgenx.anilib.controller.AppController
import com.revolgenx.anilib.controller.ThemeController
import com.revolgenx.anilib.fragment.ThemeControllerFragment
import com.revolgenx.anilib.fragment.base.BaseFragment
import com.revolgenx.anilib.fragment.base.ParcelableFragment
import kotlinx.android.synthetic.main.container_activity.*
import java.util.*

class ContainerActivity : DynamicSystemActivity() {

    companion object {
        const val fragmentContainerKey = "fragment_container_key"

        fun <T : BaseFragment> openActivity(
            context: Context,
            parcelableFragment: ParcelableFragment<T>
        ) {
            context.startActivity(Intent(context, ContainerActivity::class.java).also {
                it.putExtra(fragmentContainerKey, parcelableFragment)
            })
        }
    }

    override fun getLocale(): Locale? {
        return null
    }


    override fun getThemeRes(): Int {
        return ThemeController.appStyle
    }

    override fun onCustomiseTheme() {
        ThemeController.setLocalTheme()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.container_activity)
        fragmentContainer.setBackgroundColor(DynamicTheme.getInstance().get().backgroundColor)
        statusBarColor = statusBarColor
        val parcel =
            intent.getParcelableExtra<ParcelableFragment<BaseFragment>>(fragmentContainerKey)
                ?: return

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, parcel.clzz.newInstance().apply {
            arguments = parcel.bundle
        }).commitNow()
    }

    override fun setStatusBarColor(color: Int) {
        super.setStatusBarColor(color)
        setWindowStatusBarColor(statusBarColor);
    }

    override fun setNavigationBarTheme(): Boolean {
        return AppController.instance.isThemeNavigationBar
    }

}