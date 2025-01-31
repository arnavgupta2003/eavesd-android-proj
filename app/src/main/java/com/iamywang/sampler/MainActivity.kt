package com.iamywang.sampler

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONObject
import com.iamywang.sampler.databinding.ActivityMainBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var globalList = LinkedList<ListItem>()
    private val model = Build.MODEL
    private val manufacturer = Build.MANUFACTURER
    private val brand = Build.BRAND
    private val version = Build.VERSION.RELEASE
    private val sdk = Build.VERSION.SDK_INT

    // Launchers for permissions
//    private val requestWritePermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission granted", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }

//    private val requestReadPermissionLauncher =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                Toast.makeText(this, "READ_EXTERNAL_STORAGE permission granted", Toast.LENGTH_SHORT).show()
//            } else {
//                Toast.makeText(this, "READ_EXTERNAL_STORAGE permission denied", Toast.LENGTH_SHORT).show()
//            }
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        globalList.add(ListItem(1, "$manufacturer $brand $model", Date().toString()))
        globalList.add(ListItem(2, "Android $version (SDK $sdk)", Date().toString()))
        setList(globalList, binding.mainList)
        Log.d("EAVESD", "Starting Data Collection")
        val service = Intent(baseContext, BackService::class.java)
        startService(service)
    }

//    private fun requestPermissionsIndividually() {
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
//            requestWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        } else {
//            Toast.makeText(this, "WRITE_EXTERNAL_STORAGE permission already granted", Toast.LENGTH_SHORT).show()
//        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PERMISSION_GRANTED) {
//            requestReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
//        } else {
//            Toast.makeText(this, "READ_EXTERNAL_STORAGE permission already granted", Toast.LENGTH_SHORT).show()
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
//            requestManageStoragePermission()
//        }
//    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:" + this.packageName)
            startActivity(intent)
        } else {
            Toast.makeText(this, "MANAGE_EXTERNAL_STORAGE permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    fun trainNetwork(view: View) {
        val api = "http://100.84.210.82:8000"
        val obj = JSONObject()
        obj["model"] = "$manufacturer $brand $model"
        obj["version"] = "$version $sdk"

        val okHttpClient = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = JSON.toJSONString(obj).toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$api/train/")
            .post(requestBody)
            .build()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("failed")
            }

            override fun onResponse(call: Call, response: Response) {
                println("success")
            }
        })
    }

    private fun setList(list: LinkedList<ListItem>, listView: ListView) {
        val mAdapter = ListItemAdapter(list, this)
        listView.adapter = mAdapter
    }

    companion object {
        init {
            System.loadLibrary("sampler")
        }
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
