package ca.allanwang.exposed.graphql.kotlin

import ca.allanwang.exposed.graphql.entity.GraphQLField
import graphql.Scalars
import graphql.language.EnumTypeDefinition
import graphql.schema.*
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf

internal fun graphQLType(name: String, classifier: KClassifier, annotation: GraphQLField?): GraphQLType {
    if (classifier !is KClass<*>) fail("Classifier for $name is not a KClass<*>")
    fun subClassOf(klass: KClass<*>) = classifier.isSubclassOf(klass)
    return when {
        subClassOf(String::class) -> Scalars.GraphQLString
        subClassOf(Int::class) -> Scalars.GraphQLInt
        subClassOf(BigInteger::class) -> Scalars.GraphQLBigInteger
        subClassOf(Long::class) -> Scalars.GraphQLLong
        subClassOf(Float::class) -> Scalars.GraphQLFloat
        subClassOf(BigDecimal::class) -> Scalars.GraphQLBigDecimal
        subClassOf(Iterable::class) -> GraphQLList(graphQLType(name, annotation?.itemType
                ?: fail("Please specify the itemType for $name"), null))
        else -> fail("Unknown classifier $classifier")
    }
}

//    fun inputType(property: KProperty1<T, *>): GraphQLInputType = graphQLType(property) as GraphQLInputType

internal fun outputType(property: KProperty1<*, *>, propertyAnnotation: GraphQLField? = null): GraphQLOutputType {
    val classifer = property.returnType.classifier ?: fail("Could not get classifier for ${property.name}")
    val type = graphQLType(property.name, classifer, propertyAnnotation)
    return (if (property.returnType.isMarkedNullable) type else GraphQLNonNull(type)) as GraphQLOutputType
}




/**
 * Get the column typing
 * Note that objects are wrapped with a reference.
 * [GraphQLOutputType] are already registered when the wiring is registered
 * todo Make sure that any [GraphQLInputType] is also registered in the process
 */
internal tailrec fun graphQLType(column: Column<*>): GraphQLType {
    column.referee?.apply {
        return GraphQLTypeReference(this.table.tableName)
    }
    val type = column.columnType
    return when (type) {
        is IntegerColumnType -> Scalars.GraphQLInt
        is LongColumnType -> Scalars.GraphQLLong
        is DecimalColumnType -> Scalars.GraphQLFloat
        is StringColumnType -> Scalars.GraphQLString
        is EntityIDColumnType<*> -> return graphQLType(type.idColumn)
        is EnumerationColumnType<*> -> scalarType(type.klass)
        else -> throw RuntimeException("Unknown graphQLType ${type::class.java}: ${type.sqlType()}")
    }
}

internal fun inputType(column: Column<*>): GraphQLInputType = graphQLType(column) as GraphQLInputType

internal fun outputType(column: Column<*>): GraphQLOutputType {
    val type = graphQLType(column)
    return (if (column.columnType.nullable) type else GraphQLNonNull(type)) as GraphQLOutputType
}

internal fun scalarType(klass: Class<out Enum<*>>): GraphQLEnumType {
    val name = klass.simpleName
    println("Enum $name")
    return GraphQLEnumType.Builder()
            .name(name)
            .definition(EnumTypeDefinition(name))
//                            , klass.enumConstants.map(Any::toString).map { EnumValueDefinition(it) }, emptyList()))
            .build()
}