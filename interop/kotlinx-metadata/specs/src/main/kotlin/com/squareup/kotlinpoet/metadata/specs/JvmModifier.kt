package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

/**
 * Represents a JVM modifier that is represented as an annotation in Kotlin but as a modifier in
 * bytecode. Examples include annotations such as [@JvmStatic][JvmStatic] or
 * [@JvmSynthetic][JvmSynthetic].
 *
 * This API is considered read-only and should not be implemented outside of KotlinPoet.
 */
@KotlinPoetMetadataPreview
public interface JvmModifier {
  public fun annotationSpec(): AnnotationSpec
}
