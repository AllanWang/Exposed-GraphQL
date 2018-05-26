package ca.allanwang.exposed.graphql.entity

import ca.allanwang.exposed.graphql.data.TestItemDb
import org.junit.jupiter.api.Test

class AnnotationsTest {

    @Test
    fun test() {
        val graphQLEntity = GraphQLEntity(TestItemDb)
        println(graphQLEntity.fields)
    }

}