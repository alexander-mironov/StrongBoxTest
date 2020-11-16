package uk.nhs.covid19.strongboxtest

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class TestWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {
    override fun doWork(): Result {
        Log.d(
            TAG,
            "Worker $id: starting. Stats: ${Stats.getSuccesses(applicationContext)} of ${
                Stats.getAttempts(
                    applicationContext
                )
            } attempts were successful"
        )

        val id = inputData.getInt(INPUT_ID, -1)
        if (id == -1) {
            return Result.failure()
        }
        val useStrongBox = inputData.getBoolean(INPUT_USE_STRONGBOX, true)

        Stats.incrementAttempts(applicationContext)
        val encryptedFile = RetryMechanism.retryWithBackOff {
            EncryptionUtils.createEncryptedFile(applicationContext, "venues", useStrongBox)
        }
        Log.d(TAG, "Worker $id: encryptedFile created: " + encryptedFile.file.absoluteFile)
        val sharedPreferences = RetryMechanism.retryWithBackOff {
            EncryptionUtils.createEncryptedSharedPreferences(applicationContext, id, useStrongBox)
        }
        Log.d(TAG, "Worker $id: sharedPreferences created: " + sharedPreferences.hashCode())

        Stats.incrementSuccesses(applicationContext)

        Log.d(
            TAG,
            "Worker $id: finishing. Stats: ${Stats.getSuccesses(applicationContext)} of ${
                Stats.getAttempts(
                    applicationContext
                )
            } attempts were successful"
        )
        return Result.success()
    }

    companion object {
        const val INPUT_ID = "INPUT_ID"
        const val INPUT_USE_STRONGBOX = "INPUT_USE_STRONGBOX"
        const val TAG = EncryptionUtils.TAG
    }
}
