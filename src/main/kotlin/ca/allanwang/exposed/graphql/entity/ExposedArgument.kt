package ca.allanwang.exposed.graphql.entity

import ca.allanwang.exposed.graphql.kotlin.graphQLArgument
import ca.allanwang.exposed.graphql.kotlin.inputType
import ca.allanwang.exposed.graphql.kotlin.toCamel
import graphql.Scalars
import graphql.schema.GraphQLArgument
import graphql.schema.GraphQLInputType
import org.jetbrains.exposed.sql.*

open class ExposedArgument(val name: String, val type: GraphQLInputType, val description: String? = null) {

    fun graphQLArgument(): GraphQLArgument = graphQLArgument {
        name(name)
        type(type)
        description(description)
    }

    override fun toString(): String = "Arg ($name) $type: $description"

}

class ExposedCondition(name: String,
                       type: GraphQLInputType,
                       val where: (arg: String) -> Op<Boolean>,
                       default: Any? = null,
                       description: String? = null) : ExposedArgument(name, type, description) {

    constructor(name: String,
                column: Column<*>,
                default: Any? = null,
                description: String? = null) : this(name,
            inputType(column),
            { EqOp(column, QueryParameter(it, column.columnType)) },
            default,
            description)

    constructor(column: Column<*>) : this(column.name.toCamel(), column)

    private val default = default?.toString()

    fun call(arg: Any?): Op<Boolean>? {
        val input = arg?.toString() ?: default ?: return null
        return where(input)
    }

    companion object {

        /**
         * Converts a map of argument data to a single op boolean, or null if none are supplied
         */
        fun fold(initial: Op<Boolean>?, data: Map<ExposedCondition, Any?>) = data.entries.fold(initial) { acc, (arg, value) ->
            val op = arg.call(value)
            when {
                op == null -> acc
                acc == null -> op
                else -> acc and op
            }
        }
    }
}

open class ExposedExtension(name: String,
                            type: GraphQLInputType,
                            val extend: Query.(arg: String) -> Query,
                            default: Any? = null,
                            description: String? = null) : ExposedArgument(name, type, description) {
    private val default = default?.toString()

    fun call(query: Query, arg: Any?): Query {
        val input = arg?.toString() ?: default ?: return query
        return query.extend(input)
    }

    companion object {
        fun fold(initial: Query, data: Map<ExposedExtension, Any?>) = data.entries.fold(initial) { acc, (arg, value) ->
            arg.call(acc, value)
        }
    }
}

class ExposedLimitArg(default: Int? = null) : ExposedExtension("limit",
        Scalars.GraphQLInt,
        { limit(it.toInt()) },
        default,
        "Upper bound for number of items to retrieve")
