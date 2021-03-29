package com.squareup.kotlinpoet.interop.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.interop.metadata.KotlinPoetMetadataPreview

/** Modifiers that are annotations in Kotlin but modifier keywords in bytecode. */
@KotlinPoetMetadataPreview
public enum class JvmMethodModifier : JvmModifier {
  STATIC {
    override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(
      JvmStatic::class.asClassName()
    ).build()
  },
  SYNCHRONIZED {
    override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(
      Synchronized::class.asClassName()
    ).build()
  }
}
