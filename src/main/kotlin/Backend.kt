package org.jetbrains.interpret

abstract class EntityBackend(val storage: Storage)

class EntityBackendProperty<T>(val propertyType: Class<T>) {
    fun get(backend: EntityBackend, property: String): T {
        val value: Any? = when (propertyType.getSimpleName()) {
            "Integer" -> backend.storage.get(property)
            "Long" -> backend.storage.get(property)
            "String" -> backend.storage.get(property)
            "Short" -> backend.storage.get(property)
            "Double" -> backend.storage.get(property)
            "Byte" -> backend.storage.get(property)
            "Character" -> backend.storage.get(property)
            "Boolean" -> backend.storage.get(property)
            else -> backend.storage.get(property, propertyType)
        }
        return value as T
    }

    fun set(backend: EntityBackend, property: String, value: T) {
        return when (propertyType.getSimpleName()) {
            "Integer" -> backend.storage.set(property, value as Int)
            "Long" -> backend.storage.set(property, value as Long)
            "Double" -> backend.storage.set(property, value as Double)
            "Byte" -> backend.storage.set(property, value as Byte)
            "String" -> backend.storage.set(property, value as String)
            "Short" -> backend.storage.set(property, value as Short)
            "Character" -> backend.storage.set(property, value as Char)
            "Boolean" -> backend.storage.set(property, value as Boolean)
            else -> backend.storage.set(property, propertyType, value)
        }
    }
}
