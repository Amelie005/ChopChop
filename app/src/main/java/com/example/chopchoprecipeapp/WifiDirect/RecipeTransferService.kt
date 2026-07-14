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

/**
 * Handles sending and receiving recipes over WiFi Direct.
 * @author Amelie Dzierzawa
 */
class RecipeTransferService(private val context: Context) {

    companion object {
        const val PORT = 8888
        private const val TAG = "RecipeTransfer"
    }

    private val _transferStatus = MutableStateFlow<TransferStatus>(TransferStatus.Idle)
    val transferStatus: StateFlow<TransferStatus> = _transferStatus.asStateFlow()

    private val _receivedRecipe = MutableStateFlow<Recipe?>(null)
    val receivedRecipe: StateFlow<Recipe?> = _receivedRecipe.asStateFlow()

    private var serverThread: Thread? = null
    private var clientSocket: Socket? = null
    private var isServerRunning = false

    /**
     * Starts a server socket to listen for incoming recipes.
     */
    fun startServer() {
        if (isServerRunning) return

        serverThread = Thread {
            try {
                isServerRunning = true
                val serverSocket = ServerSocket(PORT)
                Log.d(TAG, "Server started on port $PORT")
                _transferStatus.value = TransferStatus.Waiting

                while (isServerRunning) {
                    try {
                        val socket = serverSocket.accept()
                        Log.d(TAG, "Client connected")
                        _transferStatus.value = TransferStatus.Receiving

                        val reader = BufferedReader(InputStreamReader(socket.inputStream))
                        val jsonData = reader.readLine()

                        if (jsonData != null) {
                            Log.d(TAG, "Received data: ${jsonData.take(100)}...")
                            val recipe = RecipeTransfer.jsonToRecipe(jsonData, context)
                            _receivedRecipe.value = recipe
                            _transferStatus.value = TransferStatus.ReceivedSuccess
                            Log.d(TAG, "Recipe received: ${recipe?.name}")
                        } else {
                            _transferStatus.value = TransferStatus.ReceivedFailed
                        }

                        reader.close()
                        socket.close()
                    } catch (e: Exception) {
                        if (isServerRunning) {
                            Log.e(TAG, "Error accepting client: ${e.message}")
                        }
                    }
                }
                serverSocket.close()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Server error: ${e.message}")
                _transferStatus.value = TransferStatus.ReceivedFailed
                isServerRunning = false
            }
        }
        serverThread?.start()
    }

    /**
     * Stops the server.
     */
    fun stopServer() {
        isServerRunning = false
        serverThread?.interrupt()
        try {
            serverThread?.join(1000)
        } catch (e: Exception) {
            //ignore
        }
        serverThread = null
    }

    /**
     * Sends a recipe to a connected peer.
     */
    fun sendRecipe(recipe: Recipe, hostAddress: String) {
        val thread = Thread {
            try {
                _transferStatus.value = TransferStatus.Sending
                Log.d(TAG, "Connecting to $hostAddress:$PORT")

                clientSocket = Socket(hostAddress, PORT)
                Log.d(TAG, "Connected to peer")

                val writer = PrintWriter(clientSocket!!.outputStream, true)
                val jsonData = RecipeTransfer.recipeToJson(recipe)

                Log.d(TAG, "Sending recipe: ${jsonData.take(100)}...")
                writer.println(jsonData)
                writer.flush()

                Thread.sleep(500)

                _transferStatus.value = TransferStatus.SentSuccess
                Log.d(TAG, "Recipe sent successfully")

                writer.close()
                clientSocket?.close()
                clientSocket = null

                Thread.sleep(2000)
                _transferStatus.value = TransferStatus.Idle
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e(TAG, "Send error: ${e.message}")
                _transferStatus.value = TransferStatus.SentFailed

                Thread.sleep(2000)
                _transferStatus.value = TransferStatus.Idle
            }
        }
        thread.start()
    }

    /**
     * Stops sending.
     */
    fun stopSending() {
        try {
            clientSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Cleanup resources.
     */
    fun cleanup() {
        stopServer()
        stopSending()
    }
}

/**
 * Enum for transfer status.
 */
enum class TransferStatus {
    Idle,
    Waiting,
    Receiving,
    ReceivedSuccess,
    ReceivedFailed,
    Sending,
    SentSuccess,
    SentFailed
}