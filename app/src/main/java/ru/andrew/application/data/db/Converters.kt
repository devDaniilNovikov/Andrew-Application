package ru.andrew.application.data.db

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.domain.ActionType

/**
 * Конвертеры типов Room для сериализации сложных объектов (дат и перечислений) в SQLite.
 */
class Converters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    @TypeConverter
    fun fromTimestamp(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, formatter) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): String? {
        return date?.format(formatter)
    }

    @TypeConverter
    fun fromStatus(value: String?): RequestStatus? {
        return value?.let { RequestStatus.valueOf(it) }
    }

    @TypeConverter
    fun statusToString(status: RequestStatus?): String? {
        return status?.name
    }

    @TypeConverter
    fun fromEquipmentType(value: String?): EquipmentType? {
        return value?.let { EquipmentType.valueOf(it) }
    }

    @TypeConverter
    fun equipmentTypeToString(type: EquipmentType?): String? {
        return type?.name
    }

    @TypeConverter
    fun fromActionType(value: String?): ActionType? {
        return value?.let { ActionType.valueOf(it) }
    }

    @TypeConverter
    fun actionTypeToString(type: ActionType?): String? {
        return type?.name
    }
}
