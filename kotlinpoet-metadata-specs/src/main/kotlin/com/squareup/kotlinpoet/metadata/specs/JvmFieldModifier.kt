package com.squareup.kotlinpoet.metadata.specs

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.asClassName

/** Modifiers that are annotations in Kotlin but modifier keywords in bytecode. */
enum class JvmFieldModifier : JvmModifier {
  STATIC {
    override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(
        JvmStatic::class.asClassName()).build()
  },
  TRANSIENT {
    override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(
        Transient::class.asClassName()).build()
  },
  VOLATILE {
    override fun annotationSpec(): AnnotationSpec = AnnotationSpec.builder(
        Volatile::class.asClassName()).build()
  };
}
