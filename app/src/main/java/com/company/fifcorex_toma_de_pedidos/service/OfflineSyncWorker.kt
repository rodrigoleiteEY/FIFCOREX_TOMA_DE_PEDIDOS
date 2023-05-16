package com.company.fifcorex_toma_de_pedidos.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.company.fifcorex_toma_de_pedidos.mdui.EntitySetListActivity
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch

class OfflineSyncWorker(context: Context, params: WorkerParameters) :
    OfflineBaseWorker(context, params) {

    private val progressListener = object : OfflineProgressListener() {
        override val workerType = WorkerType.SYNC

        override fun updateProgress(currentStep: Int, totalSteps: Int) {
            val requestID = System.currentTimeMillis().toInt()
            val intent = Intent(context, EntitySetListActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
            val pendingIntent = PendingIntent.getActivity(context, requestID, intent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            notificationManager.notify(
                    OFFLINE_NOTIFICATION_CHANNEL_INT_ID,
                    createNotification(totalSteps, currentStep, pendingIntent)
            )
        }

        override fun getStartPoint(): Int {
            return startPointForSync
        }
    }

    override suspend fun doWork(): Result = withContext(IO) {
        var errorMessage: String? = null
        setForeground(ForegroundInfo(OFFLINE_NOTIFICATION_CHANNEL_INT_ID, createNotification()))
        OfflineWorkerUtil.addProgressListener(progressListener)
        val countDownLatch = CountDownLatch(1)
        OfflineWorkerUtil.offlineODataProvider?.also { provider ->
            logger.info("Uploading data...")
            startPointForSync = 0
            provider.upload({
                logger.info("Downloading data...")
                startPointForSync = progressListener.totalStepsForTwoProgresses / 2
                provider.download({
                    countDownLatch.countDown()
                }, {
                    countDownLatch.countDown()
                    errorMessage = it.message ?: "Unknown offline sync error when downloading data."
                    logger.error(errorMessage)
                })
            }, {
                countDownLatch.countDown()
                errorMessage = it.message ?: "Unknown offline sync error when uploading data."
                logger.error(errorMessage)
            })
        }

        countDownLatch.await()
        OfflineWorkerUtil.removeProgressListener(progressListener)
        errorMessage?.let {
            logger.error("Offline sync error: $it")
            return@withContext Result.failure(workDataOf(OfflineWorkerUtil.OUTPUT_ERROR_DETAIL to it))
        }
        return@withContext Result.success()
    }

    companion object {
        private val logger = LoggerFactory.getLogger(OfflineSyncWorker::class.java)
        var startPointForSync = 0
            private set
    }
}