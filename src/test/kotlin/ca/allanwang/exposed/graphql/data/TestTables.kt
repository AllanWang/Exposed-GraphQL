package ca.allanwang.exposed.graphql.data

import ca.allanwang.exposed.graphql.entity.GraphQLAllFields
import ca.allanwang.exposed.graphql.entity.GraphQLField
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.transactions.transaction

object TestItems : IntIdTable() {

    val name = varchar("name", 64)

}

@GraphQLAllFields
class TestItemDb(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TestItemDb>(TestItems)

    @GraphQLField(name = "itemName")
    var name by TestItems.name

    @GraphQLField(itemType = String::class)
    val children by TestSubItemDb referrersOn TestSubItems.parent

    val greeting by lazy { "Hello $name" }

    override fun toString(): String = transaction {
        val sub = children.toList()
        "TestItem $name \n\tSubItems (${sub.size}) ${sub.joinToString("\n\t", prefix = "\n\t")}"
    }
}

object TestSubItems : IntIdTable() {

    val parent = reference("parent", TestItems, ReferenceOption.CASCADE)
    val name = varchar("name", 64)

}

@GraphQLAllFields
class TestSubItemDb(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<TestSubItemDb>(TestSubItems)

    var name by TestSubItems.name
    val parent by TestItemDb referencedOn TestSubItems.parent

    override fun toString(): String = transaction {
        "TestSubItem $name - parent $parent"
    }
}