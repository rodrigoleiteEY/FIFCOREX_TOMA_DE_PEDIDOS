package com.company.fifcorex_toma_de_pedidos.repository

import com.sap.cloud.android.odata.entitycontainer.EntityContainerMetadata.EntitySets
import com.sap.cloud.android.odata.entitycontainer.Credit
import com.sap.cloud.android.odata.entitycontainer.CustomerProducts
import com.sap.cloud.android.odata.entitycontainer.CustomerPromotions
import com.sap.cloud.android.odata.entitycontainer.Customers
import com.sap.cloud.android.odata.entitycontainer.Invoices
import com.sap.cloud.android.odata.entitycontainer.OrdersHeader
import com.sap.cloud.android.odata.entitycontainer.OrdersItem
import com.sap.cloud.android.odata.entitycontainer.Products
import com.sap.cloud.android.odata.entitycontainer.Promotions

import com.sap.cloud.mobile.odata.EntitySet
import com.sap.cloud.mobile.odata.EntityValue
import com.sap.cloud.mobile.odata.Property
import com.company.fifcorex_toma_de_pedidos.service.OfflineWorkerUtil

import java.util.WeakHashMap

/*
 * Repository factory to construct repository for an entity set
 */
class RepositoryFactory
/**
 * Construct a RepositoryFactory instance. There should only be one repository factory and used
 * throughout the life of the application to avoid caching entities multiple times.
 */
{
    private val repositories: WeakHashMap<String, Repository<out EntityValue>> = WeakHashMap()

    /**
     * Construct or return an existing repository for the specified entity set
     * @param entitySet - entity set for which the repository is to be returned
     * @param orderByProperty - if specified, collection will be sorted ascending with this property
     * @return a repository for the entity set
     */
    fun getRepository(entitySet: EntitySet, orderByProperty: Property?): Repository<out EntityValue> {
        val entityContainer = OfflineWorkerUtil.entityContainer
        val key = entitySet.localName
        var repository: Repository<out EntityValue>? = repositories[key]
        if (repository == null) {
            repository = when (key) {
                EntitySets.credit.localName -> Repository<Credit>(entityContainer, EntitySets.credit, orderByProperty)
                EntitySets.customerProducts.localName -> Repository<CustomerProducts>(entityContainer, EntitySets.customerProducts, orderByProperty)
                EntitySets.customerPromotions.localName -> Repository<CustomerPromotions>(entityContainer, EntitySets.customerPromotions, orderByProperty)
                EntitySets.customers.localName -> Repository<Customers>(entityContainer, EntitySets.customers, orderByProperty)
                EntitySets.invoices.localName -> Repository<Invoices>(entityContainer, EntitySets.invoices, orderByProperty)
                EntitySets.ordersHeader.localName -> Repository<OrdersHeader>(entityContainer, EntitySets.ordersHeader, orderByProperty)
                EntitySets.ordersItem.localName -> Repository<OrdersItem>(entityContainer, EntitySets.ordersItem, orderByProperty)
                EntitySets.products.localName -> Repository<Products>(entityContainer, EntitySets.products, orderByProperty)
                EntitySets.promotions.localName -> Repository<Promotions>(entityContainer, EntitySets.promotions, orderByProperty)
                else -> throw AssertionError("Fatal error, entity set[$key] missing in generated code")
            }
            repositories[key] = repository
        }
        return repository
    }

    /**
     * Get rid of all cached repositories
     */
    fun reset() {
        repositories.clear()
    }
}
