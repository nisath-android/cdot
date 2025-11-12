package org.naminfo.activities.main.contact.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import org.naminfo.activities.main.contact.data.dao.SipContactDao
import org.naminfo.activities.main.contact.data.entity.SipContactEntity

@Database(entities = [SipContactEntity::class], version = 1)
abstract class SipContactDatabase : RoomDatabase() {
    abstract fun sipContactDao(): SipContactDao
}
