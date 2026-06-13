package ru.andrew.application.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.domain.ActionType

/**
 * Конвертеры типов Room для сериализации сложных объектов (дат и перечислений) в SQLite.
 */
class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let { 
            LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneOffset.UTC) 
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.toInstant(ZoneOffset.UTC)?.toEpochMilli()
    }

    @TypeConverter
    fun fromStatus(value: String?): RequestStatus? {
        return value?.let { 
            try {
                RequestStatus.valueOf(it)
            } catch (e: Exception) {
                RequestStatus.ACTIVE
            }
        }
    }

    @TypeConverter
    fun statusToString(status: RequestStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun fromEquipmentType(value: String?): EquipmentType? {
        return value?.let { 
            try {
                EquipmentType.valueOf(it)
            } catch (e: Exception) {
                EquipmentType.OTHER
            }
        }
    }

    @TypeConverter
    fun equipmentTypeToString(type: EquipmentType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromActionType(value: String?): ActionType? {
        return value?.let { 
            try {
                ActionType.valueOf(it)
            } catch (e: Exception) {
                ActionType.OTHER
            }
        }
    }

    @TypeConverter
    fun actionTypeToString(type: ActionType?): String? {
        return type?.name
    }
}
