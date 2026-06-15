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
@Database(entities = [Request::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun requestDao(): RequestDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Создаем новую таблицу с обновленной схемой (где nextActionDateTime может быть NULL)
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `requests_new` (" +
                            "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`title` TEXT NOT NULL, " +
                            "`clientName` TEXT, " +
                            "`phone` TEXT NOT NULL, " +
                            "`address` TEXT, " +
                            "`equipmentType` TEXT, " +
                            "`actionType` TEXT, " +
                            "`nextActionDateTime` INTEGER, " +
                            "`comment` TEXT, " +
                            "`status` TEXT NOT NULL, " +
                            "`finalPrice` REAL, " +
                            "`finalComment` TEXT, " +
                            "`cancelReason` TEXT, " +
                            "`createdAt` INTEGER NOT NULL, " +
                            "`updatedAt` INTEGER NOT NULL, " +
                            "`closedAt` INTEGER)"
                )
                
                // Копируем данные
                database.execSQL(
                    "INSERT INTO `requests_new` (`id`, `title`, `clientName`, `phone`, `address`, " +
                            "`equipmentType`, `actionType`, `nextActionDateTime`, `comment`, `status`, " +
                            "`finalPrice`, `finalComment`, `cancelReason`, `createdAt`, `updatedAt`, `closedAt`) " +
                            "SELECT `id`, `title`, `clientName`, `phone`, `address`, `equipmentType`, " +
                            "`actionType`, `nextActionDateTime`, `comment`, `status`, `finalPrice`, " +
                            "`finalComment`, `cancelReason`, `createdAt`, `updatedAt`, `closedAt` FROM `requests`"
                )
                
                // Удаляем старую таблицу
                database.execSQL("DROP TABLE `requests`")
                
                // Переименовываем новую таблицу
                database.execSQL("ALTER TABLE `requests_new` RENAME TO `requests`")
                
                // Пересоздаем индексы
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_requests_status` ON `requests` (`status`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_requests_nextActionDateTime` ON `requests` (`nextActionDateTime`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_requests_closedAt` ON `requests` (`closedAt`)")
                database.execSQL("CREATE INDEX IF NOT EXISTS `index_requests_createdAt` ON `requests` (`createdAt`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "andrew_application.db"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
