package org.naminfo.activities.main.contact.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sip_contacts")
data class SipContactEntity(
    @PrimaryKey val mobileNumber: String,
    val name: String,
    val domain: String
)
