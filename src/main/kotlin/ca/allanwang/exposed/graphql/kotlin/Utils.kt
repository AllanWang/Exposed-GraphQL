package ca.allanwang.exposed.graphql.kotlin

class GraphQLEntityException(message: String) : RuntimeException(message)

internal fun fail(message: String): Nothing = throw GraphQLEntityException(message)
