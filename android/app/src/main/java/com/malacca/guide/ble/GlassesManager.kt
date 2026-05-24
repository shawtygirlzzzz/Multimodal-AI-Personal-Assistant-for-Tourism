package com.malacca.guide.ble

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.oudmon.ble.base.bluetooth.BleOperateManager
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyListener
import com.oudmon.ble.base.communication.bigData.resp.GlassesDeviceNotifyRsp
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

data class FoundDevice(val address: String, val name: String, val rssi: Int)

object GlassesManager {

    private const val TAG = "GlassesManager"
    private const val SCAN_DURATION_MS = 10_000L

    // Photo capture is a 3-protocol flow: BLE control + WiFi Direct + HTTP.
    // The glasses join our WiFi Direct subnet and serve the photo over HTTP.
    private const val GLASSES_ASSIGNED_IP = "192.168.49.79"
    private const val WIFI_JOIN_DELAY_MS = 4_000L

    // HeyCyan glasses advertise with these model-name prefixes
    // (e.g. W610_F83A for the W610; also G300, G3, M01s, QCY models).
    private val GLASSES_NAME_PREFIXES = listOf("W610", "G300", "G3", "M01", "QCY")

    private var appContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var scanStopRunnable: Runnable? = null
    private val photoInProgress = AtomicBoolean(false)

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scanResults = MutableStateFlow<List<FoundDevice>>(emptyList())
    val scanResults: StateFlow<List<FoundDevice>> = _scanResults.asStateFlow()

    private var photoBytesChannel: Channel<ByteArray?>? = null
    private val _deviceIp = MutableStateFlow<String?>(null)
    private var connectingAddress: String? = null

    fun init(application: Application) {
        appContext = application.applicationContext
        Log.d(TAG, "init")
    }

    private val notifyListener = object : GlassesDeviceNotifyListener() {
        override fun parseData(cmdType: Int, response: GlassesDeviceNotifyRsp) {
            val raw = response.loadData?.joinToString(" ") { "%02X".format(it) } ?: "null"
            Log.d(TAG, "parseData cmdType=$cmdType len=${response.loadData?.size ?: -1} raw=[$raw]")
            if (response.loadData.size <= 6) return
            val eventType = response.loadData[6].toInt() and 0xFF
            Log.d(TAG, "parseData cmdType=$cmdType event=0x${eventType.toString(16)}")
            when (eventType) {
                0x02 -> {
                    LargeDataHandler.getInstance().getPictureThumbnails { _, success, data ->
                        Log.d(TAG, "thumbnail received success=$success size=${data?.size ?: 0}")
                        photoBytesChannel?.trySend(if (success) data else null)
                    }
                }
                0x05 -> {
                    if (response.loadData.size > 7) {
                        val battery = response.loadData[7].toInt() and 0xFF
                        Log.d(TAG, "battery=$battery%")
                    }
                }
            }
        }
    }

    private val scanCallback = object : ScanWrapperCallback {
        override fun onStart() {
            Log.d(TAG, "scan started")
        }

        override fun onStop() {
            Log.d(TAG, "scan stopped")
        }

        override fun onLeScan(device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray?) {
            val d = device ?: return
            val name = runCatching { d.name }.getOrNull()
            Log.d(TAG, "scanned device name=$name address=${d.address} rssi=$rssi")
            if (name.isNullOrBlank()) return
            if (GLASSES_NAME_PREFIXES.none { name.startsWith(it, ignoreCase = true) }) return
            val existing = _scanResults.value
            if (existing.any { it.address == d.address }) return
            _scanResults.value = existing + FoundDevice(d.address, name, rssi)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "scan failed code=$errorCode")
            if (_connectionState.value == ConnectionState.Scanning) {
                _connectionState.value = ConnectionState.Disconnected
            }
        }

        override fun onParsedData(device: BluetoothDevice?, scanRecord: ScanRecord?) {}

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {}
    }

    fun startScan() {
        val ctx = appContext ?: run {
            Log.e(TAG, "startScan: no context")
            return
        }
        Log.d(TAG, "startScan")
        _scanResults.value = emptyList()
        _connectionState.value = ConnectionState.Scanning
        try {
            BleScannerHelper.getInstance().scanDevice(ctx, null, scanCallback)
        } catch (e: Exception) {
            Log.e(TAG, "startScan error: ${e.message}")
            _connectionState.value = ConnectionState.Disconnected
            return
        }
        scanStopRunnable?.let { mainHandler.removeCallbacks(it) }
        val r = Runnable { stopScan() }
        scanStopRunnable = r
        mainHandler.postDelayed(r, SCAN_DURATION_MS)
    }

    fun stopScan() {
        val ctx = appContext ?: return
        Log.d(TAG, "stopScan")
        try {
            BleScannerHelper.getInstance().stopScan(ctx)
        } catch (e: Exception) {
            Log.e(TAG, "stopScan error: ${e.message}")
        }
        scanStopRunnable?.let { mainHandler.removeCallbacks(it) }
        scanStopRunnable = null
        if (_connectionState.value == ConnectionState.Scanning) {
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    fun connect(deviceAddress: String) {
        Log.d(TAG, "connect $deviceAddress")
        stopScan()
        connectingAddress = deviceAddress
        _connectionState.value = ConnectionState.Connecting
        LargeDataHandler.getInstance().addOutDeviceListener(100, notifyListener)
        BleOperateManager.getInstance().connectDirectly(deviceAddress)
    }

    fun markConnected(address: String?) {
        val expected = connectingAddress ?: run {
            Log.d(TAG, "markConnected ignored — no pending connect")
            return
        }
        if (address == null || address != expected) {
            Log.d(TAG, "markConnected ignored — address mismatch (got=$address expected=$expected)")
            return
        }
        Log.d(TAG, "markConnected OK $address")
        _connectionState.value = ConnectionState.Connected

        // Init handshake per SDK guide 2.3.2 — sync time and device info so the
        // glasses are ready to accept feature commands (AI photo, etc.).
        LargeDataHandler.getInstance().syncTime { _, _ ->
            Log.d(TAG, "syncTime: completed")
        }
        LargeDataHandler.getInstance().syncDeviceInfo { _, _ ->
            Log.d(TAG, "syncDeviceInfo: completed")
        }
    }

    fun markDisconnected(address: String? = null) {
        val expected = connectingAddress ?: return
        if (address != null && address != expected) return
        Log.d(TAG, "markDisconnected $address")
        connectingAddress = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun disconnect() {
        Log.d(TAG, "disconnect")
        try {
            BleOperateManager.getInstance().unBindDevice()
        } catch (e: Exception) {
            Log.e(TAG, "disconnect error: ${e.message}")
        }
        connectingAddress = null
        _connectionState.value = ConnectionState.Disconnected
    }

    suspend fun takePhoto(): ByteArray? {
        if (!photoInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "takePhoto: ignored, capture already in progress")
            return null
        }
        try {
            Log.d(TAG, "takePhoto: called, connectionState=${_connectionState.value}")
            if (_connectionState.value != ConnectionState.Connected) {
                Log.e(TAG, "takePhoto called but glasses not connected")
                return null
            }

            // 1. Bring up the WiFi Direct group (phone = group owner) so the
            //    glasses have a network to join and serve the photo over.
            Log.d(TAG, "takePhoto: creating WiFi Direct group")
            if (!GlassesWifiManager.createGroup()) {
                Log.e(TAG, "takePhoto: WiFi Direct group creation failed")
                return null
            }

            try {
                _deviceIp.value = null

                // 2. Tell the glasses to take a photo (stored on the glasses).
                Log.d(TAG, "takePhoto: sending photo command")
                LargeDataHandler.getInstance().glassesControl(
                    byteArrayOf(0x02, 0x01, 0x01)
                ) { _, response ->
                    Log.d(TAG, "takePhoto: photo cmd resp dataType=${response?.dataType} err=${response?.errorCode} work=${response?.workTypeIng} p2pIp=${response?.p2pIp}")
                    response?.p2pIp?.let { ip ->
                        if (ip.count { c -> c == '.' } == 3) _deviceIp.value = ip
                    }
                }

                // 3. Hand the glasses an IP on our WiFi Direct subnet so they
                //    join the network and start serving media over HTTP.
                Log.d(TAG, "takePhoto: writeIpToSoc $GLASSES_ASSIGNED_IP")
                LargeDataHandler.getInstance().writeIpToSoc(GLASSES_ASSIGNED_IP) { _, _ ->
                    Log.d(TAG, "takePhoto: writeIpToSoc ack")
                }

                // 4. Give the glasses time to join the WiFi Direct group.
                delay(WIFI_JOIN_DELAY_MS)

                // 5. Locate the glasses' HTTP server on the subnet.
                val ip = GlassesMediaDownloader.discoverGlassesIp(_deviceIp.value ?: GLASSES_ASSIGNED_IP)
                if (ip == null) {
                    Log.e(TAG, "takePhoto: glasses HTTP server not found on WiFi")
                    return null
                }

                // 6. Download the newest image and return its bytes.
                val images = GlassesMediaDownloader.fetchImageList(ip)
                val newest = images.lastOrNull()
                if (newest == null) {
                    Log.e(TAG, "takePhoto: no images listed by glasses")
                    return null
                }
                val bytes = GlassesMediaDownloader.downloadFile(ip, newest)
                Log.d(TAG, "takePhoto: finished, result=${if (bytes == null) "null" else "${bytes.size} bytes"}")
                return bytes
            } finally {
                GlassesWifiManager.removeGroup()
            }
        } finally {
            photoInProgress.set(false)
        }
    }

    fun connectedDeviceAddress(): String? = DeviceManager.getInstance().deviceAddress
}
