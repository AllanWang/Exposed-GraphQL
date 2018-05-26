package ca.allanwang.exposed.graphql.entity

import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLSchema
import graphql.schema.GraphQLTypeReference
import org.jetbrains.exposed.dao.EntityCache
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.transactions.transaction

class GraphQLEntityContainer(vararg entities: GraphQLEntity<*, *>) {

    val entities: Array<GraphQLEntity<*, *>>

    init {
        val map = entities.map { it.entityClass.table to it }.toMap()
        val tableOrder = EntityCache.sortTablesByReferences(map.keys)
        this.entities = tableOrder.map { map[it]!! }.toTypedArray()
    }

    fun schema() = GraphQLSchema.newSchema()
            .query(GraphQLObjectType.newObject()
                    .name("query")
                    .fields(fields())
                    .build())
            .build()

    private val typeMapper: MutableMap<EntityClass<*, *>, GraphQLObjectType> = mutableMapOf()

    /**
     * Returns the full object type of an object reference
     */
    fun type(entity: GraphQLEntity<*, *>): GraphQLOutputType {
        val existingType = typeMapper[entity.entityClass]
        if (existingType != null) return GraphQLTypeReference(existingType.name)
        val type = entity.objectTypeFactory()
        typeMapper[entity.entityClass] = type
        return type
    }

    private fun fields() = let { container ->
        transaction {
            typeMapper.clear()
            val fields = entities.map { it.field(container) }
            typeMapper.clear()
            fields
        }
    }

}