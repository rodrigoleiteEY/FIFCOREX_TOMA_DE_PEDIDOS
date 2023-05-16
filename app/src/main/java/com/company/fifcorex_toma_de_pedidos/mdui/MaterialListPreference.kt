package com.company.fifcorex_toma_de_pedidos.mdui

import com.company.fifcorex_toma_de_pedidos.R
import android.content.Context
import android.util.AttributeSet
import androidx.preference.ListPreference
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MaterialListPreference : ListPreference {
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
            super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context,attrs,defStyleAttr)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context) : super(context)

    override fun onClick() {
        MaterialAlertDialogBuilder(context, R.style.AlertDialogStyle)
            .setSingleChoiceItems(entries, findIndexOfValue(value)) { dialog, which ->
                if (callChangeListener(entryValues[which].toString())) {
                    setValueIndex(which)
                }
                dialog.dismiss()
            }
            .setNegativeButton(
                R.string.cancel
            ) { dialog, _ ->
                dialog.cancel()
            }
            .setTitle(title)
            .create()
            .show()
    }
}
