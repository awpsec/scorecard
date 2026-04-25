package com.awper.lightscore.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.first

object NetworkGate {
    fun isWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isCellular(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    suspend fun shouldPoll(
        context: Context,
        store: SettingsStore,
        isFavoriteGame: Boolean
    ): Boolean {
        if (isWifi(context)) return true
        if (!isCellular(context)) return false
        if (store.autoUpdateCellular.first()) return true
        return isFavoriteGame && store.autoUpdateFavoritesCellular.first()
    }
}
