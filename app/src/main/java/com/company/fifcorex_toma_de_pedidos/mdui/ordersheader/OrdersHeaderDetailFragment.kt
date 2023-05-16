package com.company.fifcorex_toma_de_pedidos.mdui.ordersheader

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
import com.company.fifcorex_toma_de_pedidos.databinding.FragmentOrdersheaderDetailBinding
import com.company.fifcorex_toma_de_pedidos.mdui.EntityKeyUtil
import com.company.fifcorex_toma_de_pedidos.mdui.InterfacedFragment
import com.company.fifcorex_toma_de_pedidos.mdui.UIConstants
import com.company.fifcorex_toma_de_pedidos.repository.OperationResult
import com.company.fifcorex_toma_de_pedidos.viewmodel.ordersheader.OrdersHeaderViewModel
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntitySets;
import com.sap.cloud.android.odata.entitycontainer.OrdersHeader
import com.sap.cloud.mobile.fiori.`object`.ObjectHeader

import com.company.fifcorex_toma_de_pedidos.mdui.ordersitem.OrdersItemActivity

/**
 * A fragment representing a single OrdersHeader detail screen.
 * This fragment is contained in an OrdersHeaderActivity.
 */
class OrdersHeaderDetailFragment : InterfacedFragment<OrdersHeader>() {

    /** Generated data binding class based on layout file */
    private lateinit var binding: FragmentOrdersheaderDetailBinding

    /** OrdersHeader entity to be displayed */
    private lateinit var ordersHeaderEntity: OrdersHeader

    /** Fiori ObjectHeader component used when entity is to be displayed on phone */
    private var objectHeader: ObjectHeader? = null

    /** View model of the entity type that the displayed entity belongs to */
    private lateinit var viewModel: OrdersHeaderViewModel
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
            viewModel = ViewModelProvider(it).get(OrdersHeaderViewModel::class.java)
            viewModel.deleteResult.observe(viewLifecycleOwner, Observer { result ->
                onDeleteComplete(result!!)
            })

            viewModel.selectedEntity.observe(viewLifecycleOwner, Observer { entity ->
                ordersHeaderEntity = entity
                binding.setOrdersHeader(entity)
                setupObjectHeader()
            })
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.update_item -> {
                listener?.onFragmentStateChange(UIConstants.EVENT_EDIT_ITEM, ordersHeaderEntity)
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
    private fun onDeleteComplete(result: OperationResult<OrdersHeader>) {
        progressBar?.let {
            it.visibility = View.INVISIBLE
        }
        viewModel.removeAllSelected()
        result.error?.let {
            showError(getString(R.string.delete_failed_detail))
            return
        }
        listener?.onFragmentStateChange(UIConstants.EVENT_DELETION_COMPLETED, ordersHeaderEntity)
    }


    @Suppress("UNUSED", "UNUSED_PARAMETER") // parameter is needed because of the xml binding
    fun onNavigationClickedToOrdersItem_ITEMS(view: View) {
        val intent = Intent(currentActivity, OrdersItemActivity::class.java)
        intent.putExtra("parent", ordersHeaderEntity)
        intent.putExtra("navigation", "ITEMS")
        startActivity(intent)
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
        binding = FragmentOrdersheaderDetailBinding.inflate(inflater, container, false)
        binding.handler = this
        return binding.root
    }

    /**
     * Set detail image of ObjectHeader.
     * When the entity does not provides picture, set the first character of the masterProperty.
     */
    private fun setDetailImage(objectHeader: ObjectHeader, ordersHeaderEntity: OrdersHeader) {
        if (ordersHeaderEntity.getOptionalValue(OrdersHeader.documentType) != null && ordersHeaderEntity.getOptionalValue(OrdersHeader.documentType).toString().isNotEmpty()) {
            objectHeader.detailImageCharacter = ordersHeaderEntity.getOptionalValue(OrdersHeader.documentType).toString().substring(0, 1)
        } else {
            objectHeader.detailImageCharacter = "?"
        }
    }

    /**
     * Setup ObjectHeader with an instance of ordersHeaderEntity
     */
    private fun setupObjectHeader() {
        val secondToolbar = currentActivity.findViewById<Toolbar>(R.id.secondaryToolbar)
        if (secondToolbar != null) {
            secondToolbar.setTitle(ordersHeaderEntity.entityType.localName)
        } else {
            currentActivity.setTitle(ordersHeaderEntity.entityType.localName)
        }

        // Object Header is not available in tablet mode
        objectHeader = currentActivity.findViewById(R.id.objectHeader)
        val dataValue = ordersHeaderEntity.getOptionalValue(OrdersHeader.documentType)

        objectHeader?.let {
            it.apply {
                headline = dataValue?.toString()
                subheadline = EntityKeyUtil.getOptionalEntityKey(ordersHeaderEntity)
                body = "You can set the header body text here."
                footnote = "You can set the header footnote here."
                description = "You can add a detailed item description here."
            }
            it.setTag("#tag1", 0)
            it.setTag("#tag3", 2)
            it.setTag("#tag2", 1)

            setDetailImage(it, ordersHeaderEntity)
            it.visibility = View.VISIBLE
        }
    }
}
