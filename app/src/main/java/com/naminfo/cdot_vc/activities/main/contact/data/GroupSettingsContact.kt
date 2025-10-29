package com.naminfo.cdot_vc.activities.main.contact.data

data class GroupSettingsContact(
    var groupNumber: String? = null,
    var actionDate: String? = null,
    var id: String? = null,
    var groupName: String? = null,
    var conference: String? = null,
    var userDetails: String? = null,
    var callType: String? = null, // Nullable field for optional data
    var moderate: String? = null // Nullable field for optional data
)

data class GroupInfoContact(
    var groupUserName: String? = null,
    var groupPhoneNumber: String? = null,
    var isModerator: Boolean = false
)

data class TempGroupContact(
    var groupUserName: String? = null,
    var groupPhoneNumber: String? = null,
    var isModerator: Boolean = false,
    var isDialer: Boolean = false,
    var callTypeLocal: String? = null,
    var callTypeValue: String? = null
)

data class BroadcastContact(
    var bcNumber: String? = null,
    var bcName: String? = null,
    var userDetails: String? = null,
    var moderator: String? = null // Nullable field for optional data
)

data class BcInfoContact(
    var groupUserName: String? = null,
    var groupPhoneNumber: String? = null
)

