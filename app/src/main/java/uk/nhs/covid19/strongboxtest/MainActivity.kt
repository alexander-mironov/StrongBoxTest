package uk.nhs.covid19.strongboxtest

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    lateinit var workManager: WorkManager

    var handler = Handler()
    var delay = 15_000L
    lateinit var updateNumbers: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        workManager = WorkManager.getInstance(applicationContext)
        setContentView(R.layout.activity_main)


        val canUseStrongBox =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && packageManager.hasSystemFeature(
                PackageManager.FEATURE_STRONGBOX_KEYSTORE
            )
        useStrongBox.isEnabled = canUseStrongBox
        useStrongBox.isChecked = canUseStrongBox

        restart.setOnClickListener {
            runBackgroundTasks()
        }

        runBackgroundTasks()
    }

    private fun runBackgroundTasks() {
        workManager.cancelAllWork()

        attemptsTextView.text = "0"
        successesTextView.text = "0"
        Stats.reset(this)

//        val workersCount = workersCountEditText.text.toString().toIntOrNull()
//        invalidWorkerCount.isVisible = workersCount == null
//        if (workersCount == null) {
//            return
//        }

        val useStrongBox = useStrongBox.isChecked


        val inputData = Data.Builder()
            .putInt(TestWorker.INPUT_ID, BuildConfig.ID.toInt(10))
            .putBoolean(TestWorker.INPUT_USE_STRONGBOX, useStrongBox).build()
        val testWorkRequest = PeriodicWorkRequestBuilder<TestWorker>(15, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()
        workManager
            .enqueueUniquePeriodicWork(
                "TestWorker${BuildConfig.ID}",
                ExistingPeriodicWorkPolicy.REPLACE,
                testWorkRequest
            )

    }

    override fun onResume() {
        super.onResume()

        updateNumbers = Runnable {
            attemptsTextView.text = Stats.getAttempts(this).toString()
            successesTextView.text = Stats.getSuccesses(this).toString()
            handler.postDelayed(updateNumbers, delay)
        }
        handler.postDelayed(updateNumbers, 0L)

    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateNumbers)
    }
}
