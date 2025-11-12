package org.naminfo.activities.main.contact.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.naminfo.activities.main.contact.data.entity.SipContactEntity

@Dao
interface SipContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<SipContactEntity>)

    @Query("SELECT * FROM sip_contacts WHERE domain = :domain")
    suspend fun getContactsByDomain(domain: String): List<SipContactEntity>
}
