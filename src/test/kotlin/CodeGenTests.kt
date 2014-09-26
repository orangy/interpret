package org.jetbrains.interpret.tests

import org.junit.*
import kotlin.test.*
import java.util.Date
import org.jetbrains.interpret.*
import java.util.ArrayList

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

trait Cascade {
    val numbers: Numbers
    val dates: Dates
}

class CodeGenTests {
    Test fun interpretAsStrings() {
        val entity = mapOf("name" to "value").toStorage()
        val named = entity.interpretAs(javaClass<Strings>())
        assertEquals("value", named.name)
    }

    Test fun interpretAsNumbers() {
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

    Test fun interpretAsCascade() {
        val entity = mapOf(
                "numbers" to mapOf("count" to 1, "size" to 123L),
                "dates" to mapOf("created" to Date(2014, 1, 1))
                          ).toStorage()

        val cascade = entity.interpretAs(javaClass<Cascade>())
        assertEquals(1, cascade.numbers.count)
        assertEquals(123L, cascade.numbers.size)
        assertEquals(Date(2014, 1, 1), cascade.dates.created)
    }
}