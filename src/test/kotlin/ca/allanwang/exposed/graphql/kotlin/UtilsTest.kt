package ca.allanwang.exposed.graphql.kotlin

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UtilsTest {
    @Test
    fun toCamel() {
        assertEquals("thisIsATest", "this_IS_a_TeSt".toCamel())
    }

    @Test
    fun toUnderscore() {
        assertEquals("this_is_a_test", "thisIsATest".toUnderscore())
    }
}