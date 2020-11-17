package com.revolgenx.anilib.ui.dialog

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.core.os.bundleOf
import com.pranavpandey.android.dynamic.support.dialog.DynamicDialog
import com.revolgenx.anilib.R
import com.revolgenx.anilib.ui.adapter.TagAdapter
import com.revolgenx.anilib.common.ui.dialog.BaseDialogFragment
import com.revolgenx.anilib.constant.MediaTagFilterTypes
import com.revolgenx.anilib.infrastructure.event.TagEvent
import com.revolgenx.anilib.data.field.TagChooserField
import com.revolgenx.anilib.data.field.TagField
import com.revolgenx.anilib.ui.view.TriStateMode
import kotlinx.android.synthetic.main.tag_chooser_dialog_fragment_layout.*

class TagChooserDialogFragment : BaseDialogFragment() {
    override var positiveText: Int? = R.string.done
    override var negativeText: Int? = R.string.cancel
    override var viewRes: Int? = R.layout.tag_chooser_dialog_fragment_layout

    companion object {
        const val TAG_KEY = "tag_key"
        fun newInstance(tags: TagChooserField) = TagChooserDialogFragment().apply {
            arguments = bundleOf(TAG_KEY to tags)
        }
    }

    private val tagChooserField: TagChooserField
        get() {
            return arguments?.getParcelable<TagChooserField>(
                TAG_KEY
            )!!.also { arg ->
                arg.tags = arg.tags.map { TagField(it.tag, it.tagState) }
            }
        }

    private lateinit var tagAdapter: TagAdapter

    override fun onCustomiseBuilder(
        dialogBuilder: DynamicDialog.Builder,
        savedInstanceState: Bundle?
    ): DynamicDialog.Builder {
        dialogBuilder.setTitle(
            when (tagChooserField.tagType) {
                MediaTagFilterTypes.TAGS, MediaTagFilterTypes.SEASON_TAG -> {
                    getString(R.string.tags)
                }
                MediaTagFilterTypes.GENRES, MediaTagFilterTypes.SEASON_GENRE -> {
                    getString(R.string.genre)
                }
                MediaTagFilterTypes.STREAMING_ON -> {
                    getString(R.string.streaming_on)
                }
            }
        )
        dialogBuilder.setNeutralButton(R.string.unselect_all, null)
        return super.onCustomiseBuilder(dialogBuilder, savedInstanceState)
    }

    override fun onShowListener(alertDialog: DynamicDialog, savedInstanceState: Bundle?) {
        with(alertDialog) {
//            alertDialog.window.let { window ->
//                window!!.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
//                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
//            }
            getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                tagAdapter.deSelectAll()
            }
            val tagMode = when(tagChooserField.tagType){
                MediaTagFilterTypes.TAGS, MediaTagFilterTypes.GENRES -> {
                    TriStateMode.TRI_MODE}
                MediaTagFilterTypes.STREAMING_ON,MediaTagFilterTypes.SEASON_GENRE, MediaTagFilterTypes.SEASON_TAG -> {
                    TriStateMode.BI_MODE}
            }
            tagAdapter = TagAdapter(tagMode)
            tagAdapter.submitList(tagChooserField.tags)
            tagRecyclerView.adapter = tagAdapter
        }
    }

    override fun onPositiveClicked(dialogInterface: DialogInterface, which: Int) {
        TagEvent(tagChooserField.tagType, tagChooserField.tags).postEvent
    }
/*
    inner class TagAdapter : RecyclerView.Adapter<TagAdapter.TagHolder>() {
        private val textColor =
            DynamicTheme.getInstance().get().tintSurfaceColor

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagHolder {
            return TagHolder(
                LayoutInflater.from(parent.context).inflate(
                    R.layout.tag_holder_layout,
                    parent,
                    false
                )
            )
        }


        override fun getItemCount(): Int {
            return tagChooserField.tags.size
        }

        override fun onBindViewHolder(holder: TagHolder, position: Int) {
            val item = tagChooserField.tags[position]
            holder.bind(item)
        }

        fun deSelectAll() {
            tagChooserField.tags.forEach {
                it.isTagged = false
            }
            notifyDataSetChanged()
        }

        inner class TagHolder(v: View) : RecyclerView.ViewHolder(v) {
            fun bind(item: TagField) {
                itemView.apply {
                    tagCheckBox.setOnCheckedChangeListener { _, isChecked ->
                        item.isTagged = isChecked
                    }
                    tagCheckBox.setTextColor(textColor)
                    tagCheckBox.text = item.tag
                    tagCheckBox.isChecked = item.isTagged
                }
            }
        }
    }*/

}