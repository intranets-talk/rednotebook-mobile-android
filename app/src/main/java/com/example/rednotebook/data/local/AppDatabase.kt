package com.example.rednotebook.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@TypeConverters(Converters::class)
@Database(
    entities = [EntryEntity::class, PendingOpEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
    abstract fun pendingOpDao(): PendingOpDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rednotebook.db"
                ).build().also { INSTANCE = it }
            }
    }
}

class Converters {
    @TypeConverter fun fromOpType(value: OpType): String = value.name
    @TypeConverter fun toOpType(value: String): OpType = OpType.valueOf(value)
}
