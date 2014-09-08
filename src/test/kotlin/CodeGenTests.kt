package org.jetbrains.interpret.tests

import org.junit.*
import kotlin.test.*
import java.util.Date
import org.jetbrains.interpret.*

trait Strings {
    var name: String
}

trait Numbers {
    val count: Int
    var size: Long
    val percent: Double
}

trait Logic {
    var yesno: Boolean
}

trait Dates {
    var created: Date
}


fun Map<String, Any>.toStorage(): Storage {
    val map = this
    return object : Storage {
        override fun get(property: String): Any {
            if (property !in map)
                throw IllegalAccessException("property $property not found")
            return map.get(property)!!
        }
        override fun set(property: String, value: Any) {
            throw UnsupportedOperationException()
        }
        override fun get(property: String, propertyType: Class<out Any?>): Any {
            throw UnsupportedOperationException()
        }
        override fun set(property: String, propertyType: Class<out Any?>, value: Any) {
            throw UnsupportedOperationException()
        }
    }
}

class CodeGenTests {
    Test fun given_database_and_interface_with_string_on_load_should_create_class() {
        val entity = mapOf("name" to "value").toStorage()
        val named = entity.interpretAs(javaClass<Strings>())
        assertEquals("value", named.name)
    }

    Test fun given_database_and_interface_with_numbers_on_load_should_create_class() {
        val entity = mapOf(
                "count" to 12,
                "size" to 42L,
                "percent" to 99.9
                          ).toStorage()

        val numbers = entity.interpretAs(javaClass<Numbers>())
        assertEquals(12, numbers.count)
        assertEquals(42L, numbers.size)
        assertEquals(99.9, numbers.percent)
    }
}