package com.malacca.guide.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BluetoothReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> Log.d(TAG, "adapter state changed")
            BluetoothDevice.ACTION_BOND_STATE_CHANGED -> Log.d(TAG, "bond state changed")
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "device connected ${device?.address}")
                GlassesManager.markConnected(device?.address)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                Log.d(TAG, "device disconnected ${device?.address}")
                GlassesManager.markDisconnected(device?.address)
            }
        }
    }

    companion object {
        private const val TAG = "BluetoothReceiver"
    }
}
