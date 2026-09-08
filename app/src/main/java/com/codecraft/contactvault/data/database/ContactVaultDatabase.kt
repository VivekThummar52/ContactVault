package com.codecraft.contactvault.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.codecraft.contactvault.data.database.dao.ContactNoteDao
import com.codecraft.contactvault.data.database.dao.DeletedContactSnapshotDao
import com.codecraft.contactvault.data.database.dao.RecentlyViewedDao
import com.codecraft.contactvault.data.database.dao.TagDao
import com.codecraft.contactvault.data.database.entity.ContactNoteEntity
import com.codecraft.contactvault.data.database.entity.ContactTagCrossRef
import com.codecraft.contactvault.data.database.entity.DeletedContactSnapshotEntity
import com.codecraft.contactvault.data.database.entity.RecentlyViewedEntity
import com.codecraft.contactvault.data.database.entity.TagEntity

@Database(
    entities = [
        TagEntity::class,
        ContactTagCrossRef::class,
        ContactNoteEntity::class,
        RecentlyViewedEntity::class,
        DeletedContactSnapshotEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class ContactVaultDatabase : RoomDatabase() {

    abstract fun tagDao(): TagDao
    abstract fun contactNoteDao(): ContactNoteDao
    abstract fun recentlyViewedDao(): RecentlyViewedDao
    abstract fun deletedContactSnapshotDao(): DeletedContactSnapshotDao

    companion object {
        @Volatile
        private var INSTANCE: ContactVaultDatabase? = null

        fun getDatabase(context: Context): ContactVaultDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ContactVaultDatabase::class.java,
                    "contactvault_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
