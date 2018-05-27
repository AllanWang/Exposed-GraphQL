package ca.allanwang.exposed.graphql.utils

import graphql.Scalars
import graphql.schema.*
import org.jetbrains.exposed.dao.EntityClass
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

internal fun graphQLType(klass: KClass<*>): GraphQLType = when {
    klass.isSubclassOf(String::class) -> Scalars.GraphQLString
    klass.isSubclassOf(Boolean::class) -> Scalars.GraphQLBoolean
    klass.isSubclassOf(Int::class) -> Scalars.GraphQLInt
    klass.isSubclassOf(Long::class) -> Scalars.GraphQLLong
    klass.isSubclassOf(Float::class) -> Scalars.GraphQLFloat
    klass.isSubclassOf(Double::class) -> Scalars.GraphQLFloat
    else -> fail("Unidentified graphql type from ${klass.java.simpleName}")
}

internal fun GraphQLType.nullable(nullable: Boolean) = if (nullable) this else GraphQLNonNull(this)

internal fun graphQLOutputType(klass: KClass<*>, nullable: Boolean = true) =
        graphQLType((klass)).run { if (nullable) this else GraphQLNonNull(this) } as GraphQLOutputType

fun typeName(entityClass: EntityClass<*, *>) = "${entityClass.table.tableName}_${entityClass.hashCode()}"

fun typeRef(entityClass: EntityClass<*, *>) = GraphQLTypeReference(typeName(entityClass))