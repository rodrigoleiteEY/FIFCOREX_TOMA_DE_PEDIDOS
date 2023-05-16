package com.company.fifcorex_toma_de_pedidos.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import androidx.preference.PreferenceManager
import androidx.work.*
import com.company.fifcorex_toma_de_pedidos.app.MainBusinessActivity
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch

/**
 * Represents the worker to open the offline database.
 */
class OfflineOpenWorker(context: Context, params: WorkerParameters) :
    OfflineBaseWorker(context, params) {

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.OPEN

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            val requestID = System.currentTimeMillis().toInt()
            val intent = Intent(context, MainBusinessActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
            val pendingIntent = PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            notificationManager.notify(
                    OFFLINE_NOTIFICATION_CHANNEL_INT_ID,
                    createNotification(totalSteps, currentStep, pendingIntent)
            )
        }

        override fun getStartPoint(): Int {
            return startPointForOpen
        }
    }

    override suspend fun doWork(): Result = withContext(IO) {
        setForeground(ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification()))
        OfflineWorkerUtil.addProgressListener(progressListener)

        val countDownLatch = CountDownLatch(1)

        var result = 0
        var errorMessage: String? = null
        startPointForOpen = 0
        OfflineWorkerUtil.offlineODataProvider?.also { provider ->
            provider.open({
                logger.info("Offline provider open succeeded.")
                if (OfflineWorkerUtil.userSwitchFlag) {
                    startPointForOpen = progressListener.totalStepsForTwoProgresses / 2
                    provider.download({
                        sharedPreferences.edit()
                                .putBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, true)
                                .apply()
                        logger.info("Offline provider download succeeded.")
                        countDownLatch.countDown()
                    }, {
                        errorMessage = it.message ?: "Unknown offline sync error when downloading data."
                        logger.error(errorMessage)
                        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                        val activeNetwork = connectivityManager.activeNetwork
                        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                        result = if (capabilities == null) {
                            -1
                        } else {
                            it.errorCode
                        }
                        countDownLatch.countDown()
                    })
                } else {
                    sharedPreferences.edit()
                            .putBoolean(OfflineWorkerUtil.PREF_OFFLINE_INITIALIZED, true)
                            .apply()
                    countDownLatch.countDown()
                }
            }, { exception ->
                errorMessage = exception.message ?: "Unknown offline sync error when init opening data."
                logger.error(errorMessage)
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                result = if (capabilities == null) {
                    -1
                } else {
                    exception.errorCode
                }
                countDownLatch.countDown()
            })
        }

        countDownLatch.await()
        OfflineWorkerUtil.removeProgressListener(progressListener)
        return@withContext if (result == 0) Result.success()
        else Result.failure(workDataOf(OfflineWorkerUtil.OUTPUT_ERROR_KEY to result, OfflineWorkerUtil.OUTPUT_ERROR_DETAIL to errorMessage))
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OfflineOpenWorker::class.java)
        var startPointForOpen = 0
            private set
    }
}
