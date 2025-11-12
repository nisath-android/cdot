/*
 * Copyright (c) 2010-2020 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.naminfo.activities.main.chat.fragments

import android.app.Activity
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.MediaStore
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import java.io.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.linphone.core.Address
import org.linphone.core.ChatMessage
import org.linphone.core.ChatRoom
import org.linphone.core.Content
import org.linphone.core.EventLog
import org.linphone.core.Factory
import org.linphone.core.tools.Log
import org.naminfo.LinphoneApplication.Companion.coreContext
import org.naminfo.LinphoneApplication.Companion.corePreferences
import org.naminfo.R
import org.naminfo.activities.main.MainActivity
import org.naminfo.activities.main.chat.ChatScrollListener
import org.naminfo.activities.main.chat.adapters.ChatMessagesListAdapter
import org.naminfo.activities.main.chat.data.ChatMessageData
import org.naminfo.activities.main.chat.data.EventLogData
import org.naminfo.activities.main.chat.viewmodels.ChatMessageSendingViewModel
import org.naminfo.activities.main.chat.viewmodels.ChatMessageSendingViewModelFactory
import org.naminfo.activities.main.chat.viewmodels.ChatMessagesListViewModel
import org.naminfo.activities.main.chat.viewmodels.ChatMessagesListViewModelFactory
import org.naminfo.activities.main.chat.viewmodels.ChatRoomViewModel
import org.naminfo.activities.main.chat.viewmodels.ChatRoomViewModelFactory
import org.naminfo.activities.main.chat.views.RichEditTextSendListener
import org.naminfo.activities.main.contact.data.CallTypeWithPhoneNumber
import org.naminfo.activities.main.fragments.MasterFragment
import org.naminfo.activities.main.viewmodels.DialogViewModel
import org.naminfo.activities.navigateToAudioFileViewer
import org.naminfo.activities.navigateToConferenceScheduling
import org.naminfo.activities.navigateToConferenceWaitingRoom
import org.naminfo.activities.navigateToContacts
import org.naminfo.activities.navigateToDevices
import org.naminfo.activities.navigateToDialer
import org.naminfo.activities.navigateToEmptyChatRoom
import org.naminfo.activities.navigateToEphemeralInfo
import org.naminfo.activities.navigateToFriend
import org.naminfo.activities.navigateToGroupInfo
import org.naminfo.activities.navigateToImageFileViewer
import org.naminfo.activities.navigateToImdn
import org.naminfo.activities.navigateToNativeContact
import org.naminfo.activities.navigateToPdfFileViewer
import org.naminfo.activities.navigateToTextFileViewer
import org.naminfo.activities.navigateToVideoFileViewer
import org.naminfo.compatibility.Compatibility
import org.naminfo.databinding.ChatRoomDetailFragmentBinding
import org.naminfo.databinding.ChatRoomMenuBindingImpl
import org.naminfo.utils.AppUtils
import org.naminfo.utils.DialogUtils
import org.naminfo.utils.Event
import org.naminfo.utils.FileUtils
import org.naminfo.utils.PermissionHelper
import org.naminfo.utils.RecyclerViewHeaderDecoration
import org.naminfo.utils.RecyclerViewSwipeConfiguration
import org.naminfo.utils.RecyclerViewSwipeListener
import org.naminfo.utils.RecyclerViewSwipeUtils

class DetailChatRoomFragment :
    MasterFragment<ChatRoomDetailFragmentBinding, ChatMessagesListAdapter>() {

    private lateinit var viewModel: ChatRoomViewModel
    private lateinit var chatSendingViewModel: ChatMessageSendingViewModel
    private lateinit var listViewModel: ChatMessagesListViewModel
    private var remoteNum: String = "Remote"
    private var localNum: String = "Local"
    private lateinit var domain: String
    private var alertDialog: android.app.AlertDialog? = null

    companion object Save {
        var tempTextMessageSave: String? = null
        var tempFilePathSave: String? = null
        var tempIntentSave: Intent? = null
    }

    fun showBackPress() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            back
        )
    }

    var voiceStatus = ""
    private val back = object : OnBackPressedCallback(
        if (::chatSendingViewModel.isInitialized) {
            chatSendingViewModel.backIsEnable.value == true
        } else {
            true
        }
    ) {
        override fun handleOnBackPressed() {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
                android.util.Log.d(
                    "[FTP CHAT]",
                    "handleOnBackPressed: isDownloading=${chatSendingViewModel.isUploading.value}||" +
                        "isVoiceRecording.value=${chatSendingViewModel.isVoiceRecording.value}"
                )
                if (chatSendingViewModel.isUploading.value == true) {
                    showExitConfirmationDialog(isDownloadOrUpload = true) { isBackOk ->
                        isEnabled = isBackOk
                        if (!isBackOk) requireActivity().onBackPressedDispatcher.onBackPressed()
                        android.util.Log.d(
                            "[FTP CHAT]",
                            "handleOnBackPressed:isDownloading:${chatSendingViewModel.isUploading.value},isBackOk=$isBackOk Live-backIsEnable=${chatSendingViewModel.backIsEnable.value} ,isEnabled:$isEnabled"
                        )
                    }
                } else {
                    android.util.Log.d(
                        "[FTP CHAT]",
                        "else:"
                    )
                    // isEnabled = false
                    if (binding.footer?.message?.text?.isNotEmpty() == true) {
                        showExitConfirmationDialog(isDownloadOrUpload = false) { isBackOk ->
                            isEnabled = isBackOk
                            if (!isBackOk) requireActivity().onBackPressedDispatcher.onBackPressed()
                            android.util.Log.d(
                                "[FTP CHAT]",
                                "handleOnBackPressed:isDownloading:${chatSendingViewModel.isUploading.value},isBackOk=$isBackOk Live-backIsEnable=${chatSendingViewModel.backIsEnable.value} ,isEnabled:$isEnabled"
                            )
                        }
                    } else {
                        android.util.Log.d(
                            "[FTP CHAT]",
                            "else showBackPress:" +
                                "isVoiceRecording.value=${chatSendingViewModel.isVoiceRecording.value}"
                        )
                        if (voiceStatus.isNotEmpty()) {
                            showExitConfirmationDialog(isDownloadOrUpload = false) { isBackOk ->
                                isEnabled = isBackOk
                                if (!isBackOk) requireActivity().onBackPressedDispatcher.onBackPressed()
                                android.util.Log.d(
                                    "[FTP CHAT]",
                                    "handleOnBackPressed:isDownloading:${chatSendingViewModel.isUploading.value},isBackOk=$isBackOk Live-backIsEnable=${chatSendingViewModel.backIsEnable.value} ,isEnabled:$isEnabled"
                                )
                            }
                        } else {
                            isEnabled = false
                            binding.footer.attachFile.isEnabled = true
                            chatSendingViewModel.attachFileEnabled.value = true
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        }

        override fun handleOnBackCancelled() {
            android.util.Log.d(
                "[FTP CHAT]",
                "handleOnBackCancelled: isEnable:$isEnabled isCancel:${chatSendingViewModel.isUploading.value} : isLiveData:${chatSendingViewModel.backIsEnable.value}"
            )
            super.handleOnBackCancelled()
        }
    }

    private fun showExitConfirmationDialog(
        isDownloadOrUpload: Boolean = true,
        title: String = "Abort Sending message?",
        message: String = "Are you sure you want to close this screen?",
        onBackResult: (Boolean) -> Unit
    ) {
        if (alertDialog?.isShowing == true) return
        // viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
        android.util.Log.d("[FTP CHAT]", "showExitConfirmationDialog: ")
        alertDialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                if (isDownloadOrUpload) {
                    // cancelDownload()
                    tempTextMessageSave = chatSendingViewModel.textToSend.value
                    tempFilePathSave = chatSendingViewModel.textToSend.value
                }

                Log.w("[FTP CHAT]", "Back pressed")
                if (::chatSendingViewModel.isInitialized) {
                    chatSendingViewModel.backIsEnable.value = false
                }
                dialog.dismiss()
                onBackResult(false)
                // 🔄 Updated deprecated method
                //  requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                if (::chatSendingViewModel.isInitialized) {
                    chatSendingViewModel.backIsEnable.value = true
                }
                onBackResult(true)
            }
            .setCancelable(false)
            .show()
        // }
    }

    private val observer = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            adapter.notifyItemChanged(positionStart - 1) // For grouping purposes

            if (positionStart == 0 && adapter.itemCount == itemCount) {
                // First time we fill the list with messages
                Log.i("[Chat Room] History first $itemCount messages loaded")
            } else {
                // Scroll to newly added messages automatically only if user hasn't initiated a scroll up in the messages history
                if (viewModel.isUserScrollingUp.value == false) {
                    scrollToFirstUnreadMessageOrBottom(false)
                } else {
                    Log.d(
                        "[Chat Room] User has scrolled up manually in the messages history, don't scroll to the newly added message at the bottom & don't mark the chat room as read"
                    )
                }
            }
        }
    }

    private val globalLayoutLayout = object : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            if (isBindingAvailable()) {
                binding.chatMessagesList
                    .viewTreeObserver
                    .removeOnGlobalLayoutListener(this)

                if (::chatScrollListener.isInitialized) {
                    binding.chatMessagesList.addOnScrollListener(chatScrollListener)
                }

                if (viewModel.chatRoom.unreadMessagesCount > 0) {
                    Log.i("[Chat Room] Messages have been displayed, scrolling to first unread")
                    val notAllMessagesDisplayed = scrollToFirstUnreadMessageOrBottom(false)
                    if (notAllMessagesDisplayed) {
                        Log.w(
                            "[Chat Room] More unread messages than the screen can display, do not mark chat room as read now, wait for user to scroll to bottom"
                        )
                    } else {
                        // Consider user as scrolled to the end when marking chat room as read
                        viewModel.isUserScrollingUp.value = false
                        Log.i("[Chat Room] Marking chat room as read")
                        viewModel.chatRoom.markAsRead()
                    }
                }
            } else {
                Log.e("[Chat Room] Binding not available in onGlobalLayout callback!")
            }
        }
    }

    private val keyboardVisibilityListener = object : AppUtils.KeyboardVisibilityListener {
        override fun onKeyboardVisibilityChanged(visible: Boolean) {
            if (visible && chatSendingViewModel.isEmojiPickerOpen.value == true) {
                Log.d(
                    "[Chat Room] Emoji picker is opened, closing it because keyboard is now visible"
                )
                chatSendingViewModel.isEmojiPickerOpen.value = false
            }
        }
    }

    private lateinit var chatScrollListener: ChatScrollListener

    override fun getLayoutId(): Int {
        return R.layout.chat_room_detail_fragment
    }

    override fun onDestroyView() {
        binding.chatMessagesList.adapter = null
        alertDialog?.dismiss() // Avoid memory leaks
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (isSharedViewModelInitialized()) {
            val chatRoom = sharedViewModel.selectedChatRoom.value
            if (chatRoom != null) {
                outState.putString("LocalSipUri", chatRoom.localAddress.asStringUriOnly())
                outState.putString("RemoteSipUri", chatRoom.peerAddress.asStringUriOnly())
                Log.i(
                    "[Chat Room] 2-Saving current chat room local & remote addresses in save instance state"
                )
            }
        } else {
            Log.w(
                "[Chat Room] 3-Can't save instance state, sharedViewModel hasn't been initialized yet"
            )
        }
        super.onSaveInstanceState(outState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        domain = coreContext.core.authInfoList[0].domain.toString()
        Log.i("[Chat Room] Domain - $domain")

        binding.lifecycleOwner = viewLifecycleOwner

        binding.sharedMainViewModel = sharedViewModel

        useMaterialSharedAxisXForwardAnimation =
            sharedViewModel.isSlidingPaneSlideable.value == false

        val localSipUri = arguments?.getString("LocalSipUri") ?: savedInstanceState?.getString(
            "LocalSipUri"
        )
        val remoteSipUri = arguments?.getString("RemoteSipUri") ?: savedInstanceState?.getString(
            "RemoteSipUri"
        )
        Log.i(
            "[Chat Room] localSipUri - [$localSipUri] & remoteSipUri [$remoteSipUri]"
        )

        val textToShare = arguments?.getString("TextToShare")
        val filesToShare = arguments?.getStringArrayList("FilesToShare")

        showBackPress()
        if (remoteSipUri != null && arguments?.getString("RemoteSipUri") == null) {
            Log.w("[Chat Room] 1-Chat room will be restored from saved instance state")
        }
        arguments?.clear()
        if (localSipUri != null && remoteSipUri != null) {
            Log.i(
                "[Chat Room] Found local [$localSipUri] & remote [$remoteSipUri] addresses in arguments or saved instance state"
            )

            val localAddress = Factory.instance().createAddress(localSipUri)
            val remoteSipAddress = Factory.instance().createAddress(remoteSipUri)

            sharedViewModel.selectedChatRoom.value = coreContext.core.searchChatRoom(
                null,
                localAddress,
                remoteSipAddress,
                arrayOfNulls(
                    0
                )
            )
        }

        val chatRoom = sharedViewModel.selectedChatRoom.value
        if (chatRoom == null) {
            Log.e("[Chat Room] Chat room is null, aborting!")
            goBack()
            return
        } else {
            localNum = chatRoom.localAddress.username.toString()
            remoteNum = chatRoom.peerAddress.username.toString()
            Log.i("[Chat Room] local number form  [$localNum]")
            Log.i("[Chat Room] remote number is ${chatRoom.peerAddress.username}")
        }

        Compatibility.setLocusIdInContentCaptureSession(binding.root, chatRoom)

        isSecure = chatRoom.currentParams.isEncryptionEnabled

        viewModel = ViewModelProvider(
            this,
            ChatRoomViewModelFactory(chatRoom)
        )[ChatRoomViewModel::class.java]

        binding.viewModel = viewModel

        chatSendingViewModel = ViewModelProvider(
            this,
            ChatMessageSendingViewModelFactory(chatRoom)
        )[ChatMessageSendingViewModel::class.java]
        binding.chatSendingViewModel = chatSendingViewModel

        listViewModel = ViewModelProvider(
            this,
            ChatMessagesListViewModelFactory(chatRoom)
        )[ChatMessagesListViewModel::class.java]

        _adapter = ChatMessagesListAdapter(listSelectionViewModel, viewLifecycleOwner)
        // SubmitList is done on a background thread
        // We need this adapter data observer to know when to scroll
        binding.chatMessagesList.adapter = adapter

        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = true
        binding.chatMessagesList.layoutManager = layoutManager

        // Displays unread messages header
        val headerItemDecoration = RecyclerViewHeaderDecoration(requireContext(), adapter)
        binding.chatMessagesList.addItemDecoration(headerItemDecoration)

        // Swipe action
        val swipeConfiguration = RecyclerViewSwipeConfiguration()
        // Reply action can only be done on a ChatMessageEventLog
        swipeConfiguration.leftToRightAction = RecyclerViewSwipeConfiguration.Action(
            text = requireContext().getString(R.string.chat_message_context_menu_reply),
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.light_grey_color),
            preventFor = ChatMessagesListAdapter.EventViewHolder::class.java
        )
        // Delete action can be done on any EventLog
        swipeConfiguration.rightToLeftAction = RecyclerViewSwipeConfiguration.Action(
            text = requireContext().getString(R.string.chat_message_context_menu_delete),
            backgroundColor = ContextCompat.getColor(requireContext(), R.color.red_color)
        )
        val swipeListener = object : RecyclerViewSwipeListener {
            override fun onLeftToRightSwipe(viewHolder: RecyclerView.ViewHolder) {
                val index = viewHolder.bindingAdapterPosition
                if (index < 0 || index >= adapter.currentList.size) {
                    Log.e("[Chat Room] Index is out of bound, can't reply to chat message")
                } else {
                    adapter.notifyItemChanged(index)

                    val chatMessageEventLog = adapter.currentList[index]
                    val chatMessage = chatMessageEventLog.eventLog.chatMessage
                    if (chatMessage != null) {
                        Log.i(
                            "[Chat Room]",
                            " swipeListener =>chatMessage:[${chatMessage.messageId}]"
                        )
                        replyToChatMessage(chatMessage)
                        // replyToMessage(chatMessage.chatRoom, chatMessage.messageId)
                    }
                }
            }

            override fun onRightToLeftSwipe(viewHolder: RecyclerView.ViewHolder) {
                val index = viewHolder.bindingAdapterPosition
                if (index < 0 || index >= adapter.currentList.size) {
                    Log.e("[Chat Room] Index is out of bound, can't delete chat message")
                } else {
                    // adapter.notifyItemRemoved(index)
                    val eventLog = adapter.currentList[index]
                    addDeleteMessageTaskToQueue(eventLog, index)
                }
            }
        }
        RecyclerViewSwipeUtils(
            ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT,
            swipeConfiguration,
            swipeListener
        ).attachToRecyclerView(binding.chatMessagesList)
        // backUpMessage()
        chatScrollListener = object : ChatScrollListener(layoutManager) {
            override fun onLoadMore(totalItemsCount: Int) {
                Log.i(
                    "[Chat Room] User has scrolled up far enough, load more items from history (currently there are $totalItemsCount messages displayed)"
                )
                listViewModel.loadMoreData(totalItemsCount)
            }

            override fun onScrolledUp() {
                viewModel.isUserScrollingUp.value = true
            }

            override fun onScrolledToEnd() {
                viewModel.isUserScrollingUp.value = false

                val peerAddress = viewModel.chatRoom.peerAddress.asStringUriOnly()
                if (viewModel.unreadMessagesCount.value != 0 &&
                    coreContext.notificationsManager.currentlyDisplayedChatRoomAddress == peerAddress
                ) {
                    Log.i(
                        "[Chat Room] User has scrolled to the latest message, mark chat room as read"
                    )
                    viewModel.chatRoom.markAsRead()
                }
            }
        }

        chatSendingViewModel.textToSend.observe(
            viewLifecycleOwner
        ) {
            chatSendingViewModel.onTextToSendChanged(it)
        }

        chatSendingViewModel.isVoiceRecording.observe(
            viewLifecycleOwner
        ) { voiceRecording ->
            // Keep screen on while recording voice message
            Log.i("[FTP CHAT] Voice recording: $voiceRecording")
            if (voiceRecording) {
                voiceStatus = "start"
            } else {
                voiceStatus = "stop"
            }
            if (voiceRecording) {
                requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }

        chatSendingViewModel.requestRecordAudioPermissionEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                Log.i("[Chat Room] Asking for RECORD_AUDIO permission")
                requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 2)
            }
        }

        chatSendingViewModel.messageSentEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                Log.i("[Chat Room] Message sent")
                // Reset this to ensure sent message will be visible
                viewModel.isUserScrollingUp.value = false
            }
        }

        chatSendingViewModel.requestKeyboardHidingEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                (requireActivity() as MainActivity).hideKeyboard()
            }
        }

        listViewModel.events.observe(
            viewLifecycleOwner
        ) { events ->

            if (events.size > 0) {
                Log.w(
                    "[Chat Room] Events count - ${events.size} , ${events.get(0).eventLog.chatMessage?.utf8Text}"
                )
                android.util.Log.d("[xxxAdapter]", "3--> ${events.size}")
                adapter.setUnreadMessageCount(
                    viewModel.chatRoom.unreadMessagesCount,
                    viewModel.isUserScrollingUp.value == true
                )
                adapter.submitList(events)
            }
        }

        listViewModel.messageUpdatedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { position ->
                adapter.notifyItemChanged(position)
            }
        }

        listViewModel.requestWriteExternalStoragePermissionEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume {
                requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
            }
        }

        adapter.deleteMessageEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatMessage ->
                listViewModel.deleteMessage(chatMessage)
                sharedViewModel.refreshChatRoomInListEvent.value = Event(true)
            }
        }

        adapter.resendMessageEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatMessage ->
                listViewModel.resendMessage(chatMessage)
            }
        }

        adapter.forwardMessageEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatMessage ->
                // Remove observer before setting the message to forward
                // as we don't want to forward it in this chat room
                sharedViewModel.messageToForwardEvent.removeObservers(viewLifecycleOwner)
                sharedViewModel.messageToForwardEvent.value = Event(chatMessage)
                sharedViewModel.isPendingMessageForward.value = true

                if (sharedViewModel.isSlidingPaneSlideable.value == true) {
                    Log.i("[Chat Room] Forwarding message, going to chat rooms list")
                    goBack()
                } else {
                    navigateToEmptyChatRoom()
                }
            }
        }

        adapter.replyMessageEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatMessage ->
                Log.i("[Chat Room]", "  replyMessageEvent: chatMessage=${chatMessage.utf8Text}")
                replyToChatMessage(chatMessage)
            }
        }

        adapter.showImdnForMessageEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatMessage ->
                val args = Bundle()
                args.putString("MessageId", chatMessage.messageId)
                navigateToImdn(args)
            }
        }

        adapter.addSipUriToContactEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { sipUri ->
                Log.i("[Chat Room] Going to contacts list with SIP URI to add: $sipUri")
                sharedViewModel.updateContactsAnimationsBasedOnDestination.value =
                    Event(R.id.masterChatRoomsFragment)
                navigateToContacts(sipUri)
            }
        }

        adapter.openContentEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { content ->
                var path = content.filePath.orEmpty()

                if (path.isNotEmpty() && !File(path).exists()) {
                    Log.e("[Chat Room] File not found: $path")
                    (requireActivity() as MainActivity).showSnackBar(
                        R.string.chat_room_file_not_found
                    )
                } else {
                    if (path.isEmpty()) {
                        val name = content.name
                        if (!name.isNullOrEmpty()) {
                            val file = FileUtils.getFileStoragePath(name)
                            FileUtils.writeIntoFile(content.buffer, file)
                            path = file.absolutePath
                            content.filePath = path
                            Log.i(
                                "[Chat Room] Content file path was empty, created file from buffer at $path"
                            )
                        } else if (content.isIcalendar) {
                            val filename = "conference.ics"
                            val file = FileUtils.getFileStoragePath(filename)
                            FileUtils.writeIntoFile(content.buffer, file)
                            path = file.absolutePath
                            content.filePath = path
                            Log.i(
                                "[Chat Room] Content file path was empty, created conference.ics from buffer at $path"
                            )
                        }
                    }

                    Log.i("[Chat Room] Opening file: $path")
                    sharedViewModel.contentToOpen.value = content

                    if (corePreferences.useInAppFileViewerForNonEncryptedFiles || content.isFileEncrypted) {
                        val preventScreenshots =
                            viewModel.chatRoom.currentParams.isEncryptionEnabled

                        val extension = FileUtils.getExtensionFromFileName(path)
                        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                        when (FileUtils.getMimeType(mime)) {
                            FileUtils.MimeType.Image -> navigateToImageFileViewer(
                                preventScreenshots
                            )

                            FileUtils.MimeType.Video -> navigateToVideoFileViewer(
                                preventScreenshots
                            )

                            FileUtils.MimeType.Audio -> navigateToAudioFileViewer(
                                preventScreenshots
                            )

                            FileUtils.MimeType.Pdf -> navigateToPdfFileViewer(
                                preventScreenshots
                            )

                            FileUtils.MimeType.PlainText -> navigateToTextFileViewer(
                                preventScreenshots
                            )

                            else -> {
                                if (content.isFileEncrypted) {
                                    Log.w(
                                        "[Chat Room] File is encrypted and can't be opened in one of our viewers..."
                                    )
                                    showDialogForUserConsentBeforeExportingFileInThirdPartyApp(
                                        content
                                    )
                                } else if (!FileUtils.openFileInThirdPartyApp(
                                        requireActivity(),
                                        path
                                    )
                                ) {
                                    showDialogToSuggestOpeningFileAsText()
                                }
                            }
                        }
                    } else {
                        if (!FileUtils.openFileInThirdPartyApp(requireActivity(), path)) {
                            showDialogToSuggestOpeningFileAsText()
                        }
                    }
                }
            }
        }
        adapter.openFileContentEvent.observe(viewLifecycleOwner) {
            it.consume { msg ->
                Log.i("[Chat Room]", "   msg:$msg")
                chatSendingViewModel.textToSend.value = "${msg}\n"
            }
        }
        adapter.urlClickEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { url ->
                val uri = Uri.parse(url)
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    uri
                )
                try {
                    startActivity(browserIntent)
                } catch (se: SecurityException) {
                    Log.e("[Chat Room] Failed to start browser intent from uri [$uri]: $se")
                } catch (anfe: ActivityNotFoundException) {
                    Log.e("[Chat Room] Failed to find app matching intent from uri [$uri]: $anfe")
                }
                /*binding.chatMessagesList.visibility = View.GONE
                binding.webviewContainer.visibility = View.VISIBLE
                binding.webview.loadUrl(url)

                // Find the download button and set its click listener
                binding.download.setOnClickListener {
                    // Trigger the download function with the current URL
                    downloadFile(url)
                }*/
            }
        }

        adapter.sipUriClickedEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { sipUri ->
                Log.i("[Chat Room]", "Raw sipUri: $sipUri")
                if (sipUri.startsWith("http://") || sipUri.startsWith("https://")) {
                    // It's a URL, show it in the WebView
                    binding.chatMessagesList.visibility = View.GONE
                    binding.webviewContainer.visibility = View.VISIBLE
                    binding.webview.loadUrl(sipUri)
                    Log.i("[Chat Room]", "Opened URL in WebView: $sipUri")
                } else {
                    val args = Bundle()
                    args.putString("URI", sipUri)
                    args.putBoolean("Transfer", false)
                    // If auto start call setting is enabled, ignore it
                    args.putBoolean("SkipAutoCallStart", true)
                    navigateToDialer(args)
                    Log.i("[Chat Room]", "Navigated to Dialer with URI: $sipUri")
                }
            }
        }

        adapter.callConferenceEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { pair ->
                navigateToConferenceWaitingRoom(pair.first, pair.second)
            }
        }

        adapter.scrollToChatMessageEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatMessage ->
                var index: Int
                var loadSteps = 0
                var expectedChildCount: Int
                do {
                    val events = listViewModel.events.value.orEmpty()
                    expectedChildCount = events.size
                    Log.e("[Chat Room] expectedChildCount : $expectedChildCount")
                    val eventLog = events.find { eventLog ->
                        if (eventLog.eventLog.type == EventLog.Type.ConferenceChatMessage) {
                            (eventLog.data as ChatMessageData).chatMessage.messageId == chatMessage.messageId
                        } else {
                            false
                        }
                    }
                    index = events.indexOf(eventLog)
                    if (index == -1) {
                        loadSteps += 1
                        listViewModel.loadMoreData(events.size)
                    }
                } while (index == -1 && loadSteps < 5)

                if (index != -1) {
                    if (loadSteps == 0) {
                        scrollTo(index, true)
                    } else {
                        lifecycleScope.launch {
                            withContext(Dispatchers.Default) {
                                var retryCount = 0
                                do {
                                    // We have to wait for newly loaded items to be added to list before being able to scroll
                                    delay(500)
                                    retryCount += 1
                                } while (layoutManager.itemCount != expectedChildCount && retryCount < 5)

                                withContext(Dispatchers.Main) {
                                    scrollTo(index, true)
                                }
                            }
                        }
                    }
                } else {
                    Log.w("[Chat Room] Failed to find matching event!")
                }
            }
        }

        adapter.showReactionsListEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { message ->
                val modalBottomSheet = ChatMessageReactionsListDialogFragment()
                modalBottomSheet.setMessage(message)
                modalBottomSheet.show(
                    parentFragmentManager,
                    ChatMessageReactionsListDialogFragment.TAG
                )
            }
        }

        adapter.errorEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { message ->
                (requireActivity() as MainActivity).showSnackBar(message)
            }
        }

        binding.setMenuClickListener {
            showPopupMenu(chatRoom)
        }

        binding.setMenuLongClickListener {
            // Only show debug infos if debug mode is enabled
            if (corePreferences.debugLogs) {
                val alertDialog = MaterialAlertDialogBuilder(requireContext())

                val messageBuilder = StringBuilder()
                messageBuilder.append("Chat room id:\n")
                messageBuilder.append(viewModel.chatRoom.peerAddress.asString())
                messageBuilder.append("\n")
                messageBuilder.append("Local account:\n")
                messageBuilder.append(viewModel.chatRoom.localAddress.asString())
                val message = messageBuilder.toString()
                alertDialog.setMessage(message)

                alertDialog.setNeutralButton(R.string.chat_message_context_menu_copy_text) { _, _ ->
                    val clipboard: ClipboardManager =
                        coreContext.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Chat room info", message)
                    clipboard.setPrimaryClip(clip)
                }

                alertDialog.show()
                true
            }
            false
        }

        binding.setSecurityIconClickListener {
            showParticipantsDevices()
        }

        binding.setAttachFileClickListener {
            if (PermissionHelper.get().hasReadExternalStoragePermission() ||
                PermissionHelper.get().hasCameraPermission()
            ) {
                pickFile()
                // showAttachmentPopup(binding.root)
                // openFilePicker()
            } else {
                Log.i("[Chat Room] Asking for READ_EXTERNAL_STORAGE and CAMERA permissions")
                Compatibility.requestReadExternalStorageAndCameraPermissions(this, 0)
            }
        }

        binding.setVoiceRecordingTouchListener { _, event ->
            if (corePreferences.holdToRecordVoiceMessage) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.i(
                            "[Chat Room] Start recording voice message as long as recording button is held"
                        )
                        chatSendingViewModel.startVoiceRecording()
                    }

                    MotionEvent.ACTION_UP -> {
                        val voiceRecordingDuration =
                            chatSendingViewModel.voiceRecordingDuration.value ?: 0
                        if (voiceRecordingDuration < 1000) {
                            Log.w(
                                "[Chat Room] Voice recording button has been held for less than a second, considering miss click"
                            )
                            chatSendingViewModel.cancelVoiceRecording()
                            (activity as MainActivity).showSnackBar(
                                R.string.chat_message_voice_recording_hold_to_record
                            )
                        } else {
                            Log.i(
                                "[Chat Room] Voice recording button has been released, stop recording"
                            )
                            chatSendingViewModel.stopVoiceRecording()
                        }
                        view.performClick()
                    }
                }
            }
            false
        }

        binding.footer.message.setControlEnterListener(object : RichEditTextSendListener {
            override fun onControlEnterPressedAndReleased() {
                Log.i("[Chat Room] Detected left control + enter key presses, sending message")
                chatSendingViewModel.sendMessage()
            }
        })

        binding.setCancelReplyToClickListener {
            chatSendingViewModel.cancelReply()
        }

        binding.setScrollToBottomClickListener {
            scrollToFirstUnreadMessageOrBottom(true)
            viewModel.isUserScrollingUp.value = false
        }

        binding.setGroupCallListener {
            showGroupCallDialog()
        }

        if (textToShare?.isNotEmpty() == true) {
            Log.i("[Chat Room] Found text to share")
            chatSendingViewModel.textToSend.value = textToShare
        }

        if (filesToShare?.isNotEmpty() == true) {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    chatSendingViewModel.attachingFileInProgress.value = true
                    for (filePath in filesToShare) {
                        val path = FileUtils.copyToLocalStorage(filePath)
                        Log.i(
                            "[Chat Room] Found [$filePath] file to share, matching path is [$path]"
                        )
                        if (path != null) {
                            chatSendingViewModel.addAttachment(path)
                        }
                    }
                    chatSendingViewModel.attachingFileInProgress.value = false
                }
            }
        }

        sharedViewModel.richContentUri.observe(
            viewLifecycleOwner
        ) {
            it.consume { uri ->
                Log.i("[Chat Room] Found rich content URI: $uri")
                lifecycleScope.launch {
                    withContext(Dispatchers.Main) {
                        chatSendingViewModel.attachingFileInProgress.value = true
                        val path = FileUtils.getFilePath(requireContext(), uri)
                        Log.i("[Chat Room] Rich content URI [$uri] matching path is [$path]")
                        if (path != null) {
                            chatSendingViewModel.addAttachment(path)
                        }
                        chatSendingViewModel.attachingFileInProgress.value = false
                    }
                }
            }
        }

        sharedViewModel.messageToForwardEvent.observe(
            viewLifecycleOwner
        ) {
            it.consume { chatMessage ->
                Log.i("[Chat Room] Found message to transfer")
                showForwardConfirmationDialog(chatMessage)
                sharedViewModel.isPendingMessageForward.value = false
            }
        }
        viewModel.dialogCall.observe(viewLifecycleOwner) {
            it.consume { map ->
                if (map.isNotEmpty()) {
                    val (phoneAddress, isVideo) = map.entries.first()
                    Log.i(
                        "[xxxyyyDialer] DetailChatRoomFrag- Phone address is $phoneAddress ,$isVideo"
                    )
                    showCustomDialog(phoneAddress = phoneAddress, isVideo = isVideo)
                } else {
                    Log.i("[xxxyyyDialer] DetailChatRoomFrag- Phone address is empty")
                }
            }
        }

        startPostponedEnterTransition()
    }

    private fun backUpMessage() {
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                if (tempIntentSave != null) {
                    chatSendingViewModel.attachingFileInProgress.value = true
                    for (
                    fileToUploadPath in FileUtils.getFilesPathFromPickerIntent(
                        tempIntentSave,
                        chatSendingViewModel.temporaryFileUploadPath
                    )
                    ) {
                        Log.i(
                            "[Chat Room] temp-->>Found fileToUploadPath:[$fileToUploadPath] file from intent"
                        )
                        Log.i(
                            "[Chat Room] temp-->>Found temporaryFileUploadPath:[${chatSendingViewModel.temporaryFileUploadPath}] file from intent"
                        )
                        chatSendingViewModel.addAttachment(fileToUploadPath)
                    }
                    chatSendingViewModel.attachingFileInProgress.value = false
                    chatSendingViewModel.isUploading.value = true
                }
            }
        }
    }

    private fun showCustomDialog(phoneAddress: String, isVideo: Boolean = false) {
        Log.i(
            "[xxxDialer] DetailChatRoomFrag-showCustomDialog called phoneaddress is $phoneAddress , isVideo $isVideo"
        )
        if (isVisible) {
            val dialogView = LayoutInflater.from(requireContext()).inflate(
                R.layout.custom_call_dialog_screen,
                null
            )
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            val mobiFSCallBTN = dialogView.findViewById<MaterialButton>(R.id.mobiFSCallBTN)
            val gsmCallBTN = dialogView.findViewById<MaterialButton>(R.id.gsmCallBTN)
            val mobiWebCallBTN = dialogView.findViewById<MaterialButton>(R.id.mobiWebCallBTN)
            val closeDialog = dialogView.findViewById<ImageView>(R.id.close_dialog)
            if (isVideo) {
                mobiFSCallBTN.setText("Video")
                gsmCallBTN.visibility = View.GONE
            } else {
                mobiFSCallBTN.setText("Audio")
            }
            corePreferences.getRemoteUserPhoneNumber = phoneAddress.trim().takeLast(10)
            var phoneNumberModified = ""
            mobiFSCallBTN.setOnClickListener {
                phoneNumberModified = phoneAddress.trim().takeLast(10)
                Log.i(
                    "[xxxyyyDialer] DetailChatRoomFrag-showCustomDialog-Phone address mobiFSCallBTN-> $phoneNumberModified "
                )

                val gson = Gson()
                if (isVideo) {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Video",
                            "Contact",
                            "mobiFSCallBTN"
                        )
                    )
                    viewModel.videoCall(phoneNumberModified)
                } else {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Audio",
                            "Contact",
                            "mobiFSCallBTN"
                        )
                    )
                    viewModel.audioCall(phoneNumberModified)
                }
                dialog.dismiss()
            }
            gsmCallBTN.setOnClickListener {
                val phone = phoneAddress.trim().takeLast(10)
                phoneNumberModified = "11$phone"

                Log.i(
                    "[xxxDialer] DetailChatRoomFrag-Phone address gsmCallBTN-> $phoneNumberModified "
                )
                val gson = Gson()
                if (isVideo) {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Video",
                            "Contact",
                            "gsmCallBTN"
                        )
                    )
                } else {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Audio",
                            "Contact",
                            "gsmCallBTN"
                        )
                    )
                }
                viewModel.audioCall(phoneNumberModified)
                dialog.dismiss()
            }
            mobiWebCallBTN.setOnClickListener {
                phoneNumberModified = "00${phoneAddress.trim().takeLast(10)}"
                Log.i(
                    "[xxxDialer] DetailChatRoomFrag-showCustomDialog-Phone address mobiWebCallBTN-> $phoneNumberModified"
                )
                val gson = Gson()
                if (isVideo) {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Video",
                            "Contact",
                            "mobiWebCallBTN"
                        )
                    )
                    viewModel.videoCall(phoneNumberModified)
                } else {
                    corePreferences.callType = gson.toJson(
                        CallTypeWithPhoneNumber(
                            phoneNumberModified,
                            "Audio",
                            "Contact",
                            "mobiWebCallBTN"
                        )
                    )
                    viewModel.audioCall(phoneNumberModified)
                }
                dialog.dismiss()
            }
            closeDialog.setOnClickListener {
                corePreferences.getRemoteUserPhoneNumber = ""
                phoneNumberModified = ""
                dialog.dismiss()
            }
            dialog.show()
        }
    }

    override fun deleteItems(indexesOfItemToDelete: ArrayList<Int>) {
        val list = ArrayList<EventLogData>()
        for (index in indexesOfItemToDelete) {
            val eventLog = adapter.currentList[index]
            list.add(eventLog)
        }
        listViewModel.deleteEventLogs(list)
        sharedViewModel.refreshChatRoomInListEvent.value = Event(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        var atLeastOneGranted = false
        for (result in grantResults) {
            atLeastOneGranted = atLeastOneGranted || result == PackageManager.PERMISSION_GRANTED
        }

        when (requestCode) {
            0 -> {
                if (atLeastOneGranted) {
                    pickFile()
                }
            }

            2 -> {
                if (atLeastOneGranted) {
                    chatSendingViewModel.startVoiceRecording()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (this::viewModel.isInitialized) {
            // Prevent notifications for this chat room to be displayed
            val peerAddress = viewModel.chatRoom.peerAddress.asStringUriOnly()
            coreContext.notificationsManager.currentlyDisplayedChatRoomAddress = peerAddress

            if (_adapter != null) {
                try {
                    adapter.registerAdapterDataObserver(observer)
                } catch (_: IllegalStateException) {
                }
            }

            // Wait for items to be displayed
            binding.chatMessagesList
                .viewTreeObserver
                .addOnGlobalLayoutListener(globalLayoutLayout)
        } else {
            Log.e(
                "[Chat Room] Fragment resuming but viewModel lateinit property isn't initialized!"
            )
        }

        (requireActivity() as MainActivity).addKeyboardVisibilityListener(
            keyboardVisibilityListener
        )
    }

    override fun onPause() {
        if (::chatScrollListener.isInitialized) {
            binding.chatMessagesList.removeOnScrollListener(chatScrollListener)
        }

        binding.chatMessagesList
            .viewTreeObserver.removeOnGlobalLayoutListener(globalLayoutLayout)

        if (_adapter != null) {
            try {
                adapter.unregisterAdapterDataObserver(observer)
            } catch (_: IllegalStateException) {
            }
        }

        // Conversation isn't visible anymore, any new message received in it will trigger a notification
        coreContext.notificationsManager.currentlyDisplayedChatRoomAddress = null

        (requireActivity() as MainActivity).removeKeyboardVisibilityListener(
            keyboardVisibilityListener
        )

        super.onPause()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        chatSendingViewModel.attachFilePending.value = false
        if (resultCode == Activity.RESULT_OK) {
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    chatSendingViewModel.attachingFileInProgress.value = true
                    for (
                    fileToUploadPath in FileUtils.getFilesPathFromPickerIntent(
                        data,
                        chatSendingViewModel.temporaryFileUploadPath
                    )
                    ) {
                        Log.i(
                            "[Chat Room] Found fileToUploadPath:[$fileToUploadPath] file from intent"
                        )
                        Log.i(
                            "[Chat Room] Found temporaryFileUploadPath:[${chatSendingViewModel.temporaryFileUploadPath}] file from intent"
                        )
                        chatSendingViewModel.addAttachment(fileToUploadPath)
                    }
                    tempIntentSave = data
                    chatSendingViewModel.attachingFileInProgress.value = false
                    chatSendingViewModel.isUploading.value = true
                }
            }
        }
    }

    private fun pickFile() {
        chatSendingViewModel.attachFilePending.value = true
        val intentsList = ArrayList<Intent>()

        val pickerIntent = Intent(Intent.ACTION_GET_CONTENT)
        pickerIntent.type = "*/*"
        pickerIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

        if (PermissionHelper.get().hasCameraPermission()) {
            // Allows to capture directly from the camera
            val capturePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            val tempFileName = "Camera_" + System.currentTimeMillis().toString() + ".jpeg"
            val file = FileUtils.getFileStoragePath(tempFileName)
            chatSendingViewModel.temporaryFileUploadPath = file
            try {
                val publicUri = FileProvider.getUriForFile(
                    requireContext(),
                    requireContext().getString(R.string.file_provider),
                    file
                )
                capturePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, publicUri)
                capturePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                capturePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                intentsList.add(capturePictureIntent)

                val captureVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                intentsList.add(captureVideoIntent)
            } catch (e: Exception) {
                Log.e("[Chat Room] Failed to pick file: $e")
            }
        }

        val chooserIntent =
            Intent.createChooser(
                pickerIntent,
                getString(R.string.chat_message_pick_file_dialog)
            )
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            intentsList.toArray(arrayOf<Parcelable>())
        )

        startActivityForResult(chooserIntent, 0)
    }

    private fun enterEditionMode() {
        listSelectionViewModel.isEditionEnabled.value = true
    }

    private fun showParticipantsDevices() {
        if (corePreferences.limeSecurityPopupEnabled) {
            val dialogViewModel =
                DialogViewModel(getString(R.string.dialog_lime_security_message))
            dialogViewModel.showDoNotAskAgain = true
            val dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

            dialogViewModel.showCancelButton { doNotAskAgain ->
                if (doNotAskAgain) corePreferences.limeSecurityPopupEnabled = false
                dialog.dismiss()
            }

            val okLabel = if (viewModel.oneParticipantOneDevice) {
                getString(R.string.dialog_call)
            } else {
                getString(
                    R.string.dialog_ok
                )
            }
            dialogViewModel.showOkButton(
                { doNotAskAgain ->
                    if (doNotAskAgain) corePreferences.limeSecurityPopupEnabled = false

                    val address = viewModel.onlyParticipantOnlyDeviceAddress
                    if (viewModel.oneParticipantOneDevice) {
                        if (address != null) {
                            coreContext.startCall(address, forceZRTP = true)
                        }
                    } else {
                        navigateToDevices()
                    }

                    dialog.dismiss()
                },
                okLabel
            )

            dialog.show()
        } else {
            val address = viewModel.onlyParticipantOnlyDeviceAddress
            if (viewModel.oneParticipantOneDevice) {
                if (address != null) {
                    coreContext.startCall(address, forceZRTP = true)
                }
            } else {
                navigateToDevices()
            }
        }
    }

    private fun showGroupInfo(chatRoom: ChatRoom) {
        sharedViewModel.selectedGroupChatRoom.value = chatRoom
        sharedViewModel.chatRoomParticipants.value = arrayListOf()
        navigateToGroupInfo()
    }

    private fun showEphemeralMessages() {
        navigateToEphemeralInfo()
    }

    private fun scheduleMeeting(chatRoom: ChatRoom) {
        val participants = arrayListOf<Address>()
        for (participant in chatRoom.participants) {
            participants.add(participant.address)
        }
        sharedViewModel.participantsListForNextScheduledMeeting.value = Event(participants)
        navigateToConferenceScheduling()
    }

    private fun showForwardConfirmationDialog(chatMessage: ChatMessage) {
        val viewModel = DialogViewModel(
            getString(R.string.chat_message_forward_confirmation_dialog)
        )
        viewModel.iconResource = R.drawable.forward_message_default
        viewModel.showIcon = true
        val dialog: Dialog = DialogUtils.getDialog(requireContext(), viewModel)

        viewModel.showCancelButton {
            Log.i("[Chat Room] Transfer cancelled")
            dialog.dismiss()
        }

        viewModel.showOkButton(
            {
                Log.i("[Chat Room] Transfer confirmed")
                chatSendingViewModel.transferMessage(chatMessage)
                dialog.dismiss()
            },
            getString(R.string.chat_message_context_menu_forward)
        )

        dialog.show()
    }

    private fun showPopupMenu(chatRoom: ChatRoom) {
        val popupView: ChatRoomMenuBindingImpl = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.chat_room_menu,
            null,
            false
        )
        val readOnly = chatRoom.isReadOnly
        popupView.ephemeralEnabled = !readOnly
        popupView.devicesEnabled = !readOnly
        popupView.meetingEnabled = !readOnly

        val itemSize = AppUtils.getDimension(R.dimen.chat_room_popup_item_height).toInt()
        var totalSize = itemSize * 8

        val notificationsTurnedOff = viewModel.areNotificationsMuted()
        if (notificationsTurnedOff) {
            popupView.muteHidden = true
            totalSize -= itemSize
        } else {
            popupView.unmuteHidden = true
            totalSize -= itemSize
        }

        if (viewModel.basicChatRoom || viewModel.oneToOneChatRoom) {
            if (viewModel.contact.value != null) {
                popupView.addToContactsHidden = false
            } else {
                popupView.goToContactHidden = false

                if (corePreferences.readOnlyNativeContacts) {
                    popupView.addToContactsHidden = false
                    totalSize -= itemSize
                }
            }

            popupView.meetingHidden = true
            totalSize -= itemSize
        } else {
            popupView.addToContactsHidden = false
            popupView.goToContactHidden = false
            totalSize -= itemSize
        }

        if (viewModel.basicChatRoom) {
            popupView.groupInfoHidden = true
            totalSize -= itemSize
            popupView.devicesHidden = true
            totalSize -= itemSize
            popupView.ephemeralHidden = true
            totalSize -= itemSize
        } else {
            if (!viewModel.encryptedChatRoom) {
                popupView.devicesHidden = true
                totalSize -= itemSize
                popupView.ephemeralHidden = true
                totalSize -= itemSize
            } else {
                if (viewModel.oneToOneChatRoom) {
                    popupView.groupInfoHidden = true
                    totalSize -= itemSize
                }

                // If one participant one device, a click on security badge
                // will directly start a call or show the dialog, so don't show this menu
                if (viewModel.oneParticipantOneDevice) {
                    popupView.devicesHidden = true
                    totalSize -= itemSize
                }

                if (viewModel.ephemeralChatRoom) {
                    if (chatRoom.currentParams.ephemeralMode == ChatRoom.EphemeralMode.AdminManaged) {
                        if (chatRoom.me?.isAdmin == false) {
                            Log.w(
                                "[Chat Room] Hiding ephemeral menu as mode is admin managed and we aren't admin"
                            )
                            popupView.ephemeralHidden = true
                            totalSize -= itemSize
                        }
                    }
                }
            }
        }

        // When using WRAP_CONTENT instead of real size, fails to place the
        // popup window above if not enough space is available below
        val popupWindow = PopupWindow(
            popupView.root,
            AppUtils.getDimension(R.dimen.chat_room_popup_width).toInt(),
            totalSize,
            true
        )
        // Elevation is for showing a shadow around the popup
        popupWindow.elevation = 20f

        popupView.setGroupInfoListener {
            showGroupInfo(chatRoom)
            popupWindow.dismiss()
        }
        popupView.setDevicesListener {
            showParticipantsDevices()
            popupWindow.dismiss()
        }
        popupView.setEphemeralListener {
            showEphemeralMessages()
            popupWindow.dismiss()
        }
        popupView.setMeetingListener {
            scheduleMeeting(chatRoom)
            popupWindow.dismiss()
        }
        popupView.setEditionModeListener {
            enterEditionMode()
            popupWindow.dismiss()
        }
        popupView.setMuteListener {
            viewModel.muteNotifications(true)
            sharedViewModel.refreshChatRoomInListEvent.value = Event(true)
            popupWindow.dismiss()
        }
        popupView.setUnmuteListener {
            viewModel.muteNotifications(false)
            sharedViewModel.refreshChatRoomInListEvent.value = Event(true)
            popupWindow.dismiss()
        }
        popupView.setAddToContactsListener {
            popupWindow.dismiss()
            val copy = viewModel.getRemoteAddress()?.clone()
            if (copy != null) {
                copy.clean()
                val address = copy.asStringUriOnly()
                Log.i("[Chat Room] Creating contact with SIP URI: $address")
                navigateToContacts(address)
            }
        }
        popupView.setGoToContactListener {
            popupWindow.dismiss()
            val contactId = viewModel.contact.value?.refKey
            if (contactId != null) {
                Log.i("[Chat Room] Displaying native contact [$contactId]")
                navigateToNativeContact(contactId)
            } else {
                val copy = viewModel.getRemoteAddress()?.clone()
                if (copy != null) {
                    copy.clean()
                    val address = copy.asStringUriOnly()
                    Log.i("[Chat Room] Displaying friend with address [$address]")
                    navigateToFriend(address)
                }
            }
        }

        popupWindow.showAsDropDown(binding.menu, 0, 0, Gravity.BOTTOM)
    }

    private fun addDeleteMessageTaskToQueue(eventLog: EventLogData, position: Int) {
        val task = lifecycleScope.launch {
            delay(2800) // Duration of Snackbar.LENGTH_LONG
            withContext(Dispatchers.Main) {
                if (isActive) {
                    Log.i("[Chat Room] Message/event deletion task is still active, proceed")
                    val chatMessage = eventLog.eventLog.chatMessage
                    if (chatMessage != null) {
                        Log.i("[Chat Room] Deleting message $chatMessage at position $position")
                        listViewModel.deleteMessage(chatMessage)
                    } else {
                        Log.i("[Chat Room] Deleting event $eventLog at position $position")
                        listViewModel.deleteEventLogs(arrayListOf(eventLog))
                    }
                    sharedViewModel.refreshChatRoomInListEvent.value = Event(true)
                }
            }
        }

        (requireActivity() as MainActivity).showSnackBar(
            R.string.chat_message_removal_info,
            R.string.chat_message_abort_removal
        ) {
            Log.i(
                "[Chat Room] Canceled message/event deletion task: $task for message/event at position $position"
            )
            adapter.notifyItemRangeChanged(position, adapter.itemCount - position)
            task.cancel()
        }
    }

    private fun scrollToFirstUnreadMessageOrBottom(smooth: Boolean): Boolean {
        if (_adapter != null && adapter.itemCount > 0) {
            val recyclerView = binding.chatMessagesList

            // Scroll to first unread message if any, unless we are already on it
            val firstUnreadMessagePosition = adapter.getFirstUnreadMessagePosition()
            val currentPosition =
                (recyclerView.layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            val indexToScrollTo =
                if (firstUnreadMessagePosition != -1 && firstUnreadMessagePosition != currentPosition) {
                    firstUnreadMessagePosition
                } else {
                    adapter.itemCount - 1
                }

            Log.i(
                "[Chat Room] Scrolling to position $indexToScrollTo, first unread message is at $firstUnreadMessagePosition"
            )
            scrollTo(indexToScrollTo, smooth)

            if (firstUnreadMessagePosition == 0) {
                // Return true only if all unread messages don't fit in the recyclerview height
                return recyclerView.computeVerticalScrollRange() > recyclerView.height
            }
        }
        return false
    }

    private fun showDialogToSuggestOpeningFileAsText() {
        val dialogViewModel = DialogViewModel(
            getString(R.string.dialog_try_open_file_as_text_body),
            getString(R.string.dialog_try_open_file_as_text_title)
        )
        val dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

        dialogViewModel.showCancelButton {
            dialog.dismiss()
        }

        dialogViewModel.showOkButton({
            dialog.dismiss()
            navigateToTextFileViewer(true)
        })

        dialog.show()
    }

    private fun showDialogForUserConsentBeforeExportingFileInThirdPartyApp(content: Content) {
        val dialogViewModel = DialogViewModel(
            getString(R.string.chat_message_cant_open_file_in_app_dialog_message),
            getString(R.string.chat_message_cant_open_file_in_app_dialog_title)
        )
        val dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

        dialogViewModel.showDeleteButton(
            {
                dialog.dismiss()
                lifecycleScope.launch {
                    Log.i(
                        "[Chat Room] [VFS] Content is encrypted, requesting plain file path for file [${content.filePath}]"
                    )
                    val plainFilePath = content.exportPlainFile()
                    if (!FileUtils.openFileInThirdPartyApp(requireActivity(), plainFilePath)) {
                        showDialogToSuggestOpeningFileAsText()
                    }
                }
            },
            getString(R.string.chat_message_cant_open_file_in_app_dialog_export_button)
        )

        dialogViewModel.showOkButton(
            {
                dialog.dismiss()
                navigateToTextFileViewer(true)
            },
            getString(R.string.chat_message_cant_open_file_in_app_dialog_open_as_text_button)
        )

        dialogViewModel.showCancelButton {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showGroupCallDialog() {
        val dialogViewModel = DialogViewModel(
            getString(R.string.conference_start_group_call_dialog_message),
            getString(R.string.conference_start_group_call_dialog_title)
        )
        val dialog: Dialog = DialogUtils.getDialog(requireContext(), dialogViewModel)

        dialogViewModel.iconResource = R.drawable.icon_video_conf_incoming
        dialogViewModel.showIcon = true

        dialogViewModel.showCancelButton {
            dialog.dismiss()
        }

        dialogViewModel.showOkButton(
            {
                dialog.dismiss()
                viewModel.startGroupCall()
            },
            getString(R.string.conference_start_group_call_dialog_ok_button)
        )

        dialog.show()
    }

    private fun scrollTo(position: Int, smooth: Boolean = true) {
        try {
            if (smooth && corePreferences.enableAnimations) {
                binding.chatMessagesList.smoothScrollToPosition(position)
            } else {
                binding.chatMessagesList.scrollToPosition(position)
            }
        } catch (iae: IllegalArgumentException) {
            Log.e("[Chat Room] Can't scroll to position $position")
        }
    }

    // Assuming you have a ChatRoom instance called chatRoom
// and the message ID you want to reply to
    fun replyToMessage(chatRoom: ChatRoom, messageIdToReply: String) {
        Log.i("[Chat Room]", "replyToMessage: $messageIdToReply")
        val originalMessage = chatRoom.findMessage(messageIdToReply)

        originalMessage?.let { message ->
            // Create a reply message
            val replyMessage = chatRoom.createReplyMessage(message)

            // Get the original message's text content
            val originalText = message.textContent

            // Get file information if it exists
            val fileDetails = message.contents
                .filter { it.isFile || it.isFileTransfer }
                .map {
                    mapOf(
                        "name" to it.name,
                        "type" to it.type,
                        "path" to it.filePath,
                        "size" to it.fileSize
                    )
                }

            // Build reply text including original content
            val replyText = buildString {
                append("Replying to your message: ")
                if (originalText != null) {
                    append(originalText.take(50)) // Show first 50 chars of original
                    if (originalText.length > 50) append("...")
                }
                if (fileDetails.isNotEmpty()) {
                    append("\n\nAttachments:")
                    fileDetails.forEach { file ->
                        append("\n- ${file["name"]} (${file["type"]})")
                    }
                }
            }

            // Set the reply text
            //  replyMessage.textContent = replyText
            replyMessage.addUtf8TextContent(replyText)

            // Optionally attach a new file if needed
            /*
            val newFileContent = Factory.instance().createContent().apply {
                type = "image/jpeg"
                name = "reply_image.jpg"
                filePath = "/path/to/new_image.jpg"
            }
            replyMessage.addContent(newFileContent)
            */

            // Send the reply
            replyMessage.send()

            // Log details
            Log.i("[Chat Room]", "Sent reply to message $messageIdToReply")
            Log.i("[Chat Room]", "Original text: ${originalText?.take(20)}...")
            Log.i("[Chat Room]", "Attachments: ${fileDetails.size}")
        } ?: run {
            Log.i("[Chat Room]", "Message $messageIdToReply not found")
        }
    }

    private fun replyToChatMessage(chatMessage: ChatMessage) {
        Log.i(
            "[Chat Room]",
            " replyToChatMessage ${chatMessage.utf8Text}"
        )
        if (!chatMessage.contents[0].name.isNullOrEmpty()) {
            binding.footer.message.requestFocus()
            val textToInsert = "${chatMessage.contents[0].name}\n\n"
            chatSendingViewModel.textToSend.value = "$textToInsert[messageId:${chatMessage.messageId}]"
            chatMessage.addUtf8TextContent(textToInsert)
            // Move cursor to the next line after text is updated
            binding.footer.message.post {
                binding.footer.message.setSelection(binding.footer.message.text?.length ?: 0)
            }
            Log.i(
                "[Chat Room]",
                "if is file -> Reply:userData---- $textToInsert"
            )
        } else {
            var textToInsert = "${ chatMessage.utf8Text }\n"

            chatSendingViewModel.textToSend.value = "$textToInsert[messageId:${chatMessage.messageId}]"
            Log.i(
                "[Chat Room]",
                "if is text -> Reply:textToInsert---- $textToInsert"
            )
            binding.footer.message.post {
                binding.footer.message.setSelection(binding.footer.message.text?.length ?: 0)
            }
            binding.footer.message.requestFocus()
        }

        Log.i("[Chat Room]", "reply: ${chatMessage.contents.get(0).filePath}")

        chatSendingViewModel.pendingChatMessageToReplyTo.value?.destroy()
        chatSendingViewModel.pendingChatMessageToReplyTo.value = ChatMessageData(chatMessage)
        chatSendingViewModel.isPendingAnswer.value = true

        if (chatSendingViewModel.sendMessageEnabled.value == false) {
            // Open keyboard
            binding.footer.message.requestFocus()
            (requireActivity() as MainActivity).showKeyboard()
        }
    }
}

/*

    // Optional: Save the image locally
    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = context?.contentResolver?.openInputStream(uri)
            val file = File(
                context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "${remoteNum}_${System.currentTimeMillis()}_image.jpg"
            )
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            outputStream.close()
            inputStream?.close()
            file.absolutePath
        } catch (e: Exception) {
            Log.e("[Chat Room]", "Error saving image to internal storage: ${e.message}", e)
            null
        }
    }

    private fun uploadImage(uri: Uri) {
        // Launching a coroutine on the I/O dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert URI to a byte array
                val inputStream = context?.contentResolver?.openInputStream(uri)
                val imageBytes = inputStream?.readBytes()
                inputStream?.close()

                if (imageBytes != null) {
                    // Convert image bytes to Base64
                    val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
                    Log.i("[Chat Room]", "Image encoded to Base64 successfully")
                    Log.i("[Chat Room]", "base64Image - $base64Image")

                    // Metadata for file upload
                    val fileName = uri.path?.substringAfterLast("/").toString() + ".jpg"
                    val subscriberId = remoteNum
                    val fileType = "1"

                    // Call the SOAP web service
                    val resp = uploadImageToServer(
                        fileName,
                        base64Image,
                        true, // Beginning of File
                        true, // End of File
                        subscriberId,
                        fileType
                    )
                    val response = resp?.getProperty(0).toString()
                    val url = resp?.getProperty(1).toString()
                    Log.i("[Chat Room]", "Upload successful: $response")
                    Log.i("[Chat Room]", "Upload successful: $url")

                    // Handle the response on the main thread
                    withContext(Dispatchers.Main) {
                        if (response != null) {
                            Log.i("[Chat Room]", "Upload successful: $response")
                            binding.footer.message.setText(url)
                            chatSendingViewModel.addAttachment1(fileName, url)
                        } else {
                            Log.e("[Chat Room]", "File upload failed")
                        }
                    }
                } else {
                    Log.e("[Chat Room]", "Failed to read image bytes")
                }
            } catch (e: Exception) {
                Log.e("[Chat Room]", "Error during image upload: ${e.message}", e)
            }
        }
    }

cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val imageBitmap = result.data?.extras?.get("data") as? Bitmap
        if (imageBitmap != null) {
            Log.i("[Chat Room] Captured image: $imageBitmap")
            // Handle the captured image
        }
    } else {
        Log.w("[Chat Room] Camera action canceled")
    }
}


galleryLauncher = registerForActivityResult(
ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val imageUri = result.data?.data
        Log.i("[Chat Room] Selected image URI: $imageUri")
        // Handle the selected image
        if (imageUri != null) {
            // Convert URI to Bitmap (optional)
            val bitmap = getBitmapFromUri(imageUri)
            if (bitmap != null) {
                Log.i("[Chat Room]", "Image converted to Bitmap successfully")
                // Display the image in an ImageView
                // binding.imageViewPreview.setImageBitmap(bitmap)
            }

            // Optional: Save image locally or upload to a server
            val imagePath = saveImageToInternalStorage(imageUri)
            Log.i("[Chat Room] Image saved to: $imagePath")

            // Or handle the URI directly if sending to a server
            uploadImage(imageUri)
        } else {
            Log.e("[Chat Room]", "Image URI is null")
        }
    }
}

fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val fileUri = result.data?.data
        Log.i("[Chat Room] Selected file URI: $fileUri")
        // Handle the selected file
    }
}

videoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == Activity.RESULT_OK) {
        val videoUri = result.data?.data
        Log.i("[Chat Room] Selected video URI: $videoUri")
        // Handle the selected video
    }
}*/
