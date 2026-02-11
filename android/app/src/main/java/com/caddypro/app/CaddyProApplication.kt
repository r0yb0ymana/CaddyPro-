package com.caddypro.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * CaddyPro Application
 *
 * Main application class with Hilt DI and WorkManager integration.
 * Implements Configuration.Provider to use HiltWorkerFactory for
 * injecting dependencies into WorkManager workers (SyncWorker).
 */
@HiltAndroidApp
class CaddyProApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
