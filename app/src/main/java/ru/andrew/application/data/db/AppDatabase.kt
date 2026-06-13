package ru.andrew.application.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.andrew.application.data.dao.RequestDao
import ru.andrew.application.data.entity.Request

/**
 * Локальная база данных Room.
 * Использует конвертеры типов для сохранения LocalDateTime и enum-значений.
 */
@Database(entities = [Request::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun requestDao(): RequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "andrew_application.db"
                )
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
