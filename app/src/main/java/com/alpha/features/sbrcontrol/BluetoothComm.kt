package com.alpha.features.sbrcontrol

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.IOException
import java.util.UUID

/**
 * BluetoothComm.kt - Optimized for Low Latency
 *
 * Manages a classic Bluetooth SPP connection to the HC-05 module.
 * Optimized with BufferedOutputStream and redundant command filtering.
 */
class BluetoothComm {

    companion object {
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private var socket: BluetoothSocket? = null
    private var outputStream: BufferedOutputStream? = null
    private var lastSentByte: Int = -1

    val isConnected: Boolean
        get() = socket?.isConnected == true

    @SuppressLint("MissingPermission")
    suspend fun connect(context: Context, nameOrMac: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = manager?.adapter ?: return@withContext Result.failure(Exception("BT not supported"))
            
            if (!adapter.isEnabled) return@withContext Result.failure(Exception("BT is off"))

            val device: BluetoothDevice = adapter.bondedDevices?.firstOrNull { 
                it.name?.equals(nameOrMac, true) == true || it.address.equals(nameOrMac, true)
            } ?: return@withContext Result.failure(Exception("Device not paired"))

            adapter.cancelDiscovery()
            disconnect()

            val newSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            newSocket.connect()
            
            socket = newSocket
            outputStream = BufferedOutputStream(newSocket.outputStream, 128)
            lastSentByte = -1 // Reset on new connection

            Result.success(Unit)
        } catch (e: Exception) {
            disconnect()
            Result.failure(e)
        }
    }

    /**
     * Sends a byte if it differs from the last sent byte (deduplication).
     * Uses BufferedOutputStream for slightly better throughput.
     */
    fun sendByte(value: Int) {
        val out = outputStream ?: return
        if (value == lastSentByte) return // Optimization: Don't flood the HC-05 with identical bytes

        try {
            synchronized(this) {
                out.write(value)
                out.flush()
                lastSentByte = value
            }
        } catch (e: IOException) {
            disconnect()
        }
    }

    fun disconnect() {
        try {
            outputStream?.close()
            socket?.close()
        } catch (_: IOException) { }
        outputStream = null
        socket = null
        lastSentByte = -1
    }
}
