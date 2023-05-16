package com.company.fifcorex_toma_de_pedidos.app

import android.content.Intent
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import com.company.fifcorex_toma_de_pedidos.R

import androidx.preference.PreferenceManager
import androidx.fragment.app.DialogFragment
import android.view.MenuItem
import android.view.View
import android.app.NotificationManager
import androidx.work.*
import androidx.lifecycle.Observer
import com.sap.cloud.mobile.fiori.onboarding.ext.OfflineNetworkErrorScreenSettings
import com.sap.cloud.mobile.fiori.onboarding.ext.OfflineTransactionIssueScreenSettings
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import com.company.fifcorex_toma_de_pedidos.service.*
import java.util.*
import kotlinx.android.synthetic.main.activity_main_business.*

import org.slf4j.LoggerFactory

import com.company.fifcorex_toma_de_pedidos.mdui.EntitySetListActivity

class MainBusinessActivity : AppCompatActivity() {

    private var isOfflineStoreInitialized = false
    private var dialogFragment: DialogFragment? = null

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.OPEN

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            offlineInitSyncScreen.updateProgressBar(currentStep, totalSteps)
        }

        override fun getStartPoint(): Int {
            return OfflineOpenWorker.startPointForOpen
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_business)
        dialogFragment = DialogHelper.ErrorDialogFragment(
                message = getString(R.string.offline_navigation_dialog_message),
                title =  getString(R.string.offline_navigation_dialog_title),
                positiveButtonCaption = getString(R.string.offline_navigation_dialog_positive_option),
                negativeButtonCaption = getString(R.string.offline_navigation_dialog_negative_option),
                positiveAction =  {
                    if (!isOfflineStoreInitialized) {
                        application.getSystemService(NotificationManager::class.java).cancel(OfflineBaseWorker.OFFLINE_NOTIFICATION_CHANNEL_INT_ID)
                        WorkManager.getInstance(application).cancelUniqueWork(OfflineWorkerUtil.OFFLINE_OPEN_WORKER_UNIQUE_NAME)
                    }
                    backToWelcome()
                }
        ).also {
            it.isCancelable = false
        }
    }

    private fun navigateToEntityList() {
        startActivity(Intent(this, EntitySetListActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }

    private fun startEntitySetListActivity() {
        if (isOfflineStoreInitialized) {
            OfflineWorkerUtil.resetOpenRequest()
            navigateToEntityList()
        } else {
            LOGGER.info("Waiting for the sync finish.")
            WorkManager.getInstance(applicationContext)
                    .getWorkInfoByIdLiveData(OfflineWorkerUtil.openRequest!!.id)
                    .observe(this, Observer { workInfo ->
                        if (workInfo != null && workInfo.state.isFinished) {
                            OfflineWorkerUtil.removeProgressListener(progressListener)
                            OfflineWorkerUtil.resetOpenRequest()
                            when (workInfo.state) {
                                WorkInfo.State.FAILED -> {
                                    supportActionBar?.setTitle(R.string.initializing_offline_store_failed)
                                    when (workInfo.outputData.getInt(OfflineWorkerUtil.OUTPUT_ERROR_KEY, 0)) {
                                        -1 -> {
                                            offlineNetworkErrorAction()
                                        }
                                        -10425 -> {
                                            offlineTransactionIssueAction()
                                        }
                                        else -> {
                                            DialogHelper(application).showOKOnlyDialog(
                                                    supportFragmentManager,
                                                    message = workInfo.outputData.getString(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL) ?: "Offline sync failed.",
                                                    title = getString(R.string.offline_initial_open_error),
                                                    positiveAction = {
                                                        startActivity(Intent(this, WelcomeActivity::class.java).apply {
                                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                                        })
                                                    })
                                        }
                                    }
                                }
                                WorkInfo.State.SUCCEEDED -> {
                                    navigateToEntityList()
                                }
                            }
                        }
                    })
        }
    }

    override fun onResume() {
        super.onResume()
        offlineNetworkErrorScreen.visibility = View.INVISIBLE
        offlineTransactionIssueScreen.visibility = View.INVISIBLE
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setTitle(R.string.initializing_offline_store)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        isOfflineStoreInitialized = sharedPreferences.getBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, false)

        if (isOfflineStoreInitialized) {
            offlineInitSyncScreen.visibility = View.INVISIBLE
            main_bus_resume_progress_bar.visibility = View.VISIBLE
        } else {
            OfflineWorkerUtil.addProgressListener(progressListener)
            offlineInitSyncScreen.visibility = View.VISIBLE
            main_bus_resume_progress_bar.visibility = View.INVISIBLE
        }
        val isMultipleUserMode = UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() == true
        val appConfig = FlowContextRegistry.flowContext.appConfig!!

        OfflineWorkerUtil.initializeOffline(application, appConfig, isMultipleUserMode)
        OfflineWorkerUtil.open(application)
        startEntitySetListActivity()
    }

    override fun onStop() {
        super.onStop()
        OfflineWorkerUtil.openRequest?.let {
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                .putBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, true)
                .apply()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        android.R.id.home -> {
            if (offlineNetworkErrorScreen.visibility == View.VISIBLE || offlineTransactionIssueScreen.visibility == View.VISIBLE) {
                backToWelcome()
            } else {
                dialogFragment?.show(supportFragmentManager, "Back")
            }
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (offlineNetworkErrorScreen.visibility == View.VISIBLE || offlineTransactionIssueScreen.visibility == View.VISIBLE) {
            backToWelcome()
        } else {
            dialogFragment?.show(supportFragmentManager, "Back")
        }
    }

    private fun backToWelcome() {
        OfflineWorkerUtil.removeProgressListener(progressListener)
        OfflineWorkerUtil.resetOpenRequest()
        startActivity(Intent(this, WelcomeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        })
    }

    private fun offlineNetworkErrorAction() {
        this@MainBusinessActivity.runOnUiThread {
            offlineNetworkErrorScreen.visibility = View.VISIBLE
            offlineInitSyncScreen.visibility = View.INVISIBLE
            main_bus_resume_progress_bar.visibility = View.INVISIBLE
            val offlineNetworkErrorScreenSettings = OfflineNetworkErrorScreenSettings.Builder().build()
            with(offlineNetworkErrorScreen) {
                initialize(offlineNetworkErrorScreenSettings)
                setButtonClickListener(View.OnClickListener {
                    OfflineWorkerUtil.addProgressListener(progressListener)
                    OfflineWorkerUtil.open(application)
                    startEntitySetListActivity()
                    supportActionBar?.setTitle(R.string.initializing_offline_store)
                    offlineNetworkErrorScreen.visibility = View.INVISIBLE
                    offlineInitSyncScreen.visibility = View.VISIBLE
                })
            }
        }
    }

    private fun offlineTransactionIssueAction() {
        this@MainBusinessActivity.runOnUiThread {
            offlineTransactionIssueScreen.visibility = View.VISIBLE
            offlineInitSyncScreen.visibility = View.INVISIBLE
            main_bus_resume_progress_bar.visibility = View.INVISIBLE
            val offlineTransactionIssueScreenSettings = OfflineTransactionIssueScreenSettings.Builder().build()
            with(offlineTransactionIssueScreen) {
                initialize(offlineTransactionIssueScreenSettings)
                CoroutineScope(IO).launch {
                    val user = UserSecureStoreDelegate.getInstance().getUserInfoById(OfflineWorkerUtil.offlineODataProvider!!.previousUser)
                    withContext(Main) {
                        user?.let {
                            setPrevUserName(it.name)
                            setPrevUserMail(it.email)
                        } ?: run {
                            setPrevUserName(OfflineWorkerUtil.offlineODataProvider!!.previousUser)
                        }
                    }
                }
            }
            offlineTransactionIssueScreen.setButtonClickListener(View.OnClickListener {
                startActivity(Intent(this, WelcomeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                })
            })
        }
    }
    companion object {
        private val LOGGER = LoggerFactory.getLogger(MainBusinessActivity::class.java)
    }
}
