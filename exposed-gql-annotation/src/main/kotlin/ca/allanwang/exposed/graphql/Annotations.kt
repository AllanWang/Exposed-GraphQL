package ca.allanwang.exposed.graphql

/**
 * Created by Allan Wang on 2018-09-09.
 */

/**
 * Class annotation for an exposed entity that will be used to generate graphql objects
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class GraphQLEntity

/**
 * Used to configure any of the entity's values
 * For the best performance, properties that are expensive should be implemented through delegation
 * so that it isn't called unless the value is requested
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
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
         * Optional description
         */
        val description: String = "")

/**
 * Skips this field when generating graphql object entry
 */
@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class GraphQLFieldIgnore