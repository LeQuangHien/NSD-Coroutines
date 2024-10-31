package com.hien.le.nsd_coroutines.helper

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class NsdHelper(context: Context) {

    companion object {
        private val TAG = NsdHelper::class.java.simpleName
    }

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var discoveryListener: NsdManager.DiscoveryListener? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val serverChannel = Channel<NsdServiceInfo>(Channel.BUFFERED)

    // Lazy-initialized MulticastLock
    private val multicastLock: WifiManager.MulticastLock by lazy {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiManager.createMulticastLock("NsdMulticastLock").apply { setReferenceCounted(true) }
    }

    // Discover services and emit each server to the Channel
    suspend fun discoverServices(
        serviceType: String,
        serviceName: String
    ): ReceiveChannel<NsdServiceInfo> {
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Service discovery started for type: $serviceType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                Log.d(TAG, "Service found: $service")

                if (service.serviceType == serviceType && service.serviceName.contains(serviceName)) {
                    coroutineScope.launch {
                        val resolvedService = resolveService(service)
                        serverChannel.send(resolvedService)
                    }
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                Log.d(TAG, "Service lost: $service")
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Discovery stopped: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Discovery failed: Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
                serverChannel.close(Exception("Start discovery failed with error code: $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop Discovery failed: Error code: $errorCode")
                nsdManager.stopServiceDiscovery(this)
                serverChannel.close(Exception("Stop discovery failed with error code: $errorCode"))
            }
        }

        // Acquire MulticastLock
        multicastLock.acquire()

        nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        return serverChannel
    }

    // Helper suspend function to resolve a service
    private suspend fun resolveService(serviceInfo: NsdServiceInfo): NsdServiceInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            resolveModernService(serviceInfo)
        } else {
            resolveLegacyService(serviceInfo)
        }
    }

    // Use registerServiceInfoCallback for Android 14 and above (API 34+)
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun resolveModernService(serviceInfo: NsdServiceInfo): NsdServiceInfo =
        suspendCoroutine { continuation ->
            val executor = Dispatchers.IO.asExecutor()

            val callback = object : NsdManager.ServiceInfoCallback {
                override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {
                    Log.e(TAG, "Service callback registration failed: Error code $errorCode")
                    continuation.resumeWithException(Exception("Service callback registration failed with error code: $errorCode"))
                }

                override fun onServiceUpdated(updatedServiceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Service updated: $updatedServiceInfo")
                    continuation.resume(updatedServiceInfo)
                }

                override fun onServiceLost() {
                    Log.e(TAG, "Service lost")
                    continuation.resumeWithException(Exception("Service was lost"))
                }

                override fun onServiceInfoCallbackUnregistered() {
                    Log.d(TAG, "Service info callback unregistered")
                }
            }

            try {
                nsdManager.registerServiceInfoCallback(serviceInfo, executor, callback)
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }

    private suspend fun resolveLegacyService(serviceInfo: NsdServiceInfo): NsdServiceInfo =
        suspendCoroutine { continuation ->
            val resolveListener = object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Resolve failed: Error code: $errorCode")
                    continuation.resumeWithException(Exception("Resolve failed with error code: $errorCode"))
                }

                override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Service resolved: $resolvedServiceInfo")
                    val host = resolvedServiceInfo.host
                    val port = resolvedServiceInfo.port
                    Log.d(TAG, "Host Address: $host, Port: $port")
                    continuation.resume(resolvedServiceInfo)
                }
            }

            nsdManager.resolveService(serviceInfo, resolveListener)
        }

    // Stop discovery
    fun stopDiscovery() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
            discoveryListener = null
        }
        if (multicastLock.isHeld) {
            multicastLock.release()
        }
        serverChannel.close()
    }
}
