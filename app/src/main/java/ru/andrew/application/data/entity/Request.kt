package ru.andrew.application.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import java.time.LocalDateTime
import ru.andrew.application.domain.RequestStatus
import ru.andrew.application.domain.EquipmentType
import ru.andrew.application.domain.ActionType

/**
 * Сущность заявки для хранения в локальной базе данных Room.
 * Соответствует спецификации модели данных из PRD.md.
 */
@Entity(
    tableName = "requests",
    indices = [
        Index("status"),
        Index("nextActionDateTime"),
        Index("closedAt")
    ]
)
data class Request(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    
    val title: String,
    
    val clientName: String? = null,
    
    val phone: String,
    
    val address: String? = null,
    
    val equipmentType: EquipmentType? = null,
    
    val actionType: ActionType? = null,
    
    val nextActionDateTime: LocalDateTime,
    
    val comment: String? = null,
    
    val status: RequestStatus = RequestStatus.ACTIVE,
    
    val finalPrice: Double? = null,
    
    val finalComment: String? = null,
    
    val cancelReason: String? = null,
    
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    val closedAt: LocalDateTime? = null
)
