package ca.allanwang.exposed.graphql.entity

import graphql.Scalars
import graphql.schema.GraphQLList
import graphql.schema.GraphQLNonNull
import graphql.schema.GraphQLOutputType
import graphql.schema.GraphQLType
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.getExtensionDelegate
import kotlin.reflect.full.isSubclassOf

class GraphQLEntityException(message: String) : RuntimeException(message)

@Suppress("UNCHECKED_CAST")
class GraphQLEntity<ID : Comparable<ID>, T : Entity<ID>>(entityClass: EntityClass<ID, T>) {

    val table = entityClass.table
    val fields: List<GraphQLEntityField>

    init {
        // enclosing class also referenced in the original entity class, so it should work here
        val singleEntityClass = (entityClass::class.java.enclosingClass as Class<T>).kotlin
        val classAnnotation = singleEntityClass.findAnnotation<GraphQLAllFields>()
        val members = singleEntityClass.declaredMemberProperties
        fields = members.mapNotNull { entityField(it, classAnnotation) }
    }

    private fun fail(message: String): Nothing = throw GraphQLEntityException(message)

    private fun entityField(property: KProperty1<T, *>, classAnnotation: GraphQLAllFields?): GraphQLEntityField? {
        val annotation = property.findAnnotation<GraphQLField>()
        if (annotation == null && classAnnotation == null) return null
        val name = annotation?.name.takeIf { !it.isNullOrEmpty() } ?: property.name
        val getter = property::get
        val type = outputType(property, annotation)
        return GraphQLEntityField(name, getter, type, annotation?.description?.takeIf { it.isNotEmpty() }
                ?: "Gets ${property.name} from ${table.tableName}")
    }

    private fun type(name: String, classifier: KClassifier, annotation: GraphQLField?): GraphQLType {
        if (classifier !is KClass<*>) fail("Classifier for $name is not a KClass<*>")
        fun subClassOf(klass: KClass<*>) = classifier.isSubclassOf(klass)
        return when {
            subClassOf(String::class) -> Scalars.GraphQLString
            subClassOf(Int::class) -> Scalars.GraphQLInt
            subClassOf(BigInteger::class) -> Scalars.GraphQLBigInteger
            subClassOf(Long::class) -> Scalars.GraphQLLong
            subClassOf(Float::class) -> Scalars.GraphQLFloat
            subClassOf(BigDecimal::class) -> Scalars.GraphQLBigDecimal
            subClassOf(Iterable::class) -> GraphQLList(type(name, annotation?.itemType
                    ?: fail("Please specify the itemType for $name"), null))
            else -> fail("Unknown classifier $classifier")
        }
    }

//    fun inputType(property: KProperty1<T, *>): GraphQLInputType = type(property) as GraphQLInputType

    fun outputType(property: KProperty1<T, *>, propertyAnnotation: GraphQLField? = null): GraphQLOutputType {
        val classifer = property.returnType.classifier ?: fail("Could not get classifier for ${property.name}")
        val type = type(property.name, classifer, propertyAnnotation)
        return (if (property.returnType.isMarkedNullable) type else GraphQLNonNull(type)) as GraphQLOutputType
    }

    inner class GraphQLEntityField(val name: String,
                                   val getter: (T) -> Any?,
                                   val type: GraphQLOutputType,
                                   val description: String?) {
        override fun toString(): String = "Field ($name) $type: $description"
    }

}