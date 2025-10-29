package com.naminfo.cdot_vc.notifications

import android.app.NotificationManager
import android.app.RemoteInput
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.ensureCoreExists
import org.linphone.core.Call
import org.linphone.core.ChatRoomParams
import org.linphone.core.Core
import org.linphone.core.tools.Log

class NotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.i("[Notification Broadcast Receiver] Ensuring Core exists")
        ensureCoreExists(context.applicationContext, false)

        val notificationId = intent.getIntExtra(NotificationsManager.INTENT_NOTIF_ID, 0)
        Log.i(
            "[Notification Broadcast Receiver] Got notification broadcast for ID [$notificationId]"
        )

        if (intent.action == NotificationsManager.INTENT_REPLY_NOTIF_ACTION || intent.action == NotificationsManager.INTENT_MARK_AS_READ_ACTION) {
            handleChatIntent(context, intent, notificationId)
        } else if (intent.action == NotificationsManager.INTENT_ANSWER_CALL_NOTIF_ACTION || intent.action == NotificationsManager.INTENT_HANGUP_CALL_NOTIF_ACTION) {
            handleCallIntent(intent)
        }
    }

    private fun handleChatIntent(context: Context, intent: Intent, notificationId: Int) {
        val remoteSipAddress = intent.getStringExtra(NotificationsManager.INTENT_REMOTE_ADDRESS)
        if (remoteSipAddress == null) {
            Log.e(
                "[Notification Broadcast Receiver] Remote SIP address is null for notification id $notificationId"
            )
            return
        }
        Log.e(
            "[Notification Broadcast Receiver] Remote SIP address is null for notification id $notificationId"
        )
        val core: Core = coreContext.core

        val remoteAddress = core.interpretUrl(remoteSipAddress, false)
        if (remoteAddress == null) {
            Log.e(
                "[Notification Broadcast Receiver] Couldn't interpret remote address $remoteSipAddress"
            )
            return
        }
        Log.e(
            "[Notification Broadcast Receiver] Couldn't interpret remote address $remoteSipAddress"
        )

        val localIdentity = intent.getStringExtra(NotificationsManager.INTENT_LOCAL_IDENTITY)
        if (localIdentity == null) {
            Log.e(
                "[Notification Broadcast Receiver] Notification id $notificationId"
            )
            return
        }
        Log.e(
            "[Notification Broadcast Receiver] Local identity is null for notification id $notificationId"
        )
        val localAddress = core.interpretUrl(localIdentity, false)
        if (localAddress == null) {
            Log.e(
                "[Notification Broadcast Receiver]Local address $localIdentity"
            )
            return
        }
        Log.e(
            "[Notification Broadcast Receiver] Local address $localIdentity"
        )
        val room = core.searchChatRoom(null as ChatRoomParams?, localAddress, remoteAddress, arrayOfNulls(0))
        if (room == null) {
            Log.e(
                "[Notification Broadcast Receiver] Couldn't find chat room for remote address $remoteSipAddress and local address $localIdentity"
            )
            return
        }

        if (intent.action == NotificationsManager.INTENT_REPLY_NOTIF_ACTION) {
            val reply = getMessageText(intent)?.toString()
            if (reply == null) {
                Log.e("[Notification Broadcast Receiver] Couldn't get reply text")
                return
            }

            val msg = room.createMessageFromUtf8(reply)
            Log.e("[Notification Broadcast Receiver]", " msg:${msg.utf8Text} ,reply:$reply")
            msg.userData = notificationId
            msg.addListener(coreContext.notificationsManager.chatListener)
            msg.send()
            Log.i("[Notification Broadcast Receiver] Reply sent for notif id $notificationId")
        } else {
            room.markAsRead()
            if (!coreContext.notificationsManager.dismissChatNotification(room)) {
                Log.w(
                    "[Notification Broadcast Receiver] Notifications Manager failed to cancel notification"
                )
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.cancel(NotificationsManager.CHAT_TAG, notificationId)
            }
        }
    }

    private fun handleCallIntent(intent: Intent) {
        val remoteSipAddress = intent.getStringExtra(NotificationsManager.INTENT_REMOTE_ADDRESS)
        if (remoteSipAddress == null) {
            Log.e("[Notification Broadcast Receiver] Remote SIP address is null for notification")
            return
        }

        val core: Core = coreContext.core

        val remoteAddress = core.interpretUrl(remoteSipAddress, false)
        val call = if (remoteAddress != null) core.getCallByRemoteAddress2(remoteAddress) else null
        if (call == null) {
            Log.e(
                "[Notification Broadcast Receiver] Couldn't find call from remote address $remoteSipAddress"
            )
            return
        }

        if (intent.action == NotificationsManager.INTENT_ANSWER_CALL_NOTIF_ACTION) {
            coreContext.answerCall(call)
        } else {
            if (call.state == Call.State.IncomingReceived ||
                call.state == Call.State.IncomingEarlyMedia
            ) {
                coreContext.declineCall(call)
            } else {
                coreContext.terminateCall(call)
            }
        }
    }

    private fun getMessageText(intent: Intent): CharSequence? {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        return remoteInput?.getCharSequence(NotificationsManager.KEY_TEXT_REPLY)
    }
}
