package com.chattalkie.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.chattalkie.app.data.repository.SyncRepository
import com.chattalkie.app.data.AuthRepository
import android.util.Log

class SyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val syncRepository = SyncRepository(context)

    override suspend fun doWork(): Result {
        return try {
            val token = AuthRepository.token
            if (token != null) {
                syncRepository.performSync(token)
                Result.success()
            } else {
                Log.d("SyncWorker", "No token, skipping sync")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e("SyncWorker", "Sync failed", e)
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
