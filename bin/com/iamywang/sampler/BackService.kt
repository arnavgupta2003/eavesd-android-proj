// ============================================================================
// This file is part of EavesDroid.
//
// Author: iamywang
// Date Created: Jan 27, 2024
// ============================================================================
package com.iamywang.sampler

import android.app.IntentService
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import java.util.*

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import android.Manifest
import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import java.io.File
import java.io.FileWriter
import java.io.IOException 

class BackService : Service() {

    private external fun sysinfo_procs(): Int
    private external fun statvfs_f_bavail(): Long
    private external fun sysconf_avphys_pages(): Long
    private external fun statvfs_f_ffree(): Long
    private external fun sysinfo_freeram(): Long
    private external fun sysinfo_sharedram(): Long
    private external fun get_avphys_pages(): Long


    override fun onBind(arg0: Intent): IBinder? {
        return null
    }
    
    private fun checkStoragePermissions(context: Context): Boolean {
        val hasPermissions = context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (!hasPermissions) {
            // Request permissions if not granted
            context.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
        return hasPermissions
    }

    fun storeJsonObjectsToTimestampFolder(context: Context, jsonObject: Any) {
    // Request storage permissions if not granted
        if (checkStoragePermissions(context)) {
            // Create a timestamped folder name
            val timestamp = Date().toString()+System.currentTimeMillis().toString()
            val folderName = "eaves-nsl-data"

            // Create the folder in the external storage
            val folderPath = Environment.getExternalStorageDirectory().toString() + "/$folderName"
            val folder = File(folderPath)
            if (!folder.exists()) {
                folder.mkdirs()
            }

            // Convert the JSON object to a string
            val gson = Gson()
            val jsonString = gson.toJson(jsonObject)

            // Create a file within the folder and write the JSON string to it
            val fileName = "data$timestamp.json"
            val filePath = "$folderPath/$fileName"
            val file = File(filePath)
            try {
                FileWriter(file).use { writer ->
                    writer.write(jsonString)
                }
                Log.d("EAVESD", "JSON object stored successfully: $filePath")
            } catch (e: IOException) {
                Log.e("EAVESD", "Error storing JSON object: ${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        // new Thread
        val thread = Thread {
            val time = Date().toString()
            val total = 10000L
            val init = System.currentTimeMillis()
            var i = 0L

            var vs1 = ""
            var vs2 = ""
            var vs3 = ""
            var vs4 = ""
            var vs5 = ""
            // var vs6 = ""
            // var vs7 = ""

            val model = Build.MODEL
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            val version = Build.VERSION.RELEASE
            val sdk = Build.VERSION.SDK_INT

            while (true) {
                val t = System.currentTimeMillis() - init
                if (t > i) {
                    val v1 = sysinfo_procs()
                    val v2 = statvfs_f_bavail()
                    val v3 = sysconf_avphys_pages()
                    val v4 = statvfs_f_ffree()
                    val v5 = sysinfo_freeram()
                    // val v6 = sysinfo_sharedram()
                    // val v7 = get_avphys_pages()

                    vs1 += "$v1,"
                    vs2 += "$v2,"
                    vs3 += "$v3,"
                    vs4 += "$v4,"
                    vs5 += "$v5,"
                    // vs6 += "$v6,"
                    // vs7 += "$v7,"
                    i++

                    if (t > total - 1) {
                        vs1 += "$v1"
                        vs2 += "$v2"
                        vs3 += "$v3"
                        vs4 += "$v4"
                        vs5 += "$v5"
                        // vs6 += "$v6"
                        // vs7 += "$v7"

                        // val api = "http://100.84.210.82:8000"
                        val obj = JSONObject()
                        obj["model"] = "$manufacturer $brand $model"
                        obj["version"] = "$version $sdk"
                        obj["time"] = time
                        obj["vs1"] = vs1
                        obj["vs2"] = vs2
                        obj["vs3"] = vs3
                        obj["vs4"] = vs4
                        obj["vs5"] = vs5
                        // obj["vs6"] = vs6
                        // obj["vs7"] = vs7
                        storeJsonObjectsToTimestampFolder(this@BackService,obj)
                        Log.d("EAVESD","Data Sent")

                        //Temp Commented: To test local storage

                        // val okHttpClient = OkHttpClient()
                        // val mediaType = "application/json; charset=utf-8".toMediaType()
                        // val requestBody = JSON.toJSONString(obj).toRequestBody(mediaType)
                        // val request = Request.Builder()
                        //     .url("$api/upload/")
                        //     .post(requestBody)
                        //     .build()
                        // okHttpClient.newCall(request).enqueue(object : Callback {
                        //     override fun onFailure(call: Call, e: IOException) {
                        //         println("failed")
                        //     }

                        //     override fun onResponse(call: Call, response: Response) {
                        //         println("success")
                        //     }
                        // })
                        break
                    }
                }
            }
        }

        // start thread
        thread.start()

        return START_STICKY
    }
}
