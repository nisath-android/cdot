package org.naminfo.activities.main.contact.data

import kotlinx.coroutines.CoroutineScope

data class PbxContact(
    var name: String? = null,
    var country: String? = null,
    var extensionNumber: String? = null

)

data class PBXContactsTable(
    var First_Name: String? = null,
    var Last_Name: String? = null,
    var Subscriber_ID: String? = null,
    var Country_Code: String? = null,
    var Mobile_No: String? = null,
    var Subscriber_Password: String? = null,
    var Res_No: String? = null,
    var Email: String? = null,
    var Company_Name: String? = null,
    var Addr_1: String? = null,
    var City: String? = null,
    var State: String? = null,
    var Country: String? = null,
    var Designation: String? = null,
    var Area_Code: String? = null,
    var Extension_No: String? = null,
    var GroupID: String? = null,
    var SMSSent: Boolean = false,
    var profile: String? = null,
    var PBX: String? = null,
    var status: Int? = null,
    var timezone: String? = null,
    var DayLightSaving: Boolean = false,
    var IPPhoneno: String? = null,
    var Mobileuser: Boolean = false,
    var Landlineuser: Boolean = false,
    var Ipphoneuser: Boolean = false,
    var Mobionuser: Boolean = false,
    var Action_date: String? = null,
    var Id: Int? = null,
    val coroutineScope: CoroutineScope? = null
)
