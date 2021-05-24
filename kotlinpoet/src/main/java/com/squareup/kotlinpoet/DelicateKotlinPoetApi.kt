package com.squareup.kotlinpoet

/**
 * Marks declarations in the KotlinPoet that are **delicate** &mdash;
 * they have limited use-case and shall be used with care in general code.
 * Any use of a delicate declaration has to be carefully reviewed to make sure it is
 * properly used and does not create problems like lossy Java -> Kotlin type parsing.
 * Carefully read documentation and [message] of any declaration marked as `DelicateKotlinPoetApi`.
 */
@MustBeDocumented
@Retention(value = AnnotationRetention.BINARY)
@RequiresOptIn(
  level = RequiresOptIn.Level.WARNING,
  message = "This is a delicate API and its use requires care." +
    " Make sure you fully read and understand documentation of the declaration that is marked as a delicate API."
)
public annotation class DelicateKotlinPoetApi(val message: String)
