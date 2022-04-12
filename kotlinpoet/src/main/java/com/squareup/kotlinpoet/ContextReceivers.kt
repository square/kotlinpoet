package com.squareup.kotlinpoet

/**
 * Annotation marking APIs for the [KEEP-259](https://github.com/Kotlin/KEEP/blob/master/proposals/context-receivers.md)
 * prototype in Kotlin 1.6.20
 */
@RequiresOptIn
@MustBeDocumented
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
public annotation class ContextReceivers
