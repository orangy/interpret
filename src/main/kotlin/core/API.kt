package org.jetbrains.interpret

trait Storage {
    fun get(property: String): Any
    fun set(property: String, value: Any)

    fun get(property: String, propertyType: Class<*>): Any?
    fun set(property: String, propertyType: Class<*>, value: Any?)
}

fun Storage.interpretAs<T>(klass: Class<T>): T {
    return org.jetbrains.interpret.emitWrapper(klass).newInstance(this) as T
}

fun Iterable<Storage>.interpretAs<T>(klass: Class<T>): Iterable<T> {
    val wrapper = org.jetbrains.interpret.emitWrapper(klass)
    return map { wrapper.newInstance(it) as T }
}

class UnsupportedPropertyTypeException(message: String) : Exception(message)

