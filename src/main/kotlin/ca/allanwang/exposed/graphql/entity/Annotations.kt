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
 * For the best performance, properties should be implemented through delegation
 * such that it isn't called unless the value is requested
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class GraphQLField(
        /**
         * Field name
         * Defaults to the property name
         */
        val name: String = "",

        /**
         * Item type class
         * For the most part, we will be able to get the type from the property
         * The main use case for this is when a list/sized iterable is returned
         * In that case, the single item class must be supplied
         *
         * If the return type is not a list, this attribute will be ignored
         */
        val itemType: KClass<*> = Unit::class)