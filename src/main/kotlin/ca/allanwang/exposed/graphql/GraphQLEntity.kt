package ca.allanwang.exposed.graphql

import ca.allanwang.exposed.graphql.utils.graphQLFieldDefinition
import ca.allanwang.exposed.graphql.utils.graphQLObjectType
import ca.allanwang.exposed.graphql.utils.outputType
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import graphql.schema.GraphQLOutputType
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.Reference
import org.jetbrains.exposed.sql.Column

interface GraphQLEntity<ID : Comparable<ID>, T : Entity<ID>> {

    val graphQLFields: List<GraphQLField<T>>

    fun fetch(request: GraphQLRequest): Map<String, Any?>

    fun request(env: DataFetchingEnvironment): GraphQLRequest?

    fun objectTypeFactory(): GraphQLObjectType

    fun <T> Column<T>.graphQL(name: String, description: String? = "Get $name from ${table.tableName}"): Column<T>
    fun <ID : Comparable<ID>, Target : Entity<ID>> Reference<ID, Target>.graphQL(
            name: String, description: String? = "Get $name from ${reference.table.tableName}"): Reference<ID, Target>

}

class GraphQLEntityHolder<ID : Comparable<ID>, T : Entity<ID>>(val name: String) : GraphQLEntity<ID, T> {

    override val graphQLFields: MutableList<GraphQLField<T>> = mutableListOf()

    override fun fetch(request: GraphQLRequest): Map<String, Any?> {
        if (request.selections.isEmpty()) return emptyMap()
        return emptyMap() // todo
    }

    override fun request(env: DataFetchingEnvironment): GraphQLRequest? {
        val field = env.fields.firstOrNull { it.name == name } ?: return null
        return GraphQLRequest(env.getContext(), field)
    }

    override fun objectTypeFactory(): GraphQLObjectType = graphQLObjectType {
        name(name)
        description("SQL access to $name")
        fields(graphQLFields.map { it.graphQLField() })
    }

    override fun <T> Column<T>.graphQL(name: String, description: String?): Column<T> = apply {
        graphQLFields.add(GraphQLField(name, outputType(), { it.run { this@graphQL.lookup() } }, description))
    }

    override fun <ID : Comparable<ID>, Target : Entity<ID>> Reference<ID, Target>.graphQL(
            name: String, description: String?): Reference<ID, Target> = apply {

    }

}

class GraphQLField<T>(val name: String, val type: GraphQLOutputType, val getter: (T) -> Any?, val description: String? = null) {
    fun graphQLField(): GraphQLFieldDefinition = graphQLFieldDefinition {
        name(name)
        description(description)
        type(type)
    }
}


