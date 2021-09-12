package com.squareup.kotlinpoet.ksp

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * Indicates that a given API is part of the experimental KotlinPoet KSP support. This exists
 * because KotlinPoet support of KSP is in preview and subject to change.
 */
@RequiresOptIn
@Retention(AnnotationRetention.BINARY)
@Target(CLASS, FUNCTION, PROPERTY)
public annotation class KotlinPoetKspPreview
