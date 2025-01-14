package com.revolgenx.anilib.ui.dialog

import android.os.Bundle
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.revolgenx.anilib.R
import com.revolgenx.anilib.common.ui.dialog.BaseDialogFragment
import com.revolgenx.anilib.databinding.ReleaseInfoDialogLayoutBinding
import com.revolgenx.anilib.social.factory.AlMarkwonFactory

class ReleaseInfoDialog : BaseDialogFragment<ReleaseInfoDialogLayoutBinding>() {
    companion object {
        val tag = ReleaseInfoDialog::class.java.name
    }

    override var positiveText: Int? = R.string.close

    override fun bindView(): ReleaseInfoDialogLayoutBinding {
        return ReleaseInfoDialogLayoutBinding.inflate(provideLayoutInflater)
    }

    override fun builder(dialogBuilder: DynamicDialog.Builder, savedInstanceState: Bundle?) {
        AlMarkwonFactory.getMarkwon()
            .setMarkdown(binding.releaseInfo, requireContext().getString(R.string.release_info))
    }

}