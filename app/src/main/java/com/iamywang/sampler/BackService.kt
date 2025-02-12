package com.iamywang.sampler

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import com.google.gson.Gson
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

class BackService : Service() {

    companion object {
        private const val TAG = "EAVESD-BackSer"
    }

    private external fun sysinfo_procs(): Int
    private external fun statvfs_f_bavail(): Long
    private external fun sysconf_avphys_pages(): Long
    private external fun statvfs_f_ffree(): Long
    private external fun sysinfo_freeram(): Long

    override fun onBind(intent: Intent?): IBinder? = null

    private fun storeJsonObjectsToTimestampFolder(context: Context, jsonObject: Any) {
        val externalFolderPath = "${Environment.getExternalStorageDirectory()}/eaves-nsl-data"
        val externalFolder = File(externalFolderPath).apply { if (!exists()) mkdirs() }

        val fileName = "data-${System.currentTimeMillis()}.json"
        val externalFile = File(externalFolder, fileName)

        try {
            FileWriter(externalFile).use { it.write(Gson().toJson(jsonObject)) }
            Log.d(TAG, "JSON object stored successfully: $externalFile")
        } catch (e: IOException) {
            Log.e(TAG, "Error storing JSON object in external storage: ${e.message}, falling back to internal storage")

            // Fallback to app's local storage
            val internalFile = File(context.filesDir, fileName)
            try {
                FileWriter(internalFile).use { it.write(Gson().toJson(jsonObject)) }
                Log.d(TAG, "JSON object stored successfully in internal storage: $internalFile")
            } catch (internalException: IOException) {
                Log.e(TAG, "Error storing JSON object in internal storage: ${internalException.message}")
            }
        }
        Log.d(TAG, "Ended Data Collection, Filename: $fileName")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            val time = Date().toString()
            val iterations = 10_000 // 10,000 samples
            val vs = Array(5) { StringBuilder() }

            val deviceInfo = mapOf(
                "model" to "${Build.MANUFACTURER} ${Build.BRAND} ${Build.MODEL}",
                "version" to "${Build.VERSION.RELEASE} ${Build.VERSION.SDK_INT}",
                "time" to time
            )

            val startTime = SystemClock.elapsedRealtime() // Start timestamp

            for (i in 0 until iterations) {
                val values = arrayOf(
                    sysinfo_procs(), statvfs_f_bavail(), sysconf_avphys_pages(),
                    statvfs_f_ffree(), sysinfo_freeram()
                )

                values.forEachIndexed { index, value ->
                    vs[index].append(if (i == iterations - 1) "$value" else "$value,")
                }

                // Ensure exactly 1 millisecond per sample
                val elapsed = SystemClock.elapsedRealtime() - startTime
                val expectedTime = i + 1 // Expected time in milliseconds
                if (elapsed < expectedTime) {
                    Thread.sleep(expectedTime - elapsed)
                }
            }

            storeJsonObjectsToTimestampFolder(this@BackService, deviceInfo + vs.mapIndexed { idx, sb -> "vs${idx + 1}" to sb.toString() })
        }.start()

        return START_STICKY
    }

}
