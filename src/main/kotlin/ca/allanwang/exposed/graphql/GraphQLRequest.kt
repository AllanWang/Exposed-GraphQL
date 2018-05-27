package ca.allanwang.exposed.graphql

import graphql.language.*
import graphql.schema.DataFetchingEnvironment

class GraphQLRequest(
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

//    fun condition(wiring: FieldDbWiring<*, *>): Op<Boolean>? {
//        val data = wiring.conditions.filter { it.name in argMap }.map {
//            it to argMap[it.name]
//        }.toMap()
//        return GraphDbConditionArg.fold(data)
//    }
//
//    fun extensions(wiring: FieldDbWiring<*, *>): Map<GraphDbExtensionArg, String?> =
//            wiring.extensions.filter { it.name in argMap }.map { it to argMap[it.name] }.toMap()

    fun subRequest(name: String): GraphQLRequest? {
        val subField = field.selectionSet.selections.find { (it as? Field)?.name == name } as Field? ?: return null
        return GraphQLRequest(context, subField)
    }

    private fun Value.extractString(): String? = when (this) {
        is ArrayValue -> values.toString()
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