package org.jetbrains.interpret

import org.jetbrains.interpret.Storage
import org.jetbrains.interpret.emitWrapper

fun Map<*, *>.toStorage(): Storage = MapStorage(this)

public class MapStorage(val map: Map<*, *>) : Storage {
    override fun get(property: String): Any {
        val value = map.get(property) ?: throw IllegalAccessException("property $property not found")
        return value
    }

    override fun set(property: String, value: Any) {
        throw UnsupportedOperationException()
    }

    override fun get(property: String, propertyType: Class<*>): Any? {
        val value = map.get(property) ?: throw IllegalAccessException("property $property not found")
        if (propertyType.isAssignableFrom(value.javaClass))
            return value
        if (value is Map<*, *>)
            return emitWrapper(propertyType).newInstance(MapStorage(value))
        else
            return null
    }

    override fun set(property: String, propertyType: Class<out Any?>, value: Any?) {
        throw UnsupportedOperationException()
    }
}
