package com.tzuhsien.pinpisode.spotifynote

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.tzuhsien.pinpisode.MainActivity
import com.tzuhsien.pinpisode.R

class NotificationBuilder {

    fun build(context: Context, isClipping: Boolean, channelId: String, packageName: String): Notification {
        val notificationView = RemoteViews(packageName, R.layout.notification_layout)
        notificationView.setOnClickPendingIntent(R.id.btn_take_timestamp, timestampPendingIntent(context))
        notificationView.setOnClickPendingIntent(R.id.btn_clip, startClippingPendingIntent(context))
        notificationView.setOnClickPendingIntent(R.id.btn_clipping, endClippingPendingIntent(context))
        notificationView.setOnClickPendingIntent(R.id.blank_view, noteFragmentPendingIntent(context))

        if (isClipping) {
            notificationView.setViewVisibility(R.id.btn_clip, View.GONE)
            notificationView.setViewVisibility(R.id.btn_clipping, View.VISIBLE)
        } else {
            notificationView.setViewVisibility(R.id.btn_clip, View.VISIBLE)
            notificationView.setViewVisibility(R.id.btn_clipping, View.GONE)
        }

        val bmp = BitmapFactory.decodeResource(context.resources, R.mipmap.launcher)

        return NotificationCompat.Builder(context, channelId)
            .setAutoCancel(false)
            .setOngoing(true)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationView)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_NONE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentTitle(context.getString(R.string.app_name))
            .setColor(context.getColor(R.color.beige))
            .setSmallIcon(R.drawable.pinpisode_clipart)
//            .setLargeIcon(bmp)
            .build()
    }

    private fun timestampPendingIntent(context: Context): PendingIntent {
        val timestampIntent = Intent().apply {
            action = TimestampReceiver.ACTION_TAKE_TIMESTAMP
        }
        return PendingIntent.getBroadcast(context, 0, timestampIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun startClippingPendingIntent(context: Context): PendingIntent {
        val clipIntent = Intent().apply {
            action = TimestampReceiver.ACTION_CLIP_START
        }
        return PendingIntent.getBroadcast(context, 0, clipIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun endClippingPendingIntent(context: Context): PendingIntent {
        val clipEndIntent = Intent().apply {
            action = TimestampReceiver.ACTION_CLIP_END
        }
        return PendingIntent.getBroadcast(context, 0, clipEndIntent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun noteFragmentPendingIntent(context: Context): PendingIntent {
        val fragmentIntent = Intent(context, MainActivity::class.java)
        return PendingIntent.getActivity(
            context,
            0,
            fragmentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    }
}