package com.squareup.kotlinpoet.metadata.classinspectors

/**
 * Simple `Optional` implementation for use in collections that don't allow `null` values.
 *
 * TODO: Make this an inline class when inline classes are stable.
 */
internal data class Optional<out T : Any>(val nullableValue: T?)

internal fun <T : Any> T?.toOptional(): Optional<T> = Optional(
  this
)
