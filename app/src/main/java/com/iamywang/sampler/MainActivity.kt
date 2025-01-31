package com.iamywang.sampler

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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

    private val requestManageStoragePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                Toast.makeText(this, "Storage permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestStoragePermissions()

        globalList.add(ListItem(1, "$manufacturer $brand $model", Date().toString()))
        globalList.add(ListItem(2, "Android $version (SDK $sdk)", Date().toString()))
        setList(globalList, binding.mainList)

        Log.d("EAVESD", "Starting Data Collection")
        startService(Intent(this, BackService::class.java))
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    .setData(Uri.parse("package:$packageName"))
                requestManageStoragePermissionLauncher.launch(intent)
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val deniedPermissions = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (deniedPermissions.isNotEmpty()) {
                requestPermissions(deniedPermissions.toTypedArray(), PERMISSION_REQUEST_CODE)
            }
        }
    }


    private fun setList(list: LinkedList<ListItem>, listView: ListView) {
        listView.adapter = ListItemAdapter(list, this)
    }

    companion object {
        init {
            System.loadLibrary("sampler")
        }
        private const val PERMISSION_REQUEST_CODE = 1001
    }
}
