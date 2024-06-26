package com.example.routemapper.network

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WebClient {

    var ip: String = "192.168.1.49"
    var port: String = "8080";
    fun registerUser(lat: Double, long: Double, callback: (Int?) -> Unit) {
        val client = OkHttpClient()
        val jsonBody = """
            {
                "lat": $lat,
                "long": $long
            }
        """.trimIndent()
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://$ip:$port/register")

            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Problem fetching data: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.e("WebClient", "Got userId: $responseBody");

                    callback(responseBody.toInt())
                }
            }
        })
    }

    fun postStep(userId: Int, heading: Double, callback: (String?) -> Unit) {
        /***
         * Send heading to the server
         */
        val client = OkHttpClient()
        val jsonBody = """
            {
                "userId": $userId,
                "heading": $heading
            }
        """.trimIndent()
        val requestBody = jsonBody.toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder()
            .url("http://$ip:$port/" + "steps")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Problem fetching data: ${e.message}")
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                if (responseBody != null) {
                    Log.e("WebClient", "Got response for sent step: $responseBody");

                    callback(responseBody)
                }
            }
        })
    }

}