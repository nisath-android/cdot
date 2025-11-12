package org.naminfo.activities.main.contact.data.database

import android.content.Context
import androidx.room.Room

object SipContactDatabaseInstance {
    @Volatile
    private var INSTANCE: SipContactDatabase? = null

    fun getDatabase(context: Context): SipContactDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                SipContactDatabase::class.java,
                "sip_contact_database"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}
