package com.hien.le.nsd_coroutines

import android.net.nsd.NsdServiceInfo
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hien.le.nsd_coroutines.helper.NsdHelper
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

sealed class DiscoveryState {
    data object Idle : DiscoveryState()
    data object Loading : DiscoveryState()
    data object Stop: DiscoveryState()
    data class Success(val serverInfo: NsdServiceInfo) : DiscoveryState()
    data class Error(val error: String) : DiscoveryState()
}

class DiscoveryViewModel(private val nsdHelper: NsdHelper) : ViewModel() {

    companion object {
        private val TAG = DiscoveryViewModel::class.java.simpleName
    }

    private val _discoveryState = MutableStateFlow<DiscoveryState>(DiscoveryState.Idle)
    val discoveryState: StateFlow<DiscoveryState> = _discoveryState.asStateFlow()

    fun discoverService(serviceType: String, serviceName: String) {
        _discoveryState.update { DiscoveryState.Loading }
        viewModelScope.launch {
            try {
                val channel: ReceiveChannel<NsdServiceInfo> =
                    nsdHelper.discoverServices(serviceType, serviceName)
                for (serviceInfo in channel) {
                    _discoveryState.update { DiscoveryState.Success(serviceInfo) }
                    // If only one server is needed, stop discovery and break
                    // stopDiscovery()
                    // break
                }
            } catch (e: IOException) {
                _discoveryState.update { DiscoveryState.Error(e.message ?: "Unknown error") }
                Log.e(TAG, "Failed to discover or resolve service: ${e.message}")
            }
        }
    }

    fun stopDiscovery() {
        viewModelScope.launch {
            nsdHelper.stopDiscovery()
            _discoveryState.update { DiscoveryState.Stop }
        }
    }
}

