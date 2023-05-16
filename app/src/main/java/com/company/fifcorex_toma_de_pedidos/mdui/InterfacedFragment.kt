package com.company.fifcorex_toma_de_pedidos.mdui

import android.os.Bundle
import android.os.Parcelable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import android.widget.ProgressBar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.company.fifcorex_toma_de_pedidos.R
import com.sap.cloud.mobile.flowv2.core.DialogHelper

open class InterfacedFragment<T>: Fragment(), MenuProvider {

    /** Hold the current context */
    internal lateinit var currentActivity: FragmentActivity

    /** Store the toolbar title of the actual fragment */
    internal var activityTitle: String = ""

    /** Store the toolbar menu resource of the actual fragment */
    internal var menu: Int = 0

    /** Navigation parameter: name of the link */
    internal var parentEntityData: Parcelable? = null

    /** Navigation parameter: starting entity */
    internal var navigationPropertyName: String? = null

    /** The progress bar */
    internal val secondaryToolbar: Toolbar?
        get() = currentActivity.findViewById<Toolbar>(R.id.secondaryToolbar)

    /** The progress bar */
    internal val progressBar : ProgressBar?
        get() = currentActivity.findViewById<ProgressBar>(R.id.indeterminateBar)

    /** The listener **/
    internal var listener: InterfacedFragmentListener<T>? = null

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.let {
            currentActivity = it
            if (it is InterfacedFragmentListener<*>) {
                listener = it as InterfacedFragmentListener<T>
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val menuHost : MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        secondaryToolbar?.let {
            it.menu.clear()
            it.inflateMenu(this.menu)
            it.setOnMenuItemClickListener(this::onMenuItemSelected)
            return@onCreateMenu
        }
        menuInflater.inflate(this.menu, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return false
    }

    protected fun showError(message: String) {
        DialogHelper(requireContext()).showOKOnlyDialog(
            fragmentManager = requireActivity().supportFragmentManager,
            message = message
        )
    }

    interface InterfacedFragmentListener<T> {
        fun onFragmentStateChange(evt: Int, entity: T?)
    }
}