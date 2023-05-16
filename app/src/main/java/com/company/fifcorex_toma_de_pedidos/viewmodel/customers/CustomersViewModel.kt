package com.company.fifcorex_toma_de_pedidos.viewmodel.customers

import android.app.Application
import android.os.Parcelable

import com.company.fifcorex_toma_de_pedidos.viewmodel.EntityViewModel
import com.sap.cloud.android.odata.entitycontainer.Customers
import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntitySets

/*
 * Represents View model for Customers
 *
 * Having an entity view model for each <T> allows the ViewModelProvider to cache and return the view model of that
 * type. This is because the ViewModelStore of ViewModelProvider cannot not be able to tell the difference between
 * EntityViewModel<type1> and EntityViewModel<type2>.
 */
class CustomersViewModel(application: Application): EntityViewModel<Customers>(application, EntitySets.customers, Customers.name) {
    /**
     * Constructor for a specific view model with navigation data.
     * @param [navigationPropertyName] - name of the navigation property
     * @param [entityData] - parent entity (starting point of the navigation)
     */
    constructor(application: Application, navigationPropertyName: String, entityData: Parcelable): this(application) {
        EntityViewModel<Customers>(application, EntitySets.customers, Customers.name, navigationPropertyName, entityData)
    }
}
