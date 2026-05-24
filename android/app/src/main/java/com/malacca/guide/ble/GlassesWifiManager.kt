package com.malacca.guide.ble

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages the WiFi Direct (P2P) group used to receive media from the glasses.
 * The phone acts as the group owner; the glasses join as a client and report
 * their IP back over BLE (see GlassesManager's BLE->IP bridge).
 */
object GlassesWifiManager {

    private const val TAG = "GlassesWifiManager"

    private var appContext: Context? = null
    private var manager: WifiP2pManager? = null
    private var channel: WifiP2pManager.Channel? = null
    private var receiver: BroadcastReceiver? = null

    private val _groupInfo = MutableStateFlow<WifiP2pInfo?>(null)
    /** Non-null with groupFormed == true once the P2P group is up. */
    val groupInfo: StateFlow<WifiP2pInfo?> = _groupInfo.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        val mgr = appContext!!.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        manager = mgr
        channel = mgr?.initialize(appContext, Looper.getMainLooper(), null)
        Log.d(TAG, "init, available=${mgr != null}")
    }

    @SuppressLint("MissingPermission")
    suspend fun createGroup(): Boolean {
        val mgr = manager ?: run { Log.e(TAG, "createGroup: WifiP2pManager unavailable"); return false }
        val ch = channel ?: run { Log.e(TAG, "createGroup: channel unavailable"); return false }
        registerReceiver()
        return suspendCancellableCoroutine { cont ->
            mgr.createGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "createGroup: success")
                    if (cont.isActive) cont.resume(true)
                }

                override fun onFailure(reason: Int) {
                    // BUSY (2) means a group already exists — reuse it.
                    val ok = reason == WifiP2pManager.BUSY
                    Log.w(TAG, "createGroup: failure reason=$reason treatedAsOk=$ok")
                    if (cont.isActive) cont.resume(ok)
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    fun removeGroup() {
        val mgr = manager
        val ch = channel
        if (mgr != null && ch != null) {
            mgr.removeGroup(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    Log.d(TAG, "removeGroup: success")
                }

                override fun onFailure(reason: Int) {
                    Log.w(TAG, "removeGroup: failure reason=$reason")
                }
            })
        }
        unregisterReceiver()
        _groupInfo.value = null
    }

    private fun registerReceiver() {
        val ctx = appContext ?: return
        if (receiver != null) return
        val filter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action != WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION) return
                val mgr = manager ?: return
                val ch = channel ?: return
                mgr.requestConnectionInfo(ch) { info ->
                    Log.d(
                        TAG,
                        "connectionInfo groupFormed=${info.groupFormed} isGO=${info.isGroupOwner} go=${info.groupOwnerAddress?.hostAddress}"
                    )
                    _groupInfo.value = if (info.groupFormed) info else null
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ctx.registerReceiver(r, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            ctx.registerReceiver(r, filter)
        }
        receiver = r
    }

    private fun unregisterReceiver() {
        val ctx = appContext ?: return
        receiver?.let {
            runCatching { ctx.unregisterReceiver(it) }
            receiver = null
        }
    }
}
