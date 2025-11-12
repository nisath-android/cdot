package org.naminfo.activities.main.chat.fragments

import android.os.Build
import java.io.ByteArrayInputStream
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.linphone.core.tools.Log

object CallLogsManagement {
    suspend fun appendCallLogToFTP(
        server: String, // FTP server address
        username: String, // FTP server details
        password: String, // FTP credentials
        caller: String, // Caller's phone number
        receiver: String, // Receiver's phone number
        callType: String, // "outgoing", "incoming", "missed"
        duration: Int // Call duration in seconds (0 for missed calls)
    ) {
        Log.i(
            "[Main Activity]",
            "==>>appendCallLogToFTP() called with: server = $server, username = $username, password = $password, caller = $caller, receiver = $receiver, callType = $callType, duration = $duration"
        )
        withContext(Dispatchers.IO) {
            val ftpClient = FTPClient()
            try {
                ftpClient.connectTimeout = 30000 // Set connection timeout (30 seconds)
                ftpClient.defaultTimeout = 30000 // Set default timeout (30 seconds)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ftpClient.dataTimeout = Duration.ofSeconds(30)
                } // Set data transfer timeout (30 seconds)

                ftpClient.connect(server)
                ftpClient.login(username, password)
                ftpClient.enterLocalPassiveMode()
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                val timestamp = System.currentTimeMillis()
                val logEntry = "$timestamp,$callType,$duration,$receiver\n"

                // Append call log for caller
                appendToFile(ftpClient, "/CallLogs/$caller.txt", logEntry)

                // If it's a missed call or incoming call, also update receiver's log
                if (callType == "missed" || callType == "incoming") {
                    val receiverLogEntry = "$timestamp,$callType,$duration,$caller\n"
                    appendToFile(ftpClient, "/CallLogs/$receiver.txt", receiverLogEntry)
                }

                Log.i(
                    "[Main Activity]",
                    "==>>Call log updated successfully for $caller and $receiver"
                )
            } catch (e: Exception) {
                Log.e("[Main Activity]", "==>>Error updating call log: ${e.message}")
                // e.printStackTrace()
            } finally {
                Log.e("[Main Activity]", "==>> finally")
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            }
        }
    }

    // Function to append directly to FTP file without downloading
    private fun appendToFile(ftpClient: FTPClient, filePath: String, data: String) {
        val inputStream = ByteArrayInputStream(data.toByteArray())
        ftpClient.appendFile(filePath, inputStream)
        inputStream.close()
    }

    suspend fun cleanOldLogsOnFTP(
        server: String,
        username: String,
        password: String,
        userPhone: String
    ) {
        withContext(Dispatchers.IO) {
            val ftpClient = FTPClient()
            try {
                ftpClient.connect(server)
                ftpClient.login(username, password)
                ftpClient.enterLocalPassiveMode()
                // ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                val filePath = "/CallLogs/$userPhone.txt"
                val inputStream = ftpClient.retrieveFileStream(filePath) ?: return@withContext
                val lines = inputStream.bufferedReader().readLines()
                inputStream.close()

                val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000) // 30 days in milliseconds

                val newLogs = lines.filter { it.isNotEmpty() && it.split(",")[0].toLong() > cutoffTime }
                    .joinToString("\n")

                // Re-upload filtered logs
                val outputStream = ByteArrayInputStream(newLogs.toByteArray())
                ftpClient.storeFile(filePath, outputStream)
                outputStream.close()

                println("Old logs removed for $userPhone")
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ftpClient.logout()
                ftpClient.disconnect()
            }
        }
    }

    suspend fun fetchCallLogsFromFTP(
        server: String,
        username: String,
        password: String,
        userPhone: String
    ): List<CallDetails> {
        return withContext(Dispatchers.IO) {
            val ftpClient = FTPClient()
            val callList = mutableListOf<CallDetails>()

            try {
                ftpClient.connect(server)
                ftpClient.login(username, password)
                ftpClient.enterLocalPassiveMode()
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

                val filePath = "/CallLogs/$userPhone.txt"
                val inputStream = ftpClient.retrieveFileStream(filePath) ?: return@withContext callList

                val reader = inputStream.bufferedReader()
                reader.forEachLine { line ->
                    val data = line.split(",")
                    if (data.size == 4) {
                        val timestamp = data[0].toLong()
                        val callType = data[1]
                        val duration = data[2].toInt()
                        val peerNumber = data[3]
                        callList.add(CallDetails(peerNumber, callType, timestamp, duration))
                    }
                }
                reader.close()
                inputStream.close()
                ftpClient.completePendingCommand()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                ftpClient.logout()
                ftpClient.disconnect()
            }

            return@withContext callList
        }
    }
    data class CallDetails(
        val peerNumber: String, // The other user's phone number
        val callType: String, // "incoming", "outgoing", "missed"
        val timestamp: Long, // Time of call in milliseconds
        val duration: Int // Call duration in seconds (0 for missed calls)
    )
}
