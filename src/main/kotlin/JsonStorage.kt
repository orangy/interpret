package org.jetbrains.interpret

import net.minidev.json.*

fun jsonStorage(text : String) : JsonStorage {
    val parsed = JSONValue.parseWithException(text)
    return JsonStorage(parsed as JSONObject)
}

public class JsonStorage(val map: JSONObject) : Storage {
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
        if (value is JSONObject)
            return emitWrapper(propertyType).newInstance(JsonStorage(value))
        else
            return null
    }

    override fun set(property: String, propertyType: Class<out Any?>, value: Any?) {
        throw UnsupportedOperationException()
    }
}
