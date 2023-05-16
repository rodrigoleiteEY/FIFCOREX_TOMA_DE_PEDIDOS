package com.company.fifcorex_toma_de_pedidos.mdui

import com.company.fifcorex_toma_de_pedidos.app.SAPWizardApplication

import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.sap.cloud.mobile.flowv2.core.Flow
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate
import androidx.preference.PreferenceManager
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.company.fifcorex_toma_de_pedidos.service.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.*
import android.widget.ArrayAdapter
import android.content.Context
import android.content.Intent
import java.util.ArrayList
import java.util.HashMap
import com.company.fifcorex_toma_de_pedidos.app.WelcomeActivity
import com.company.fifcorex_toma_de_pedidos.mdui.credit.CreditActivity
import com.company.fifcorex_toma_de_pedidos.mdui.customerproducts.CustomerProductsActivity
import com.company.fifcorex_toma_de_pedidos.mdui.customerpromotions.CustomerPromotionsActivity
import com.company.fifcorex_toma_de_pedidos.mdui.customers.CustomersActivity
import com.company.fifcorex_toma_de_pedidos.mdui.invoices.InvoicesActivity
import com.company.fifcorex_toma_de_pedidos.mdui.ordersheader.OrdersHeaderActivity
import com.company.fifcorex_toma_de_pedidos.mdui.ordersitem.OrdersItemActivity
import com.company.fifcorex_toma_de_pedidos.mdui.products.ProductsActivity
import com.company.fifcorex_toma_de_pedidos.mdui.promotions.PromotionsActivity
import org.slf4j.LoggerFactory
import com.company.fifcorex_toma_de_pedidos.R

import kotlinx.android.synthetic.main.activity_entity_set_list.*
import kotlinx.android.synthetic.main.element_entity_set_list.view.*

/*
 * An activity to display the list of all entity types from the OData service
 */
class EntitySetListActivity : AppCompatActivity() {
    private val entitySetNames = ArrayList<String>()
    private val entitySetNameMap = HashMap<String, EntitySetName>()

    private var syncItem: MenuItem? = null

    enum class EntitySetName constructor(val entitySetName: String, val titleId: Int, val iconId: Int) {
        Credit("Credit", R.string.eset_credit,
            BLUE_ANDROID_ICON),
        CustomerProducts("CustomerProducts", R.string.eset_customerproducts,
            WHITE_ANDROID_ICON),
        CustomerPromotions("CustomerPromotions", R.string.eset_customerpromotions,
            BLUE_ANDROID_ICON),
        Customers("Customers", R.string.eset_customers,
            WHITE_ANDROID_ICON),
        Invoices("Invoices", R.string.eset_invoices,
            BLUE_ANDROID_ICON),
        OrdersHeader("OrdersHeader", R.string.eset_ordersheader,
            WHITE_ANDROID_ICON),
        OrdersItem("OrdersItem", R.string.eset_ordersitem,
            BLUE_ANDROID_ICON),
        Products("Products", R.string.eset_products,
            WHITE_ANDROID_ICON),
        Promotions("Promotions", R.string.eset_promotions,
            BLUE_ANDROID_ICON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //navigate to launch screen if SAPServiceManager or OfflineOdataProvider is not initialized
        navForInitialize()
        setContentView(R.layout.activity_entity_set_list)
        val toolbar = findViewById<Toolbar>(R.id.toolbar) // to avoid ambiguity
        setSupportActionBar(toolbar)

        entitySetNames.clear()
        entitySetNameMap.clear()
        for (entitySet in EntitySetName.values()) {
            val entitySetTitle = resources.getString(entitySet.titleId)
            entitySetNames.add(entitySetTitle)
            entitySetNameMap[entitySetTitle] = entitySet
        }

        val listView = entity_list
        val adapter = EntitySetListAdapter(this, R.layout.element_entity_set_list, entitySetNames)

        listView.adapter = adapter

        listView.setOnItemClickListener listView@{ _, _, position, _ ->
            val entitySetName = entitySetNameMap[adapter.getItem(position)!!]
            val context = this@EntitySetListActivity
            val intent: Intent = when (entitySetName) {
                EntitySetName.Credit -> Intent(context, CreditActivity::class.java)
                EntitySetName.CustomerProducts -> Intent(context, CustomerProductsActivity::class.java)
                EntitySetName.CustomerPromotions -> Intent(context, CustomerPromotionsActivity::class.java)
                EntitySetName.Customers -> Intent(context, CustomersActivity::class.java)
                EntitySetName.Invoices -> Intent(context, InvoicesActivity::class.java)
                EntitySetName.OrdersHeader -> Intent(context, OrdersHeaderActivity::class.java)
                EntitySetName.OrdersItem -> Intent(context, OrdersItemActivity::class.java)
                EntitySetName.Products -> Intent(context, ProductsActivity::class.java)
                EntitySetName.Promotions -> Intent(context, PromotionsActivity::class.java)
                else -> return@listView
            }
            context.startActivity(intent)
        }
    }

    inner class EntitySetListAdapter internal constructor(context: Context, resource: Int, entitySetNames: List<String>)
                    : ArrayAdapter<String>(context, resource, entitySetNames) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            var view = convertView
            val entitySetName = entitySetNameMap[getItem(position)!!]
            if (view == null) {
                view = LayoutInflater.from(context).inflate(R.layout.element_entity_set_list, parent, false)
            }
            val entitySetCell = view!!.entity_set_name
            entitySetCell.headline = entitySetName?.titleId?.let {
                context.resources.getString(it)
            }
            entitySetName?.iconId?.let { entitySetCell.setDetailImage(it) }
            return view
        }
    }

    override fun onBackPressed() {
        moveTaskToBack(true)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.entity_set_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        menu?.findItem(R.id.menu_delete_registration)?.isEnabled =
            UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() == true
        menu?.findItem(R.id.menu_delete_registration)?.isVisible =
            UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() == true
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        LOGGER.debug("onOptionsItemSelected: " + item.title)
        return when (item.itemId) {
            R.id.menu_settings -> {
                LOGGER.debug("settings screen menu item selected.")
                Intent(this, SettingsActivity::class.java).also {
                    this.startActivity(it)
                }
                true
            }
            R.id.menu_sync -> {
                syncItem = item
                synchronize()
                true
            }
            R.id.menu_logout -> {
                Flow.start(this, FlowContextRegistry.flowContext.copy(
                    flowType = FlowType.LOGOUT,
                )) { _, resultCode, _ ->
                    if (resultCode == RESULT_OK) {
                        Intent(this, WelcomeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(this)
                        }
                    }
                }
                true
            }
            R.id.menu_delete_registration -> {
                DialogHelper.ErrorDialogFragment(
                    message = getString(R.string.delete_registration_warning),
                    title = getString(R.string.dialog_warn_title),
                    positiveButtonCaption = getString(R.string.confirm_yes),
                    negativeButtonCaption = getString(R.string.cancel),
                    positiveAction = {
                        Flow.start(this, FlowContextRegistry.flowContext.copy(
                            flowType = FlowType.DEL_REGISTRATION
                        )) { _, resultCode, _ ->
                            if (resultCode == RESULT_OK) {
                                PreferenceManager.getDefaultSharedPreferences(this)
                                    .edit()
                                    .putBoolean(OfflineWorkerUtil.PREF_DELETE_REGISTRATION, true)
                                    .apply()
                                Intent(this, WelcomeActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    startActivity(this)
                                }
                            }
                        }
                    }
                ).apply {
                    isCancelable = false
                    show(supportFragmentManager, this@EntitySetListActivity.getString(R.string.delete_registration))
                }
                true
            }
            else -> false
        }
    }

    private fun navForInitialize() {
        if (OfflineWorkerUtil.offlineODataProvider == null) {
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        OfflineWorkerUtil.syncRequest?.let {
            updateProgressForSync()
        }
    }

    override fun onStop() {
        super.onStop()
        OfflineWorkerUtil.syncRequest?.let {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, true)
                .apply()
        }
    }

    private fun synchronize() {
        OfflineWorkerUtil.sync(applicationContext)
        updateProgressForSync()
    }

    private fun updateProgressForSync() {
        syncItem?.isEnabled = false
        OfflineWorkerUtil.addProgressListener(progressListener)
        sync_determinate.visibility = View.VISIBLE
        WorkManager.getInstance(applicationContext)
            .getWorkInfoByIdLiveData(OfflineWorkerUtil.syncRequest!!.id)
            .observe(this, { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    syncItem?.isEnabled = true
                    OfflineWorkerUtil.removeProgressListener(progressListener)
                    OfflineWorkerUtil.resetSyncRequest()
                    with(sync_determinate) {
                        this.visibility = View.INVISIBLE
                        this.progress = 0
                    }
                    when(workInfo.state) {
                        WorkInfo.State.SUCCEEDED -> {
                            LOGGER.info("Offline sync done.")
                        }
                        WorkInfo.State.FAILED -> {
                            DialogHelper(this@EntitySetListActivity).showOKOnlyDialog(
                                fragmentManager = supportFragmentManager,
                                message = workInfo.outputData.getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL) ?: getString(R.string.synchronize_failure_detail)
                            )
                        }
                    }
                }
            })
    }

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.SYNC

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            with(sync_determinate) {
                this.max = totalSteps
                this.progress = currentStep
            }
        }

        override fun getStartPoint(): Int {
            return OfflineSyncWorker.startPointForSync
        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(EntitySetListActivity::class.java)
        private const val BLUE_ANDROID_ICON = R.drawable.ic_android_blue
        private const val WHITE_ANDROID_ICON = R.drawable.ic_android_white
    }
}
