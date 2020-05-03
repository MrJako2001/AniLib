package com.revolgenx.anilib.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.pranavpandey.android.dynamic.support.dialog.fragment.DynamicDialogFragment
import com.pranavpandey.android.dynamic.support.widget.DynamicButton

class MessageDialog : DynamicDialogFragment() {
    var dialogCallback: ((dialogInterface: DialogInterface, which: Int) -> Unit)? = null
    var dialogOnShowListener: DialogInterface.OnShowListener? = null

    companion object Builder {

        val messageDialogTag = MessageDialog::class.java.simpleName

        const val titleKey = "titleKey"
        const val messageKey = "messageKey"
        const val messageResKey = "messageResKey"
        const val positiveKey = "positiveKey"
        const val negativeKey = "negativeKey"
        const val neutralKey = "neutralKey"
        const val viewKey = "viewKey"

        var titleRes: Int? = null
        var messageRes: Int? = null
        var message: String? = null
        var view: Int? = null
        var positiveTextRes: Int? = null
        var negativeTextRes: Int? = null
        var neutralTextRes: Int? = null

        fun build(): MessageDialog {
            return MessageDialog().also {
                it.arguments = bundleOf(
                    titleKey to titleRes,
                    messageResKey to messageRes,
                    messageKey to message,
                    positiveKey to positiveTextRes,
                    negativeKey to negativeTextRes,
                    neutralKey to neutralTextRes,
                    viewKey to view
                )
            }
        }
    }

    override fun onCustomiseBuilder(
        dialogBuilder: DynamicDialog.Builder,
        savedInstanceState: Bundle?
    ): DynamicDialog.Builder {
        with(dialogBuilder) {
            arguments?.apply {
                if (getInt(titleKey) != 0) {
                    setTitle(getInt(titleKey))
                }

                if (getInt(messageResKey) != 0) {
                    setMessage(getInt(messageResKey))
                }


                getString(messageKey)?.let {
                    setMessage(it)
                }
                if (getInt(viewKey) != 0) {
                    setView(getInt(viewKey))
                }
                if (getInt(positiveKey) != 0) {
                    setPositiveButton(getInt(positiveKey)) { dialogInterface, which ->
                        dialogCallback?.invoke(dialogInterface, which)
                    }
                }
                if (getInt(negativeKey) != 0) {
                    setNegativeButton(getInt(negativeKey)) { dialogInterface, which ->
                        dialogCallback?.invoke(dialogInterface, which)
                    }
                }
                if (getInt(neutralKey) != 0) {
                    setNeutralButton(getInt(neutralKey)) { dialogInterface, which ->
                        dialogCallback?.invoke(dialogInterface, which)
                    }
                }
            }
        }

        return super.onCustomiseBuilder(dialogBuilder, savedInstanceState)
    }


    override fun onCustomiseDialog(
        alertDialog: DynamicDialog,
        savedInstanceState: Bundle?
    ): DynamicDialog {
        with(alertDialog) {
            setOnShowListener {
                findViewById<TextView>(android.R.id.message)?.textSize = 14f
                dialogOnShowListener?.onShow(it)
                getButton(AlertDialog.BUTTON_POSITIVE)?.let {
                    (it as DynamicButton).isAllCaps = false
                }
                getButton(AlertDialog.BUTTON_NEGATIVE)?.let {
                    (it as DynamicButton).isAllCaps = false
                }
            }
            return super.onCustomiseDialog(alertDialog, savedInstanceState)
        }
    }
}