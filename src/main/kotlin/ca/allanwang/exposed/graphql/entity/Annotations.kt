package ca.allanwang.exposed.graphql.entity

import kotlin.reflect.KClass

/**
 * Used to indicate that an entity should have all fields added to the graphql object
 * Fields that are not annotated will be added with defaults
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class GraphQLAllFields

/**
 * Used to configure any of the entity's values
 * For the best performance, properties that are expensive should be implemented through delegation
 * so that it isn't called unless the value is requested
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
//@Repeatable
annotation class GraphQLField(
        /**
         * Field name
         * Defaults to the property name
         */
        val name: String = "",

        /**
         * Tag identifier to allow for different graphql entities based around the same exposed entity
         */
        val tag: String = "",

        /**
         * Item graphQLType class
         * For the most part, we will be able to get the graphQLType from the property
         * The main use case for this is when a list/sized iterable is returned
         * In that case, the single item class must be supplied
         *
         * If the return graphQLType is not a list, this attribute will be ignored
         */
        val itemType: KClass<*> = Unit::class,

        /**
         * Optional description
         */
        val description: String = "")

/**
 * Skips this field when generating graphql object entry
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class GraphQLFieldIgnore