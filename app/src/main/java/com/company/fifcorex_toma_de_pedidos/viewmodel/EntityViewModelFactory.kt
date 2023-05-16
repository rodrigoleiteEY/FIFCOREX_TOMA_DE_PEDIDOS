package com.company.fifcorex_toma_de_pedidos.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.os.Parcelable

import com.company.fifcorex_toma_de_pedidos.viewmodel.credit.CreditViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.customerproducts.CustomerProductsViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.customerpromotions.CustomerPromotionsViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.customers.CustomersViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.invoices.InvoicesViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.ordersheader.OrdersHeaderViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.ordersitem.OrdersItemViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.products.ProductsViewModel
import com.company.fifcorex_toma_de_pedidos.viewmodel.promotions.PromotionsViewModel

/**
 * Custom factory class, which can create view models for entity subsets, which are
 * reached from a parent entity through a navigation property.
 *
 * @param application parent application
 * @param navigationPropertyName name of the navigation link
 * @param entityData parent entity
 */
class EntityViewModelFactory (
        val application: Application, // name of the navigation property
        val navigationPropertyName: String, // parent entity
        val entityData: Parcelable) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass.simpleName) {
			"CreditViewModel" -> CreditViewModel(application, navigationPropertyName, entityData) as T
            			"CustomerProductsViewModel" -> CustomerProductsViewModel(application, navigationPropertyName, entityData) as T
            			"CustomerPromotionsViewModel" -> CustomerPromotionsViewModel(application, navigationPropertyName, entityData) as T
            			"CustomersViewModel" -> CustomersViewModel(application, navigationPropertyName, entityData) as T
            			"InvoicesViewModel" -> InvoicesViewModel(application, navigationPropertyName, entityData) as T
            			"OrdersHeaderViewModel" -> OrdersHeaderViewModel(application, navigationPropertyName, entityData) as T
            			"OrdersItemViewModel" -> OrdersItemViewModel(application, navigationPropertyName, entityData) as T
            			"ProductsViewModel" -> ProductsViewModel(application, navigationPropertyName, entityData) as T
             else -> PromotionsViewModel(application, navigationPropertyName, entityData) as T
        }
    }
}
