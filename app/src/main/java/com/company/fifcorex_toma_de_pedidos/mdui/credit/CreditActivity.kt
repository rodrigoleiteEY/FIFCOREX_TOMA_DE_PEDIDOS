package com.company.fifcorex_toma_de_pedidos.mdui.credit

import android.content.Context
import android.os.Bundle
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.company.fifcorex_toma_de_pedidos.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.company.fifcorex_toma_de_pedidos.mdui.BundleKeys
import com.company.fifcorex_toma_de_pedidos.mdui.InterfacedFragment
import com.company.fifcorex_toma_de_pedidos.mdui.EntitySetListActivity
import com.company.fifcorex_toma_de_pedidos.mdui.UIConstants
import com.company.fifcorex_toma_de_pedidos.viewmodel.credit.CreditViewModel
import com.sap.cloud.android.odata.entitycontainer.Credit

/**
 * This activity handles three kind of [InterfacedFragment]s:
 * - [CreditCreateFragment]: to create or edit a(n) Credit,
 * - [CreditDetailFragment]: to display the details of a(n) Credit,
 * - [CreditListFragment]: to list Credit.
 *
 * The visibility of frames inside this activity depends on the width of the device. When screen provides at least 900dp
 * of width both masterFrame and detailFrame are visible. Only one frame is visible in smaller screen sizes.
 *
 * This activity is responsible to place, change and control visibilities of fragments. Fragments have no information
 * about other fragments, so when an user action occurs the fragment uses an interface to command the activity
 * what to do.
 */
class CreditActivity : AppCompatActivity(), InterfacedFragment.InterfacedFragmentListener<Credit> {

    /** Flag to indicate whether both master and detail frames should be visible at the same time  */
    private var isMasterDetailView = false

    /** Flag to indicate whether requesting user confirmation before navigation is needed */
    internal var isNavigationDisabled = false

    /** Flag to tell whether back action is from home click or or others */
    private var isConfirmDataLossFromHomeButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.isMasterDetailView = resources.getBoolean(R.bool.two_pane)
        setContentView(R.layout.activity_entityitem)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.masterFrame, CreditListFragment(), UIConstants.LIST_FRAGMENT_TAG)
                .commit()
        } else {
            isNavigationDisabled = savedInstanceState.getBoolean(KEY_IS_NAVIGATION_DISABLED)
            isConfirmDataLossFromHomeButton = savedInstanceState.getBoolean(KEY_IS_NAVIGATION_FROM_HOME)
        }
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this.isEnabled = false
                exitOnBackPressed()
            }
        })
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_IS_NAVIGATION_DISABLED, this.isNavigationDisabled)
        outState.putBoolean(KEY_IS_NAVIGATION_FROM_HOME, this.isConfirmDataLossFromHomeButton)
        super.onSaveInstanceState(outState)
    }

    /** Let the Navigate Up button work like Back button */
    override fun onSupportNavigateUp(): Boolean {
        exitOnBackPressed()
        return true
    }

    private fun exitOnBackPressed() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        //Find the currently focused view, so we can grab the correct window token from it.
        val view = currentFocus
        if (view != null) {
            //hide the soft keyboard.
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        askUserBeforeNavigation()
    }

    override fun onFragmentStateChange(evt: Int, entity: Credit?) {
        when (evt) {
            UIConstants.EVENT_CREATE_NEW_ITEM -> onCreateNewItem()
            UIConstants.EVENT_ITEM_CLICKED -> onItemClicked(entity)
            UIConstants.EVENT_DELETION_COMPLETED -> onDeletionComplete()
            UIConstants.EVENT_EDIT_ITEM -> onEditItem(entity)
            UIConstants.EVENT_ASK_DELETE_CONFIRMATION -> onConfirmDelete()
            UIConstants.EVENT_BACK_NAVIGATION_CONFIRMED -> {
                isNavigationDisabled = false
                if( isConfirmDataLossFromHomeButton ) {
                    intent = Intent(this, EntitySetListActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    startActivity(intent)
                } else
                    onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    /**
     * Shows an AlertDialog when user wants to leave {@link CreditCreateFragment}.
     */
    fun askUserBeforeNavigation() {
        val editFragmentPresent = supportFragmentManager.findFragmentByTag(UIConstants.MODIFY_FRAGMENT_TAG) != null ||
            supportFragmentManager.findFragmentByTag(UIConstants.CREATE_FRAGMENT_TAG) != null
        if (editFragmentPresent && isNavigationDisabled) {
            ConfirmationDialogFragment().also {
                it.isCancelable = false
                it.show(supportFragmentManager, UIConstants.CONFIRMATION_FRAGMENT_TAG)
            }
        } else {
            onFragmentStateChange(UIConstants.EVENT_BACK_NAVIGATION_CONFIRMED,null)
        }
    }

    /**
     * Every fragment handles its own OptionsMenu so the activity does not have to.
     *
     * @return false, because this activity does not handles OptionItems
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.menu_home -> {
                isConfirmDataLossFromHomeButton = true
                askUserBeforeNavigation()
                true
            }
            else -> false
        }
    }

    /**
     * Handles the click event of [entity] from the list.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun onItemClicked(entity: Credit?) {
        if (!isMasterDetailView) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.masterFrame, CreditDetailFragment(), UIConstants.DETAIL_FRAGMENT_TAG)
                .addToBackStack(UIConstants.DETAIL_FRAGMENT_TAG)
                .commit()
        } else {
            val detail = supportFragmentManager.findFragmentByTag(UIConstants.DETAIL_FRAGMENT_TAG)
            if (detail == null) {
                if (entity != null)
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.detailFrame, CreditDetailFragment(), UIConstants.DETAIL_FRAGMENT_TAG)
                        .commit()
            } else {
                if( entity == null) {
                    supportFragmentManager.beginTransaction().remove(detail).commit()
                    findViewById<Toolbar>(R.id.secondaryToolbar)?.let {
                        it.title = ""
                        it.menu.clear()
                    }
                }
            }
        }
    }

    /** Opens the UI to create a new entity. */
    private fun onCreateNewItem() {
        val count = supportFragmentManager.backStackEntryCount
        if (count > 0) {
            supportFragmentManager.getBackStackEntryAt(count - 1).let { entry ->
                if (entry.name == UIConstants.CREATE_FRAGMENT_TAG || entry.name == UIConstants.MODIFY_FRAGMENT_TAG) {
                    Toast.makeText(this, "Please save your changes first...", Toast.LENGTH_SHORT).show()
                    return@onCreateNewItem
                }
            }
        }

        val arguments = Bundle()
        arguments.putString(BundleKeys.OPERATION, UIConstants.OP_CREATE)
        val fragment = CreditCreateFragment()
        fragment.arguments = arguments
        val containerId = if (isMasterDetailView) R.id.detailFrame else R.id.masterFrame
        supportFragmentManager.beginTransaction()
            .replace(containerId, fragment, UIConstants.CREATE_FRAGMENT_TAG)
            .addToBackStack(UIConstants.CREATE_FRAGMENT_TAG)
            .commit()
    }

    /** 
     * Handles the situatoion after an entity is deleted, hide the progress bar, go back if neccessary.
     */
    private fun onDeletionComplete() {
        findViewById<View>(R.id.indeterminateBar).visibility = View.INVISIBLE
        if (!isMasterDetailView) {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    /** Edits the given [entity] */
    @Suppress("UNUSED_PARAMETER")
    private fun onEditItem(entity: Credit?) {
        val updateFragment = CreditCreateFragment()
        val arguments = Bundle()
        arguments.putString(BundleKeys.OPERATION, UIConstants.OP_UPDATE)
        updateFragment.arguments = arguments
        val containerId = if (isMasterDetailView) R.id.detailFrame else R.id.masterFrame
        supportFragmentManager.beginTransaction()
            .replace(containerId, updateFragment, UIConstants.MODIFY_FRAGMENT_TAG)
            .addToBackStack(UIConstants.MODIFY_FRAGMENT_TAG)
            .commit()
    }

    /** Opens a dialog for confirmation when user wants to delete an entity*/
    private fun onConfirmDelete() {
        DeleteConfirmationDialogFragment().also {
            it.isCancelable = false
            it.show(supportFragmentManager,  UIConstants.CONFIRMATION_FRAGMENT_TAG)
        }
    }

    /**
     * Represents the confirmation dialog fragment when users tries to leave the create or edit
     * fragment to prevent data loss.
     */
    internal class ConfirmationDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val fragmentActivity = requireActivity() as AppCompatActivity
            val builder = MaterialAlertDialogBuilder(fragmentActivity, R.style.AlertDialogStyle)
            builder.setTitle(R.string.before_navigation_dialog_title)
            builder.setMessage(R.string.before_navigation_dialog_message)
            builder.setPositiveButton(R.string.before_navigation_dialog_positive_button) { _, _ ->
                (fragmentActivity as CreditActivity).onFragmentStateChange(UIConstants.EVENT_BACK_NAVIGATION_CONFIRMED, null)
            }
            builder.setNegativeButton(R.string.before_navigation_dialog_negative_button) { _, _ -> }
            return builder.create()
        }
    }

    /**
     * Represents the delete confirmation dialog fragment when users tries to delete an entity or entities
     */
    internal class DeleteConfirmationDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val fragmentActivity = requireActivity() as AppCompatActivity
            val builder = MaterialAlertDialogBuilder(fragmentActivity, R.style.AlertDialogStyle)
            val viewModel = ViewModelProvider(fragmentActivity).get(CreditViewModel::class.java)
            if (viewModel.numberOfSelected() > 1) {
                builder.setTitle(R.string.delete_dialog_title).setMessage(R.string.delete_more_items)
            } else {
                builder.setTitle(R.string.delete_dialog_title).setMessage(R.string.delete_one_item)
            }

            builder.setPositiveButton(R.string.delete) { _, _ ->
                try {
                    fragmentActivity.findViewById<View>(R.id.indeterminateBar)?.let {
                        it.visibility = View.VISIBLE
                    }
                    if(viewModel.numberOfSelected() == 0) {
                        viewModel.addSelected(viewModel.selectedEntity.value!!)
                    }
                    viewModel.deleteSelected()
                } catch (exception: Exception) {
                    DialogHelper(requireActivity()).showOKOnlyDialog(
                        fragmentManager = requireActivity().supportFragmentManager,
                        message = resources.getString(R.string.delete_failed_detail)
                    )
                }
            }

            builder.setNegativeButton(R.string.cancel) { _, _ -> }
            return builder.create()
        }
    }


    companion object {
        private val KEY_IS_NAVIGATION_DISABLED = "isNavigationDisabled"
        private val KEY_IS_NAVIGATION_FROM_HOME = "isNavigationFromHome"
    }
}
