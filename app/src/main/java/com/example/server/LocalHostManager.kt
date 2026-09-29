package com.example.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.text.format.Formatter
import com.example.data.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

object LocalHostManager {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _requestCount = MutableStateFlow(0)
    val requestCount: StateFlow<Int> = _requestCount.asStateFlow()

    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    // Feature 2: Passcode protection state
    private val _isPasscodeEnabled = MutableStateFlow(false)
    val isPasscodeEnabled: StateFlow<Boolean> = _isPasscodeEnabled.asStateFlow()

    private val _passcode = MutableStateFlow("1234")
    val passcode: StateFlow<String> = _passcode.asStateFlow()

    // Feature 3: Live Server Logs & Request Inspector buffer (keep last 50 requests)
    private val _serverLogs = MutableStateFlow<List<ServerLogEntry>>(emptyList())
    val serverLogs: StateFlow<List<ServerLogEntry>> = _serverLogs.asStateFlow()

    // Feature 5: IP Access Restriction (Whitelist)
    private val _isIpWhitelistEnabled = MutableStateFlow(false)
    val isIpWhitelistEnabled: StateFlow<Boolean> = _isIpWhitelistEnabled.asStateFlow()

    private val _allowedIps = MutableStateFlow<Set<String>>(emptySet())
    val allowedIps: StateFlow<Set<String>> = _allowedIps.asStateFlow()

    private var serverInstance: LocalHostServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun setIpWhitelistEnabled(enabled: Boolean) {
        _isIpWhitelistEnabled.value = enabled
    }

    fun addAllowedIp(ip: String) {
        val clean = ip.trim()
        if (clean.isNotBlank()) {
            _allowedIps.value = _allowedIps.value + clean
        }
    }

    fun removeAllowedIp(ip: String) {
        _allowedIps.value = _allowedIps.value - ip
    }

    fun setAllowedIps(ips: Collection<String>) {
        _allowedIps.value = ips.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    fun isIpAllowed(clientIp: String): Boolean {
        if (!_isIpWhitelistEnabled.value) return true
        if (clientIp == "127.0.0.1" || clientIp == "::1" || clientIp == "localhost") return true
        val allowed = _allowedIps.value
        if (allowed.isEmpty()) return false

        return allowed.any { rule ->
            if (rule.endsWith("*")) {
                val prefix = rule.removeSuffix("*")
                clientIp.startsWith(prefix)
            } else {
                rule.equals(clientIp, ignoreCase = true)
            }
        }
    }

    fun setPasscodeProtection(enabled: Boolean, code: String = _passcode.value) {
        _isPasscodeEnabled.value = enabled
        if (code.isNotBlank()) {
            _passcode.value = code.trim()
        }
    }

    fun logRequest(entry: ServerLogEntry) {
        val current = _serverLogs.value.toMutableList()
        current.add(0, entry)
        if (current.size > 50) {
            _serverLogs.value = current.take(50)
        } else {
            _serverLogs.value = current
        }
    }

    fun clearLogs() {
        _serverLogs.value = emptyList()
    }

    fun startServer(context: Context, repository: AppRepository, port: Int = 8080): Boolean {
        if (_isRunning.value) {
            return true
        }
        try {
            _serverPort.value = port
            val ip = detectLocalIpAddress(context)
            _localIp.value = ip
            _requestCount.value = 0
            _lastErrorMessage.value = null

            val server = LocalHostServer(
                port = port,
                deviceIp = ip,
                repository = repository,
                onRequestHandled = {
                    _requestCount.value += 1
                },
                onError = { err ->
                    _lastErrorMessage.value = err
                }
            )
            server.start()
            serverInstance = server
            _isRunning.value = true
            _startTime.value = System.currentTimeMillis()

            // Also launch Foreground Service to keep server alive in background
            LocalHostService.start(context, port, ip)

            return true
        } catch (e: Exception) {
            _lastErrorMessage.value = "Failed to start server: ${e.message}"
            _isRunning.value = false
            return false
        }
    }

    fun stopServer(context: Context) {
        try {
            serverInstance?.stop()
            serverInstance = null
        } catch (_: Exception) {}
        _isRunning.value = false
        _startTime.value = 0L

        LocalHostService.stop(context)
    }

    fun setPort(port: Int, context: Context, repository: AppRepository) {
        if (port in 1024..65535) {
            _serverPort.value = port
            if (_isRunning.value) {
                stopServer(context)
                startServer(context, repository, port)
            }
        }
    }

    fun refreshIp(context: Context) {
        _localIp.value = detectLocalIpAddress(context)
    }

    fun getWebUrl(): String {
        return "http://${_localIp.value}:${_serverPort.value}"
    }

    fun getOpenAiBaseUrl(): String {
        return "http://${_localIp.value}:${_serverPort.value}/v1"
    }

    private fun detectLocalIpAddress(context: Context): String {
        // Method 1: Check Wi-Fi manager
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null) {
                @Suppress("DEPRECATION")
                val ipInt = wifiManager.connectionInfo.ipAddress
                if (ipInt != 0) {
                    @Suppress("DEPRECATION")
                    val ip = Formatter.formatIpAddress(ipInt)
                    if (!ip.isNullOrBlank() && ip != "0.0.0.0") {
                        return ip
                    }
                }
            }
        } catch (_: Exception) {}

        // Method 2: Enumerate network interfaces (wlan0, eth0, etc.)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val host = addr.hostAddress
                        if (host != null && host.startsWith("192.") || host.startsWith("10.") || host.startsWith("172.")) {
                            return host
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return "127.0.0.1"
    }
}
