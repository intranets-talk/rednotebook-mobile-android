package com.example.rednotebook

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.work.WorkManager
import com.example.rednotebook.data.network.ApiClient
import com.example.rednotebook.sync.SyncWorker
import com.example.rednotebook.ui.settings.SettingsFragment

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply saved theme before any UI is created
        SettingsFragment.applyTheme(this)

        ApiClient.init(this)
        WorkManager.getInstance(this)
        registerWifiCallback()
    }

    private fun registerWifiCallback() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                SyncWorker.enqueue(this@App)
            }
        })
    }
}
