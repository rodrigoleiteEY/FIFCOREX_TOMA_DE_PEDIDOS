package com.company.fifcorex_toma_de_pedidos.mdui.promotions

import android.os.Build
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import com.company.fifcorex_toma_de_pedidos.R
import com.company.fifcorex_toma_de_pedidos.databinding.FragmentPromotionsCreateBinding
import com.company.fifcorex_toma_de_pedidos.mdui.BundleKeys
import com.company.fifcorex_toma_de_pedidos.mdui.InterfacedFragment
import com.company.fifcorex_toma_de_pedidos.mdui.UIConstants
import com.company.fifcorex_toma_de_pedidos.repository.OperationResult
import com.company.fifcorex_toma_de_pedidos.viewmodel.promotions.PromotionsViewModel
import com.sap.cloud.android.odata.entitycontainer.Promotions
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntityTypes
import com.sap.cloud.mobile.fiori.formcell.SimplePropertyFormCell
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader
import com.sap.cloud.mobile.odata.Property
import org.slf4j.LoggerFactory

/**
 * A fragment that is used for both update and create for users to enter values for the properties. When used for
 * update, an instance of the entity is required. In the case of create, a new instance of the entity with defaults will
 * be created. The default values may not be acceptable for the OData service.
 * This fragment is either contained in a [PromotionsListActivity] in two-pane mode (on tablets) or a
 * [PromotionsDetailActivity] on handsets.
 *
 * Arguments: Operation: [OP_CREATE | OP_UPDATE]
 *            Promotions if Operation is update
 */
class PromotionsCreateFragment : InterfacedFragment<Promotions>() {

    /** Promotions object and it's copy: the modifications are done on the copied object. */
    private lateinit var promotionsEntity: Promotions
    private lateinit var promotionsEntityCopy: Promotions

    /** DataBinding generated class */
    private lateinit var binding: FragmentPromotionsCreateBinding

    /** Indicate what operation to be performed */
    private lateinit var operation: String

    /** promotionsEntity ViewModel */
    private lateinit var viewModel: PromotionsViewModel

    /** The update menu item */
    private lateinit var updateMenuItem: MenuItem

    private val isPromotionsValid: Boolean
        get() {
            var isValid = true
            view?.findViewById<LinearLayout>(R.id.create_update_promotions)?.let { linearLayout ->
                for (i in 0 until linearLayout.childCount) {
                    val simplePropertyFormCell = linearLayout.getChildAt(i) as SimplePropertyFormCell
                    val propertyName = simplePropertyFormCell.tag as String
                    val property = EntityTypes.promotions.getProperty(propertyName)
                    val value = simplePropertyFormCell.value.toString()
                    if (!isValidProperty(property, value)) {
                        simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, true)
                        val errorMessage = resources.getString(R.string.mandatory_warning)
                        simplePropertyFormCell.isErrorEnabled = true
                        simplePropertyFormCell.error = errorMessage
                        isValid = false
                    } else {
                        if (simplePropertyFormCell.isErrorEnabled) {
                            val hasMandatoryError = simplePropertyFormCell.getTag(R.id.TAG_HAS_MANDATORY_ERROR) as Boolean
                            if (!hasMandatoryError) {
                                isValid = false
                            } else {
                                simplePropertyFormCell.isErrorEnabled = false
                            }
                        }
                        simplePropertyFormCell.setTag(R.id.TAG_HAS_MANDATORY_ERROR, false)
                    }
                }
            }
            return isValid
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_edit_options

        arguments?.let {
            (it.getString(BundleKeys.OPERATION))?.let { operationType ->
                operation = operationType
                activityTitle = when (operationType) {
                    UIConstants.OP_CREATE -> resources.getString(R.string.title_create_fragment, EntityTypes.promotions.localName)
                    else -> resources.getString(R.string.title_update_fragment) + " " + EntityTypes.promotions.localName

                }
            }
        }

        activity?.let {
            (it as PromotionsActivity).isNavigationDisabled = true
            viewModel = ViewModelProvider(it).get(PromotionsViewModel::class.java)
            viewModel.createResult.observe(this, Observer { result -> onComplete(result!!) })
            viewModel.updateResult.observe(this, Observer { result -> onComplete(result!!) })

            if (operation == UIConstants.OP_CREATE) {
                promotionsEntity = createPromotions()
            } else {
                promotionsEntity = viewModel.selectedEntity.value!!
            }

            val workingCopy = when{ (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) -> {
                    savedInstanceState?.getParcelable<Promotions>(KEY_WORKING_COPY, Promotions::class.java)
                } else -> @Suppress("DEPRECATION") savedInstanceState?.getParcelable<Promotions>(KEY_WORKING_COPY)
            }

            if (workingCopy == null) {
                promotionsEntityCopy = promotionsEntity.copy()
                promotionsEntityCopy.setEntityTag(promotionsEntity.getEntityTag())
                promotionsEntityCopy.setOldEntity(promotionsEntity)
                promotionsEntityCopy.editLink = promotionsEntity.editLink
            } else {
                promotionsEntityCopy = workingCopy
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        currentActivity.findViewById<ObjectHeader>(R.id.objectHeader)?.let {
            it.visibility = View.GONE
        }
        val rootView = setupDataBinding(inflater, container)
        return rootView
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.save_item -> {
                updateMenuItem = menuItem
                enableUpdateMenuItem(false)
                onSaveItem()
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if(secondaryToolbar != null) secondaryToolbar!!.title = activityTitle else activity?.title = activityTitle
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(KEY_WORKING_COPY, promotionsEntityCopy)
        super.onSaveInstanceState(outState)
    }

    /** Enables the update menu item based on [enable] */
    private fun enableUpdateMenuItem(enable : Boolean = true) {
        updateMenuItem.also {
            it.isEnabled = enable
            it.icon?.alpha = if(enable) 255 else 130
        }
    }

    /** Saves the entity */
    private fun onSaveItem(): Boolean {
        if (!isPromotionsValid) {
            return false
        }
        (currentActivity as PromotionsActivity).isNavigationDisabled = false
        progressBar?.visibility = View.VISIBLE
        when (operation) {
            UIConstants.OP_CREATE -> {
                viewModel.create(promotionsEntityCopy)
            }
            UIConstants.OP_UPDATE -> viewModel.update(promotionsEntityCopy)
        }
        return true
    }

    /**
     * Create a new Promotions instance and initialize properties to its default values
     * Nullable property will remain null
     * For offline, keys will be unset to avoid collision should more than one is created locally
     * @return new Promotions instance
     */
    private fun createPromotions(): Promotions {
        val entity = Promotions(true)
        entity.unsetDataValue(Promotions.material)
        return entity
    }

    /** Callback function to complete processing when updateResult or createResult events fired */
    private fun onComplete(result: OperationResult<Promotions>) {
        progressBar?.visibility = View.INVISIBLE
        enableUpdateMenuItem(true)
        if (result.error != null) {
            (currentActivity as PromotionsActivity).isNavigationDisabled = true
            handleError(result)
        } else {
            if (operation == UIConstants.OP_UPDATE && !currentActivity.resources.getBoolean(R.bool.two_pane)) {
                viewModel.selectedEntity.value = promotionsEntityCopy
            }
            if (currentActivity.resources.getBoolean(R.bool.two_pane)) {
                val listFragment = currentActivity.supportFragmentManager.findFragmentByTag(UIConstants.LIST_FRAGMENT_TAG)
                (listFragment as PromotionsListFragment).refreshListData()
            }
            (currentActivity as PromotionsActivity).onBackPressedDispatcher.onBackPressed()
        }
    }

    /** Simple validation: checks the presence of mandatory fields. */
    private fun isValidProperty(property: Property, value: String): Boolean {
        return !(!property.isNullable && value.isEmpty())
    }

    /**
     * Set up data binding for this view
     *
     * @param [inflater] layout inflater from onCreateView
     * @param [container] view group from onCreateView
     *
     * @return rootView from generated data binding code
     */
    private fun setupDataBinding(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = FragmentPromotionsCreateBinding.inflate(inflater, container, false)
        binding.setPromotions(promotionsEntityCopy)
        return binding.root
    }

    /**
     * Notify user of error encountered while execution the operation
     *
     * @param [result] operation result with error
     */
    private fun handleError(result: OperationResult<Promotions>) {
        val errorMessage = when (result.operation) {
            OperationResult.Operation.UPDATE -> getString(R.string.update_failed_detail)
            OperationResult.Operation.CREATE -> getString(R.string.create_failed_detail)
            else -> throw AssertionError()
        }
        showError(errorMessage)
    }


    companion object {
        private val KEY_WORKING_COPY = "WORKING_COPY"
        private val LOGGER = LoggerFactory.getLogger(PromotionsActivity::class.java)
    }
}
