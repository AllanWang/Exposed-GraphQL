package ca.allanwang.exposed.graphql

import ca.allanwang.exposed.graphql.utils.*
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLInputType
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLOutputType
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KClass

class GraphQLColumnArgBuilder<T>(var name: String, val column: Column<T>, val type: GraphQLInputType) {
    var mandatory = false
    var sortable = false
}

class GraphQLFieldBuilder<T, R>(private val name: String,
                                private var getter: T.(GraphQLRequest) -> R,
                                private val nullable: Boolean,
                                private val returnClass: KClass<*>) {
    var sortOption: Boolean = false
    private val graphQLType = graphQLType(returnClass)

    fun using(column: Column<R>, build: GraphQLColumnArgBuilder<R>.() -> Unit) {
        GraphQLColumnArgBuilder(name, column, graphQLType as GraphQLInputType).apply(build)
    }

    fun build() = GraphQLFieldBuild(name, getter, graphQLType.nullable(nullable) as GraphQLOutputType, sortOption)
}


data class GraphQLFieldBuild<T, R>(val name: String,
                                   val getter: T.(GraphQLRequest) -> R,
                                   val type: GraphQLOutputType,
                                   val sortOption: Boolean = false) // todo separate field from args

class GraphQLEntityBuilder<ID : Comparable<ID>, T : Entity<ID>>(val name: String, val entityClass: EntityClass<ID, T>) {

    val fields: MutableList<GraphQLFieldBuild<T, *>> = mutableListOf()

    inline fun <reified R> field(name: String, noinline getter: T.(GraphQLRequest) -> R, build: GraphQLFieldBuilder<T, R>.() -> Unit = {}) {
        val field = GraphQLFieldBuilder(name, getter, null is R, R::class).apply(build).build()
        fields.add(field)
    }

    fun <T, ID : Comparable<ID>, R : Entity<ID>> field(name: String, getter: T.(GraphQLRequest) -> R, reference: GraphQLEntityBuild<ID, R>) {
        GraphQLFieldBuild<T, Any>(name,
                { reference.getData(getter(it), it.subRequest(name) ?: fail("Could not get subrequest for $name")) },
                typeRef(reference.entityClass))
    }

    fun build() = GraphQLEntityBuild(name, entityClass, fields.toList())
}

data class GraphQLEntityBuild<ID : Comparable<ID>, T : Entity<ID>>(val name: String,
                                                                   val entityClass: EntityClass<ID, T>,
                                                                   val fields: List<GraphQLFieldBuild<T, *>>) {

    fun getData(entity: T, request: GraphQLRequest): Map<String, Any?> = transaction {
        fields.filter { it.name in request.selections }.map { it.name to it.getter(entity, request) }.toMap()
    }

//    fun getField(): GraphQLFieldDefinition {
//
//    }
}

fun <ID : Comparable<ID>, T : Entity<ID>> graphQLEntity(name: String,
                                                        entityClass: EntityClass<ID, T>,
                                                        build: GraphQLEntityBuilder<ID, T>.() -> Unit) =
        GraphQLEntityBuilder(name, entityClass).apply(build).build()


class GraphQLEntity<ID : Comparable<ID>, T : Entity<ID>>(val name: String, val entityClass: EntityClass<ID, T>) {

    val graphQLFields: MutableList<GraphQLField<T>> = mutableListOf()


//
//     fun fetch(request: GraphQLRequest): Map<String, Any?> {
//        if (request.selections.isEmpty()) return emptyMap()
//        return emptyMap() // todo
//    }
//
//     fun request(env: DataFetchingEnvironment): GraphQLRequest? {
//        val field = env.fields.firstOrNull { it.name == name } ?: return null
//        return GraphQLRequest(env.getContext(), field)
//    }
//
//     fun field(container: GraphQLEntityContainer): GraphQLFieldDefinition = graphQLFieldDefinition {
//        name(name)
////        argument(conditions.map { it.graphQLArgument() })
////        type(type(container))
////        dataFetcher {
////            val fieldEnv = toDbEnvironment(it) ?: return@dataFetcher null
////            fetch(fieldEnv)
////        }
//    }
//
//     fun objectTypeFactory(name: String): GraphQLObjectType = graphQLObjectType {
//        name(name)
//        description("SQL access to ${entityClass.table.tableName}")
//        fields(graphQLFields.map { it.graphQLField() })
//    }
//
//     fun <T> Column<T>.graphQL(name: String, description: String?) = apply {
//        graphQLFields.add(GraphQLField(name, outputType(), { it.run { this@graphQL.getValue(this, null) } }, description))
//    }
//
//     fun <ID : Comparable<ID>, Target : Entity<ID>> Reference<ID, Target>.graphQL(
//            name: String, description: String?) = apply {
//        graphQLFields.add(GraphQLField(name, reference.outputType(), { it.run { this@graphQL.getValue(this, null) } }, description))
//    }
//
//     fun <ID : Comparable<ID>, Target : Entity<ID>> OptionalReference<ID, Target>.graphQL(
//            name: String, description: String?) = apply {
//        graphQLFields.add(GraphQLField(name, reference.outputType(), { it.run { this@graphQL.getValue(this, null) } }, description))
//    }
//
//     fun <ChildID : Comparable<ChildID>, Child : Entity<ChildID>> Referrers<ID, T, ChildID, Child>.graphQL(
//            name: String, description: String?) = apply {
//        graphQLFields.add(GraphQLField(name, GraphQLEntityContainer.typeRef(factory), { it.run { this@graphQL.getValue(this, null) } }, description))
//    }
}

class GraphQLField<T>(val name: String, val type: GraphQLOutputType, val getter: (T) -> Any?, val description: String? = null) {
    fun graphQLField(): GraphQLFieldDefinition = graphQLFieldDefinition {
        name(name)
        description(description)
        type(type)
    }
}


