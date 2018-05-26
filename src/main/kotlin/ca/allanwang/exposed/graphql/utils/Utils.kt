package ca.allanwang.exposed.graphql.utils

import graphql.Scalars
import graphql.language.EnumTypeDefinition
import graphql.schema.*
import org.jetbrains.exposed.sql.*

/**
 * Get the column typing
 * Note that objects are wrapped with a reference.
 * [GraphQLOutputType] are already registered when the wiring is registered
 * todo Make sure that any [GraphQLInputType] is also registered in the process
 */
private tailrec fun Column<*>.graphQLType(): GraphQLType {
    referee?.apply {
        return GraphQLTypeReference(this.table.tableName)
    }
    val type = columnType
    return when (type) {
        is IntegerColumnType -> Scalars.GraphQLInt
        is LongColumnType -> Scalars.GraphQLLong
        is DecimalColumnType -> Scalars.GraphQLFloat
        is StringColumnType -> Scalars.GraphQLString
        is EntityIDColumnType<*> -> return type.idColumn.graphQLType()
        is EnumerationColumnType<*> -> scalarType(type.klass)
        else -> throw RuntimeException("Unknown type ${type::class.java}: ${type.sqlType()}")
    }
}

internal fun Column<*>.inputType(): GraphQLInputType = graphQLType() as GraphQLInputType

internal fun Column<*>.outputType(): GraphQLOutputType =
        with(graphQLType()) { if (columnType.nullable) this else GraphQLNonNull(this) } as GraphQLOutputType

internal fun scalarType(klass: Class<out Enum<*>>): GraphQLEnumType {
    val name = klass.simpleName
    println("Enum $name")
    return GraphQLEnumType.Builder()
            .name(name)
            .definition(EnumTypeDefinition(name))
//                            , klass.enumConstants.map(Any::toString).map { EnumValueDefinition(it) }, emptyList()))
            .build()
}

class ExposedGraphQLException(message: String) : RuntimeException(message)

internal fun fail(message: String): Nothing = throw ExposedGraphQLException(message)
