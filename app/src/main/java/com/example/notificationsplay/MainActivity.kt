package com.example.notificationsplay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notificationsplay.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val notificationManager by lazy {
        getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding?.root?.also(::setContentView)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        createChannel()

        binding?.buttonNormalNotification?.setOnClickListener { btn ->
            notificationManager.notify(
                1,
                getNotification()
            )
        }

        binding?.buttonFisNotification?.setOnClickListener { btn ->
            notificationManager.notify(
                2,
                getFullScreenIntent()
            )
        }

        binding?.buttonService?.setOnClickListener { btn ->
            ContextCompat.startForegroundService(this, Intent(this, TestForegroundService::class.java))
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).also(notificationManager::createNotificationChannel)

            NotificationChannel(
                FSI_NOTIFICATION_CHANNEL_ID,
                FSI_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH,
            ).also(notificationManager::createNotificationChannel)
        }
    }

    private val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun getNotification() =
        NotificationCompat.Builder(baseContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Test Notification Title")
            .setContentText("Test Notification Text")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(getActivityIntent())
            .build()

    fun getFullScreenIntent() =
        NotificationCompat.Builder(baseContext, FSI_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("FSI Test Notification Title")
            .setContentText("FSI Test Notification Text")
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setFullScreenIntent(getActivityIntent(), true)
            .build()

    private fun getActivityIntent() = PendingIntent.getActivity(
        this,
        1,
        Intent(this, MainActivity::class.java),
        flags,
    )

    companion object {
        val NOTIFICATION_CHANNEL_ID = "Test Notification Channel ID"
        val NOTIFICATION_CHANNEL_NAME = "Test Notification Channel Name"

        val FSI_NOTIFICATION_CHANNEL_ID = "FIS Test Notification Channel ID"
        val FSI_NOTIFICATION_CHANNEL_NAME = "FIS Test Notification Channel Name"
    }

    class TestForegroundService : Service() {

        override fun onCreate() {
            super.onCreate()
            createChannel()
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            // Foreground service with FSI
            val fullScreenIntent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, FSI_NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Foreground Service")
                .setContentText("This is a foreground service with FSI.")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(pendingIntent, true)
                .build()

            startForeground(1001, notification)

            GlobalScope.launch(Dispatchers.Main) {
                delay(10000L)
                val nm = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                nm.notify(3, notification)
            }

            return START_NOT_STICKY
        }

        override fun onBind(intent: Intent?): IBinder? = null

        private fun createChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    FSI_NOTIFICATION_CHANNEL_ID,
                    FSI_NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Test Channel for foreground service with FSI"
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val nm = (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                nm.createNotificationChannel(channel)
            }
        }

        override fun onDestroy() {
            super.onDestroy()
            Log.d("samtest", "onDestroy, TestForegroundService:")
        }
    }
}