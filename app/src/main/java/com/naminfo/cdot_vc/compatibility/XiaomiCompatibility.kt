package com.naminfo.cdot_vc.compatibility

import android.annotation.TargetApi
import android.app.*
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import com.naminfo.cdot_vc.contact.getThumbnailUri
import org.linphone.core.Call
import org.linphone.core.Friend
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.notifications.Notifiable
import com.naminfo.cdot_vc.notifications.NotificationsManager
import com.naminfo.cdot_vc.utils.ImageUtils
import com.naminfo.cdot_vc.utils.LinphoneUtils

@TargetApi(26)
class XiaomiCompatibility {
    companion object {
        fun createIncomingCallNotification(
            context: Context,
            call: Call,
            notifiable: Notifiable,
            pendingIntent: PendingIntent,
            notificationsManager: NotificationsManager
        ): Notification {
            val contact: Friend?
            val roundPicture: Bitmap?
            val displayName: String
            val address: String
            val info: String

            val remoteContact = call.remoteContact
            val conferenceAddress = if (remoteContact != null) {
                coreContext.core.interpretUrl(
                    remoteContact,
                    false
                )
            } else {
                null
            }
            val conferenceInfo = if (conferenceAddress != null) {
                coreContext.core.findConferenceInformationFromUri(
                    conferenceAddress
                )
            } else {
                null
            }
            if (conferenceInfo == null) {
                Log.i(
                    "[Notifications Manager] No conference info found for remote contact address $remoteContact"
                )
                contact = coreContext.contactsManager.findContactByAddress(call.remoteAddress)
                roundPicture =
                    ImageUtils.getRoundBitmapFromUri(context, contact?.getThumbnailUri())
                displayName = contact?.name ?: LinphoneUtils.getDisplayName(call.remoteAddress)
                address = LinphoneUtils.getDisplayableAddress(call.remoteAddress)
                info = context.getString(R.string.incoming_call_notification_title)
            } else {
                contact = null
                displayName = conferenceInfo.subject ?: context.getString(R.string.conference)
                address = LinphoneUtils.getDisplayableAddress(conferenceInfo.organizer)
                roundPicture = coreContext.contactsManager.groupBitmap
                info = context.getString(R.string.incoming_group_call_notification_title)
                Log.i(
                    "[Notifications Manager] Displaying incoming group call notification with subject $displayName and remote contact address $remoteContact"
                )
            }

            val builder = NotificationCompat.Builder(
                context,
                context.getString(R.string.notification_channel_incoming_call_id)
            )
                .addPerson(notificationsManager.getPerson(contact, displayName, roundPicture))
                .setSmallIcon(R.drawable.topbar_call_notification)
                .setLargeIcon(
                    roundPicture ?: BitmapFactory.decodeResource(
                        context.resources,
                        R.drawable.voip_single_contact_avatar_alt
                    )
                )
                .setContentTitle(displayName)
                .setContentText(address)
                .setSubText(info)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(false)
                .setShowWhen(true)
                .setOngoing(true)
                .setColor(ContextCompat.getColor(context, R.color.primary_color))
                .setFullScreenIntent(pendingIntent, true)
                .addAction(notificationsManager.getCallDeclineAction(notifiable))
                .addAction(notificationsManager.getCallAnswerAction(notifiable))

            if (!corePreferences.preventInterfaceFromShowingUp) {
                builder.setContentIntent(pendingIntent)
            }

            return builder.build()
        }
    }
}
