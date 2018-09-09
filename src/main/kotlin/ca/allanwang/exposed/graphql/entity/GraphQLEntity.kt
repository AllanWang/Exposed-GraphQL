package ca.allanwang.exposed.graphql.entity

import ca.allanwang.exposed.graphql.kotlin.fail
import ca.allanwang.exposed.graphql.kotlin.graphQLFieldDefinition
import ca.allanwang.exposed.graphql.kotlin.graphQLObjectType
import ca.allanwang.exposed.graphql.kotlin.outputType
import graphql.schema.DataFetchingEnvironment
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLList
import graphql.schema.GraphQLOutputType
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

@Suppress("UNCHECKED_CAST")
open class GraphQLEntity<ID : Comparable<ID>, T : Entity<ID>>(val name: String,
                                                              val entityClass: EntityClass<ID, T>,
                                                              val returnsList: Boolean,
                                                              val allFieldsByDefault: Boolean = false) {

    val table = entityClass.table

    val fields: List<ExposedField> by lazy {
        if (!entityClass::class.isCompanion) fail("Entity class is not a companion object")
        // enclosing class also referenced in the original entity class, so it should work here
        val singleEntityClass = (entityClass::class.java.enclosingClass as Class<T>).kotlin
        val members = singleEntityClass.declaredMemberProperties
        members.mapNotNull { entityField(it) }
    }

    open val conditions: List<ExposedCondition> = emptyList()

    open val extensions: List<ExposedExtension> = if (returnsList) listOf(ExposedLimitArg()) else emptyList()

    fun fetch(env: ExposedEntityEnvironment): Any? {
        if (env.selections.isEmpty()) return null
        return transaction {
            val entity = getEntities(env)
            if (returnsList) entity.map { it.toOutput(env) }
            else entity.firstOrNull()?.toOutput(env)
        }
    }

    private fun getEntities(env: ExposedEntityEnvironment): SizedIterable<T> {
        val condition = env.condition(this)
        val query = (if (condition != null) table.select(condition) else table.selectAll())
        env.extensions(this)
        return entityClass.wrapRows(ExposedExtension.fold(query, env.extensions(this)))
    }

    private fun T.toOutput(env: ExposedEntityEnvironment): Map<String, Any?> =
            fields.filter { it.name in env.selections }.map {
                it.name to it.getter(this)
            }.toMap()

    fun field(container: GraphQLEntityContainer): GraphQLFieldDefinition = graphQLFieldDefinition {
        name(name)
        argument(conditions.map { it.graphQLArgument() })
        type(type(container))
        dataFetcher {
            val fieldEnv = toDbEnvironment(it) ?: return@dataFetcher null
            fetch(fieldEnv)
        }
    }

    /**
     * Attempts to fetch the field environment for the current wiring
     * from the provided data environment
     */
    fun toDbEnvironment(env: DataFetchingEnvironment): ExposedEntityEnvironment? {
        val field = env.fields.firstOrNull { it.name == name } ?: return null
        return ExposedEntityEnvironment(env.getContext(), field)
    }

    fun type(container: GraphQLEntityContainer): GraphQLOutputType =
            container.type(this).run { if (returnsList) GraphQLList(this) else this }

    internal open fun objectTypeFactory() = graphQLObjectType {
        name(name)
        description("SQL access to $name")
        fields(fields.map { it.graphQLField() })
    }

    private fun entityField(property: KProperty1<T, *>): ExposedField? {
        if (property.findAnnotation<GraphQLFieldIgnore>() != null) return null
        val annotation = property.findAnnotation<GraphQLField>()
        if (annotation == null && !allFieldsByDefault) return null
        return entityField(property, annotation)
    }

    protected open fun entityField(property: KProperty1<T, *>, annotation: GraphQLField?): ExposedField? {
        val name = annotation?.name.takeIf { !it.isNullOrEmpty() } ?: property.name
        val getter = property::get
        val type = outputType(property, annotation)
        return ExposedField(name, getter, type, annotation?.description?.takeIf { it.isNotEmpty() }
                ?: "Gets $name from ${table.tableName}")
    }

    private fun StringBuilder.append(tag: String, data: List<Any?>) {
        if (data.isEmpty()) append("\tNo $tag\n")
        else append("\t$tag\n\t\t${data.joinToString("\n\t\t")}\n")
    }

    fun toDataString() = StringBuilder().apply {
        append("Entity $name\n")
        append("Conditions", conditions)
        append("Extensions", extensions)
        append("Fields", fields)
    }.toString()

    inner class ExposedField(val name: String,
                             val getter: (T) -> Any?,
                             val type: GraphQLOutputType,
                             val description: String?) {

        override fun toString(): String = "Field ($name) $type: $description"

        fun graphQLField(): GraphQLFieldDefinition = graphQLFieldDefinition {
            name(name)
            type(type)
            description(description)
        }
    }

}