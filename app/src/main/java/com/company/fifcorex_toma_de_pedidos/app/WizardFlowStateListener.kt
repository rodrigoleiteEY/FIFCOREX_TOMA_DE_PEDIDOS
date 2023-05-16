package com.company.fifcorex_toma_de_pedidos.app

import android.content.Intent
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry.flowContext
import com.sap.cloud.mobile.flowv2.ext.FlowStateListener
import com.sap.cloud.mobile.foundation.model.AppConfig
import android.widget.Toast
import com.sap.cloud.mobile.foundation.authentication.AppLifecycleCallbackHandler
import com.sap.cloud.mobile.foundation.settings.policies.ClientPolicies
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import org.slf4j.LoggerFactory
import ch.qos.logback.classic.Level
import com.company.fifcorex_toma_de_pedidos.R
import com.sap.cloud.mobile.flowv2.securestore.UserSecureStoreDelegate
import com.company.fifcorex_toma_de_pedidos.service.OfflineWorkerUtil

class WizardFlowStateListener(private val application: SAPWizardApplication) :
    FlowStateListener() {

    private var userSwitchFlag = false

    override fun onAppConfigRetrieved(appConfig: AppConfig) {
        logger.debug("onAppConfigRetrieved: $appConfig")
    }

    override fun onApplicationReset() {
        this.application.resetApplication()
    }

    override fun onApplicationLocked() {
        super.onApplicationLocked()
        application.isApplicationUnlocked = false
    }

    override fun onFlowFinished(flowName: String?) {
        flowName?.let{
            application.isApplicationUnlocked = true
        }

        if (userSwitchFlag || application.preferenceManager.getBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, false)) {
            if (application.preferenceManager.getBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, false)) {
                application.preferenceManager.edit().putBoolean(OfflineWorkerUtil.PREF_FOREGROUND_SERVICE, false).apply()
            }
            Intent(application, MainBusinessActivity::class.java).also {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                application.startActivity(it)
            }
        }
    }

    override fun onClientPolicyRetrieved(policies: ClientPolicies) {
        policies.logPolicy?.also { logSettings ->
            val sharedPreferences = application.preferenceManager
            val existing =
                    sharedPreferences.getString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE, "")
            val currentSettings = if (existing.isNullOrEmpty()) {
                LogPolicy()
            } else {
                LogPolicy.createFromJsonString(existing)
            }
            if (currentSettings.logLevel != logSettings.logLevel || existing.isNullOrEmpty()) {
                val editor = sharedPreferences.edit()
                editor.putString(
                    SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE,
                    logSettings.toString()
                )
                editor.apply()
                AppLifecycleCallbackHandler.getInstance().activity?.let {
                    it.runOnUiThread {
                        val logString = when (LogPolicy.getLogLevel(logSettings)) {
                            Level.ALL -> application.getString(R.string.log_level_path)
                            Level.INFO -> application.getString(R.string.log_level_info)
                            Level.WARN -> application.getString(R.string.log_level_warning)
                            Level.ERROR -> application.getString(R.string.log_level_error)
                            Level.OFF -> application.getString(R.string.log_level_none)
                            else -> application.getString(R.string.log_level_debug)
                        }
                        Toast.makeText(
                                application,
                                String.format(
                                        application.getString(R.string.log_level_changed),
                                        logString
                                ),
                                Toast.LENGTH_SHORT
                        ).show()
                        logger.info(String.format(
                                application.getString(R.string.log_level_changed),
                                logString
                        ))
                    }
                }
            }
        }
    }

    override fun onOfflineEncryptionKeyReady(key: String?) {
        logger.info("offline key ready.")
        userSwitchFlag =  if (!application.preferenceManager.getBoolean(OfflineWorkerUtil.PREF_DELETE_REGISTRATION, false)) {
            flowContext.getPreviousUserId()?.let {
                flowContext.getCurrentUserId() != it
            } ?: false
        } else {
            application.preferenceManager.edit().putBoolean(OfflineWorkerUtil.PREF_DELETE_REGISTRATION, false).apply();
            OfflineWorkerUtil.resetOffline(application)
            true
        }

        OfflineWorkerUtil.userSwitchFlag = userSwitchFlag

        /*
         * In single user scenario,
         * if offline data store initialized and its encryption key not found,
         * close and remove offline data store, meanwhile, set userSwitchFlag to true to trigger some important setting
         */
        if (UserSecureStoreDelegate.getInstance().getRuntimeMultipleUserModeAsync() == false
                && application.preferenceManager.getBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, false)
                && UserSecureStoreDelegate.getInstance().getData<String>(OfflineWorkerUtil.OFFLINE_DATASTORE_ENCRYPTION_KEY) == null) {
                userSwitchFlag = true
                OfflineWorkerUtil.resetOffline(application)
        }

        if (userSwitchFlag) {
            application.preferenceManager.apply {
                edit().putBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, false)
                        .apply()
            }
            application.repositoryFactory.reset()
            OfflineWorkerUtil.offlineODataProvider?.close()
            OfflineWorkerUtil.resetOfflineODataProvider()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(WizardFlowStateListener::class.java)
    }
}
