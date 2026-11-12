package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProjectTaskEntity::class,
        CalendarEventEntity::class,
        WorkflowMacroEntity::class,
        CodeSnippetEntity::class,
        HfModelEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class OmniDatabase : RoomDatabase() {
    abstract fun omniDao(): OmniDao

    companion object {
        @Volatile
        private var INSTANCE: OmniDatabase? = null

        fun getDatabase(context: Context): OmniDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OmniDatabase::class.java,
                    "omni_workspace_enclave.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
