package ca.allanwang.exposed.graphql.entity

import ca.allanwang.exposed.graphql.data.TestItemDb
import ca.allanwang.exposed.graphql.data.TestSubItemDb
import ca.allanwang.exposed.graphql.data.TestSubItems
import graphql.Scalars
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

object TestEntityTable : IntIdTable() {

    val name = ca.allanwang.exposed.graphql.data.TestItems.varchar("name", 64)

}

@GraphQLAllFields
class TestEntityDb(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TestItemDb>(TestEntityTable)

    @GraphQLField(name = "itemName")
    var name by TestEntityTable.name

    @GraphQLField(itemType = String::class)
    val children by TestSubItemDb referrersOn TestSubItems.parent

    val greeting: String? by lazy { "Hello $name" }

    @GraphQLFieldIgnore
    var goodBye = "Bye"

    override fun toString(): String = transaction {
        val sub = children.toList()
        "TestItem $name \n\tSubItems (${sub.size}) ${sub.joinToString("\n\t", prefix = "\n\t")}"
    }
}

class GraphQLEntityTest {

    @Test
    fun `get fields`() {
        val graphQLEntity = GraphQLEntity("testItem", TestEntityDb, returnsList = false, allFieldsByDefault = true)
        val fields = graphQLEntity.fields.map { it.name to it }.toMap()

        assertEquals(3, fields.size, "@GraphQLFieldIgnore not working")

        assertEquals(GraphQLNonNull(Scalars.GraphQLString), fields["itemName"]?.type)
        assertEquals(GraphQLNonNull(GraphQLList(Scalars.GraphQLString)), fields["children"]?.type)
        assertEquals(Scalars.GraphQLString, fields["greeting"]?.type)

        println(graphQLEntity.toDataString())
    }

}