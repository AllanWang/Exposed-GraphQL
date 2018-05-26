package ca.allanwang.exposed.graphql.entity

import graphql.language.*
import graphql.schema.DataFetchingEnvironment
import org.jetbrains.exposed.sql.Op

/**
 * A direct extraction of attributes from [DataFetchingEnvironment]
 */
data class ExposedEntityEnvironment(
        /**
         * The context value retrieved from [DataFetchingEnvironment.getContext]
         * without any casting
         */
        val context: Any?,
        /**
         * All the query information related to the current field
         * selection set fields can be used to further propagate the environment
         */
        val field: Field
) {

    val selections by lazy {
        field.selectionSet.selections.mapNotNull { (it as? Field)?.name }.toSet()
    }

    val argMap: Map<String, String?> by lazy {
        field.arguments.map { it.name to it.value.extractString() }.toMap()
    }

    fun condition(entity: GraphQLEntity<*, *>): Op<Boolean>? {
        val data = entity.conditions.filter { it.name in argMap }.map {
            it to argMap[it.name]
        }.toMap()
        return ExposedCondition.fold(null, data)
    }

    fun extensions(entity: GraphQLEntity<*, *>): Map<ExposedExtension, String?> =
            entity.extensions.filter { it.name in argMap }.map { it to argMap[it.name] }.toMap()

    private fun Value.extractString(): String? = when (this) {
        is ArrayValue -> values.joinToString()
        is BooleanValue -> isValue.toString()
        is EnumValue -> name
        is FloatValue -> value.toString()
        is IntValue -> value.toString()
        is NullValue -> null
        is ObjectValue -> null // todo see if we want to support it
        is StringValue -> value
        is VariableReference -> name
        else -> {
            System.err.println("Unknown value type $this")
            null
        }
    }
}