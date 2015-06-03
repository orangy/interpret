package org.jetbrains.interpret

interface Storage {
    fun get(property: String): Any
    fun set(property: String, value: Any)

    fun get(property: String, propertyType: Class<*>): Any?
    fun set(property: String, propertyType: Class<*>, value: Any?)
}

fun Storage.interpretAs<T>(klass: Class<T>): T {
    return emitWrapper(klass).newInstance(this) as T
}

fun Iterable<Storage>.interpretAs<T>(klass: Class<T>): Iterable<T> {
    val wrapper = emitWrapper(klass)
    return map { wrapper.newInstance(it) as T }
}

class UnsupportedPropertyTypeException(message: String) : Exception(message)

