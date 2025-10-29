package com.naminfo.cdot_vc.utils

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract
import java.io.*
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import org.linphone.core.tools.Log

class ContactUtils {
    companion object {
        fun getContactVcardFilePath(contactUri: Uri): String? {
            val contentResolver: ContentResolver = coreContext.context.contentResolver
            val lookupUri = ContactsContract.Contacts.getLookupUri(contentResolver, contactUri)
            Log.i("[Contact Utils] Contact lookup URI is $lookupUri")

            val contactID = FileUtils.getNameFromFilePath(lookupUri.toString())
            Log.i("[Contact Utils] Contact ID is $contactID")

            val contact = coreContext.contactsManager.findContactById(contactID)
            if (contact == null) {
                Log.e("[Contact Utils] Failed to find contact with ID $contactID")
                return null
            }

            val vcard = contact.vcard?.asVcard4String()
            if (vcard == null) {
                Log.e("[Contact Utils] Failed to get vCard from contact $contactID")
                return null
            }

            val contactName = contact.name?.replace(" ", "_") ?: contactID
            val vcardPath = FileUtils.getFileStoragePath("$contactName.vcf")
            val inputStream = ByteArrayInputStream(vcard.toByteArray())
            try {
                FileOutputStream(vcardPath).use { out ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } >= 0) {
                        out.write(buffer, 0, bytesRead)
                    }
                }
            } catch (e: IOException) {
                Log.e("[Contact Utils] creating vcard file exception: $e")
                return null
            }

            Log.i("[Contact Utils] Contact vCard path is $vcardPath")
            return vcardPath.absolutePath
        }
    }
}
