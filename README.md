# WhatsAppNotificationParser

This Kotlin script enables the parsing of WhatsApp (com.whatsapp) SBN (Status Bar Notifications) into a data class of messages split up into type, content, sender, group, cited, duration, and event timestamp.
This process should be used for personal projects such as a custom home screen with summarized content (that is for me, personally).

## How to Use This Script

### Getting the SBN
If your app has the following permission declared in the manifest, you will be able to register a listening service:

```xml
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY"/>
```

Declare the service in your `AndroidManifest.xml`:

```xml
<service
    android:name=".notification.NotificationListenerService"
    android:exported="true"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

### Parsing the SBN

In this file, you can then start to read and parse the messages:

```kotlin
class NotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.whatsapp") {
            val parsed = sbn.parseWhatsAppStatusBarNotification()
            // Further processing
        }
    }
}
```

### Choosing the Right SBN to Parse

Another important notice is that WhatsApp (for me personally) sends two notifications: one for the notification panel and one in a popup. The key difference is that one holds the notification history and the other just the latest message. A simple filter script is required:

```kotlin
class NotificationListenerService : NotificationListenerService() {
    private val votableSBNs = mutableListOf<StatusBarNotification>()
    private var latestReceivedTS = 0L

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == "com.whatsapp") {
            votableSBNs.add(sbn)
            if (System.currentTimeMillis() - latestReceivedTS > 200) { // Change delay
                CoroutineScope(Dispatchers.Main).launch {
                    delay(200)
                    sendToMainThread(
                        votableSBNs
                            .sortedByDescending { it.notification.extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE) != null }
                            .sortedByDescending { it.notification.extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT) != null }
                            .first()
                            .parseWhatsAppStatusBarNotification()
                    )
                    votableSBNs.clear()
                }
            }
        }
    }
}
```

### Sending to Main Thread

To further use the received data (e.g., in UI), you will want to send it to the main thread:

```kotlin
fun sendToMainThread(messages: List<WhatsAppMessage>) {
    val intent = Intent(notificationIntentName) // Name needs to also be used in the main thread
    intent.putExtra(
        "content",
        ParcelableWhatsAppMessages(messages.map { ParcelableWhatsAppMessage.byWhatsAppMessage(it) })
    )
}
```

### Example Parcelable Interface

```kotlin
@Parcelize
data class ParcelableWhatsAppMessages(
    val messages: List<ParcelableWhatsAppMessage>
) : Parcelable {
    fun asWhatsAppMessages(): List<WhatsAppMessage> {
        return messages.map { it.asWhatsAppMessage() }
    }
}

@Parcelize
data class ParcelableWhatsAppMessage(
    val content: String?,
    val group: String?,
    val messageType: WhatsAppMessageType,
    val cited: String?,
    val sender: String,
    val durationInSeconds: Int?,
    val eventTimestamp: String?
) : Parcelable {
    fun asWhatsAppMessage(): WhatsAppMessage {
        return WhatsAppMessage(group, messageType, cited, sender, durationInSeconds, eventTimestamp, content)
    }

    companion object {
        fun byWhatsAppMessage(message: WhatsAppMessage): ParcelableWhatsAppMessage {
            return ParcelableWhatsAppMessage(
                message.content,
                message.group,
                message.messageType,
                message.cited,
                message.sender,
                message.durationInSeconds,
                message.eventTimestamp
            )
        }
    }
}
```

### Receiver (on Main Thread)

```kotlin
val notificationReceiver = object : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val whatsAppParcelable = intent.extras?.getParcelable("content", ParcelableWhatsAppMessages::class.java)
        if (whatsAppParcelable != null) {
            val messages = whatsAppParcelable.asWhatsAppMessages()
        }
    }
}
```

Register the receiver:

```kotlin
registerReceiver(notificationHub.notificationReceiver, IntentFilter(NotificationHub.INTENTNAME), RECEIVER_EXPORTED)
```

### Disclaimer

**This project is not affiliated with, endorsed by, or associated with WhatsApp or its parent company Meta. Use this software at your own risk.**

