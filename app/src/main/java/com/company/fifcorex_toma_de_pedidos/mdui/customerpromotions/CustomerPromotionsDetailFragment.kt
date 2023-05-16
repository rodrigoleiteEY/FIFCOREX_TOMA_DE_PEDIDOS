package com.company.fifcorex_toma_de_pedidos.mdui.customerpromotions

import androidx.lifecycle.Observer
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import com.company.fifcorex_toma_de_pedidos.R
import com.company.fifcorex_toma_de_pedidos.databinding.FragmentCustomerpromotionsDetailBinding
import com.company.fifcorex_toma_de_pedidos.mdui.EntityKeyUtil
import com.company.fifcorex_toma_de_pedidos.mdui.InterfacedFragment
import com.company.fifcorex_toma_de_pedidos.mdui.UIConstants
import com.company.fifcorex_toma_de_pedidos.repository.OperationResult
import com.company.fifcorex_toma_de_pedidos.viewmodel.customerpromotions.CustomerPromotionsViewModel
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntitySets;
import com.sap.cloud.android.odata.entitycontainer.CustomerPromotions
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader


/**
 * A fragment representing a single CustomerPromotions detail screen.
 * This fragment is contained in an CustomerPromotionsActivity.
 */
class CustomerPromotionsDetailFragment : InterfacedFragment<CustomerPromotions>() {

    /** Generated data binding class based on layout file */
    private lateinit var binding: FragmentCustomerpromotionsDetailBinding

    /** CustomerPromotions entity to be displayed */
    private lateinit var customerPromotionsEntity: CustomerPromotions

    /** Fiori ObjectHeader component used when entity is to be displayed on phone */
    private var objectHeader: ObjectHeader? = null

    /** View model of the entity type that the displayed entity belongs to */
    private lateinit var viewModel: CustomerPromotionsViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menu = R.menu.itemlist_view_options
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return setupDataBinding(inflater, container)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            currentActivity = it
            viewModel = ViewModelProvider(it).get(CustomerPromotionsViewModel::class.java)
            viewModel.deleteResult.observe(viewLifecycleOwner, Observer { result ->
                onDeleteComplete(result!!)
            })

            viewModel.selectedEntity.observe(viewLifecycleOwner, Observer { entity ->
                customerPromotionsEntity = entity
                binding.setCustomerPromotions(entity)
                setupObjectHeader()
            })
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.update_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, customerPromotionsEntity)
                true
            }
            R.id.delete_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_ASK_DELETE_CONFIRMATION,null)
                true
            }
            else -> super.onMenuItemSelected(menuItem)
        }
    }

    /**
     * Completion callback for delete operation
     *
     * @param [result] of the operation
     */
    private fun onDeleteComplete(result: OperationResult<CustomerPromotions>) {
        progressBar?.let {
            it.visibility = View.INVISIBLE
        }
        viewModel.removeAllSelected()
        result.error?.let {
            showError(getString(R.string.delete_failed_detail))
            return
        }
        listener?.onFragmentStateChange(UIConstants.EVENT_DELETION_COMPLETED, customerPromotionsEntity)
    }


    /**
     * Set up databinding for this view
     *
     * @param [inflater] layout inflater from onCreateView
     * @param [container] view group from onCreateView
     *
     * @return [View] rootView from generated databinding code
     */
    private fun setupDataBinding(inflater: LayoutInflater, container: ViewGroup?): View {
        binding = FragmentCustomerpromotionsDetailBinding.inflate(inflater, container, false)
        binding.handler = this
        return binding.root
    }

    /**
     * Set detail image of ObjectHeader.
     * When the entity does not provides picture, set the first character of the masterProperty.
     */
    private fun setDetailImage(objectHeader: ObjectHeader, customerPromotionsEntity: CustomerPromotions) {
        if (customerPromotionsEntity.getOptionalValue(CustomerPromotions.customerID) != null && customerPromotionsEntity.getOptionalValue(CustomerPromotions.customerID).toString().isNotEmpty()) {
            objectHeader.detailImageCharacter = customerPromotionsEntity.getOptionalValue(CustomerPromotions.customerID).toString().substring(0, 1)
        } else {
            objectHeader.detailImageCharacter = "?"
        }
    }

    /**
     * Setup ObjectHeader with an instance of customerPromotionsEntity
     */
    private fun setupObjectHeader() {
        val secondToolbar = currentActivity.findViewById<Toolbar>(R.id.secondaryToolbar)
        if (secondToolbar != null) {
            secondToolbar.setTitle(customerPromotionsEntity.entityType.localName)
        } else {
            currentActivity.setTitle(customerPromotionsEntity.entityType.localName)
        }

        // Object Header is not available in tablet mode
        objectHeader = currentActivity.findViewById(R.id.objectHeader)
        val dataValue = customerPromotionsEntity.getOptionalValue(CustomerPromotions.customerID)

        objectHeader?.let {
            it.apply {
                headline = dataValue?.toString()
                subheadline = EntityKeyUtil.getOptionalEntityKey(customerPromotionsEntity)
                body = "You can set the header body text here."
                footnote = "You can set the header footnote here."
                description = "You can add a detailed item description here."
            }
            it.setTag("#tag1", 0)
            it.setTag("#tag3", 2)
            it.setTag("#tag2", 1)

            setDetailImage(it, customerPromotionsEntity)
            it.visibility = View.VISIBLE
        }
    }
}
