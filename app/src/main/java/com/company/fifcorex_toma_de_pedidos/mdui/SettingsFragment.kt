package com.company.fifcorex_toma_de_pedidos.mdui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.preference.*

import com.company.fifcorex_toma_de_pedidos.R
import com.company.fifcorex_toma_de_pedidos.app.WelcomeActivity
import com.sap.cloud.mobile.flowv2.model.FlowType
import com.sap.cloud.mobile.flowv2.core.Flow.Companion.start
import com.sap.cloud.mobile.flowv2.model.FlowConstants
import com.sap.cloud.mobile.flowv2.core.FlowContextRegistry.flowContext

import android.util.Log
import androidx.preference.PreferenceManager
import androidx.preference.ListPreference
import ch.qos.logback.classic.Level
import com.sap.cloud.mobile.foundation.settings.policies.LogPolicy
import com.company.fifcorex_toma_de_pedidos.app.SAPWizardApplication
import com.sap.cloud.mobile.foundation.logging.LoggingService
import android.widget.Toast
import com.sap.cloud.mobile.flowv2.core.DialogHelper
import com.sap.cloud.mobile.foundation.mobileservices.ServiceListener
import com.sap.cloud.mobile.foundation.mobileservices.ServiceResult
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.sap.cloud.mobile.foundation.mobileservices.SDKInitializer


/** This fragment represents the settings screen. */
class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var logLevelPreference: ListPreference
    private lateinit var logUploadPreference: Preference
    private var changePassCodePreference: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        logLevelPreference = findPreference(getString(R.string.log_level))!!
        prepareLogSetting(logLevelPreference)
        // Upload log
        logUploadPreference = findPreference(getString(R.string.upload_log))!!
        logUploadPreference.setOnPreferenceClickListener {
            logUploadPreference.isEnabled = false
            SDKInitializer.getService(LoggingService::class)?.also { logging ->
                logging.upload(owner = this, listener = object : ServiceListener<Boolean> {
                    override fun onServiceDone(result: ServiceResult<Boolean>) {
                        logUploadPreference.isEnabled = true
                        if (result is ServiceResult.SUCCESS) {
                            Toast.makeText(requireActivity(), R.string.log_upload_ok, Toast.LENGTH_LONG).show()
                            LOGGER.info("Log is uploaded to the server.")
                        } else {
                            val message: String = (result as ServiceResult.FAILURE).message
                            DialogHelper(requireActivity()).showOKOnlyDialog(
                                fragmentManager = requireActivity().supportFragmentManager,
                                message = message
                            )
                            LOGGER.error("Log upload failed with error message: $message")
                        }

                    }
                })
            }
            false
        }

        changePassCodePreference = findPreference(getString(R.string.manage_passcode))
        changePassCodePreference!!.setOnPreferenceClickListener {
            changePassCodePreference!!.isEnabled = false
            val flowContext =
                flowContext.copy(flowType = FlowType.CHANGEPASSCODE)
            start(requireActivity(), flowContext) { requestCode, _, _ ->
                if (requestCode == FlowConstants.FLOW_ACTIVITY_REQUEST_CODE) {
                    changePassCodePreference!!.isEnabled = true
                }
            }
            false
        }
        // Reset App
        val resetAppPreference : Preference = findPreference(getString(R.string.reset_app))!!
        resetAppPreference.setOnPreferenceClickListener {
            start(
                activity = requireActivity(),
                flowContext = flowContext.copy(flowType = FlowType.RESET),
                flowActivityResultCallback = { _, resultCode, _ ->
                    if (resultCode == Activity.RESULT_OK) {
                        Intent(requireActivity(), WelcomeActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(this)
                        }
                    }
                })
            false
        }

    }

    override fun onResume() {
        super.onResume()
        prepareLogSetting(logLevelPreference)
    }

    private fun logStrings() = mapOf<Level, String>(
        Level.ALL to getString(R.string.log_level_path),
        Level.DEBUG to getString(R.string.log_level_debug),
        Level.INFO to getString(R.string.log_level_info),
        Level.WARN to getString(R.string.log_level_warning),
        Level.ERROR to getString(R.string.log_level_error),
        Level.OFF to getString(R.string.log_level_none)
    )

    private fun prepareLogSetting(logLevelPreference: ListPreference) {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(requireActivity().applicationContext)
        val str: String? = sharedPreferences.getString(
            SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE,
            LogPolicy().toString()
        )
        val settings = LogPolicy.createFromJsonString(str!!)
        Log.d(TAG, "log settings: $settings")
        logLevelPreference.entries = logStrings().values.toTypedArray()
        logLevelPreference.entryValues = arrayOf(
                Level.ALL.levelInt.toString(),
                Level.DEBUG.levelInt.toString(),
                Level.INFO.levelInt.toString(),
                Level.WARN.levelInt.toString(),
                Level.ERROR.levelInt.toString(),
                Level.OFF.levelInt.toString()
        )
        logLevelPreference.isPersistent = true
        logLevelPreference.summary = logStrings()[LogPolicy.getLogLevel(settings)]
        logLevelPreference.value = LogPolicy.getLogLevel(settings).levelInt.toString()
        logLevelPreference.setOnPreferenceChangeListener { preference, newValue ->
            val logLevel = Level.toLevel(Integer.valueOf(newValue as String))
            val newSettings = settings.copy(logLevel = LogPolicy.getLogLevelString(logLevel))
            sharedPreferences.edit()
                .putString(SAPWizardApplication.KEY_LOG_SETTING_PREFERENCE, newSettings.toString())
                .apply()
            SDKInitializer.getService(LoggingService::class)?.policy = newSettings
            preference.summary = logStrings()[LogPolicy.getLogLevel(newSettings)]
            true
        }
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(SettingsFragment::class.java)
        private val TAG = SettingsFragment::class.simpleName
    }
}
