package com.naminfo.cdot_vc.contact

import android.database.Cursor
import android.os.Bundle
import android.provider.ContactsContract
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import kotlinx.coroutines.launch
import com.naminfo.cdot_vc.LinphoneApplication.Companion.coreContext
import com.naminfo.cdot_vc.LinphoneApplication.Companion.corePreferences
import com.naminfo.cdot_vc.R
import org.linphone.core.*
import org.linphone.core.tools.Log
import com.naminfo.cdot_vc.utils.AppUtils

private const val TAG = "[Contact-ContactLoader]"
class ContactLoader : LoaderManager.LoaderCallbacks<Cursor> {
    companion object {
        val projection = arrayOf(
            ContactsContract.Data.CONTACT_ID,
            ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ContactsContract.Data.MIMETYPE,
            ContactsContract.Contacts.STARRED,
            ContactsContract.Contacts.LOOKUP_KEY,
            "data1", // ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS, ContactsContract.CommonDataKinds.Organization.COMPANY
            ContactsContract.CommonDataKinds.Phone.TYPE,
            ContactsContract.CommonDataKinds.Phone.LABEL,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
        )
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val lastFetch = coreContext.contactsManager.latestContactFetch
        Log.i(
            TAG,
            "  onCreateLoader=> Loader created, ${if (lastFetch.isEmpty()) "first fetch" else "last fetch happened at [$lastFetch]"}"
        )
        coreContext.contactsManager.fetchInProgress.value = true

        val mimeType = ContactsContract.Data.MIMETYPE
        val mimeSelection = "$mimeType = ? OR $mimeType = ? OR $mimeType = ? OR $mimeType = ?"

        val selection = if (corePreferences.fetchContactsFromDefaultDirectory) {
            ContactsContract.Data.IN_DEFAULT_DIRECTORY + " == 1 AND ($mimeSelection)"
        } else {
            mimeSelection
        }

        val linphoneMime = "linphone"/*AppUtils.getString(R.string.linphone_address_mime_type)*/
        val selectionArgs = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
            ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE,
            linphoneMime,
            ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE
        )

        return CursorLoader(
            coreContext.context,
            ContactsContract.Data.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            ContactsContract.Data.CONTACT_ID + " ASC"
        )
    }

    override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor?) {
        if (cursor == null) {
            Log.e(TAG, " onLoadFinished=> Cursor is null!")
            return
        }
        Log.i(TAG, " onLoadFinished=> Load finished, found ${cursor.count} entries in cursor")
        coreContext.contactsManager.fetchFinished()
        /*    val core = coreContext.core
          val linphoneMime = loader.context.getString(R.string.linphone_address_mime_type)
          val preferNormalizedPhoneNumber = corePreferences.preferNormalizedPhoneNumbersFromAddressBook

          if (core.globalState == GlobalState.Shutdown || core.globalState == GlobalState.Off) {
              Log.w(TAG, " onLoadFinished=> Core is being stopped or already destroyed, abort")
              return
          }

          coreContext.lifecycleScope.launch {
              val friends = HashMap<String, Friend>()

              withContext(Dispatchers.IO) {
                  try {
                      // Cursor can be null now that we are on a different dispatcher according to Crashlytics
                      val friendsPhoneNumbers = arrayListOf<String>()
                      val friendsAddresses = arrayListOf<Address>()
                      var previousId = ""
                      while (cursor != null && !cursor.isClosed && cursor.moveToNext()) {
                          try {
                              val id: String =
                                  cursor.getString(
                                      cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID)
                                  )
                              val mime: String? =
                                  cursor.getString(
                                      cursor.getColumnIndexOrThrow(ContactsContract.Data.MIMETYPE)
                                  )

                              if (previousId.isEmpty() || previousId != id) {
                                  friendsPhoneNumbers.clear()
                                  friendsAddresses.clear()
                                  previousId = id
                              }

                              val friend = friends[id] ?: core.createFriend()
                              friend.refKey = id
                              if (friend.name.isNullOrEmpty()) {
                                  val displayName: String? =
                                      cursor.getString(
                                          cursor.getColumnIndexOrThrow(
                                              ContactsContract.Data.DISPLAY_NAME_PRIMARY
                                          )
                                      )
                                  friend.name = displayName

                                  friend.photo = Uri.withAppendedPath(
                                      ContentUris.withAppendedId(
                                          ContactsContract.Contacts.CONTENT_URI,
                                          id.toLong()
                                      ),
                                      ContactsContract.Contacts.Photo.CONTENT_DIRECTORY
                                  ).toString()

                                  val starred =
                                      cursor.getInt(
                                          cursor.getColumnIndexOrThrow(
                                              ContactsContract.Contacts.STARRED
                                          )
                                      ) == 1
                                  friend.starred = starred
                                  val lookupKey =
                                      cursor.getString(
                                          cursor.getColumnIndexOrThrow(
                                              ContactsContract.Contacts.LOOKUP_KEY
                                          )
                                      )
                                  friend.nativeUri =
                                      "${ContactsContract.Contacts.CONTENT_LOOKUP_URI}/$lookupKey"

                                  // Disable short term presence
                                  friend.isSubscribesEnabled = false
                                  friend.incSubscribePolicy = SubscribePolicy.SPDeny
                              }

                              when (mime) {
                                  ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE -> {
                                      val data1: String? =
                                          cursor.getString(
                                              cursor.getColumnIndexOrThrow(
                                                  ContactsContract.CommonDataKinds.Phone.NUMBER
                                              )
                                          )
                                      val data2: String? =
                                          cursor.getString(
                                              cursor.getColumnIndexOrThrow(
                                                  ContactsContract.CommonDataKinds.Phone.TYPE
                                              )
                                          )
                                      val data3: String? =
                                          cursor.getString(
                                              cursor.getColumnIndexOrThrow(
                                                  ContactsContract.CommonDataKinds.Phone.LABEL
                                              )
                                          )
                                      val data4: String? =
                                          cursor.getString(
                                              cursor.getColumnIndexOrThrow(
                                                  ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER
                                              )
                                          )

                                      val label =
                                          PhoneNumberUtils.addressBookLabelTypeToVcardParamString(
                                              data2?.toInt()
                                                  ?: ContactsContract.CommonDataKinds.BaseTypes.TYPE_CUSTOM,
                                              data3
                                          )

                                      val number =
                                          if (preferNormalizedPhoneNumber ||
                                              data1.isNullOrEmpty() ||
                                              !Patterns.PHONE.matcher(data1).matches()
                                          ) {
                                              data4 ?: data1
                                          } else {
                                              data1
                                          }

                                      if (number != null) {
                                          if (
                                              friendsPhoneNumbers.find {
                                                  PhoneNumberUtils.arePhoneNumberWeakEqual(
                                                      it,
                                                      number
                                                  )
                                              } == null
                                          ) {
                                              val phoneNumber = Factory.instance()
                                                  .createFriendPhoneNumber(number, label)
                                              friend.addPhoneNumberWithLabel(phoneNumber)
                                              friendsPhoneNumbers.add(number)
                                          }
                                      }
                                      Log.i(
                                          TAG,
                                          " CONTENT_ITEM_TYPE: data1=$data1, data2=$data2, data3=$data3, data4=$data4"
                                      )
                                  }
                                  linphoneMime, ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE -> {
                                      val sipAddress: String? =
                                          cursor.getString(
                                              cursor.getColumnIndexOrThrow(
                                                  ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS
                                              )
                                          )
                                      if (sipAddress != null) {
                                          val address = core.interpretUrl(sipAddress, false)
                                          if (address != null &&
                                              friendsAddresses.find {
                                                  it.weakEqual(address)
                                              } == null
                                          ) {
                                              friend.addAddress(address)
                                              friendsAddresses.add(address)
                                          }
                                      }
                                      Log.i(
                                          TAG,
                                          " SipAddress.CONTENT_ITEM_TYPE: address= $sipAddress"
                                      )
                                  }
                                  ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE -> {
                                      val organization: String? =
                                          cursor.getString(
                                              cursor.getColumnIndexOrThrow(
                                                  ContactsContract.CommonDataKinds.Organization.COMPANY
                                              )
                                          )
                                      if (organization != null) {
                                          friend.organization = organization
                                      }
                                      Log.i(
                                          TAG,
                                          " Organization.CONTENT_ITEM_TYPE: organization= $organization"
                                      )
                                  }
                                  // Our API not being thread safe this causes crashes sometimes given the Play Store reports
                                  // So these values will be fetched at the only moment they are required: contact edition
                                  *//*ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE -> {
                                    if (data2 != null && data3 != null) {
                                        val vCard = friend.vcard
                                        vCard?.givenName = data2
                                        vCard?.familyName = data3
                                    }
                                }*//*
                            }
                            Log.i(TAG, "  onLoadFinished=> name:${friend.name}")
                            friends[id] = friend
                        } catch (e: Exception) {
                            Log.e("[Contacts Loader] Exception: $e")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        if (core.globalState == GlobalState.Shutdown || core.globalState == GlobalState.Off) {
                            Log.w(
                                "[Contacts Loader] Core is being stopped or already destroyed, abort"
                            )
                        } else {
                            Log.i("[Contacts Loader] ${friends.size} friends created")
                            val contactId = coreContext.contactsManager.contactIdToWatchFor
                            if (contactId.isNotEmpty()) {
                                val friend = friends[contactId]
                                Log.i(
                                    "[Contacts Loader] Manager was asked to monitor contact id $contactId"
                                )
                                if (friend != null) {
                                    Log.i(
                                        "[Contacts Loader] Found new contact matching id $contactId, notifying listeners"
                                    )
                                    coreContext.contactsManager.notifyListeners(friend)
                                }
                            }

                            val fl = core.defaultFriendList ?: core.createFriendList()
                            for (friend in fl.friends) {
                                fl.removeFriend(friend)
                            }

                            if (fl != core.defaultFriendList) core.addFriendList(fl)

                            val friendsList = friends.values
                            for (friend in friendsList) {
                                fl.addLocalFriend(friend)
                            }

                            Log.i("[Contacts Loader] Friends added")

                            // Only update subscriptions when default account is registered or anytime if it isn't the first contacts fetch
                            if (core.defaultAccount?.state == RegistrationState.Ok || coreContext.contactsManager.latestContactFetch.isNotEmpty()) {
                                Log.i(
                                    "[Contacts Loader] Updating friend list [${fl.friends.size}] subscriptions"
                                )

                                fl.updateSubscriptions()
                            }
                            friends.clear()
                            coreContext.contactsManager.fetchFinished()
                        }
                    }
                } catch (sde: StaleDataException) {
                    Log.e(TAG, "  onLoadFinished=> [Contacts Loader] State Data Exception: $sde")
                } catch (ise: IllegalStateException) {
                    Log.e(TAG, "  onLoadFinished=> [Contacts Loader] Illegal State Exception: $ise")
                } catch (e: Exception) {
                    Log.e(TAG, "  onLoadFinished=> [Contacts Loader] Exception: $e")
                } finally {
                    cancel()
                }
            }
        }*/
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        Log.i(TAG, "onLoaderReset => [Contacts Loader] Loader reset")

        // If using a RecyclerView adapter, clear the dataset
        coreContext.lifecycleScope.launch {
            coreContext.contactsManager.fetchInProgress.value = false
        }
    }
}
