import android.app.Notification
import android.service.notification.StatusBarNotification

fun whatsAppNotificationParser(sbn: StatusBarNotification): ArrayList<WhatsAppMessage>? {
    val extras=sbn.notification.extras
    val allMessages = ArrayList<WhatsAppMessage>()
    if(sbn.packageName!="com.whatsapp") return null
    var senderOrGroup: String?=null;
    if(extras.getString(Notification.EXTRA_TITLE)!="WhatsApp") { // One Sender
        senderOrGroup=extras.getString(Notification.EXTRA_TITLE)
    }

    if(extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)==null || extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.size==null){ // One Message
        allMessages.add(whatsAppMessage(extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?: "One group message", senderOrGroup?: "Fake Sender", true))
    } else {
        val messages: Array<CharSequence> = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)!!
        if(senderOrGroup==null){ // Multiple Chats/Groups
            messages.forEach { allMessages.add(whatsAppMessage(it.toString(), null, false)) }
        } else { // One Chat/Group
            messages.forEach { allMessages.add(whatsAppMessage(it.toString(), senderOrGroup, true)) }
        }
    }
    return allMessages
}
private fun whatsAppMessage(content: String, senderOrGroup: String?, mightBeUnnoticedGroup: Boolean): WhatsAppMessage{
    if(mightBeUnnoticedGroup){
        return if(content.split(": ").size==1) { // No Group
            WhatsAppMessage(content, senderOrGroup!!, null)
        } else { // Group
            WhatsAppMessage(content.split(": ", limit = 2)[1], content.split(": ", limit = 2)[0], senderOrGroup)
        }
    } else {
        val senderInMaybeGroup = content.split(": ")[0];
        val isGroup=senderInMaybeGroup.contains(" @ ")
        return WhatsAppMessage(content.split(": ", limit = 2)[1], if (isGroup) senderInMaybeGroup.split(" @ ")[0] else content.split(":", limit = 2)[0], if (isGroup) senderInMaybeGroup.split(" @ ")[1] else null)
    }
}
class WhatsAppMessage(val content: String, val sender: String, val group: String?)