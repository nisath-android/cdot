package com.naminfo.cdot_vc.utils

import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.linphone.core.tools.Log

class CdrUtils {
    fun sendCdrUsingGetRequest(cdrDetails: ArrayList<Array<String>>) {
        // Base URL of your web service
        val baseUrl = "http://192.168.1.31/fs_webservice/WebService.asmx/Insert_Call_Details_FS"

        // Build query string from cdrDetails
        val queryString = StringBuilder()
        for (param in cdrDetails) {
            if (queryString.isNotEmpty()) {
                queryString.append('&')
            }
            queryString.append(URLEncoder.encode(param[0], "UTF-8"))
            queryString.append('=')
            queryString.append(URLEncoder.encode(param[1], "UTF-8"))
        }

        // Full URL with query parameters
        val fullUrl = "$baseUrl?$queryString"

        // Create a request using OkHttp
        val client = OkHttpClient()
        val request = Request.Builder()
            .url(fullUrl)
            .build()

        // Execute the request asynchronously
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("CDR Upload Failure", "Failed to send CDR: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: Response) {
                if (response.isSuccessful) {
                    Log.i("CDR Upload", "CDR sent successfully: ${response.body?.string()}")
                } else {
                    Log.e(
                        "CDR Upload Error",
                        "Failed to send CDR: ${response.code} - ${response.message}"
                    )
                }
            }
        })
    }

    fun convertUnixTimestampToFormattedDate(unixTimestamp: Long): String {
        // Create a Date object from the Unix timestamp (in milliseconds)
        val date = Date(unixTimestamp * 1000)

        // Define the desired date format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        // Set the timezone to IST
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))

        // Format the Date object to the desired string format
        return dateFormat.format(date)
    }

    fun getCurrentFormattedDate(): String {
        // Get the current time in milliseconds
        val currentTimeMillis = System.currentTimeMillis()

        // Create a Date object from the current time in milliseconds
        val date = Date(currentTimeMillis)

        // Define the desired date format
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")

        // Set the timezone to IST
        dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"))

        // Format the Date object to the desired string format
        return dateFormat.format(date)
    }
}
