package com.hien.le.nsd_coroutines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hien.le.nsd_coroutines.helper.NsdHelper
import com.hien.le.nsd_coroutines.ui.theme.NSDCoroutinesTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: DiscoveryViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = DiscoveryViewModel(NsdHelper(this))

        enableEdgeToEdge()
        setContent {
            NSDCoroutinesTheme {
                DiscoveryScreen(viewModel)
            }
        }
    }

    override fun onPause() {
        viewModel.stopDiscovery()
        super.onPause()
    }
}

@Composable
fun DiscoveryScreen(viewModel: DiscoveryViewModel) {
    val discoveryState by viewModel.discoveryState.collectAsState()
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(discoveryState) {
        when (discoveryState) {
            is DiscoveryState.Loading -> isLoading = true
            is DiscoveryState.Success, is DiscoveryState.Error, is DiscoveryState.Stop -> isLoading = false
            is DiscoveryState.Idle -> {}
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    viewModel.discoverService("_http._tcp.", "Secure")
                }
            ) {
                Text("Start Discover")
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                CircularProgressIndicator()
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = when (discoveryState) {
                    is DiscoveryState.Success -> "Found server: ${(discoveryState as DiscoveryState.Success).serverInfo}"
                    is DiscoveryState.Error -> "Error: ${(discoveryState as DiscoveryState.Error).error}"
                    else -> ""
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DiscoveryScreenPreview() {
    NSDCoroutinesTheme {
        DiscoveryScreen(DiscoveryViewModel(NsdHelper(LocalContext.current)))
    }
}
