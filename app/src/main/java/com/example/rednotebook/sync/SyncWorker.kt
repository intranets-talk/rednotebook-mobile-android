package com.example.rednotebook.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.rednotebook.data.repository.EntryRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = EntryRepository(applicationContext)
            repo.syncPendingOps()
            Result.success()
        } catch (e: Exception) {
            // Retry later if something unexpected goes wrong
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "rednotebook_sync"

        /** Enqueue a one-time sync that only runs on WiFi. */
        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)  // WiFi only
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,  // don't enqueue if one is already pending
                request
            )
        }
    }
}
