# Дополнительные правила для корректной работы Room DB при R8/ProGuard обфускации
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Сохраняем сущности Room и их поля
-keep class * { @androidx.room.Entity *; }

# Сохраняем DAO интерфейсы и их методы
-keep interface * { @androidx.room.Dao *; }

# Сохраняем класс базы данных
-keep class * extends androidx.room.RoomDatabase

# Предотвращаем удаление классов базы данных из-за рефлексии
-keep class androidx.room.Room
-dontwarn androidx.room.paging.**
