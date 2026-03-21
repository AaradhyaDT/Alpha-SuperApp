package com.alpha.features.sbrcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * BluetoothComm.kt
 *
 * Manages a classic Bluetooth SPP connection to the HC-05 module.
 *
 * HC-05 uses the standard SPP UUID: 00001101-0000-1000-8000-00805F9B34FB
 *
 * Usage:
 *   val bt = BluetoothComm()
 *   val ok = bt.connect("HC-05")          // or pass MAC address directly
 *   bt.sendByte(70)                        // send Forward command
 *   bt.disconnect()
 *
 * connect() must be called from a coroutine (it is a suspend fun).
 * sendByte() is safe to call from any thread — uses synchronized write.
 *
 * Requires permissions in AndroidManifest.xml:
 *   BLUETOOTH, BLUETOOTH_CONNECT (API 31+), BLUETOOTH_SCAN (API 31+)
 *
 * The caller (ViewModel) is responsible for checking that permissions
 * have been granted before calling connect().
 */
class BluetoothComm {

    companion object {
        // Standard SPP UUID — same for all HC-05 modules
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    val isConnected: Boolean
        get() = socket?.isConnected == true

    /**
     * Connect to a paired HC-05 device by name or MAC address.
     *
     * @param nameOrMac  Device name (e.g. "HC-05") or MAC (e.g. "20:16:05:xx:xx:xx")
     * @return           Result.success(Unit) or Result.failure(exception)
     *
     * The device MUST already be paired in Android Bluetooth settings.
     * This does NOT pair the device — pairing must be done manually once.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(context: android.content.Context, nameOrMac: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val adapter = (context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
                ?: return@withContext Result.failure(Exception("Bluetooth not supported on this device"))
            if (!adapter.isEnabled) {
                return@withContext Result.failure(Exception("Bluetooth is off — enable it in Settings"))
            }

            // Find the device in the paired devices list
            val device: BluetoothDevice = adapter.bondedDevices
                ?.firstOrNull { dev ->
                    dev.name.equals(nameOrMac, ignoreCase = true) ||
                    dev.address.equals(nameOrMac, ignoreCase = true)
                }
                ?: return@withContext Result.failure(
                    Exception("Device \"$nameOrMac\" not found in paired devices. Pair it in Bluetooth Settings first.")
                )

            // Cancel any ongoing discovery — it slows down connection
            adapter.cancelDiscovery()

            // Close any existing socket before creating a new one
            disconnect()

            socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket!!.connect()   // blocks until connected or throws IOException

            Result.success(Unit)
        } catch (e: IOException) {
            socket = null
            Result.failure(Exception("Connection failed: ${e.message}"))
        } catch (e: SecurityException) {
            socket = null
            Result.failure(Exception("Bluetooth permission denied: ${e.message}"))
        }
    }

    /**
     * Send a single raw integer byte to the Arduino.
     *
     * Command byte map (same as Python V7):
     *   70=Forward  66=Backward  76=Left   82=Right
     *   79=CW spin  88=CCW spin  83=Stop
     *
     * Thread-safe via synchronized block.
     */
    fun sendByte(value: Int) {
        val s = socket ?: return
        if (!s.isConnected) return
        try {
            synchronized(this) {
                s.outputStream.write(value)
                s.outputStream.flush()
            }
        } catch (e: IOException) {
            // Connection dropped — caller will notice via isConnected = false
        }
    }

    fun disconnect() {
        try {
            socket?.close()
        } catch (_: IOException) { }
        socket = null
    }
}
