package ca.allanwang.exposed.graphql

import com.google.common.truth.Truth.assertAbout
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourceSubjectFactory.javaSource
import org.junit.jupiter.api.Test

/**
 * Created by Allan Wang on 2018-09-09.
 */
class EntityProcessorTest {

    val test = JavaFileObjects.forSourceString("test/Test",
            """
package test;

@ca.allanwang.exposed.graphql.GraphQLEntity
class Test {}
            """.trimIndent())

    @Test
    fun test() {
        assertAbout(javaSource()).that(test)
                .withCompilerOptions("-Xlint:-processing")
                .processedWith(EntityProcessor())
                .compilesWithoutWarnings()
                .and()
                .generatesSources(JavaFileObjects.forSourceLines("test/Test", ""))
    }

}