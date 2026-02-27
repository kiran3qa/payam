package com.example.pay_speaker

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.Spanned
import android.text.TextUtils
import java.util.Locale
import java.util.regex.Pattern

class PayNotificationListener : NotificationListenerService() {

    private val allowedPackages = setOf(
        "com.google.android.apps.nbu.paisa.user",
        "com.paytm.business"
    )

    private val ignoreKeywords = listOf(
        "otp", "one time password", "one-time password", "pin", "mpin", "password", "verification"
    )

    private val triggerKeywords = listOf(
        "received", "credited", "paid", "payment", "success"
    )

    private val amountPattern = Pattern.compile(
        "(?:₹|Rs\\.?|INR|rupees?)\\s*([0-9]+(?:[\\,0-9]*(?:\\.[0-9]+)?)?)",
        Pattern.CASE_INSENSITIVE
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg !in allowedPackages) return

        val notification = sbn.notification ?: return
        val text = extractNotificationText(notification)
        if (text.isBlank()) return

        val lower = text.lowercase(Locale.getDefault())

        if (ignoreKeywords.any { lower.contains(it) }) return
        if (!triggerKeywords.any { lower.contains(it) }) return

        val amount = extractAmount(text)
        val message = if (amount != null) {
            "Payment received. Rupees $amount."
        } else {
            "Payment notification received."
        }

        PayTts.speak(applicationContext, message)
    }

    private fun extractNotificationText(notification: Notification): String {
        val extras = notification.extras ?: return ""
        val parts = ArrayList<CharSequence?>()

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)

        if (!title.isNullOrBlank()) parts.add(title)
        if (!text.isNullOrBlank()) parts.add(text)
        if (!bigText.isNullOrBlank()) parts.add(bigText)

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null && textLines.isNotEmpty()) {
            for (cs in textLines) {
                if (!cs.isNullOrBlank()) parts.add(cs)
            }
        }

        val combined = parts.filterNotNull().joinToString(" ") { it.toString() }
        return if (combined.isBlank()) {
            // fallback: contentView or tickerText
            val ticker = notification.tickerText
            ticker?.toString() ?: ""
        } else combined
    }

    private fun extractAmount(text: String): String? {
        val matcher = amountPattern.matcher(text)
        return if (matcher.find()) {
            val raw = matcher.group(1) ?: return null
            raw.replace("[,]".toRegex(), "")
        } else {
            null
        }
    }
}