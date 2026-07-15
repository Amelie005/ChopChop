package com.example.chopchoprecipeapp.wifidirect

import android.content.Context
import android.util.Log
import com.example.chopchoprecipeapp.data.Recipe
import com.example.chopchoprecipeapp.data.RecipeTransfer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException

/**
 * Service to manage sending and receiving recipes and handshakes over WiFi Direct.
 * @param context The application context.
 * @author Amelie Dzierzawa
 */
class RecipeTransferService(private val context: Context) {

    /**
     * Constants for the server socket.
     */
    companion object {
        const val PORT = 8888
        const val SERVER_TIMEOUT = 60000
        private const val TAG = "RecipeTransfer"
        private const val HANDSHAKE_MSG = "CHOP_CHOP_HANDSHAKE"
    }

    private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus.Idle)
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()

    private val _transferError = MutableStateFlow<String?>(null)
    val transferError: StateFlow<String?> = _transferError.asStateFlow()

    private val _receivedRecipe = MutableStateFlow<Recipe?>(null)
    val receivedRecipe: StateFlow<Recipe?> = _receivedRecipe.asStateFlow()

    private val _peerIpAddress = MutableStateFlow<String?>(null)
    val peerIpAddress: StateFlow<String?> = _peerIpAddress.asStateFlow()

    private var serverThread: Thread? = null
    private var clientSocket: Socket? = null
    private var serverSocket: ServerSocket? = null
    private var isServerRunning = false

    /**
     * Sets the target peer IP address manually (used by client).
     * @param ip The IP address of the target peer.
     */
    fun setPeerIpAddress(ip: String?) {
        _peerIpAddress.value = ip
        Log.d(TAG, "Peer IP address set to: $ip")
    }

    /**
     * Starts a server socket to listen for incoming recipes and handshakes.
     */
    fun startServer() {
        if (isServerRunning) return

        serverThread = Thread {
            try {
                isServerRunning = true
                serverSocket = ServerSocket(PORT)
                serverSocket?.soTimeout = SERVER_TIMEOUT

                Log.d(TAG, "Server started on port $PORT")
                _transferStatus.value = TransferStatus.Waiting

                while (isServerRunning) {
                    try {
                        val socket = serverSocket?.accept() ?: continue
                        Log.d(TAG, "Incoming connection accepted")

                        Thread {
                            try {
                                socket.soTimeout = 15000
                                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                                val firstLine = reader.readLine()

                                if (!firstLine.isNullOrEmpty()) {
                                    if (firstLine == HANDSHAKE_MSG) {
                                        val clientIp = socket.inetAddress.hostAddress
                                        Log.d(TAG, "Handshake received from client: $clientIp")
                                        _peerIpAddress.value = clientIp
                                    } else {
                                        Log.d(TAG, "Data payload received, processing recipe...")
                                        _transferStatus.value = TransferStatus.Receiving
                                        val recipe = RecipeTransfer.jsonToRecipe(firstLine, context)
                                        if (recipe != null) {
                                            _receivedRecipe.value = recipe
                                            _transferStatus.value = TransferStatus.ReceivedSuccess
                                            Log.d(
                                                TAG,
                                                "Recipe successfully parsed and saved: ${recipe.name}"
                                            )
                                        } else {
                                            _transferStatus.value = TransferStatus.ErrorInvalidData
                                        }
                                    }
                                }
                                reader.close()
                                socket.close()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error handling connection: ${e.message}")
                                if (_transferStatus.value == TransferStatus.Receiving) {
                                    _transferStatus.value = TransferStatus.ReceivedFailed
                                }
                            }
                        }.start()
                    } catch (e: SocketTimeoutException) {
                        //keep server alive, just socket wait timeout
                    }
                }
                serverSocket?.close()
            } catch (e: Exception) {
                Log.e(TAG, "Server run error: ${e.message}")
                _transferStatus.value = TransferStatus.ErrorServerCrashed
                isServerRunning = false
            }
        }
        serverThread?.start()
    }

    /**
     * Stops the running server.
     */
    fun stopServer() {
        isServerRunning = false
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing ServerSocket: ${e.message}")
        }
        serverThread?.interrupt()
        try {
            serverThread?.join(1000)
        } catch (e: Exception) {
            // Ignored
        }
        serverThread = null
        serverSocket = null
    }

    /**
     * Sends a handshake signal to the target host to exchange IP info.
     * @param hostAddress The IP address of the target host.
     */
    fun sendHandshake(hostAddress: String) {
        Thread {
            try {
                Log.d(TAG, "Attempting to send handshake to GO: $hostAddress")
                val socket = Socket(hostAddress, PORT)
                socket.soTimeout = 5000
                val writer = PrintWriter(socket.outputStream, true)
                writer.println(HANDSHAKE_MSG)
                writer.flush()
                writer.close()
                socket.close()
                Log.d(TAG, "Handshake sent successfully to $hostAddress")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send handshake: ${e.message}")
            }
        }.start()
    }

    /**
     * Sends a recipe to the connected peer.
     * @param recipe The recipe to send.
     * @param hostAddress The IP address of the target peer.
     */
    fun sendRecipe(recipe: Recipe, hostAddress: String) {
        val thread = Thread {
            try {
                _transferStatus.value = TransferStatus.Sending
                _transferError.value = null
                Log.d(TAG, "Connecting to $hostAddress:$PORT")

                clientSocket = Socket(hostAddress, PORT)
                clientSocket?.soTimeout = 15000
                Log.d(TAG, "Connected to receiver socket")

                try {
                    val writer = PrintWriter(clientSocket!!.outputStream, true)
                    val jsonData = RecipeTransfer.recipeToJson(context, recipe)

                    Log.d(TAG, "Transmitting data (${jsonData.length} bytes)...")
                    writer.println(jsonData)
                    writer.flush()

                    if (!writer.checkError()) {
                        Thread.sleep(500)
                        _transferStatus.value = TransferStatus.SentSuccess
                        _transferError.value = null
                        Log.d(TAG, "Data transmitted successfully")
                    } else {
                        _transferStatus.value = TransferStatus.SentFailed
                        _transferError.value = "Failed to write data"
                        Log.e(TAG, "Transmission failed via print writer")
                    }
                    writer.close()
                } catch (e: SocketTimeoutException) {
                    _transferStatus.value = TransferStatus.ErrorTimeout
                    _transferError.value = "Connection timeout. Receiver did not respond"
                    Log.e(TAG, "Transmission timeout")
                } catch (e: Exception) {
                    _transferStatus.value = TransferStatus.SentFailed
                    _transferError.value = "Transmission error: ${e.message}"
                    Log.e(TAG, "Transmission error: ${e.message}")
                } finally {
                    clientSocket?.close()
                    clientSocket = null
                }

                Thread.sleep(3000)
                if (_transferStatus.value == TransferStatus.SentSuccess) {
                    _transferStatus.value = TransferStatus.Idle
                }
            } catch (e: Exception) {
                Log.e(TAG, "Critical error during send: ${e.message}")
                _transferStatus.value = TransferStatus.ErrorConnectionLost
                _transferError.value = "Connection lost: ${e.message}"

                Thread.sleep(3000)
                _transferStatus.value = TransferStatus.Idle
            }
        }
        thread.start()
    }

    /**
     * Clears current transfer errors.
     */
    fun clearError() {
        _transferError.value = null
    }

    /**
     * Releases active sockets.
     */
    fun cleanup() {
        stopServer()
        try {
            clientSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup: ${e.message}")
        }
    }
}

/**
 * Enum class representing the current transfer status.
 * @author Amelie Dzierzawa
 */
enum class TransferStatus {
    Idle,
    Waiting,
    Receiving,
    ReceivedSuccess,
    ReceivedFailed,
    Sending,
    SentSuccess,
    SentFailed,

    //specific errors
    ErrorTimeout,
    ErrorConnectionLost,
    ErrorInvalidData,
    ErrorServerCrashed
}